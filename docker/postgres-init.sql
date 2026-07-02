CREATE TABLE IF NOT EXISTS customer (
    id       SERIAL PRIMARY KEY,
    nombre   VARCHAR(150) NOT NULL,
    apellido VARCHAR(150) NOT NULL,
    estado   VARCHAR(50)  NOT NULL CHECK (estado IN ('ACTIVE', 'INACTIVE')),
    edad     INTEGER      NOT NULL CHECK (edad > 0 AND edad <= 150)
);

CREATE INDEX IF NOT EXISTS idx_customer_estado ON customer(estado);
