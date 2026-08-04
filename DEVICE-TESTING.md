# Device testing commands

Replace only explicitly marked `<PLACEHOLDER>` values. This file is append-only:
the next agent who derives a new device command adds it here in the same task
where it was needed.

## 1. Connection and device state

| Command | Question answered |
| --- | --- |
| `adb devices -l` | Is a physical device connected, authorized, and which serial should be selected? |
| `adb -s <DEVICE_SERIAL> get-state` | Is the selected device online? |
| `adb -s <DEVICE_SERIAL> shell dumpsys window | rg 'mDreamingLockscreen|isStatusBarKeyguard'` | Is Android reporting a lock-screen/keyguard state? |

## 2. Fresh installation and launch

| Command | Question answered |
| --- | --- |
| `adb -s <DEVICE_SERIAL> uninstall com.d1onix.dishlab` | Has all app data from an earlier run been removed? |
| `export JAVA_HOME=/home/denis/work/IDE/android-studio/jbr && ./gradlew :androidApp:installDebug` | Can the current debug APK be installed on the selected device? |
| `adb -s <DEVICE_SERIAL> shell am force-stop com.d1onix.dishlab` | Has the running process been stopped before a controlled relaunch? |
| `adb -s <DEVICE_SERIAL> shell am start -W -n com.d1onix.dishlab/.MainActivity` | Does the intended activity launch, and what is its launch state/time? |
| `adb -s <DEVICE_SERIAL> shell sleep 2` | Has the launched UI had a fixed two seconds to render before inspection? |

## 3. Inspect the UI without guessing

| Command | Question answered |
| --- | --- |
| `adb -s <DEVICE_SERIAL> shell uiautomator dump /sdcard/dishlab-ui.xml` | What accessibility hierarchy is currently rendered on the device? |
| `adb -s <DEVICE_SERIAL> pull /sdcard/dishlab-ui.xml /tmp/dishlab-ui.xml` | How is the hierarchy made available for local inspection? |
| `sed 's/></>\n</g' /tmp/dishlab-ui.xml | rg 'text="Look up"|content-desc="Back"'` | Which exact UI node, bounds, visible text, or content description identifies the target? |
| `sed 's/></>\n</g' /tmp/dishlab-ui.xml | rg 'text="<TEXT>"|content-desc="<DESCRIPTION>"'` | How can a different target be found using explicitly supplied text or content description? |

## 4. Input and animation control

| Command | Question answered |
| --- | --- |
| `adb -s <DEVICE_SERIAL> shell input keyevent 4` | What happens after the system Back key/gesture? |
| `adb -s <DEVICE_SERIAL> shell input tap <X> <Y>` | What happens when tapping coordinates obtained from the UI hierarchy bounds? |
| `adb -s <DEVICE_SERIAL> shell settings put global window_animation_scale 0` | Disable window animations so navigation timing does not mask the result. |
| `adb -s <DEVICE_SERIAL> shell settings put global transition_animation_scale 0` | Disable transition animations for the test run. |
| `adb -s <DEVICE_SERIAL> shell settings put global animator_duration_scale 0` | Disable animator-duration scaling for the test run. |

## 5. Diagnostics

| Command | Question answered |
| --- | --- |
| `adb -s <DEVICE_SERIAL> logcat -c` | Clear old messages before reproducing the issue. |
| `adb -s <DEVICE_SERIAL> shell pidof com.d1onix.dishlab` | Is the app process still alive after the interaction? |
| `adb -s <DEVICE_SERIAL> logcat -d --pid="$(adb -s <DEVICE_SERIAL> shell pidof com.d1onix.dishlab)" -v time` | What did the app process log during the reproduction? |
| `adb -s <DEVICE_SERIAL> logcat -d -v time | rg -i 'com.d1onix.dishlab|FATAL EXCEPTION|ANR|AndroidRuntime'` | Is there an app exception, Android runtime failure, or ANR evidence? |
| `adb -s <DEVICE_SERIAL> shell dumpsys activity activities | rg 'topResumedActivity|mResumedActivity|mLastPausedActivity|com\\.d1onix\\.dishlab/.MainActivity'` | Is the activity resumed, paused, or gone after the interaction? |
| `adb -s <DEVICE_SERIAL> exec-out screencap -p > /tmp/dishlab-screen.png` | What was visibly rendered at the moment of diagnosis? |

## 6. Cleanup

| Command | Question answered |
| --- | --- |
| `adb -s <DEVICE_SERIAL> uninstall com.d1onix.dishlab` | Has the test application and its local state been removed after the run? |

## 7. Emulator availability

| Command | Question answered |
| --- | --- |
| `/home/denis/Android/Sdk/emulator/emulator -list-avds` | Which configured Android Virtual Devices are available for an emulator reproduction? |

## t-45 evidence

| Command | Question answered |
| --- | --- |
| `adb shell input keyevent 4` | Does system Back on fresh `ScanRoute` exit the activity? |
| `adb shell input tap 116 203` | Does the screen's UI-hierarchy node `content-desc="Back"` trigger the hang on this 1080×2400 device? |
| `adb shell input tap 523 1460` | Does the `Look up` control remain interactive after the screen Back arrow was tapped? |
