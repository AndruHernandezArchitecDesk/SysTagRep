#!/bin/bash
cd "$(dirname "$0")"
java -Xmx1024m -Xms128m -jar target/SysTagRep-2.0-SNAPSHOT.jar "$@"
