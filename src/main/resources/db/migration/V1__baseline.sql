-- =============================================================
-- V1__baseline.sql — Esquema inicial consolidado (LINEAMIENTO_MIGRACIONES_FLYWAY §3 Opción A)
-- Baseline desde pg_dump --schema-only de instalación referencia.
-- PLACEHOLDER: reemplazar contenido con dump real antes de primer despliegue prod.
-- Para instalaciones existentes se usa `flyway baseline -baselineVersion=1` manual por cliente
-- (no se ejecuta V1, se marca como aplicado). Para instalaciones nuevas, V1..Vn construyen desde cero.
-- Contenido actual: base mínima idempotente (IF NOT EXISTS) generada desde sql/*.sql artesanales
-- + ensure* de DatabaseConnection. Mantiene artesanales tal cual — no reescribir.
-- =============================================================
-- Base tablas catálogo / transaccional (IF NOT EXISTS, FKs tolerantes)
CREATE TABLE IF NOT EXISTS marca (id SERIAL PRIMARY KEY, nombre VARCHAR(100) UNIQUE NOT NULL, estado BOOLEAN DEFAULT true);
CREATE TABLE IF NOT EXISTS grupo (id SERIAL PRIMARY KEY, nombre VARCHAR(100) UNIQUE NOT NULL, estado BOOLEAN DEFAULT true);
CREATE TABLE IF NOT EXISTS codigo (id SERIAL PRIMARY KEY, nombre VARCHAR(100) UNIQUE NOT NULL, estado BOOLEAN DEFAULT true);
CREATE TABLE IF NOT EXISTS vendedor (id SERIAL PRIMARY KEY, nombre VARCHAR(150) NOT NULL, identificacion VARCHAR(13), estado BOOLEAN DEFAULT true);
CREATE TABLE IF NOT EXISTS cliente (id SERIAL PRIMARY KEY, nombre VARCHAR(100) NOT NULL, identificacion VARCHAR(13), direccion VARCHAR(100), correo VARCHAR(60), telefono VARCHAR(10), celular VARCHAR(10), fecha_registro TIMESTAMP DEFAULT now(), estado BOOLEAN DEFAULT true);
CREATE TABLE IF NOT EXISTS proveedor (id SERIAL PRIMARY KEY, nombre VARCHAR(100) NOT NULL, identificacion VARCHAR(13), direccion VARCHAR(100), correo VARCHAR(60), telefono VARCHAR(10), celular VARCHAR(10), fecha_registro TIMESTAMP DEFAULT now(), estado BOOLEAN DEFAULT true);
CREATE TABLE IF NOT EXISTS empresa (id SERIAL PRIMARY KEY, ruc VARCHAR(13), razon_social VARCHAR(300), direccion_calle_principal VARCHAR(200), direccion_calle_secundaria VARCHAR(200), telefono VARCHAR(20), celular VARCHAR(20), correo VARCHAR(100), sucursal VARCHAR(10) DEFAULT '001', agente_retencion VARCHAR(10), resolucion VARCHAR(20));
CREATE TABLE IF NOT EXISTS usuarios (id SERIAL PRIMARY KEY, username VARCHAR(50) UNIQUE, password VARCHAR(200), nombre VARCHAR(100), apellido VARCHAR(100), rol VARCHAR(50), permisos TEXT, estado BOOLEAN DEFAULT true, fecha_registro TIMESTAMP DEFAULT now());
CREATE TABLE IF NOT EXISTS inventario (id SERIAL PRIMARY KEY, codigo VARCHAR(50) NOT NULL, descripcion VARCHAR(300) NOT NULL, grupo VARCHAR(100), marca VARCHAR(100), cantidad INTEGER NOT NULL DEFAULT 0, precio_compra NUMERIC(12,2), precio_venta NUMERIC(12,2), ubicacion_percha VARCHAR(50));
CREATE TABLE IF NOT EXISTS factura_registro (id SERIAL PRIMARY KEY, empresa_id INTEGER REFERENCES empresa(id), cliente_id INTEGER REFERENCES cliente(id), fecha TIMESTAMP DEFAULT now(), codigo VARCHAR(30) UNIQUE, forma_pago VARCHAR(20), subtotal NUMERIC(12,2), iva NUMERIC(12,2), descuento NUMERIC(12,2), total NUMERIC(12,2), clave_acceso VARCHAR(49) UNIQUE, num_comprobante VARCHAR(30), ambiente_sri VARCHAR(10), estado_sri VARCHAR(20) DEFAULT 'PENDIENTE', mensaje_sri VARCHAR(500), fecha_registro TIMESTAMP DEFAULT now());
CREATE TABLE IF NOT EXISTS factura_detalle (id SERIAL PRIMARY KEY, factura_registro_id INTEGER REFERENCES factura_registro(id) ON DELETE CASCADE, inventario_id INTEGER REFERENCES inventario(id), codigo VARCHAR(50), descripcion VARCHAR(300), cantidad INTEGER NOT NULL, precio_unitario NUMERIC(12,2), precio_total NUMERIC(12,2));
CREATE TABLE IF NOT EXISTS nota_venta_registro (id SERIAL PRIMARY KEY, empresa_id INTEGER REFERENCES empresa(id), cliente_id INTEGER REFERENCES cliente(id), fecha TIMESTAMP DEFAULT now(), codigo VARCHAR(30) UNIQUE, forma_pago VARCHAR(20), estado_sri VARCHAR(20) DEFAULT 'PENDIENTE');
CREATE TABLE IF NOT EXISTS nota_venta_detalle (id SERIAL PRIMARY KEY, nota_venta_id INTEGER REFERENCES nota_venta_registro(id) ON DELETE CASCADE, inventario_id INTEGER REFERENCES inventario(id), descripcion VARCHAR(500), cantidad INTEGER, precio_unitario NUMERIC(12,2), precio_total NUMERIC(12,2));
CREATE TABLE IF NOT EXISTS secuencia_documento (tipo VARCHAR(20) PRIMARY KEY, prefijo VARCHAR(10) DEFAULT '001', establecimiento VARCHAR(3) DEFAULT '001', punto_emision VARCHAR(3) DEFAULT '001', siguiente_numero INTEGER NOT NULL DEFAULT 1);
INSERT INTO secuencia_documento(tipo, siguiente_numero) VALUES ('FACTURA',1), ('PROFORMA',1) ON CONFLICT (tipo) DO NOTHING;
CREATE TABLE IF NOT EXISTS secuenciales (tipo_comprobante VARCHAR(10) PRIMARY KEY, secuencial INTEGER NOT NULL DEFAULT 0, punto_emision VARCHAR(3) DEFAULT '001', establecimiento VARCHAR(3) DEFAULT '001');
CREATE TABLE IF NOT EXISTS logs (id SERIAL PRIMARY KEY, controlador VARCHAR(100), metodo VARCHAR(100), mensaje TEXT, stacktrace TEXT, fecha TIMESTAMP DEFAULT now());
CREATE TABLE IF NOT EXISTS comprobantes_electronicos (id SERIAL PRIMARY KEY, documento_relacionado_id INTEGER, tipo_comprobante VARCHAR(2) DEFAULT '01', clave_acceso VARCHAR(49) UNIQUE, numero_comprobante VARCHAR(30), ambiente VARCHAR(10), estado_sri VARCHAR(20) DEFAULT 'PENDIENTE', mensaje_sri VARCHAR(500), xml_generado TEXT, xml_autorizado TEXT, numero_autorizacion VARCHAR(49), fecha_autorizacion TIMESTAMP);
CREATE TABLE IF NOT EXISTS xml_enviados (id SERIAL PRIMARY KEY, clave_acceso VARCHAR(49), numero_comprobante VARCHAR(30), ambiente VARCHAR(10), tipo_comprobante VARCHAR(2), xml_enviado TEXT, respuesta_recepcion TEXT, respuesta_autorizacion TEXT, estado_sri VARCHAR(20), mensaje_sri VARCHAR(500), numero_autorizacion VARCHAR(49), fecha_autorizacion TIMESTAMP);
-- Tablas base adicionales (perchero/ubicacion) se crean en V2 si no existen, pero se dejan aquí por FKs
-- NOTA: Este V1 es placeholder idempotente. Reemplazar con pg_dump --schema-only real para fidelidad 42 tablas.
