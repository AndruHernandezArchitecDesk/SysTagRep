-- =============================================================
-- Reset + test data: 50 automotive spare parts (repuestos)
-- Includes TAG Crédito items: new + near-expiry.
-- =============================================================
BEGIN;

TRUNCATE TABLE
  historial_producto, cuentas_por_pagar, cuentas_por_cobrar,
  comprobantes_electronicos, factura_detalle, factura_registro,
  nota_venta_detalle, nota_venta_registro, ubicacion, inventario,
  perchero, secuenciales, logs, empresa, cliente, vendedor,
  grupo, marca, proveedor, ubicacion_percha
RESTART IDENTITY CASCADE;

-- ==============================
-- LOOKUPS
-- ==============================
INSERT INTO grupo (nombre, estado) VALUES
  ('Motor', true), ('Transmisión', true), ('Frenos', true),
  ('Suspensión', true), ('Eléctrico', true), ('Filtros', true),
  ('Carrocería', true);

INSERT INTO marca (nombre, estado) VALUES
  ('Bosch', true), ('NGK', true), ('SKF', true),
  ('Continental', true), ('KYB', true), ('Valeo', true),
  ('Gates', true), ('Monroe', true), ('Brembo', true),
  ('AC Delco', true);

INSERT INTO proveedor (nombre, identificacion, direccion, correo, telefono, celular, estado) VALUES
  ('Importadora AutoPartes','1790012340001','Av. Principal 123','ventas@autopartes.cm','022345678','0991234567',true),
  ('Dist. del Motor','1790056780001','Calle Sec. 456','info@distmotor.com','022987654','0997654321',true),
  ('Comercial Automotriz','1790098760001','Av. Siempre 789','comercial@auto.cm','023456789','0987654321',true),
  ('Repuestos Nacionales','1790033330001','Av. Ind. 321','ventas@repnac.com','024567890','0976543210',true);

INSERT INTO ubicacion_percha (nombre, estado) VALUES
  ('Estante A',true), ('Estante B',true), ('Estante C',true), ('Estante D',true);

INSERT INTO perchero (nombre_perchero, seccion, cantidad_lugares, estado) VALUES
  ('P1','A',10,true), ('P2','B',8,true), ('P3','C',6,true);

INSERT INTO ubicacion (codigo_ubicacion, id_perchero, estado) VALUES
  ('P1-A1',1,'DISPONIBLE'),('P1-A2',1,'DISPONIBLE'),('P1-A3',1,'DISPONIBLE'),
  ('P1-A4',1,'DISPONIBLE'),('P1-A5',1,'DISPONIBLE'),
  ('P2-B1',2,'DISPONIBLE'),('P2-B2',2,'DISPONIBLE'),('P2-B3',2,'DISPONIBLE'),
  ('P2-B4',2,'DISPONIBLE'),
  ('P3-C1',3,'DISPONIBLE'),('P3-C2',3,'DISPONIBLE');

INSERT INTO empresa (ruc,razon_social,sucursal,direccion_calle_principal,direccion_calle_secundaria,telefono,celular,correo,estado)
VALUES ('1799999999001','Mi Empresa S.A.S.','Matriz','Av. República','Calle Amazonas','022000000','0990000000','info@miempresa.com',true);

INSERT INTO cliente (nombre,identificacion,direccion,correo,telefono,celular) VALUES
  ('Cliente Genérico','9999999999999','N/A','N/A','N/A','N/A'),
  ('Juan Pérez','1234567890','Av. Los Shyris 456','juan@email.com','022111111','0991111111'),
  ('María García','0987654321','Calle de las Flores 789','maria@email.com','022222222','0992222222'),
  ('Carlos López','1122334455','Av. América 321','carlos@email.com','023333333','0993333333');

INSERT INTO vendedor (nombre,identificacion,correo,estado) VALUES
  ('Vendedor Principal','1700000001','vendedor@miempresa.com',true);

