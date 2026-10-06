-- Accesos directos del usuario: iconos arrastrados del sidebar a la barra superior.
CREATE TABLE IF NOT EXISTS acceso_directo (
    id SERIAL PRIMARY KEY,
    usuario_id INT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    vista_ruta VARCHAR(100) NOT NULL,
    titulo VARCHAR(80) NOT NULL,
    icono_literal VARCHAR(60) NOT NULL,
    posicion INT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(usuario_id, vista_ruta)
);

CREATE INDEX IF NOT EXISTS idx_acceso_directo_usuario ON acceso_directo(usuario_id);
