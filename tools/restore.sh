#!/bin/sh
set -eu
SERIAL=${SERIAL:-$(adb get-serialno)}
export SERIAL
adb -s "$SERIAL" shell 'su -c "/data/adb/modules/zygisk_vector/cli modules disable dev.lain.classicui"'
python3 tools/grid.py --restore
for overlay in classic_settings classic_shade classic_probe; do
    adb -s "$SERIAL" shell "su -c 'cmd overlay disable com.android.shell:$overlay'" || true
done
adb -s "$SERIAL" shell 'am force-stop com.android.settings; su -c "killall com.android.systemui"'