-- ==============================
-- INVENTARIO — 50 repuestos automotrices
-- ==============================
INSERT INTO inventario (descripcion,grupo_id,marca_id,proveedor_id,costo_sin_iva,cantidad,ubicacion_percha_id,precio_venta,fecha_ingreso,estado,codigo,forma_pago,meses_plazo,interes)
VALUES
-- Motor (grupo 1)
('Bujías Iridium Juego 4',1,1,1,18.00,40,1,25.20,TIMESTAMP '2026-07-01 09:00:00',true,'BUJMOTBOS18','Efectivo',0,null),
('Bobina de Encendido',1,1,1,22.50,25,2,31.50,TIMESTAMP '2026-07-01 10:00:00',true,'BOBMOTBOS22','Efectivo',0,null),
('Correa de Distribución',1,7,2,15.00,30,3,21.00,TIMESTAMP '2026-07-01 11:00:00',true,'CORMOTGAT15','Efectivo',0,null),
('Tensor Correa Distribución',1,7,2,28.00,15,4,39.20,TIMESTAMP '2026-07-01 12:00:00',true,'TENMOTGAT28','Efectivo',0,null),
('Bomba de Agua',1,3,3,35.00,20,1,49.00,TIMESTAMP '2026-07-01 13:00:00',true,'BOMMOTSKF35','Efectivo',0,null),
('Termostato',1,1,1,8.50,50,2,11.90,TIMESTAMP '2026-07-15 10:06:00',true,'TERMOTBOS8','TAG Crédito',6,8),
('Sensor de Oxígeno',1,1,1,45.00,12,3,63.00,TIMESTAMP '2026-07-01 15:00:00',true,'SENMOTBOS45','Efectivo',0,null),

-- Transmisión (grupo 2)
('Kit de Embrague',2,6,3,85.00,8,4,119.00,TIMESTAMP '2026-07-02 09:00:00',true,'KITTRAVAL85','Efectivo',0,null),
('Aceite Transmisión 1L',2,10,4,6.00,60,1,8.40,TIMESTAMP '2026-07-02 10:00:00',true,'ACETRAACD6','Efectivo',0,null),
('Cables de Cambio',2,6,3,12.00,20,2,16.80,TIMESTAMP '2026-03-10 10:10:00',true,'CABTRAVAL12','TAG Crédito',4,6),
('Soporte Motor',2,3,1,18.00,18,3,25.20,TIMESTAMP '2026-07-02 12:00:00',true,'SOPTRAVSKF18','Efectivo',0,null),
('Junta de Transmisión',2,3,1,9.50,25,4,13.30,TIMESTAMP '2026-07-02 13:00:00',true,'JUNTRAVSKF9','Efectivo',0,null),

-- Frenos (grupo 3)
('Pastillas de Freno Delant',3,9,1,25.00,35,1,35.00,TIMESTAMP '2026-07-03 09:00:00',true,'PASFREBRE25','Efectivo',0,null),
('Discos de Freno Delanteros',3,9,1,40.00,20,2,56.00,TIMESTAMP '2026-07-03 10:00:00',true,'DISFREBRE40','Efectivo',0,null),
('Líquido de Frenos 500ml',3,1,2,5.50,80,3,7.70,TIMESTAMP '2026-07-03 11:00:00',true,'LIQFREBOS5','Efectivo',0,null),
('Cilindro de Rueda',3,1,2,12.00,30,4,16.80,TIMESTAMP '2026-07-15 10:16:00',true,'CILFREBOS12','TAG Crédito',6,9),
('Kit de Tambor',3,9,1,32.00,15,1,44.80,TIMESTAMP '2026-07-03 13:00:00',true,'KITFREBRE32','Tarjeta de Débito',0,null),

-- Suspensión (grupo 4)
('Amortiguador Delantero',4,5,3,45.00,18,2,63.00,TIMESTAMP '2026-07-04 09:00:00',true,'AMOSUSKYB45','Efectivo',0,null),
('Amortiguador Trasero',4,8,3,42.00,18,3,58.80,TIMESTAMP '2026-07-04 10:00:00',true,'AMOSUSMON42','Efectivo',0,null),
('Rótula de Suspensión',4,3,1,14.00,28,4,19.60,TIMESTAMP '2026-07-04 11:00:00',true,'ROTSUSSKF14','Efectivo',0,null),
('Terminal de Dirección',4,3,1,11.00,30,1,15.40,TIMESTAMP '2026-07-04 12:00:00',true,'TERSUSSKF11','Efectivo',0,null),
('Barra Estabilizadora',4,5,3,22.00,12,2,30.80,TIMESTAMP '2026-03-10 10:22:00',true,'BARSUSKYB22','TAG Crédito',4,6),

-- Eléctrico (grupo 5)
('Batería 60Ah',5,1,2,65.00,10,3,91.00,TIMESTAMP '2026-07-05 09:00:00',true,'BATELEBOS65','Efectivo',0,null),
('Alternador',5,6,4,75.00,8,4,105.00,TIMESTAMP '2026-07-05 10:00:00',true,'ALTELEVAL75','Efectivo',0,null),
('Motor de Arranque',5,1,2,80.00,7,1,112.00,TIMESTAMP '2026-07-05 11:00:00',true,'MOTELEBOS80','Efectivo',0,null),
('Faro Delantero Derecho',5,6,4,35.00,15,2,49.00,TIMESTAMP '2026-07-05 12:00:00',true,'FARELEVAL35','Tarjeta de Crédito',0,null),
('Bomba de Combustible',5,1,2,55.00,10,3,77.00,TIMESTAMP '2026-07-15 10:28:00',true,'BOMELEBOS55','TAG Crédito',6,7),
('Relay de Arranque',5,1,2,10.00,40,4,14.00,TIMESTAMP '2026-07-05 14:00:00',true,'RELELEBOS10','Efectivo',0,null),
('Sensor de Velocidad',5,2,4,28.00,20,1,39.20,TIMESTAMP '2026-07-05 15:00:00',true,'SENELEBOS28','Efectivo',0,null),

