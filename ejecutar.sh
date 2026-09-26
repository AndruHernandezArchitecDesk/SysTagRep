#!/bin/bash
cd "$(dirname "$0")"
java --enable-native-access=ALL-UNNAMED -Xmx1024m -Xms128m -jar target/Vendex-2.0-SNAPSHOT.jar "$@"
