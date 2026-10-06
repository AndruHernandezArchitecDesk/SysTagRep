#!/bin/bash
# Reconfigurar-Vendex-BD.sh — Reabre el asistente de conexion a la BD existente (produccion, sin tocar la BD).
# Uso: ./Reconfigurar-Vendex-BD.sh  (pide Servidor/IP + puerto + BD + usuario + clave, con Probar conexion).
set -e
cd "$(dirname "$0")"
JAR="Vendex-2.0-SNAPSHOT.jar"
if [ -f "target/$JAR" ]; then
  exec java --enable-native-access=ALL-UNNAMED -Xmx1024m -Dvendex.configDb=true -jar "target/$JAR"
elif [ -f "$JAR" ]; then
  exec java --enable-native-access=ALL-UNNAMED -Xmx1024m -Dvendex.configDb=true -jar "$JAR"
else
  echo "[ERROR] No se encontro $JAR junto a este script ni en target/."
  echo "Copia este script a la carpeta donde esta el JAR o ejecuta ./empaquetar_instalador.sh primero."
  exit 1
fi