-- Filtros (grupo 6)
('Filtro de Aceite',6,1,2,7.00,100,2,9.80,TIMESTAMP '2026-07-06 09:00:00',true,'FILFILBOS7','Efectivo',0,null),
('Filtro de Aire',6,1,2,12.00,70,3,16.80,TIMESTAMP '2026-07-06 10:00:00',true,'FILFILBOS12','Efectivo',0,null),
('Filtro de Combustible',6,1,2,9.00,50,4,12.60,TIMESTAMP '2026-07-06 11:00:00',true,'FILFILBOS9','Efectivo',0,null),
('Filtro de Cabina',6,1,2,8.50,45,1,11.90,TIMESTAMP '2026-03-10 10:33:00',true,'FILFILBOS8','TAG Crédito',4,6),
('Filtro de Aceite Premium',6,10,1,10.00,60,2,14.00,TIMESTAMP '2026-07-06 13:00:00',true,'FILFILACD10','Efectivo',0,null),

-- Carrocería (grupo 7)
('Espejo Retrovisor Derecho',7,6,3,20.00,14,3,28.00,TIMESTAMP '2026-07-07 09:00:00',true,'ESPCARVAL20','Efectivo',0,null),
('Cinturón de Seguridad',7,6,3,25.00,20,4,35.00,TIMESTAMP '2026-07-07 10:00:00',true,'CINVARVAL25','Efectivo',0,null),
('Bisagra de Puerta',7,3,1,8.00,30,1,11.20,TIMESTAMP '2026-07-07 11:00:00',true,'BISCARSKF8','Efectivo',0,null),
('Manilla de Puerta',7,6,3,14.00,25,2,19.60,TIMESTAMP '2026-03-10 10:38:00',true,'MANCARVAL14','TAG Crédito',6,10),
('Guardafango Delantero',7,6,3,30.00,10,3,42.00,TIMESTAMP '2026-07-07 13:00:00',true,'GUACARVAL30','Efectivo',0,null),

-- Más misceláneos
('Rodamiento de Rueda',4,3,1,18.00,22,4,25.20,TIMESTAMP '2026-07-08 09:00:00',true,'RODSUSSKF18','Efectivo',0,null),
('Retén de Aceite',1,3,1,3.50,80,1,4.90,TIMESTAMP '2026-07-08 10:00:00',true,'RETMOTSKF3','Efectivo',0,null),
('Manguera del Radiador',1,7,2,12.00,25,2,16.80,TIMESTAMP '2026-07-08 11:00:00',true,'MANMOTGAT12','Efectivo',0,null),
('Neumático 205/55R16',6,4,3,55.00,16,3,77.00,TIMESTAMP '2026-03-10 10:43:00',true,'NEUFILCON55','TAG Crédito',6,10),
('Válvula de Neumático',6,4,3,2.50,120,4,3.50,TIMESTAMP '2026-07-08 13:00:00',true,'VALFILCON2','Efectivo',0,null),
('Cable de Bujía Juego 4',1,2,4,14.00,35,1,19.60,TIMESTAMP '2026-07-08 14:00:00',true,'CABMOTNGK14','Efectivo',0,null),
('Aceite Motor 10W40 1L',1,10,4,5.00,90,2,7.00,TIMESTAMP '2026-07-08 15:00:00',true,'ACEMOTACD5','Efectivo',0,null),
('Refrigerante 1L',1,1,2,4.50,70,3,6.30,TIMESTAMP '2026-07-08 16:00:00',true,'REFMOTBOS4','Efectivo',0,null),
('Líquido Limpiaparabrisas',7,1,2,2.00,100,4,2.80,TIMESTAMP '2026-03-10 10:48:00',true,'LIQCARBOS2','TAG Crédito',4,9),
('Cera para Autos 500ml',7,10,1,8.00,30,1,11.20,TIMESTAMP '2026-07-08 18:00:00',true,'CERACARACD8','Efectivo',0,null),
('Limpiador de Inyectores',1,1,1,6.50,40,2,9.10,TIMESTAMP '2026-07-09 09:00:00',true,'LIMMOTBOS6','Efectivo',0,null);

