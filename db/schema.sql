-- =====================================================================
-- Sistema de Gestión de Vehículos - Esquema PostgreSQL (UTF-8)
-- Ejecutar ANTES del primer arranque de la aplicación para que existan
-- los CHECK, los índices y las claves foráneas (Hibernate ddl-auto=update
-- no los crea con esta precisión).
--   psql -U docker -d vehiculosdb -f db/schema.sql
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. CONDUCTORES
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS conductores (
    id           BIGSERIAL    PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    departamento VARCHAR(30)  NOT NULL,
    telefono     VARCHAR(20),
    CONSTRAINT ck_conductores_departamento
        CHECK (departamento IN ('Logística', 'Comercial', 'Administración', 'Dirección'))
);

-- ---------------------------------------------------------------------
-- 2. VEHÍCULOS
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vehiculos (
    id          BIGSERIAL    PRIMARY KEY,
    matricula   VARCHAR(20)  NOT NULL,
    modelo      VARCHAR(100),
    kilometraje INTEGER      NOT NULL DEFAULT 0,
    estado      VARCHAR(20)  NOT NULL DEFAULT 'DISPONIBLE',
    CONSTRAINT uq_vehiculos_matricula UNIQUE (matricula),
    CONSTRAINT ck_vehiculos_kilometraje CHECK (kilometraje >= 0),
    CONSTRAINT ck_vehiculos_estado CHECK (estado IN ('DISPONIBLE', 'EN_RUTA', 'TALLER'))
);

-- ---------------------------------------------------------------------
-- 3. USUARIOS (seguridad y roles)
-- password: hash BCrypt, nunca texto plano.
-- conductor_id: enlace opcional con la ficha del conductor.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id           BIGSERIAL    PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL,
    password     VARCHAR(100) NOT NULL,
    rol          VARCHAR(20)  NOT NULL,
    conductor_id BIGINT,
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMIN', 'CONDUCTOR')),
    CONSTRAINT fk_usuarios_conductor FOREIGN KEY (conductor_id)
        REFERENCES conductores (id) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------
-- 4. ASIGNACIONES
-- conductor_id / vehiculo_id son NULOS cuando el conductor o el vehículo se han
-- eliminado (ON DELETE SET NULL). El histórico se conserva gracias a las copias
-- conductor_nombre y vehiculo_matricula. Solo se pueden eliminar asignaciones
-- FINALIZADAS con una antigüedad mínima (lo controla la aplicación).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS asignaciones (
    id                    BIGSERIAL   PRIMARY KEY,
    conductor_id          BIGINT,
    vehiculo_id           BIGINT,
    conductor_nombre      VARCHAR(100) NOT NULL,
    vehiculo_matricula    VARCHAR(20)  NOT NULL,
    fecha_salida          TIMESTAMP   NOT NULL,
    fecha_prevista_regreso TIMESTAMP  NOT NULL,
    fecha_regreso_real    TIMESTAMP,
    km_salida             INTEGER     NOT NULL,
    km_regreso            INTEGER,
    estado_asignacion     VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    motivo                VARCHAR(10) NOT NULL,
    CONSTRAINT fk_asignaciones_conductor FOREIGN KEY (conductor_id)
        REFERENCES conductores (id) ON DELETE SET NULL,
    CONSTRAINT fk_asignaciones_vehiculo FOREIGN KEY (vehiculo_id)
        REFERENCES vehiculos (id) ON DELETE SET NULL,
    CONSTRAINT ck_asignaciones_estado CHECK (estado_asignacion IN ('ACTIVA', 'FINALIZADA')),
    CONSTRAINT ck_asignaciones_motivo CHECK (motivo IN ('Ruta', 'Taller')),
    CONSTRAINT ck_asignaciones_km_salida CHECK (km_salida >= 0),
    CONSTRAINT ck_asignaciones_km_regreso CHECK (km_regreso IS NULL OR km_regreso >= km_salida),
    CONSTRAINT ck_asignaciones_fechas CHECK (fecha_prevista_regreso >= fecha_salida),
    -- Coherencia entre estado y datos de cierre
    CONSTRAINT ck_asignaciones_coherencia CHECK (
        (estado_asignacion = 'ACTIVA'
            AND km_regreso IS NULL AND fecha_regreso_real IS NULL)
        OR
        (estado_asignacion = 'FINALIZADA'
            AND km_regreso IS NOT NULL AND fecha_regreso_real IS NOT NULL)
    )
);

-- Un vehículo solo puede tener UNA asignación activa a la vez
CREATE UNIQUE INDEX IF NOT EXISTS uq_asignaciones_vehiculo_activa
    ON asignaciones (vehiculo_id) WHERE estado_asignacion = 'ACTIVA';

-- Un conductor solo puede tener UNA asignación de Ruta activa (de Taller, todas las que haga falta)
CREATE UNIQUE INDEX IF NOT EXISTS uq_asignaciones_conductor_ruta_activa
    ON asignaciones (conductor_id) WHERE estado_asignacion = 'ACTIVA' AND motivo = 'Ruta';

CREATE INDEX IF NOT EXISTS idx_asignaciones_conductor ON asignaciones (conductor_id);
CREATE INDEX IF NOT EXISTS idx_asignaciones_vehiculo  ON asignaciones (vehiculo_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_conductor     ON usuarios (conductor_id);

-- ---------------------------------------------------------------------
-- NOTA: no se inserta ningún usuario aquí para no dejar contraseñas ni
-- hashes de ejemplo en el repositorio. El primer ADMIN lo crea la clase
-- DataInitializer al arrancar, leyendo la contraseña del secreto
-- "admin_password" (o de la variable ADMIN_PASSWORD).
-- ---------------------------------------------------------------------
