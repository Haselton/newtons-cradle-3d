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
    private Model sphereModel, rodModel, stringModel, floorModel, roomFloorModel, backWallModel, shadowModel;
    private Texture chromeTexture;
    private final Array<ModelInstance> balls = new Array<>();
    private final Array<ModelInstance> strings = new Array<>();
    private final Array<ModelInstance> frame = new Array<>();
    private final Array<ModelInstance> shadows = new Array<>();
    private ModelInstance floor;
    private ModelInstance roomFloor, backWall;
    private Sound impactSound;
    private long lastImpactMs;

    private static final int N = 5;
    private static final float L = 3.05f;
    private static final float R = 0.49f;
    private static final float PIVOT_Y = 3.15f;
    // The native hinge remains at PIVOT_Y.  The suspension wire is visibly
    // fastened to the rail above it, so rendering must use the rail height.
    private static final float CABLE_ANCHOR_Y = 3.43f;
    private static final float SPACING = 0.98f;
    private static final float STRING_Z = 0.48f;
    private final float[] physicsState = new float[N * 3];
    private boolean nativeReady;
    private int grabbed = -1;
    private float grabStartX;
    private float grabStartTheta;
    private long lastDragNanos;
    private float lastDragAngle;
    private float releaseAngularVelocity;

    public NewtonsCradleGame(PlatformBridge bridge) { this.bridge = bridge; }

    @Override public void create() {
        batch = new ModelBatch();
        camera = new PerspectiveCamera(42f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 1.55f, 18.6f);
        camera.lookAt(0f, 1.42f, 0f);
        camera.near = 0.1f; camera.far = 100f; camera.update();

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.18f, 0.19f, 0.21f, 1f));
        // One dominant upper-left studio key light.  The rendered ground
        // shadows below use this same direction.
        env.add(new DirectionalLight().set(1f, .97f, .90f, -0.42f, -0.83f, -0.36f));

        ModelBuilder mb = new ModelBuilder();
        chromeTexture=createChromeTexture();
        Material chrome = new Material(
                TextureAttribute.createDiffuse(chromeTexture),
                ColorAttribute.createDiffuse(new Color(.96f,.97f,1f,1f)),
                ColorAttribute.createEmissive(new Color(.035f,.04f,.052f,1f)),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(180f));
        Material frameChrome = new Material(
                ColorAttribute.createDiffuse(new Color(0.34f,0.37f,0.43f,1f)),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(180f));
        Material cord = new Material(ColorAttribute.createDiffuse(new Color(0.17f,0.18f,0.21f,1f)), ColorAttribute.createSpecular(new Color(.55f,.58f,.64f,1f)), FloatAttribute.createShininess(72f));
        Material floorMat = new Material(ColorAttribute.createDiffuse(new Color(0.006f,0.007f,0.009f,1f)), ColorAttribute.createSpecular(new Color(.08f,.09f,.11f,1f)), FloatAttribute.createShininess(32f));
        Material roomFloorMat = new Material(ColorAttribute.createDiffuse(new Color(.23f,.245f,.27f,1f)), ColorAttribute.createSpecular(new Color(.15f,.16f,.18f,1f)), FloatAttribute.createShininess(42f));
        Material wallMat = new Material(ColorAttribute.createDiffuse(new Color(.30f,.32f,.35f,1f)));
        Material shadowMat = new Material(
                ColorAttribute.createDiffuse(new Color(.01f,.012f,.016f,.42f)),
                new BlendingAttribute(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA,.42f));

        sphereModel = mb.createSphere(R*2, R*2, R*2, 64, 64, chrome, VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal|VertexAttributes.Usage.TextureCoordinates);
        rodModel = mb.createCylinder(1f,1f,1f,32, frameChrome, VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        stringModel = mb.createCylinder(0.024f,1f,0.024f,16,cord,VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        floorModel = mb.createBox(6.55f,0.34f,2.65f,floorMat,VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        roomFloorModel = mb.createBox(14f,.10f,10f,roomFloorMat,VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        backWallModel = mb.createBox(14f,9f,.12f,wallMat,VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        shadowModel = mb.createSphere(1f,.035f,.62f,32,8,shadowMat,VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);

        floor = new ModelInstance(floorModel); floor.transform.setToTranslation(0f,-0.76f,0f);
        roomFloor = new ModelInstance(roomFloorModel); roomFloor.transform.setToTranslation(0f,-1.01f,-1.4f);
        backWall = new ModelInstance(backWallModel); backWall.transform.setToTranslation(0f,3.38f,-4.25f);
        makeFrame();
        for(int i=0;i<N;i++) {
            balls.add(new ModelInstance(sphereModel));
            strings.add(new ModelInstance(stringModel));
            strings.add(new ModelInstance(stringModel));
            shadows.add(new ModelInstance(shadowModel));
        }
        try { impactSound = Gdx.audio.newSound(Gdx.files.internal("newton_clack.wav")); }
        catch (RuntimeException ignored) { impactSound = null; }
        nativeReady = NewtonPhysics.nativeCreate() != 0;
        if (!nativeReady) throw new GdxRuntimeException("Newton Dynamics failed to initialize");
        Gdx.input.setInputProcessor(this);
        reset();
    }

    private Texture createChromeTexture(){
        Pixmap p=new Pixmap(512,256,Pixmap.Format.RGBA8888);
        Color c=new Color();
        for(int y=0;y<256;y++) for(int x=0;x<512;x++){
            float v=y/255f, u=x/511f;
            // A wrapped studio panorama: bright ceiling, narrow horizon and a
            // dark room/floor.  On a sphere these bend like real reflections.
            float value=0.16f + 0.60f*(1f-v);
            value += 0.31f*(float)Math.exp(-Math.pow((v-.48f)/.055f,2));
            value -= 0.14f*(float)Math.exp(-Math.pow((v-.69f)/.15f,2));
            float panels=(u<.20f || (u>.40f&&u<.57f) || u>.82f)?1f:0f;
            float panelY=MathUtils.clamp(1f-Math.abs(v-.29f)/.25f,0f,1f);
            value += panels*panelY*.30f;
            float seam=Math.abs((u*8f)%1f-.5f);
            if(seam>.475f && v<.58f) value-=.23f;
            // Small softboxes keep the highlights asymmetric and photographic.
            float dx=(u-.315f)/.052f, dy=(v-.235f)/.105f;
            value += MathUtils.clamp(1f-dx*dx-dy*dy,0f,1f)*.70f;
            dx=(u-.665f)/.072f; dy=(v-.33f)/.075f;
            value += MathUtils.clamp(1f-dx*dx-dy*dy,0f,1f)*.50f;
            value=MathUtils.clamp(value,0.045f,1f);
            float cool=MathUtils.clamp((.55f-v)*.10f,0f,.05f);
            c.set(value*.94f-cool*.25f,value*.965f,value+cool,1f);
            p.drawPixel(x,y,Color.rgba8888(c));
        }
        Texture t=new Texture(p); p.dispose();
        t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
        return t;
    }

    private void makeFrame() {
        frame.clear();
        addRod(new Vector3(-2.82f,-0.56f,-.92f),new Vector3(-2.82f,3.43f,-.92f),.088f);
        addRod(new Vector3(-2.82f,-0.56f, .92f),new Vector3(-2.82f,3.43f, .92f),.088f);
        addRod(new Vector3( 2.82f,-0.56f,-.92f),new Vector3( 2.82f,3.43f,-.92f),.088f);
        addRod(new Vector3( 2.82f,-0.56f, .92f),new Vector3( 2.82f,3.43f, .92f),.088f);
        addRod(new Vector3(-2.82f,3.43f,-.92f),new Vector3(2.82f,3.43f,-.92f),.088f);
        addRod(new Vector3(-2.82f,3.43f, .92f),new Vector3(2.82f,3.43f, .92f),.088f);
    }

    private void addRod(Vector3 a, Vector3 b, float diameter){
        ModelInstance m=new ModelInstance(rodModel);
        setCylinderBetween(m,a,b);
        m.transform.scale(diameter,1f,diameter);
        frame.add(m);
    }

    private float baseX(int i){ return (i-(N-1)/2f)*SPACING; }

    private void reset(){
        if(nativeReady) NewtonPhysics.nativeReset();
        grabbed=-1;
        for(int i=0;i<N;i++) {
            physicsState[i*3]=baseX(i);
            physicsState[i*3+1]=PIVOT_Y-L;
            physicsState[i*3+2]=0f;
        }
        updateTransforms();
    }

    @Override public void render() {
        float dt=Math.min(Gdx.graphics.getDeltaTime(),1f/30f);
        stepPhysics(dt);
        updateTransforms();

        Gdx.gl.glViewport(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.20f,0.21f,0.23f,1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera);
        batch.render(backWall,env);
        batch.render(roomFloor,env);
        batch.render(floor,env);
        for(ModelInstance s:shadows) batch.render(s);
        for(ModelInstance m:frame) batch.render(m,env);
        for(ModelInstance s:strings) batch.render(s,env);
        for(ModelInstance b:balls) batch.render(b,env);
        batch.end();
    }

    private void stepPhysics(float dt){
        float impulse=NewtonPhysics.nativeStep(dt,physicsState);
        float strength=MathUtils.clamp(impulse/2.2f,0f,1f);
        long now=TimeUtils.millis();
        if(strength>0.055f && now-lastImpactMs>=95L) {
            lastImpactMs=now;
            float volume=0.16f+strength*0.72f;
            if(impactSound!=null) {
                long id=impactSound.play(volume);
                impactSound.setPitch(id,0.99f+MathUtils.random()*0.02f);
            }
            if(bridge!=null) bridge.impact(strength);
        }
    }

    private void updateTransforms(){
        for(int i=0;i<N;i++){
            float bx=baseX(i); float x=physicsState[i*3]; float y=physicsState[i*3+1]; float z=physicsState[i*3+2];
            balls.get(i).transform.setToTranslation(x,y,z);
            // Project the ball toward the lower-right from the same upper-left
            // key light.  Higher balls cast longer, softer shadows.
            float height=Math.max(0f,y+.58f);
            float stretch=1f+height*.24f;
            float fade=MathUtils.clamp(.48f-height*.055f,.22f,.48f);
            ModelInstance shadow=shadows.get(i);
            shadow.transform.idt().translate(x+height*.18f,-.565f,z+height*.15f).scale(stretch,1f,1f+height*.13f);
            ColorAttribute sc=shadow.materials.get(0).get(ColorAttribute.class,ColorAttribute.Diffuse);
            if(sc!=null) sc.color.a=fade;
            BlendingAttribute blend=shadow.materials.get(0).get(BlendingAttribute.class,BlendingAttribute.Type);
            if(blend!=null) blend.opacity=fade;
            Vector3 aFront=new Vector3(bx,CABLE_ANCHOR_Y,-STRING_Z), bFront=new Vector3(x,y,-R*.43f);
            Vector3 aBack=new Vector3(bx,CABLE_ANCHOR_Y,STRING_Z), bBack=new Vector3(x,y,R*.43f);
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
        Vector3 left=camera.project(new Vector3(physicsState[0],physicsState[1],physicsState[2]));
        Vector3 right=camera.project(new Vector3(physicsState[(N-1)*3],physicsState[(N-1)*3+1],physicsState[(N-1)*3+2]));
        float sy=Gdx.graphics.getHeight()-y;
        float dl=Vector2.dst(x,sy,left.x,left.y), dr=Vector2.dst(x,sy,right.x,right.y);
        float grabRadius=Gdx.graphics.getWidth()*.22f;
        if(Math.min(dl,dr)>grabRadius) return false;
        grabbed=dl<dr?0:N-1;
        grabStartX=x;
        grabStartTheta=(float)Math.asin(MathUtils.clamp((physicsState[grabbed*3]-baseX(grabbed))/L,-1f,1f));
        lastDragAngle=grabStartTheta; releaseAngularVelocity=0f; lastDragNanos=TimeUtils.nanoTime(); return true;
    }
    @Override public boolean touchDragged(int x,int y,int pointer){
        if(grabbed<0)return false;
        float dx=(x-grabStartX)/(float)Math.max(1,Gdx.graphics.getWidth());
        float target=grabStartTheta+dx*2.7f;
        if(grabbed==0) target=Math.min(0.2f,target); else target=Math.max(-0.2f,target);
        target=MathUtils.clamp(target,-0.98f,0.98f);
        long now=TimeUtils.nanoTime(); float elapsed=Math.max((now-lastDragNanos)/1_000_000_000f,0.008f);
        releaseAngularVelocity=MathUtils.clamp((target-lastDragAngle)/elapsed,-2.2f,2.2f);
        lastDragAngle=target; lastDragNanos=now;
        NewtonPhysics.nativeSetAngle(grabbed,target); return true;
    }
    @Override public boolean touchUp(int x,int y,int pointer,int button){ if(grabbed>=0) { if(TimeUtils.nanoTime()-lastDragNanos>80_000_000L) releaseAngularVelocity=0f; NewtonPhysics.nativeRelease(grabbed,releaseAngularVelocity); } grabbed=-1; return true; }
    @Override public boolean keyDown(int key){ if(key==Input.Keys.R) reset(); return false; }
    @Override public boolean keyUp(int key){return false;} @Override public boolean keyTyped(char c){return false;}
    @Override public boolean mouseMoved(int x,int y){return false;} @Override public boolean scrolled(float ax,float ay){return false;}
    @Override public boolean touchCancelled(int x,int y,int pointer,int button){if(grabbed>=0) NewtonPhysics.nativeRelease(grabbed,0f);grabbed=-1;return true;}

    @Override public void resize(int w,int h){ camera.viewportWidth=w;camera.viewportHeight=h;camera.update(); }
    @Override public void dispose(){ if(nativeReady) NewtonPhysics.nativeDestroy(); batch.dispose(); sphereModel.dispose(); rodModel.dispose(); stringModel.dispose(); floorModel.dispose(); roomFloorModel.dispose(); backWallModel.dispose(); shadowModel.dispose(); if(chromeTexture!=null) chromeTexture.dispose(); if(impactSound!=null) impactSound.dispose(); }
}
