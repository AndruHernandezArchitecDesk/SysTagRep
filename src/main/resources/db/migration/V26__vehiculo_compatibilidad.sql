-- V26__vehiculo_compatibilidad.sql — Catálogo vehículos + M:N inventario_vehiculo (API NHTSA)

CREATE TABLE IF NOT EXISTS vehiculo (
    id SERIAL PRIMARY KEY,
    marca VARCHAR(50) NOT NULL,
    modelo VARCHAR(50) NOT NULL,
    anio_desde INT,
    anio_hasta INT,
    motor VARCHAR(50),
    combustible VARCHAR(20),
    vin_prefijo VARCHAR(8),
    pais_origen VARCHAR(50),
    tipo_vehiculo VARCHAR(30),
    creado_en TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(marca, modelo, COALESCE(anio_desde,0), COALESCE(motor,''))
);
CREATE INDEX IF NOT EXISTS idx_vehiculo_marca_modelo ON vehiculo(marca, modelo);
CREATE INDEX IF NOT EXISTS idx_vehiculo_anio ON vehiculo(anio_desde, anio_hasta);
CREATE INDEX IF NOT EXISTS idx_vehiculo_vin ON vehiculo(vin_prefijo) WHERE vin_prefijo IS NOT NULL;

CREATE TABLE IF NOT EXISTS inventario_vehiculo (
    inventario_id INTEGER NOT NULL REFERENCES inventario(id) ON DELETE CASCADE,
    vehiculo_id INTEGER NOT NULL REFERENCES vehiculo(id) ON DELETE CASCADE,
    notas VARCHAR(200),
    PRIMARY KEY (inventario_id, vehiculo_id)
);
CREATE INDEX IF NOT EXISTS idx_iv_veh_vehiculo ON inventario_vehiculo(vehiculo_id);
CREATE INDEX IF NOT EXISTS idx_iv_veh_inventario ON inventario_vehiculo(inventario_id);

-- Seed mínimo 10 vehículos (CHEV, TOYOTA, HYUNDAI, KIA, FORD, NISSAN)
INSERT INTO vehiculo(marca, modelo, anio_desde, anio_hasta, motor, combustible, vin_prefijo, tipo_vehiculo) VALUES
('CHEVROLET','AVEO',2012,2017,'1.6','Gasolina','3G1', 'Sedán'),
('CHEVROLET','SPARK',2015,2022,'1.2','Gasolina','KL1', 'Hatchback'),
('CHEVROLET','SAIL',2014,2019,'1.4','Gasolina','8L', 'Sedán'),
('TOYOTA','HILUX',2015,2024,'2.8','Diésel','8AJ', 'Pickup'),
('TOYOTA','COROLLA',2018,2024,'1.8','Gasolina','2T1', 'Sedán'),
('HYUNDAI','ACCENT',2016,2023,'1.6','Gasolina','KMH', 'Sedán'),
('KIA','RIO',2017,2023,'1.6','Gasolina','KNADM', 'Sedán'),
('FORD','F150',2015,2024,'3.5','Gasolina','1FT', 'Pickup'),
('NISSAN','FRONTIER',2016,2024,'2.5','Diésel','3N6', 'Pickup'),
('SUZUKI','GRAND VITARA',2014,2020,'2.0','Gasolina','JS3', 'SUV')
ON CONFLICT (marca, modelo, COALESCE(anio_desde,0), COALESCE(motor,'')) DO NOTHING;

-- Migración embedida: parsear descripcion existente que contiene modelo (LIKE) -> poblar inventario_vehiculo
INSERT INTO inventario_vehiculo(inventario_id, vehiculo_id)
SELECT i.id, v.id FROM inventario i JOIN vehiculo v ON LOWER(i.descripcion) LIKE '%' || LOWER(v.modelo) || '%'
ON CONFLICT DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='app_vendex') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE vehiculo TO app_vendex;
        GRANT SELECT, INSERT, DELETE ON TABLE inventario_vehiculo TO app_vendex;
        GRANT USAGE, SELECT ON SEQUENCE vehiculo_id_seq TO app_vendex;
    END IF;
END $$;
