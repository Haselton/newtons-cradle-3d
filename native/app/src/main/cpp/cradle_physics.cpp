#include <jni.h>
#include <algorithm>
#include <cmath>
#include <iterator>
#include <mutex>
#include "Newton.h"
#include "dCustomHinge.h"
#include "dMatrix.h"

namespace {
constexpr int kBallCount = 5;
constexpr float kRadius = 0.49f;
constexpr float kLength = 3.05f;
constexpr float kPivotY = 3.15f;
constexpr float kSpacing = 0.98f;
constexpr float kMass = 0.52f;

NewtonWorld* gWorld = nullptr;
NewtonBody* gBalls[kBallCount]{};
dCustomHinge* gHinges[kBallCount]{};
float gPeakImpact = 0.0f;
bool gImpactLatched[kBallCount - 1]{};
std::mutex gMutex;

float baseX(int i) { return (i - (kBallCount - 1) * 0.5f) * kSpacing; }

void identity(float* m) {
    std::fill(m, m + 16, 0.0f);
    m[0] = m[5] = m[10] = m[15] = 1.0f;
}

void gravity(const NewtonBody* body, dFloat, int) {
    dFloat mass, ix, iy, iz;
    NewtonBodyGetMass(body, &mass, &ix, &iy, &iz);
    const dFloat force[4] = {0.0f, -9.81f * mass, 0.0f, 0.0f};
    NewtonBodySetForce(body, force);
}

void contacts(const NewtonJoint* joint, dFloat, int) {
    for (void* contact = NewtonContactJointGetFirstContact(joint); contact;
         contact = NewtonContactJointGetNextContact(joint, contact)) {
        NewtonMaterial* material = NewtonContactGetMaterial(contact);
        NewtonMaterialSetContactElasticity(material, 0.997f);
        NewtonMaterialSetContactFrictionCoef(material, 0.03f, 0.02f, 0);
        NewtonMaterialSetContactFrictionCoef(material, 0.03f, 0.02f, 1);
    }
}

void destroyWorld() {
    if (gWorld) {
        NewtonDestroy(gWorld);
        gWorld = nullptr;
    }
    std::fill(std::begin(gBalls), std::end(gBalls), nullptr);
    std::fill(std::begin(gHinges), std::end(gHinges), nullptr);
}

void setBallPosition(int i, float angle) {
    float matrix[16]; identity(matrix);
    matrix[12] = baseX(i) + kLength * std::sin(angle);
    matrix[13] = kPivotY - kLength * std::cos(angle);
    NewtonBodySetMatrixNoSleep(gBalls[i], matrix);
    const dFloat zero[4] = {0, 0, 0, 0};
    NewtonBodySetVelocity(gBalls[i], zero);
    NewtonBodySetOmega(gBalls[i], zero);
}
}

