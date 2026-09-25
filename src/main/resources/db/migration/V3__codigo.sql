CREATE TABLE IF NOT EXISTS codigo (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    estado BOOLEAN DEFAULT TRUE
);

INSERT INTO codigo(nombre, estado)
SELECT DISTINCT codigo, TRUE FROM inventario
WHERE codigo IS NOT NULL AND TRIM(codigo) <> ''
  AND codigo NOT IN (SELECT nombre FROM codigo);
