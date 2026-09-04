SysTagRep 1.5 - Paquete PRODUCCION multi-PC
===========================================
HOST 192.168.1.7 (PostgreSQL) + CLIENTE 192.168.1.5 (BD remota)
Numeracion 001-001 centralizada sin solapamiento (UPDATE RETURNING)
Etiqueta 6x2.5cm 480x200 @203 DPI, barcode 234x50 izq, N copias 1/N por stock
Consumidor final >50 bloqueado

CONTENIDO:
- comun/SysTagRep-1.5-SNAPSHOT.jar (44M, Java 17 + JavaFX embebido via shade)
- comun/ejecutar.sh , comun/run.bat (java -jar)
- comun/sql/fix_multipc_numeracion_20260831.sql (UNIQUE + sincroniza secuencia)
- host/ , cliente/ con plantillas db.properties

REQUISITOS:
- Windows 10/11 64-bit
- JDK 17+ (Temurin) en PATH: java -version debe dar 17
- HOST: PostgreSQL 14+ con BD dbTag
- Red LAN 192.168.1.0/24 accesible puerto 5432

INSTALACION HOST 192.168.1.7:
1) Copiar produccion/comun/SysTagRep-1.5-SNAPSHOT.jar a C:\SysTagRep\
   y copiar produccion/comun/run.bat ahi (editar si ruta cambia)
2) Si no tienes MSI: generar MSI en Windows con empaquetar_instalador.bat
   (requiere JDK 17 + WiX 3.11) -> dist\SysTagRep-1.5.0.msi e instalar en ambas PCs.
   Si usas JAR: asegurar JDK 17 instalado.
3) Configurar PostgreSQL:
   - postgresql.conf: listen_addresses='*'
   - pg_hba.conf: host dbTag app_systag 192.168.1.0/24 scram-sha-256
   - CREATE USER app_systag WITH PASSWORD 'CAMBIAR';
     GRANT CONNECT ON DATABASE dbTag TO app_systag;
     GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO app_systag;
     ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT,INSERT,UPDATE,DELETE ON TABLES TO app_systag;
   - Firewall Windows: regla entrada TCP 5432 desde 192.168.1.5
   - Reserva DHCP en router para 192.168.1.7 por MAC
4) Ejecutar SQL 1 vez en HOST:
   psql -h localhost -U postgres -d dbTag -f comun/sql/fix_multipc_numeracion_20260831.sql
   o pgAdmin -> Query Tool -> abrir y ejecutar.
   Verifica: SELECT tipo, establecimiento||'-'||punto_emision, siguiente_numero FROM secuencia_documento;
5) Copiar firma .p12 a ambas: ej C:\Systag\firma.p12 y seleccionar en app
   Firma ya viene en C:\Users\TU_USUARIO\.systag\firma.properties (rutaP12 + clave cifrada)
6) Primera ejecucion crea C:\Users\TU_USUARIO\.systag\db.properties con localhost (dejar asi)
   Licencia: activar solo en HOST con 449E9-2E789-B71DA-6D23A-20260930 para maquina emugoKABUsDQZhckLohKymLW
7) Ejecutar: run.bat o java -Xmx1024m --enable-native-access=ALL-UNNAMED -jar SysTagRep-1.5-SNAPSHOT.jar

INSTALACION CLIENTE 192.168.1.5:
1) Copiar mismo SysTagRep-1.5-SNAPSHOT.jar + run.bat a C:\SysTagRep\
   (o mismo MSI)
2) Copiar misma firma .p12 a misma ruta C:\Systag\firma.p12
   + copiar C:\Users\HOST\.systag\firma.properties y ambiente.properties a C:\Users\Cliente\.systag\
3) Editar C:\Users\Cliente\.systag\db.properties:
   db.url=jdbc:postgresql://192.168.1.7:5432/dbTag
   db.user=app_systag
   db.password=CAMBIAR
   (si no existe, ejecutar una vez y se crea, luego editar)
4) No requiere licencia (MainApp omite si es db remota)
5) Ejecutar run.bat, debe ver inventario/etiquetas/ubicaciones igual que host
6) Test numeracion: crear proforma/factura simultanea en ambas -> 001-001-000... sin duplicar
   Test etiqueta: producto 10 uds en 2 ubicaciones -> 1 icono TAG por fila -> dialogo 2 cards -> Generar N copias 1/5...5/5
   Test CF: cliente 9999999999999 con total >50 debe bloquear con advertencia

NOTAS:
- Para generar MSI en Windows: empaquetar_instalador.bat -> dist\SysTagRep-1.5.0.msi
  Ese MSI ya lleva JRE y no necesitas JDK en cliente.
- Etiquetas: 480x200 JPG en Documentos/etiquetas SYSTAG/etiqueta_ID_COD_UBI_1de5.jpg
  Imprimir en A4 al 100% da 6x2.5cm cada una (mosaico ~30 por hoja)
- Ingreso por Producto: boton Inventario "Ingreso por Producto" (descripcion, grupo, marca, codigo, cantidad, precio)
  Ingreso por Factura: boton "Ingreso por Factura" (flujo completo con proveedor/fecha)
