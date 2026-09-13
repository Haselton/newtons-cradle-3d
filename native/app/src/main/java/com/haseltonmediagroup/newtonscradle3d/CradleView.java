package com.haseltonmediagroup.newtonscradle3d;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class CradleView extends GLSurfaceView {
    public interface ImpactListener { void onImpact(float strength); }

    private final SceneRenderer renderer;
    private int activeBall = -1;

    public CradleView(Context context, ImpactListener impactListener) {
        super(context);
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        renderer = new SceneRenderer(impactListener);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setKeepScreenOn(true);
    }

    public void resume() { onResume(); }
    public void pause() { onPause(); }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        final float nx = e.getX() / Math.max(1f, getWidth());
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activeBall = nx < .5f ? 0 : 4;
                queueEvent(() -> renderer.beginDrag(activeBall));
                return true;
            case MotionEvent.ACTION_MOVE:
                if (activeBall >= 0) {
                    final int ball = activeBall;
                    queueEvent(() -> renderer.drag(ball, nx));
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activeBall >= 0) {
                    final int ball = activeBall;
                    queueEvent(() -> renderer.endDrag(ball));
                }
                activeBall = -1;
                return true;
            default:
                return true;
        }
    }

    private static class SceneRenderer implements GLSurfaceView.Renderer {
        private final ImpactListener impactListener;
        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mv = new float[16];
        private final float[] mvp = new float[16];
        private final float[] angle = new float[5];
        private final float[] omega = new float[5];

        private Mesh sphere;
        private Mesh cube;
        private int program;
        private int aPos, aNormal, uMvp, uModel, uColor, uMetallic;
        private long lastNs;
        private long lastImpactNs;
        private int dragged = -1;

        SceneRenderer(ImpactListener impactListener) {
            this.impactListener = impactListener;
        }

        @Override
        public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig config) {
            GLES20.glClearColor(0.018f, 0.020f, 0.025f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            aPos = GLES20.glGetAttribLocation(program, "aPos");
            aNormal = GLES20.glGetAttribLocation(program, "aNormal");
            uMvp = GLES20.glGetUniformLocation(program, "uMvp");
            uModel = GLES20.glGetUniformLocation(program, "uModel");
            uColor = GLES20.glGetUniformLocation(program, "uColor");
            uMetallic = GLES20.glGetUniformLocation(program, "uMetallic");
            sphere = makeSphere(28, 20);
            cube = makeCube();
            lastNs = System.nanoTime();
        }

        @Override
        public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float aspect = width / (float)Math.max(1, height);
            Matrix.perspectiveM(projection, 0, 38f, aspect, .1f, 30f);
            Matrix.setLookAtM(view, 0, 0f, .15f, 8.7f, 0f, -.3f, 0f, 0f, 1f, 0f);
        }

        @Override
        public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            updatePhysics();
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(program);

            drawBox(0f, -2.64f, .45f, 5.8f, .08f, 4.0f, .055f, .047f, .040f, .05f);
            drawBox(0f, -2.28f, 0f, 4.15f, .46f, 1.68f, .24f, .105f, .045f, .12f);
            drawBox(0f, -2.03f, -.03f, 4.0f, .10f, 1.55f, .48f, .23f, .09f, .08f);

            float railX = 2.18f;
            drawBox(-railX, -.10f, -.58f, .11f, 4.35f, .11f, .68f, .70f, .72f, .85f);
            drawBox( railX, -.10f, -.58f, .11f, 4.35f, .11f, .68f, .70f, .72f, .85f);
            drawBox(0f, 2.08f, -.58f, 4.45f, .11f, .11f, .68f, .70f, .72f, .85f);
            drawBox(-railX, -.10f, .58f, .11f, 4.35f, .11f, .86f, .88f, .90f, 1.0f);
            drawBox( railX, -.10f, .58f, .11f, 4.35f, .11f, .86f, .88f, .90f, 1.0f);
            drawBox(0f, 2.08f, .58f, 4.45f, .11f, .11f, .86f, .88f, .90f, 1.0f);

            float anchorY = 1.94f;
            float ropeLen = 2.18f;
            float spacing = .74f;
            float startX = -spacing * 2f;
            float radius = .365f;

            for (int i = 0; i < 5; i++) {
                float ax = startX + i * spacing;
                float bx = ax + (float)Math.sin(angle[i]) * ropeLen;
                float by = anchorY - (float)Math.cos(angle[i]) * ropeLen;
                drawLine(ax - .10f, anchorY, .48f, bx - .10f, by + .08f, .18f);
                drawLine(ax + .10f, anchorY, -.48f, bx + .10f, by + .08f, -.18f);
            }

            for (int i = 0; i < 5; i++) {
                float ax = startX + i * spacing;
                float bx = ax + (float)Math.sin(angle[i]) * ropeLen;
                float by = anchorY - (float)Math.cos(angle[i]) * ropeLen;
                drawSphere(bx, by, 0f, radius, .72f, .75f, .79f, 1.0f);
            }

            drawBox(0f, -2.17f, .86f, 1.38f, .21f, .03f, .68f, .46f, .18f, .50f);
        }

        void beginDrag(int ball) {
            dragged = ball;
            omega[ball] = 0f;
        }

        void drag(int ball, float nx) {
            dragged = ball;
            if (ball == 0) {
                float amount = Math.max(0f, Math.min(1f, (.52f - nx) / .46f));
                angle[0] = -amount * 1.06f;
            } else {
                float amount = Math.max(0f, Math.min(1f, (nx - .48f) / .46f));
                angle[4] = amount * 1.06f;
            }
            omega[ball] = 0f;
        }

        void endDrag(int ball) {
            dragged = -1;
        }

        private void updatePhysics() {
            long now = System.nanoTime();
            float dt = Math.min(.030f, Math.max(.001f, (now - lastNs) / 1_000_000_000f));
            lastNs = now;
            int steps = 6;
            float h = dt / steps;
            for (int s = 0; s < steps; s++) {
                stepPendulum(0, h);
                stepPendulum(4, h);
                angle[1] = angle[2] = angle[3] = 0f;
                omega[1] = omega[2] = omega[3] = 0f;

                if (dragged != 0 && angle[0] >= -0.0035f && omega[0] > .03f) {
                    float incoming = omega[0];
                    angle[0] = 0f;
                    omega[0] = 0f;
                    angle[4] = 0f;
                    omega[4] = incoming * .985f;
                    impact(Math.min(1f, Math.abs(incoming) / 1.9f));
                }
                if (dragged != 4 && angle[4] <= .0035f && omega[4] < -.03f) {
                    float incoming = omega[4];
                    angle[4] = 0f;
                    omega[4] = 0f;
                    angle[0] = 0f;
                    omega[0] = incoming * .985f;
                    impact(Math.min(1f, Math.abs(incoming) / 1.9f));
                }
            }
        }

        private void stepPendulum(int i, float dt) {
            if (dragged == i) return;
            float alpha = -(9.81f / 1.72f) * (float)Math.sin(angle[i]) - omega[i] * .020f;
            omega[i] += alpha * dt;
            angle[i] += omega[i] * dt;
            if (Math.abs(angle[i]) < .0002f && Math.abs(omega[i]) < .002f) {
                angle[i] = 0f;
                omega[i] = 0f;
            }
        }

        private void impact(float strength) {
            long now = System.nanoTime();
            if (now - lastImpactNs < 55_000_000L) return;
            lastImpactNs = now;
            if (impactListener != null) impactListener.onImpact(Math.max(.22f, strength));
        }

        private void drawSphere(float x, float y, float z, float s, float r, float g, float b, float metal) {
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, x, y, z);
            Matrix.scaleM(model, 0, s, s, s);
            drawMesh(sphere, r, g, b, metal);
        }

        private void drawBox(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float metal) {
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, x, y, z);
            Matrix.scaleM(model, 0, sx, sy, sz);
            drawMesh(cube, r, g, b, metal);
        }

        private void drawMesh(Mesh mesh, float r, float g, float b, float metal) {
            Matrix.multiplyMM(mv, 0, view, 0, model, 0);
            Matrix.multiplyMM(mvp, 0, projection, 0, mv, 0);
            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
            GLES20.glUniform4f(uColor, r, g, b, 1f);
            GLES20.glUniform1f(uMetallic, metal);
            mesh.vertices.position(0);
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 24, mesh.vertices);
            GLES20.glEnableVertexAttribArray(aPos);
            mesh.vertices.position(3);
            GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 24, mesh.vertices);
            GLES20.glEnableVertexAttribArray(aNormal);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indices);
        }

        private void drawLine(float x1, float y1, float z1, float x2, float y2, float z2) {
            float[] v = {x1,y1,z1, 0,1,0, x2,y2,z2, 0,1,0};
            FloatBuffer fb = ByteBuffer.allocateDirect(v.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            fb.put(v).position(0);
            Matrix.setIdentityM(model, 0);
            Matrix.multiplyMM(mv, 0, view, 0, model, 0);
            Matrix.multiplyMM(mvp, 0, projection, 0, mv, 0);
            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
            GLES20.glUniform4f(uColor, .64f, .67f, .71f, 1f);
            GLES20.glUniform1f(uMetallic, .8f);
            fb.position(0);
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 24, fb);
            GLES20.glEnableVertexAttribArray(aPos);
            fb.position(3);
            GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 24, fb);
            GLES20.glEnableVertexAttribArray(aNormal);
            GLES20.glLineWidth(2.0f);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);
        }

        private static Mesh makeSphere(int lon, int lat) {
            int vc = (lon + 1) * (lat + 1);
            float[] verts = new float[vc * 6];
            int p = 0;
            for (int y = 0; y <= lat; y++) {
                double v = y / (double)lat;
                double phi = Math.PI * v;
                for (int x = 0; x <= lon; x++) {
                    double u = x / (double)lon;
                    double theta = Math.PI * 2.0 * u;
                    float sx = (float)(Math.sin(phi) * Math.cos(theta));
                    float sy = (float)Math.cos(phi);
                    float sz = (float)(Math.sin(phi) * Math.sin(theta));
                    verts[p++] = sx; verts[p++] = sy; verts[p++] = sz;
                    verts[p++] = sx; verts[p++] = sy; verts[p++] = sz;
                }
            }
            short[] idx = new short[lon * lat * 6];
            int q = 0;
            for (int y = 0; y < lat; y++) {
                for (int x = 0; x < lon; x++) {
                    short a = (short)(y * (lon + 1) + x);
                    short b = (short)(a + lon + 1);
                    short c = (short)(a + 1);
                    short d = (short)(b + 1);
                    idx[q++] = a; idx[q++] = b; idx[q++] = c;
                    idx[q++] = c; idx[q++] = b; idx[q++] = d;
                }
            }
            return new Mesh(verts, idx);
        }

        private static Mesh makeCube() {
            float[] v = {
                -0.5f,-0.5f, 0.5f, 0,0,1,   0.5f,-0.5f, 0.5f, 0,0,1,   0.5f,0.5f,0.5f,0,0,1,  -0.5f,0.5f,0.5f,0,0,1,
                -0.5f,-0.5f,-0.5f,0,0,-1, -0.5f,0.5f,-0.5f,0,0,-1,  0.5f,0.5f,-0.5f,0,0,-1, 0.5f,-0.5f,-0.5f,0,0,-1,
                -0.5f,0.5f,-0.5f,0,1,0,   -0.5f,0.5f,0.5f,0,1,0,    0.5f,0.5f,0.5f,0,1,0,   0.5f,0.5f,-0.5f,0,1,0,
                -0.5f,-0.5f,-0.5f,0,-1,0, 0.5f,-0.5f,-0.5f,0,-1,0, 0.5f,-0.5f,0.5f,0,-1,0,-0.5f,-0.5f,0.5f,0,-1,0,
                0.5f,-0.5f,-0.5f,1,0,0,   0.5f,0.5f,-0.5f,1,0,0,    0.5f,0.5f,0.5f,1,0,0,   0.5f,-0.5f,0.5f,1,0,0,
                -0.5f,-0.5f,-0.5f,-1,0,0,-0.5f,-0.5f,0.5f,-1,0,0, -0.5f,0.5f,0.5f,-1,0,0,-0.5f,0.5f,-0.5f,-1,0,0
            };
            short[] i = {
                0,1,2,0,2,3, 4,5,6,4,6,7, 8,9,10,8,10,11,
                12,13,14,12,14,15, 16,17,18,16,18,19, 20,21,22,20,22,23
            };
            return new Mesh(v, i);
        }

        private static int buildProgram(String vs, String fs) {
            int v = compile(GLES20.GL_VERTEX_SHADER, vs);
            int f = compile(GLES20.GL_FRAGMENT_SHADER, fs);
            int p = GLES20.glCreateProgram();
            GLES20.glAttachShader(p, v);
            GLES20.glAttachShader(p, f);
            GLES20.glLinkProgram(p);
            return p;
        }

        private static int compile(int type, String src) {
            int s = GLES20.glCreateShader(type);
            GLES20.glShaderSource(s, src);
            GLES20.glCompileShader(s);
            return s;
        }

        private static class Mesh {
            final FloatBuffer vertices;
            final ShortBuffer indices;
            final int indexCount;
            Mesh(float[] v, short[] i) {
                vertices = ByteBuffer.allocateDirect(v.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                vertices.put(v).position(0);
                indices = ByteBuffer.allocateDirect(i.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
                indices.put(i).position(0);
                indexCount = i.length;
            }
        }

        private static final String VERTEX_SHADER =
                "uniform mat4 uMvp; uniform mat4 uModel;" +
                "attribute vec3 aPos; attribute vec3 aNormal;" +
                "varying vec3 vNormal; varying vec3 vWorld;" +
                "void main(){ vec4 w=uModel*vec4(aPos,1.0); vWorld=w.xyz; vNormal=normalize(mat3(uModel)*aNormal); gl_Position=uMvp*vec4(aPos,1.0); }";

        private static final String FRAGMENT_SHADER =
                "precision mediump float; uniform vec4 uColor; uniform float uMetallic;" +
                "varying vec3 vNormal; varying vec3 vWorld;" +
                "void main(){" +
                "vec3 N=normalize(vNormal); vec3 L=normalize(vec3(-0.45,0.85,0.65)); vec3 V=normalize(vec3(0.0,0.0,8.5)-vWorld);" +
                "float d=max(dot(N,L),0.0); vec3 H=normalize(L+V); float spec=pow(max(dot(N,H),0.0), mix(20.0,90.0,uMetallic));" +
                "float rim=pow(1.0-max(dot(N,V),0.0),2.4);" +
                "vec3 warm=vec3(1.0,0.68,0.38)*max(dot(N,normalize(vec3(0.7,0.25,0.3))),0.0)*0.16;" +
                "vec3 c=uColor.rgb*(0.18+0.72*d)+warm+vec3(spec)*(0.3+1.1*uMetallic)+vec3(rim)*0.22*uMetallic;" +
                "gl_FragColor=vec4(c,1.0); }";
    }
}
