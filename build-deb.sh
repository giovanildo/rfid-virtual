#!/usr/bin/env bash
# Gera o pacote .deb do RFID Virtual (com Java embutido) via jpackage.
# Requisitos: JDK 21+ (com jpackage), Maven, dpkg-deb e fakeroot.
set -euo pipefail
cd "$(dirname "$0")"

VERSION=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)
JAR="rfid-virtual-${VERSION}.jar"

echo "Gerando JAR ${VERSION}..."
mvn clean package -q

echo "Gerando .deb..."
rm -rf target/jpackage-input && mkdir -p target/jpackage-input
cp "target/${JAR}" target/jpackage-input/

jpackage \
    --type deb \
    --name rfid-virtual \
    --app-version "${VERSION}" \
    --description "Leitor RFID virtual para testes do sistema de catraca" \
    --vendor "UNILAB" \
    --icon packaging/linux/rfid-virtual.png \
    --resource-dir packaging/linux \
    --input target/jpackage-input \
    --main-jar "${JAR}" \
    --main-class Launcher \
    --linux-package-name rfid-virtual \
    --linux-app-category utils \
    --linux-menu-group Utility \
    --linux-shortcut \
    --linux-deb-maintainer "giovanildos@gmail.com" \
    --dest target

echo
echo "Pacote gerado: $(ls target/*.deb)"
echo "Instalar com: sudo apt install ./$(ls target/*.deb)"
