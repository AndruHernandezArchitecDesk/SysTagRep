-- ============================================================
-- TABLAS PARA EL MÓDULO DE PERCHEROS Y UBICACIONES
-- ============================================================

-- TABLA PERCHERO: Almacena los percheros (estanterías)
CREATE TABLE IF NOT EXISTS perchero (
    id SERIAL PRIMARY KEY,
    nombre_perchero VARCHAR(10) NOT NULL,
    seccion VARCHAR(10) NOT NULL,
    cantidad_lugares INTEGER NOT NULL,
    estado BOOLEAN DEFAULT true
);

-- TABLA UBICACION: Almacena cada posición individual dentro de un perchero
CREATE TABLE IF NOT EXISTS ubicacion (
    id SERIAL PRIMARY KEY,
    codigo_ubicacion VARCHAR(20) NOT NULL,
    id_perchero INTEGER NOT NULL REFERENCES perchero(id) ON DELETE CASCADE,
    estado VARCHAR(20) DEFAULT 'DISPONIBLE',
    id_producto INTEGER REFERENCES inventario(id)
);

CREATE INDEX IF NOT EXISTS idx_ubicacion_id_perchero ON ubicacion(id_perchero);
CREATE INDEX IF NOT EXISTS idx_ubicacion_codigo ON ubicacion(codigo_ubicacion);
