package com.haseltonmediagroup.newtonscradle3d;

final class NewtonPhysics {
    static { System.loadLibrary("cradle_physics"); }
    static native int nativeCreate();
    static native float nativeStep(float dt, float[] state);
    static native void nativeSetAngle(int index, float angle);
    static native void nativeRelease(int index, float angularVelocity);
    static native void nativeReset();
    static native void nativeDestroy();
    private NewtonPhysics() { }
}
