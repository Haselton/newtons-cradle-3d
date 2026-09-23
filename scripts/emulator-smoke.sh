#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa
adb shell svc wifi disable
adb shell svc data disable
adb install -r native/app/build/outputs/apk/release/app-release.apk
adb logcat -c
adb shell am start -W -n com.haseltonmediagroup.newtonscradle3d/.MainActivity
sleep 12
adb shell pidof com.haseltonmediagroup.newtonscradle3d
adb exec-out screencap -p > qa/study-at-rest.png
adb shell input swipe 285 1100 70 940 800
sleep 1
adb exec-out screencap -p > qa/study-in-motion.png
sleep 2
adb shell input keyevent KEYCODE_HOME
sleep 1
adb shell am start -W -n com.haseltonmediagroup.newtonscradle3d/.MainActivity
sleep 3
adb shell pidof com.haseltonmediagroup.newtonscradle3d
adb exec-out screencap -p > qa/study-after-resume.png
adb logcat -d > qa/logcat.txt
if rg 'FATAL EXCEPTION|Fatal signal|UnsatisfiedLinkError|Newton Dynamics failed|Unable to decode stream' qa/logcat.txt; then
    exit 1
fi
if cmp -s qa/study-at-rest.png qa/study-in-motion.png; then
    echo 'Interaction produced no visible movement' >&2
    exit 1
fi
printf 'Release APK starts, renders study, responds to drag, and resumes without fatal errors.\n' > qa/smoke-result.txt
