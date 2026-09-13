package com.haseltonmediagroup.newtonscradle3d;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.TimeUtils;

public class NewtonsCradleGame extends ApplicationAdapter implements InputProcessor {
    public interface PlatformBridge { void impact(float strength); }

    private final PlatformBridge bridge;
    private PerspectiveCamera camera;
    private ModelBatch batch;
    private Environment env;
    private Model sphereModel, beamModel, stringModel, floorModel;
    private final Array<ModelInstance> balls = new Array<>();
    private final Array<ModelInstance> strings = new Array<>();
    private final Array<ModelInstance> frame = new Array<>();
    private ModelInstance floor;
    private Sound impactSound;
    private long lastImpactMs;

    private static final int N = 5;
    private static final float L = 3.05f;
    private static final float R = 0.49f;
    private static final float PIVOT_Y = 3.15f;
    private static final float SPACING = 0.985f;
    private static final float STRING_Z = 0.48f;
    private final float[] physicsState = new float[N * 3];
    private boolean nativeReady;
    private int grabbed = -1;
    private float grabStartX;
    private float grabStartTheta;
    private float autoTimer = 0f;

    public NewtonsCradleGame(PlatformBridge bridge) { this.bridge = bridge; }

    @Override public void create() {
        batch = new ModelBatch();
        camera = new PerspectiveCamera(42f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 2.25f, 13.35f);
        camera.lookAt(0f, 1.15f, 0f);
        camera.near = 0.1f; camera.far = 100f; camera.update();

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.20f, 0.22f, 0.27f, 1f));
        env.add(new DirectionalLight().set(1.0f, 0.93f, 0.82f, -0.55f, -1f, -0.45f));
        env.add(new DirectionalLight().set(0.34f, 0.48f, 0.75f, 0.55f, -0.25f, 0.65f));
        env.add(new PointLight().set(0.95f, 0.98f, 1f, -2.7f, 3.8f, 4.2f, 11f));

        ModelBuilder mb = new ModelBuilder();
        Material chrome = new Material(
                ColorAttribute.createDiffuse(new Color(0.40f, 0.44f, 0.50f, 1f)),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(128f));
        Material frameChrome = new Material(
                ColorAttribute.createDiffuse(new Color(0.30f,0.33f,0.38f,1f)),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(112f));
        Material cord = new Material(ColorAttribute.createDiffuse(new Color(0.55f,0.58f,0.62f,1f)), ColorAttribute.createSpecular(Color.WHITE), FloatAttribute.createShininess(48f));
        Material floorMat = new Material(ColorAttribute.createDiffuse(new Color(0.018f,0.020f,0.024f,1f)), ColorAttribute.createSpecular(new Color(.32f,.35f,.40f,1f)), FloatAttribute.createShininess(64f));

        sphereModel = mb.createSphere(R*2, R*2, R*2, 64, 64, chrome, VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        beamModel = mb.createBox(1f,1f,1f, frameChrome, VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        stringModel = mb.createCylinder(0.024f,1f,0.024f,16,cord,VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        floorModel = mb.createBox(14f,0.3f,8f,floorMat,VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);

        floor = new ModelInstance(floorModel); floor.transform.setToTranslation(0f,-0.72f,0f);
        makeFrame();
        for(int i=0;i<N;i++) {
            balls.add(new ModelInstance(sphereModel));
            strings.add(new ModelInstance(stringModel));
            strings.add(new ModelInstance(stringModel));
        }
        try { impactSound = Gdx.audio.newSound(Gdx.files.internal("newton_impact.mp3")); }
        catch (RuntimeException ignored) { impactSound = null; }
        nativeReady = NewtonPhysics.nativeCreate() != 0;
        if (!nativeReady) throw new GdxRuntimeException("Newton Dynamics failed to initialize");
        Gdx.input.setInputProcessor(this);
        reset();
    }

    private void makeFrame() {
        frame.clear();
        addBeam(0,-0.45f,0,6.8f,0.38f,2.75f);
        addBeam(-3.10f,1.42f,-1.02f,0.25f,4.15f,0.25f);
        addBeam(-3.10f,1.42f,1.02f,0.25f,4.15f,0.25f);
        addBeam(3.10f,1.42f,-1.02f,0.25f,4.15f,0.25f);
        addBeam(3.10f,1.42f,1.02f,0.25f,4.15f,0.25f);
        addBeam(0,3.48f,-1.02f,6.45f,0.24f,0.24f);
        addBeam(0,3.48f,1.02f,6.45f,0.24f,0.24f);
        addBeam(0,-0.20f,-1.10f,6.45f,0.18f,0.18f);
        addBeam(0,-0.20f,1.10f,6.45f,0.18f,0.18f);
    }

    private void addBeam(float x,float y,float z,float sx,float sy,float sz){
        ModelInstance m=new ModelInstance(beamModel);
        m.transform.setToTranslation(x,y,z).scale(sx,sy,sz);
        frame.add(m);
    }

    private float baseX(int i){ return (i-(N-1)/2f)*SPACING; }

    private void reset(){
        if(nativeReady) NewtonPhysics.nativeReset();
        grabbed=-1; autoTimer=0;
        for(int i=0;i<N;i++) {
            physicsState[i*3]=baseX(i);
            physicsState[i*3+1]=PIVOT_Y-L;
            physicsState[i*3+2]=0f;
        }
        updateTransforms();
    }

    @Override public void render() {
        float dt=Math.min(Gdx.graphics.getDeltaTime(),1f/30f);
        autoTimer += dt;
        if(grabbed<0 && autoTimer>4.5f && allQuiet()) { NewtonPhysics.nativeSetAngle(0,-0.72f); autoTimer=0; }
        stepPhysics(dt);
        updateTransforms();

        Gdx.gl.glViewport(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.018f,0.022f,0.032f,1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera);
        batch.render(floor,env);
        for(ModelInstance m:frame) batch.render(m,env);
        for(ModelInstance s:strings) batch.render(s,env);
        for(ModelInstance b:balls) batch.render(b,env);
        batch.end();
    }

    private boolean allQuiet(){ return autoTimer > 4.5f; }

    private void stepPhysics(float dt){
        float impulse=NewtonPhysics.nativeStep(dt,physicsState);
        float strength=MathUtils.clamp(impulse/2.2f,0f,1f);
        long now=TimeUtils.millis();
        if(strength>0.055f && now-lastImpactMs>=42L) {
            lastImpactMs=now;
            float volume=0.12f+strength*0.78f;
            if(impactSound!=null) {
                long id=impactSound.play(volume);
                impactSound.setPitch(id,0.97f+MathUtils.random()*0.06f);
            }
            if(bridge!=null) bridge.impact(strength);
        }
    }

    private void updateTransforms(){
        for(int i=0;i<N;i++){
            float bx=baseX(i); float x=physicsState[i*3]; float y=physicsState[i*3+1]; float z=physicsState[i*3+2];
            balls.get(i).transform.setToTranslation(x,y,z);
            Vector3 aFront=new Vector3(bx,PIVOT_Y,-STRING_Z), bFront=new Vector3(x,y,-R*.43f);
            Vector3 aBack=new Vector3(bx,PIVOT_Y,STRING_Z), bBack=new Vector3(x,y,R*.43f);
            setCylinderBetween(strings.get(i*2),aFront,bFront);
            setCylinderBetween(strings.get(i*2+1),aBack,bBack);
        }
    }

    private void setCylinderBetween(ModelInstance inst, Vector3 a, Vector3 b){
        Vector3 mid=new Vector3(a).add(b).scl(0.5f); Vector3 dir=new Vector3(b).sub(a); float len=dir.len();
        dir.nor(); Quaternion q=new Quaternion().setFromCross(Vector3.Y,dir);
        inst.transform.idt().translate(mid).rotate(q).scale(1f,len,1f);
    }

    @Override public boolean touchDown(int x,int y,int pointer,int button){
        float nx=x/(float)Math.max(1,Gdx.graphics.getWidth());
        grabbed = nx<0.5f ? 0 : N-1;
        grabStartX=x;
        grabStartTheta=(float)Math.asin(MathUtils.clamp((physicsState[grabbed*3]-baseX(grabbed))/L,-1f,1f));
        autoTimer=0; return true;
    }
    @Override public boolean touchDragged(int x,int y,int pointer){
        if(grabbed<0)return false;
        float dx=(x-grabStartX)/(float)Math.max(1,Gdx.graphics.getWidth());
        float target=grabStartTheta+dx*2.7f;
        if(grabbed==0) target=Math.min(0.2f,target); else target=Math.max(-0.2f,target);
        NewtonPhysics.nativeSetAngle(grabbed,MathUtils.clamp(target,-1.15f,1.15f)); return true;
    }
    @Override public boolean touchUp(int x,int y,int pointer,int button){ grabbed=-1; return true; }
    @Override public boolean keyDown(int key){ if(key==Input.Keys.R) reset(); return false; }
    @Override public boolean keyUp(int key){return false;} @Override public boolean keyTyped(char c){return false;}
    @Override public boolean mouseMoved(int x,int y){return false;} @Override public boolean scrolled(float ax,float ay){return false;}
    @Override public boolean touchCancelled(int x,int y,int pointer,int button){grabbed=-1;return true;}

    @Override public void resize(int w,int h){ camera.viewportWidth=w;camera.viewportHeight=h;camera.update(); }
    @Override public void dispose(){ if(nativeReady) NewtonPhysics.nativeDestroy(); batch.dispose(); sphereModel.dispose(); beamModel.dispose(); stringModel.dispose(); floorModel.dispose(); if(impactSound!=null) impactSound.dispose(); }
}
