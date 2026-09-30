#!/bin/sh
set -eu
TOOLS=${TOOLS:-/tmp/opencode/classic-tools}
JDK=${JDK:-$TOOLS/jdk-17.0.20.1+1}
BT=$TOOLS/android-15
mkdir -p build/app-classes build/app-dex
"$JDK/bin/javac" --release 8 -cp "$TOOLS/android-36/android.jar:$TOOLS/xposed-api.jar" -d build/app-classes app/src/dev/lain/classicui/*.java
"$BT/d8" --lib "$TOOLS/android-36/android.jar" --classpath "$TOOLS/xposed-api.jar" --output build/app-dex build/app-classes/dev/lain/classicui/*.class
"$BT/aapt2" compile --dir app/res -o build/resources.zip
"$BT/aapt2" link -I "$TOOLS/android-36/android.jar" --manifest app/AndroidManifest.xml -A app/assets -o build/classic-unsigned.apk build/resources.zip
python3 -c 'import zipfile; z=zipfile.ZipFile("build/classic-unsigned.apk","a"); z.write("build/app-dex/classes.dex","classes.dex"); z.close()'
if [ ! -f build/debug.keystore ]; then
    "$JDK/bin/keytool" -genkeypair -keystore build/debug.keystore -storepass android -keypass android -alias classic -dname CN=NothingClassicUI -keyalg RSA -validity 3650
fi
"$BT/zipalign" -f 4 build/classic-unsigned.apk build/classic-aligned.apk
JAVA_HOME="$JDK" "$BT/apksigner" sign --ks build/debug.keystore --ks-pass pass:android --out build/nothing-classic-ui.apk build/classic-aligned.apk
"$BT/apksigner" verify build/nothing-classic-ui.apk
