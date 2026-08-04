# Device testing commands

Replace only explicitly marked `<PLACEHOLDER>` values. This file is append-only:
the next agent who derives a new device command adds it here in the same task
where it was needed.

## 0. Worktree preparation

Before an Android build in a new worktree, copy the machine-local Firebase
configuration but never stage it or change its existing `.gitignore` rule.

| Command | Question answered |
| --- | --- |
| `cp /home/denis/work/Projects/DishLab/androidApp/google-services.json /tmp/dishlab-<ROLE>/androidApp/` | Does this worktree have the untracked configuration required by the `googleServices` plugin, so an Android build tests code rather than fail on missing local configuration? |

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

## t-44 device verification

| Command | Question answered |
| --- | --- |
| `adb -s 37705998 shell am force-stop com.d1onix.dishlab` | Has the installed t-44 APK been stopped before a root-Scanner launch? |
| `adb -s 37705998 shell am start -W -n com.d1onix.dishlab/.MainActivity` | Does the t-44 APK launch into its root activity? |
| `adb -s 37705998 shell sleep 2` | Has the Scanner UI had time to render before its hierarchy is inspected? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t44-root.xml` | Does the root Scanner hierarchy contain a `content-desc="Back"` node? |
| `adb -s 37705998 pull /sdcard/t44-root.xml /tmp/t44-root.xml` | How can the root-Scanner hierarchy be inspected locally? |
| `adb -s 37705998 shell input keyevent 4` | Does system Back from root Scanner exit to the launcher? |
| `adb -s 37705998 shell sleep 1` | Has Android settled after system Back? |
| `adb -s 37705998 shell dumpsys activity activities | rg 'topResumedActivity|mResumedActivity'` | Is the launcher, rather than DishLab, top-resumed after system Back? |
| `adb -s 37705998 shell am start -W -n com.d1onix.dishlab/.MainActivity` | Can DishLab be returned to after the root system-Back exit? |
| `adb -s 37705998 shell input tap 540 2275` | Does `Enter barcode manually` still react after returning to the app? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t44-returned-interactive.xml` | Does the returned Scanner render the manual-entry controls? |
| `adb -s 37705998 pull /sdcard/t44-returned-interactive.xml /tmp/t44-returned-interactive.xml` | How can the post-return interactive hierarchy be inspected locally? |
| `adb -s 37705998 shell am force-stop com.d1onix.dishlab` | Has the final t-44 APK been stopped before its final root-Scanner verification? |
| `adb -s 37705998 shell am start -W -n com.d1onix.dishlab/.MainActivity` | Does the final t-44 APK cold-launch its root Scanner? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t44-final-root.xml` | Does the final root Scanner hierarchy omit `content-desc="Back"`? |
| `adb -s 37705998 pull /sdcard/t44-final-root.xml /tmp/t44-final-root.xml` | How can the final root hierarchy be inspected locally? |
| `adb -s 37705998 shell input keyevent 4` | Does system Back from the final root Scanner still exit to the launcher? |
| `adb -s 37705998 shell dumpsys activity activities | rg 'topResumedActivity|mResumedActivity'` | Is the launcher top-resumed after final-root system Back? |
| `adb -s 37705998 shell input tap 540 2275` | Does the final returned Scanner still accept the manual-entry action? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t44-final-returned.xml` | Does the final returned Scanner show interactive manual-entry controls? |
| `adb -s 37705998 pull /sdcard/t44-final-returned.xml /tmp/t44-final-returned.xml` | How can final post-return interactivity be inspected locally? |

## t-46 evidence

These are the literal `adb` commands used to establish
`DEVICE-ENTRY-POINT-MAP.md`; repeated hierarchy dumps are intentionally kept
as separate rows so a later run can reproduce each observed transition.

