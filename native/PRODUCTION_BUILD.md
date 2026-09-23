# Newton's Cradle 3D 1.12.1 (14)

This repairs production 1.0.0 (2), which was built from `main` before the
approved study scene and collision-sound changes were merged.

The renderer, JNI bridge, physics wrapper, study photo and 145 ms clack are
restored from `newton-engine-rebuild` / `8273633368b085ac3eb4cd43192384dae4c77ec5`.
The scene matches approved APK `NewtonsCradle3D-1.12.0-perspective-match.apk`.
The background and sound SHA-256 values are enforced against that APK by
`scripts/verify-release.py` for both final release files.

The production package, launcher icon, AdMob app/banner IDs, upload keystore,
minimum API 26 and target API 36 remain those of the live app. The study
branch's test signing key and alternate AdMob app are not used. Debug uses a
separate package and Google's sample ads. Banner space tracks its actual height.
The study implementation includes the existing consent and privacy-choice UI.

Newton Dynamics is pinned to the upstream revision current when the approved
September 13 build was made. NDK 27's flexible page size option is enabled.
CI builds a matching signed release APK/AAB, verifies artwork/audio/native
alignment, and launches the actual release APK in an Android 15 emulator,
checking interaction and pause/resume with network disabled. QA screenshots
and logs are published with each build. Artifacts are uploaded only after gates pass.

Google Play submission and acceptance must be checked separately; a successful
GitHub build is not a Play production release.