extern "C" JNIEXPORT jint JNICALL
Java_com_haseltonmediagroup_newtonscradle3d_NewtonPhysics_nativeCreate(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(gMutex);
    destroyWorld();
    gWorld = NewtonCreate();
    if (!gWorld) return 0;
    NewtonSetThreadsCount(gWorld, 1);
    NewtonSetSolverIterations(gWorld, 8);
    NewtonSetNumberOfSubsteps(gWorld, 4);
    NewtonSetContactMergeTolerance(gWorld, 0.0001f);
    const int material = NewtonMaterialGetDefaultGroupID(gWorld);
    NewtonMaterialSetDefaultElasticity(gWorld, material, material, 0.997f);
    NewtonMaterialSetDefaultFriction(gWorld, material, material, 0.03f, 0.02f);
    NewtonMaterialSetCollisionCallback(gWorld, material, material, nullptr, contacts);

    NewtonCollision* sphere = NewtonCreateSphere(gWorld, kRadius, 1, nullptr);
    for (int i = 0; i < kBallCount; ++i) {
        float matrix[16]; identity(matrix);
        matrix[12] = baseX(i);
        matrix[13] = kPivotY - kLength;
        gBalls[i] = NewtonCreateDynamicBody(gWorld, sphere, matrix);
        NewtonBodySetMassProperties(gBalls[i], kMass, sphere);
        NewtonBodySetLinearDamping(gBalls[i], 0.0004f);
        const dFloat angularDamping[3] = {0.0004f, 0.0004f, 0.0004f};
        NewtonBodySetAngularDamping(gBalls[i], angularDamping);
        NewtonBodySetForceAndTorqueCallback(gBalls[i], gravity);
        dMatrix hingeFrame(dGetIdentityMatrix());
        hingeFrame.m_front = dVector(0.0f, 0.0f, 1.0f, 0.0f);
        hingeFrame.m_up = dVector(0.0f, 1.0f, 0.0f, 0.0f);
        hingeFrame.m_right = hingeFrame.m_front.CrossProduct(hingeFrame.m_up);
        hingeFrame.m_posit = dVector(baseX(i), kPivotY, 0.0f, 1.0f);
        gHinges[i] = new dCustomHinge(hingeFrame, gBalls[i], nullptr);
    }
    NewtonDestroyCollision(sphere);
    gPeakImpact = 0.0f;
    std::fill(std::begin(gImpactLatched), std::end(gImpactLatched), false);
    return NewtonWorldGetVersion();
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_haseltonmediagroup_newtonscradle3d_NewtonPhysics_nativeStep(
        JNIEnv* env, jclass, jfloat dt, jfloatArray state) {
    std::lock_guard<std::mutex> lock(gMutex);
    if (!gWorld || env->GetArrayLength(state) < kBallCount * 3) return 0.0f;
    gPeakImpact = 0.0f;
    const float step = std::min(static_cast<float>(dt), 1.0f / 30.0f);
    for (int i = 0; i < kBallCount - 1; ++i) {
        dFloat m0[16], m1[16], v0[4], v1[4];
        NewtonBodyGetMatrix(gBalls[i], m0); NewtonBodyGetMatrix(gBalls[i + 1], m1);
        NewtonBodyGetVelocity(gBalls[i], v0); NewtonBodyGetVelocity(gBalls[i + 1], v1);
        const float gap = (m1[12] - m0[12]) - 2.0f * kRadius;
        const float approach = v0[0] - v1[0];
        const bool imminent = approach > 0.08f && gap <= 0.012f + approach * step * 1.35f;
        if (imminent && !gImpactLatched[i]) {
            gPeakImpact = std::max(gPeakImpact, approach * kMass * 2.4f);
            gImpactLatched[i] = true;
        } else if (approach <= 0.0f || gap > 0.045f) {
            gImpactLatched[i] = false;
        }
    }
    NewtonUpdate(gWorld, step);
    float out[kBallCount * 3];
    for (int i = 0; i < kBallCount; ++i) {
        dFloat matrix[16]; NewtonBodyGetMatrix(gBalls[i], matrix);
        out[i * 3] = matrix[12]; out[i * 3 + 1] = matrix[13]; out[i * 3 + 2] = matrix[14];
    }
    env->SetFloatArrayRegion(state, 0, kBallCount * 3, out);
    return gPeakImpact;
}

extern "C" JNIEXPORT void JNICALL
Java_com_haseltonmediagroup_newtonscradle3d_NewtonPhysics_nativeSetAngle(
        JNIEnv*, jclass, jint index, jfloat angle) {
    std::lock_guard<std::mutex> lock(gMutex);
    if (gWorld && index >= 0 && index < kBallCount) setBallPosition(index, angle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_haseltonmediagroup_newtonscradle3d_NewtonPhysics_nativeRelease(
        JNIEnv*, jclass, jint index, jfloat angularVelocity) {
    std::lock_guard<std::mutex> lock(gMutex);
    if (!gWorld || index < 0 || index >= kBallCount) return;
    dFloat matrix[16];
    NewtonBodyGetMatrix(gBalls[index], matrix);
    const float dx = matrix[12] - baseX(index);
    const float dy = matrix[13] - kPivotY;
    const dFloat velocity[4] = {
        -dy * angularVelocity, dx * angularVelocity, 0.0f, 0.0f
    };
    NewtonBodySetVelocity(gBalls[index], velocity);
}

extern "C" JNIEXPORT void JNICALL
Java_com_haseltonmediagroup_newtonscradle3d_NewtonPhysics_nativeReset(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(gMutex);
    if (!gWorld) return;
    for (int i = 0; i < kBallCount; ++i) setBallPosition(i, 0.0f);
    gPeakImpact = 0.0f;
    std::fill(std::begin(gImpactLatched), std::end(gImpactLatched), false);
}

extern "C" JNIEXPORT void JNICALL
Java_com_haseltonmediagroup_newtonscradle3d_NewtonPhysics_nativeDestroy(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(gMutex);
    destroyWorld();
}
