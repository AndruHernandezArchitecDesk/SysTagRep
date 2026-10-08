#!/bin/bash
# empaquetar_instalador.sh — Equivalente Ubuntu de empaquetar_instalador.bat
# Instalador PRODUCCION (sin base de datos): solo app, la BD ya existe en el servidor.
# Al primer arranque pide Servidor/IP + puerto + BD + usuario + clave con Probar conexion.
set -e
cd "$(dirname "$0")"

VERSION="2.0.0"
JAR="Vendex-2.0-SNAPSHOT.jar"
NOMBRE="Vendex"
ICON_PNG="src/main/resources/img/logoVendex.png"

echo "============================================"
echo " Vendex - Instalador PRODUCCION Ubuntu (sin base de datos)"
echo "============================================"
echo " Este paquete solo instala la app. La BD ya existe en el servidor."
echo " Al primer arranque pide Servidor/IP + puerto + BD + usuario + clave"
echo " con boton Probar conexion. No crea tablas ni roles."
echo ""

if ! command -v jpackage >/dev/null 2>&1; then
  echo "[ERROR] jpackage no encontrado."
  echo "  Instala un JDK 17+ y asegurate de que su carpeta bin este en el PATH."
  exit 1
fi

echo "[0/2] Compilando el proyecto..."
./mvnw clean package -DskipTests -q

if [ ! -f "target/$JAR" ]; then
  echo "[ERROR] No se genero target/$JAR."
  exit 1
fi

mkdir -p dist

echo "[1/2] Generando paquete .deb PRODUCCION sin BD..."
if jpackage --input target \
  --name "$NOMBRE" \
  --app-version "$VERSION" \
  --vendor "Vendex Repuestos" \
  --description "Vendex - cliente produccion. Se conecta a la BD existente del servidor (sin instalar PostgreSQL)." \
  --main-jar "$JAR" \
  --main-class com.vendex.Launcher \
  --icon "$ICON_PNG" \
  --type deb \
  --linux-menu-group "Office" \
  --linux-shortcut \
  --java-options "-Xmx1024m" \
  --java-options "-Xms128m" \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --dest dist; then
  DEB=$(ls -t dist/*.deb 2>/dev/null | head -n 1)
  echo "[OK] Instalador generado: $DEB"
  echo "     Produccion sin BD: instalalo en cada PC cliente con:"
  echo "       sudo apt install ./$DEB"
  echo "     Primer arranque: ingresa IP del servidor + Probar conexion + Guardar."
  echo "     Para cambiar de servidor luego: ./Reconfigurar-Vendex-BD.sh"
  exit 0
fi

echo "[AVISO] No se genero el .deb."
echo "[2/2] Generando app-image portable (no requiere dpkg)..."
jpackage --input target \
  --name "$NOMBRE" \
  --app-version "$VERSION" \
  --vendor "Vendex Repuestos" \
  --description "Vendex - cliente produccion. Se conecta a la BD existente del servidor (sin instalar PostgreSQL)." \
  --main-jar "$JAR" \
  --main-class com.vendex.Launcher \
  --icon "$ICON_PNG" \
  --type app-image \
  --java-options "-Xmx1024m" \
  --java-options "-Xms128m" \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --dest dist

tar -czf "dist/${NOMBRE}-portable.tar.gz" -C dist "$NOMBRE"
echo "[OK] Portable generado: dist/${NOMBRE}-portable.tar.gz"
echo "     Envia el tar.gz al cliente; descomprime y abre ${NOMBRE}/bin/${NOMBRE}"
