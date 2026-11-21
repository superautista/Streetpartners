#!/bin/bash
set -e

BUILD_TOOLS="$ANDROID_HOME/build-tools/33.0.2"
PLATFORM="$ANDROID_HOME/platforms/android-33"

echo "Limpando pastas antigas..."
rm -rf gen obj bin compiled.zip base.apk aligned.apk StreetPartners.apk
mkdir -p gen obj

echo "1. Compilando Recursos (AAPT2)..."
$BUILD_TOOLS/aapt2 compile --dir res -o compiled.zip
$BUILD_TOOLS/aapt2 link -o base.apk -I $PLATFORM/android.jar --manifest AndroidManifest.xml compiled.zip --java gen/

echo "2. Compilando Java (JAVAC)..."
find gen -name "R.java" > sources.txt
echo "src/com/angelo/streetpartners/MainActivity.java" >> sources.txt
javac -cp $PLATFORM/android.jar -d obj @sources.txt

echo "3. Convertendo DEX (D8)..."
find obj -name "*.class" > classes.txt
$BUILD_TOOLS/d8 --lib $PLATFORM/android.jar --output . @classes.txt

echo "4. Empacotando..."
zip -uj base.apk classes.dex

echo "5. Alinhando..."
$BUILD_TOOLS/zipalign -p -f 4 base.apk aligned.apk

echo "6. Assinando..."
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias -storepass 123456 -keypass 123456 -dname "CN=Angelo, OU=StreetPartners, O=Dev, L=BR, S=SP, C=BR" > /dev/null 2>&1
$BUILD_TOOLS/apksigner sign --ks my-release-key.jks --ks-pass pass:123456 --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out StreetPartners.apk aligned.apk

echo "✅ SUCESSO! O APK está pronto."