-- ==============================
-- Assign ubicaciones to some products
-- ==============================
UPDATE ubicacion SET estado='OCUPADO', id_producto=1  WHERE codigo_ubicacion='P1-A1';
UPDATE ubicacion SET estado='OCUPADO', id_producto=3  WHERE codigo_ubicacion='P1-A2';
UPDATE ubicacion SET estado='OCUPADO', id_producto=7  WHERE codigo_ubicacion='P2-B1';
UPDATE ubicacion SET estado='OCUPADO', id_producto=13 WHERE codigo_ubicacion='P2-B2';
UPDATE ubicacion SET estado='OCUPADO', id_producto=18 WHERE codigo_ubicacion='P3-C1';
UPDATE ubicacion SET estado='OCUPADO', id_producto=23 WHERE codigo_ubicacion='P1-A3';
UPDATE ubicacion SET estado='OCUPADO', id_producto=29 WHERE codigo_ubicacion='P1-A4';
UPDATE ubicacion SET estado='OCUPADO', id_producto=41 WHERE codigo_ubicacion='P2-B3';

-- ==============================
-- CUENTAS POR PAGAR (TAG Crédito)
-- New (Jul 2026): items 6, 16, 27, 38, 43
-- Near expiry (Mar 2026): items 10, 22, 34, 49
-- ==============================
INSERT INTO cuentas_por_pagar (inventario_id,proveedor_id,total,meses_plazo,interes,cuota_mensual,estado,fecha_registro)
SELECT i.id, i.proveedor_id,
       ROUND((i.costo_sin_iva * i.cantidad) * (1 + COALESCE(i.interes,0)/100.0), 2),
       i.meses_plazo, COALESCE(i.interes,0),
       ROUND(((i.costo_sin_iva * i.cantidad) * (1 + COALESCE(i.interes,0)/100.0)) / i.meses_plazo, 2),
       'Pendiente', i.fecha_ingreso
FROM inventario i
WHERE i.forma_pago = 'TAG Crédito'
ORDER BY i.fecha_ingreso;

-- ==============================
-- SAMPLE VENTAS + CUENTAS POR COBRAR
-- ==============================
-- Venta 1: new credit (Jul 2026)
INSERT INTO nota_venta_registro (empresa_id,cliente_id,fecha,codigo,forma_pago,fecha_registro)
VALUES (1,2,TIMESTAMP '2026-07-20 14:30:00','NV-001','TAG Crédito',NOW());
INSERT INTO nota_venta_detalle (nota_venta_registro_id,descripcion,cantidad,precio_unitario,precio_total,subtotal,iva,descuento,total,fecha_registro)
VALUES (1,'Pastillas de Freno Delant',2,35.00,70.00,70.00,0,0,70.00,NOW());
INSERT INTO cuentas_por_cobrar (nota_venta_id,cliente_id,total,meses_plazo,interes,cuota_mensual,estado,fecha_registro)
VALUES (1,2,75.60,6,8,12.60,'Pendiente',TIMESTAMP '2026-07-20 14:30:00');

-- Venta 2: near-expiry credit (Mar 2026, 4-month term → expires Jul 2026)
INSERT INTO nota_venta_registro (empresa_id,cliente_id,fecha,codigo,forma_pago,fecha_registro)
VALUES (1,3,TIMESTAMP '2026-03-15 11:00:00','NV-002','TAG Crédito',NOW());
INSERT INTO nota_venta_detalle (nota_venta_registro_id,descripcion,cantidad,precio_unitario,precio_total,subtotal,iva,descuento,total,fecha_registro)
VALUES (2,'Kit de Embrague',1,119.00,119.00,119.00,0,0,119.00,NOW());
INSERT INTO cuentas_por_cobrar (nota_venta_id,cliente_id,total,meses_plazo,interes,cuota_mensual,estado,fecha_registro)
VALUES (2,3,126.14,4,6,31.54,'Pendiente',TIMESTAMP '2026-03-15 11:00:00');

-- Venta 3: cash sale
INSERT INTO nota_venta_registro (empresa_id,cliente_id,fecha,codigo,forma_pago,fecha_registro)
VALUES (1,4,TIMESTAMP '2026-07-25 16:00:00','NV-003','Efectivo',NOW());
INSERT INTO nota_venta_detalle (nota_venta_registro_id,descripcion,cantidad,precio_unitario,precio_total,subtotal,iva,descuento,total,fecha_registro)
VALUES (3,'Aceite Motor 10W40 1L',4,7.00,28.00,28.00,0,0,28.00,NOW());

COMMIT;
