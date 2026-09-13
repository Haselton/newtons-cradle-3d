# Newton's Cradle 3D — Android

A meditative realistic 3D Newton's cradle for Android by Haselton Media Group.

## Included
- Five individually simulated polished-steel balls
- Real rigid-body collisions with high solver accuracy and planar pendulum constraints
- Touch-drag and release interaction
- 3D camera orbit and pinch zoom
- Chrome, graphite, walnut materials and studio lighting generated at runtime
- Velocity-sensitive spatial steel impact sound (procedurally generated fallback)
- Android haptic impact feedback
- Reset, slow-motion, auto/meditation, camera reset controls
- AdMob adapter with adaptive banner + conservative interstitial cadence
- Debug APK and release AAB build menu commands

## Open
Use Unity 2022.3 LTS or newer compatible 2022 LTS editor with Android Build Support installed.

Open the project folder. The Main scene is intentionally empty: AppBootstrap builds the entire experience at runtime.

## Build a test APK
Unity menu: **Newton Cradle > Build Debug APK**
Output: `Builds/NewtonsCradle3D-debug.apk`

## Release AAB
Unity menu: **Newton Cradle > Build Release AAB**
Output: `Builds/NewtonsCradle3D.aab`

Package name: `com.haseltonmediagroup.newtonscradle3d`

## AdMob
This source intentionally uses placeholder ad IDs so no accidental live traffic is generated during development.
1. Import the current Google Mobile Ads Unity plugin.
2. Add `ADMOB_PRESENT` under Player Settings > Scripting Define Symbols.
3. Put your Android AdMob app ID in the Android manifest/plugin settings as required by the current plugin.
4. Replace `bannerId` and `interstitialId` in `AdManager.cs` with the production IDs for this app.
5. Keep test ads enabled until the Play build is ready.

No IAP is included.

## Sound quality
A generated metallic impact sound is bundled in code so the project has working audio with zero external assets. For store release, replace it with 3–6 licensed close-mic steel-sphere impact recordings and randomize them by impact strength for a more photoreal result.

## Recommended release QA
- Verify one-ball-in/one-ball-out and two-ball-in/two-ball-out behavior at multiple pull angles.
- Test 60 FPS on mid-range Android hardware.
- Confirm no UI overlaps gesture navigation or adaptive banner.
- Verify AdMob test IDs before switching to production IDs.
- Add privacy policy URL and Play Data safety declarations for ads.
