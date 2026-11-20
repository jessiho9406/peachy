#!/usr/bin/env bash
# Chụp màn hình thật của Peachy (bản debug + dữ liệu mẫu) cho 6 ngôn ngữ.
set -e
PKG=com.cindy.peachy.debug
CLS=com.cindy.ngaykyniem
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile hide
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
mkdir -p shots
for L in vi en ko ja th zh-TW; do
  adb shell cmd locale set-app-locales $PKG --locales $L
  for S in widgets main edit; do
    adb shell am force-stop $PKG
    adb shell am start -W -n $PKG/$CLS.DemoActivity --es screen $S
    sleep 5
    adb exec-out screencap -p > shots/${L}_${S}.png
  done
done
ls -la shots