| Command | Question answered |
| --- | --- |
| `adb devices -l` | Is the physical device required for the entry-point map connected? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-current.xml` | What Scanner/manual-entry UI is currently rendered before continuing the run? |
| `adb -s 37705998 pull /sdcard/t46-current.xml /tmp/t46-current.xml` | How can that current hierarchy be inspected locally? |
| `adb -s 37705998 shell input keyevent 4` | Does Back dismiss the manual-entry keyboard before pressing its action? |
| `adb -s 37705998 shell sleep 1` | Has the UI settled after the Back input? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-manual-ready.xml` | Which bounds identify the enabled `Look up barcode` action? |
| `adb -s 37705998 pull /sdcard/t46-manual-ready.xml /tmp/t46-manual-ready.xml` | How can the manual-entry action be inspected locally? |
| `adb -s 37705998 shell input tap 540 2105` | Does the manually entered unknown barcode lead to a route with a Home entry? |
| `adb -s 37705998 shell sleep 5` | Has barcode lookup had time to return its result UI? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-lookup-result.xml` | Does the unknown-barcode result expose a Home entry? |
| `adb -s 37705998 pull /sdcard/t46-lookup-result.xml /tmp/t46-lookup-result.xml` | How can the unknown-barcode result be inspected locally? |
| `adb -s 37705998 shell input tap 540 2270` | Does `back to home` navigate to Home? |
| `adb -s 37705998 shell sleep 2` | Has Home had time to render? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-home.xml` | What selectable controls does Home expose? |
| `adb -s 37705998 pull /sdcard/t46-home.xml /tmp/t46-home.xml` | How can Home controls and their bounds be inspected locally? |
| `adb -s 37705998 shell input tap 280 1285` | What destination does Home's `Compare` tile actually open? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-compare.xml` | Which screen followed the Compare-tile tap? |
| `adb -s 37705998 pull /sdcard/t46-compare.xml /tmp/t46-compare.xml` | How can the Compare-tile destination be inspected locally? |
| `adb -s 37705998 shell input keyevent 4` | Does system Back return from the wrongly reached account screen to Home? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-home-return.xml` | Is Home restored before testing its Recipes tile? |
| `adb -s 37705998 pull /sdcard/t46-home-return.xml /tmp/t46-home-return.xml` | How can the restored Home hierarchy be inspected locally? |
| `adb -s 37705998 shell input tap 780 1285` | What destination does Home's `Recipes` tile actually open? |
| `adb -s 37705998 shell sleep 3` | Has the Recipes-tile destination had time to render? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-recipes.xml` | Which screen followed the Recipes-tile tap? |
| `adb -s 37705998 pull /sdcard/t46-recipes.xml /tmp/t46-recipes.xml` | How can the Recipes-tile destination be inspected locally? |
| `adb -s 37705998 shell input tap 965 205` | What destination does Home's `?` control actually open? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-help.xml` | Which screen followed the question-mark tap? |
| `adb -s 37705998 pull /sdcard/t46-help.xml /tmp/t46-help.xml` | How can the question-mark destination be inspected locally? |
| `adb -s 37705998 shell input tap 300 970` | Does Home's `Scan a product` tile return to Scanner? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-scan-from-home.xml` | Which exact Scanner element starts manual barcode entry? |
| `adb -s 37705998 pull /sdcard/t46-scan-from-home.xml /tmp/t46-scan-from-home.xml` | How can the Scanner entry element be inspected locally? |
| `adb -s 37705998 shell input tap 540 2275` | Does `Enter barcode manually` open the barcode field? |
| `adb -s 37705998 shell input tap 540 1930` | Does the barcode field receive focus for a known code? |
| `adb -s 37705998 shell input text 3017620422003` | Can the known barcode be entered without guessing a product-screen route? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-known-barcode-ready.xml` | Does the manual form contain the known barcode and `Look up barcode` action? |
| `adb -s 37705998 pull /sdcard/t46-known-barcode-ready.xml /tmp/t46-known-barcode-ready.xml` | How can the known-barcode form be inspected locally? |
| `adb -s 37705998 shell sleep 8` | Has the known-barcode lookup had time to resolve? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-known-barcode-result.xml` | Is lookup still resolving after eight seconds? |
| `adb -s 37705998 pull /sdcard/t46-known-barcode-result.xml /tmp/t46-known-barcode-result.xml` | How can the intermediate lookup state be inspected locally? |
| `adb -s 37705998 shell sleep 15` | Has the device-fetched product result had sufficient time to render? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-known-barcode-final.xml` | Does product result expose `Add to combination graph`? |
| `adb -s 37705998 pull /sdcard/t46-known-barcode-final.xml /tmp/t46-known-barcode-final.xml` | How can the product-result entry element be inspected locally? |
| `adb -s 37705998 shell input tap 540 2125` | Does `Add to combination graph` open the Combination graph screen? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-graph.xml` | Which exact graph controls open recipes, Saved, and Profile? |
| `adb -s 37705998 pull /sdcard/t46-graph.xml /tmp/t46-graph.xml` | How can graph entry controls be inspected locally? |
| `adb -s 37705998 shell input tap 350 2265` | Does `Find recipes (1)` open the recipes screen? |
| `adb -s 37705998 shell sleep 4` | Has the recipes screen had time to render its filters? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-recipe-results.xml` | Which recipe filters and cooking entries are currently rendered? |
| `adb -s 37705998 pull /sdcard/t46-recipe-results.xml /tmp/t46-recipe-results.xml` | How can the recipe-result hierarchy be inspected locally? |
| `adb -s 37705998 shell input tap 777 2270` | Does graph's `Saved recipes` icon open Saved? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-saved.xml` | Which screen followed the Saved-recipes icon? |
| `adb -s 37705998 pull /sdcard/t46-saved.xml /tmp/t46-saved.xml` | How can the Saved hierarchy be inspected locally? |
| `adb -s 37705998 shell input tap 960 2270` | Does graph's bottom-right `AK` control open Profile? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-profile.xml` | Does the `AK` control render Profile and any History entry? |
| `adb -s 37705998 pull /sdcard/t46-profile.xml /tmp/t46-profile.xml` | How can the Profile hierarchy be inspected locally? |
| `adb -s 37705998 shell input swipe 540 1900 540 500 400` | Does Profile reveal a lower History entry after an upward swipe? |
| `adb -s 37705998 shell uiautomator dump /sdcard/t46-profile-lower.xml` | Is a History entry rendered after inspecting the lower Profile area? |
| `adb -s 37705998 pull /sdcard/t46-profile-lower.xml /tmp/t46-profile-lower.xml` | How can the lower Profile hierarchy be inspected locally? |
