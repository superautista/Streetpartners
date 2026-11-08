#!/bin/bash
set -e # Para tudo se der erro

BUILD_TOOLS="$ANDROID_HOME/build-tools/33.0.2"
PLATFORM="$ANDROID_HOME/platforms/android-33"

echo "Criando pastas..."
mkdir -p gen obj

echo "1. Compilando Recursos (AAPT2)..."
$BUILD_TOOLS/aapt2 compile --dir res -o compiled.zip
$BUILD_TOOLS/aapt2 link -o base.apk -I $PLATFORM/android.jar --manifest AndroidManifest.xml -R compiled.zip --java gen/
echo "Verificando se o R.java foi criado:"
ls -R gen/

echo "2. Compilando Java (JAVAC)..."
# O comando abaixo procura o R.java em qualquer lugar que ele tenha sido criado
javac -cp $PLATFORM/android.jar -d obj $(find gen -name "R.java") src/com/angelo/streetpartners/MainActivity.java
echo "Verificando se o MainActivity.class foi criado:"
ls -R obj/

echo "3. Convertendo DEX (D8)..."
$BUILD_TOOLS/d8 --lib $PLATFORM/android.jar --output . $(find obj -name "*.class")

echo "4. Empacotando..."
zip -uj base.apk classes.dex

echo "5. Alinhando..."
$BUILD_TOOLS/zipalign -p -f 4 base.apk aligned.apk

echo "6. Criando chave de assinatura..."
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias -storepass 123456 -keypass 123456 -dname "CN=Angelo, OU=StreetPartners, O=Dev, L=BR, S=SP, C=BR" > /dev/null 2>&1

echo "7. Assinando o APK..."
$BUILD_TOOLS/apksigner sign --ks my-release-key.jks --ks-pass pass:123456 --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out StreetPartners.apk aligned.apk

echo "✅ SUCESSO! O APK está pronto."
