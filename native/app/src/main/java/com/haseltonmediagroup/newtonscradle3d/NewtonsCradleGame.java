package com.haseltonmediagroup.newtonscradle3d;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;

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

    private static final int N = 5;
    private static final float L = 3.15f;
    private static final float R = 0.56f;
    private static final float PIVOT_Y = 3.2f;
    private static final float SPACING = 1.12f;
    private static final float G = 9.81f;
    private final float[] theta = new float[N];
    private final float[] omega = new float[N];
    private int grabbed = -1;
    private float grabStartX;
    private float grabStartTheta;
    private float autoTimer = 0f;

    public NewtonsCradleGame(PlatformBridge bridge) { this.bridge = bridge; }

    @Override public void create() {
        batch = new ModelBatch();
        camera = new PerspectiveCamera(42f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 2.3f, 11.8f);
        camera.lookAt(0f, 1.2f, 0f);
        camera.near = 0.1f; camera.far = 100f; camera.update();

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.32f, 0.34f, 0.38f, 1f));
        env.add(new DirectionalLight().set(1f, 0.96f, 0.9f, -0.55f, -1f, -0.35f));
        env.add(new DirectionalLight().set(0.35f, 0.45f, 0.65f, 0.45f, -0.4f, 0.5f));

        ModelBuilder mb = new ModelBuilder();
        Material chrome = new Material(
                ColorAttribute.createDiffuse(new Color(0.62f, 0.66f, 0.72f, 1f)),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(96f));
        Material darkMetal = new Material(
                ColorAttribute.createDiffuse(new Color(0.055f,0.06f,0.075f,1f)),
                ColorAttribute.createSpecular(new Color(0.5f,0.52f,0.56f,1f)),
                FloatAttribute.createShininess(48f));
        Material cord = new Material(ColorAttribute.createDiffuse(new Color(0.18f,0.18f,0.2f,1f)));
        Material floorMat = new Material(ColorAttribute.createDiffuse(new Color(0.08f,0.055f,0.035f,1f)));

        sphereModel = mb.createSphere(R*2, R*2, R*2, 48, 48, chrome, VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        beamModel = mb.createBox(1f,1f,1f, darkMetal, VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        stringModel = mb.createCylinder(0.035f,1f,0.035f,12,cord,VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        floorModel = mb.createBox(14f,0.3f,8f,floorMat,VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);

        floor = new ModelInstance(floorModel); floor.transform.setToTranslation(0f,-0.72f,0f);
        makeFrame();
        for(int i=0;i<N;i++) { balls.add(new ModelInstance(sphereModel)); strings.add(new ModelInstance(stringModel)); }
        Gdx.input.setInputProcessor(this);
        reset();
    }

    private void makeFrame() {
        frame.clear();
        addBeam(0,-0.45f,0,7.6f,0.42f,3.0f);
        addBeam(-3.55f,1.45f,0,0.34f,4.4f,0.34f);
        addBeam(3.55f,1.45f,0,0.34f,4.4f,0.34f);
        addBeam(0,3.58f,0,7.35f,0.28f,0.30f);
        addBeam(0,-0.05f,-1.15f,7.1f,0.22f,0.22f);
        addBeam(0,-0.05f,1.15f,7.1f,0.22f,0.22f);
    }

    private void addBeam(float x,float y,float z,float sx,float sy,float sz){
        ModelInstance m=new ModelInstance(beamModel);
        m.transform.setToTranslation(x,y,z).scale(sx,sy,sz);
        frame.add(m);
    }

    private float baseX(int i){ return (i-(N-1)/2f)*SPACING; }

    private void reset(){
        for(int i=0;i<N;i++){theta[i]=0;omega[i]=0;}
        grabbed=-1; autoTimer=0;
        updateTransforms();
    }

    @Override public void render() {
        float dt=Math.min(Gdx.graphics.getDeltaTime(),1f/30f);
        autoTimer += dt;
        if(grabbed<0 && autoTimer>4.5f && allQuiet()) { theta[0]=-0.72f; omega[0]=0; autoTimer=0; }
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

    private boolean allQuiet(){ for(float w:omega) if(Math.abs(w)>0.06f) return false; return true; }

    private void stepPhysics(float dt){
        int sub=4; float h=dt/sub;
        for(int k=0;k<sub;k++){
            for(int i=0;i<N;i++) if(i!=grabbed){
                float a=-(G/L)*(float)Math.sin(theta[i]) - 0.0055f*omega[i];
                omega[i]+=a*h; theta[i]+=omega[i]*h;
            }
            for(int i=0;i<N-1;i++){
                float xi=baseX(i)+L*(float)Math.sin(theta[i]);
                float xj=baseX(i+1)+L*(float)Math.sin(theta[i+1]);
                float gap=xj-xi;
                float vi=L*omega[i]*(float)Math.cos(theta[i]);
                float vj=L*omega[i+1]*(float)Math.cos(theta[i+1]);
                if(gap<=2f*R+0.018f && vi>vj+0.012f){
                    float wi=omega[i], wj=omega[i+1];
                    omega[i]=wj*0.996f; omega[i+1]=wi*0.996f;
                    float strength=Math.min(1f,Math.abs(vi-vj)/4.5f);
                    if(bridge!=null && strength>0.08f) bridge.impact(strength);
                }
            }
        }
    }

    private void updateTransforms(){
        for(int i=0;i<N;i++){
            float bx=baseX(i); float x=bx+L*(float)Math.sin(theta[i]); float y=PIVOT_Y-L*(float)Math.cos(theta[i]);
            balls.get(i).transform.setToTranslation(x,y,0f);
            Vector3 a=new Vector3(bx,PIVOT_Y,0f), b=new Vector3(x,y,0f);
            setCylinderBetween(strings.get(i),a,b);
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
        grabStartX=x; grabStartTheta=theta[grabbed]; omega[grabbed]=0; autoTimer=0; return true;
    }
    @Override public boolean touchDragged(int x,int y,int pointer){
        if(grabbed<0)return false;
        float dx=(x-grabStartX)/(float)Math.max(1,Gdx.graphics.getWidth());
        float target=grabStartTheta+dx*2.7f;
        if(grabbed==0) target=Math.min(0.2f,target); else target=Math.max(-0.2f,target);
        theta[grabbed]=MathUtils.clamp(target,-1.15f,1.15f); omega[grabbed]=0; return true;
    }
    @Override public boolean touchUp(int x,int y,int pointer,int button){ grabbed=-1; return true; }
    @Override public boolean keyDown(int key){ if(key==Input.Keys.R) reset(); return false; }
    @Override public boolean keyUp(int key){return false;} @Override public boolean keyTyped(char c){return false;}
    @Override public boolean mouseMoved(int x,int y){return false;} @Override public boolean scrolled(float ax,float ay){return false;}
    @Override public boolean touchCancelled(int x,int y,int pointer,int button){grabbed=-1;return true;}

    @Override public void resize(int w,int h){ camera.viewportWidth=w;camera.viewportHeight=h;camera.update(); }
    @Override public void dispose(){ batch.dispose(); sphereModel.dispose(); beamModel.dispose(); stringModel.dispose(); floorModel.dispose(); }
}
