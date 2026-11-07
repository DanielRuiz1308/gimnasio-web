-- =====================================================
-- Base de datos: Sistema de Gimnasio (Lifting Club)
-- Motor: MySQL 8+
-- Version 3: incluye usuarios (login), genero, fecha_nacimiento, catalogo de
-- tipos de membresia, historial de pagos y foto de las personas.
--
-- Como usarlo: abrir en MySQL Workbench (File > Open SQL Script)
-- y ejecutar todo (icono del rayo). Crea la base desde cero.
-- =====================================================

CREATE DATABASE IF NOT EXISTS gimnasio_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE gimnasio_db;

-- -----------------------------------------------------
-- persona (tabla base de la herencia)
-- -----------------------------------------------------
CREATE TABLE persona (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(100) NOT NULL,
    documento         VARCHAR(20)  NOT NULL,
    telefono          VARCHAR(20),
    email             VARCHAR(100),
    genero            ENUM('M', 'F', 'Otro') NOT NULL,
    fecha_nacimiento  DATE NOT NULL,
    foto              MEDIUMBLOB NULL,
    foto_tipo         VARCHAR(50) NULL,
    CONSTRAINT uq_persona_documento UNIQUE (documento)
);

-- -----------------------------------------------------
-- cliente (hereda de persona)
-- -----------------------------------------------------
CREATE TABLE cliente (
    id INT PRIMARY KEY,
    CONSTRAINT fk_cliente_persona
        FOREIGN KEY (id) REFERENCES persona(id)
        ON DELETE CASCADE
);

-- -----------------------------------------------------
-- entrenador (hereda de persona)
-- -----------------------------------------------------
CREATE TABLE entrenador (
    id            INT PRIMARY KEY,
    especialidad  VARCHAR(100),
    CONSTRAINT fk_entrenador_persona
        FOREIGN KEY (id) REFERENCES persona(id)
        ON DELETE CASCADE
);

-- -----------------------------------------------------
-- administrador (hereda de persona)
-- -----------------------------------------------------
CREATE TABLE administrador (
    id        INT PRIMARY KEY,
    permisos  VARCHAR(255),
    CONSTRAINT fk_administrador_persona
        FOREIGN KEY (id) REFERENCES persona(id)
        ON DELETE CASCADE
);

-- -----------------------------------------------------
-- tipo_membresia (catalogo de planes y precios)
-- -----------------------------------------------------
CREATE TABLE tipo_membresia (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    nombre  VARCHAR(50) NOT NULL UNIQUE,
    precio  DECIMAL(10,2) NOT NULL
);

INSERT INTO tipo_membresia (nombre, precio) VALUES
    ('Mensual', 80000),
    ('Trimestral', 210000),
    ('Anual', 750000);

-- -----------------------------------------------------
-- membresia (estado actual, 1 a 1 con cliente)
-- -----------------------------------------------------
CREATE TABLE membresia (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id         INT NOT NULL,
    tipo_membresia_id  INT NOT NULL,
    fecha_pago         DATE NOT NULL,
    fecha_vencimiento  DATE NOT NULL,
    CONSTRAINT uq_membresia_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_membresia_cliente
        FOREIGN KEY (cliente_id) REFERENCES cliente(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_membresia_tipo
        FOREIGN KEY (tipo_membresia_id) REFERENCES tipo_membresia(id)
);

-- -----------------------------------------------------
-- pago (historial de todos los pagos)
-- -----------------------------------------------------
CREATE TABLE pago (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id         INT NOT NULL,
    tipo_membresia_id  INT NOT NULL,
    fecha_pago         DATE NOT NULL,
    monto              DECIMAL(10,2) NOT NULL,
    metodo_pago        ENUM('efectivo', 'transferencia', 'tarjeta') NOT NULL,
    CONSTRAINT fk_pago_cliente
        FOREIGN KEY (cliente_id) REFERENCES cliente(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_pago_tipo
        FOREIGN KEY (tipo_membresia_id) REFERENCES tipo_membresia(id)
);

-- -----------------------------------------------------
-- registro_fisico (historial de peso y altura)
-- -----------------------------------------------------
CREATE TABLE registro_fisico (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id  INT NOT NULL,
    fecha       DATE NOT NULL,
    peso        DECIMAL(5,2) NOT NULL,
    altura      DECIMAL(5,2) NOT NULL,
    CONSTRAINT fk_registro_cliente
        FOREIGN KEY (cliente_id) REFERENCES cliente(id)
        ON DELETE CASCADE
);

-- -----------------------------------------------------
-- rutina (rutina actual, 1 a 1 con cliente)
-- -----------------------------------------------------
CREATE TABLE rutina (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id     INT NOT NULL,
    entrenador_id  INT NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    nivel          VARCHAR(50),
    objetivo       VARCHAR(100),
    CONSTRAINT uq_rutina_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_rutina_cliente
        FOREIGN KEY (cliente_id) REFERENCES cliente(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rutina_entrenador
        FOREIGN KEY (entrenador_id) REFERENCES entrenador(id)
        ON DELETE RESTRICT
);

-- -----------------------------------------------------
-- ejercicio (muchos por rutina)
-- -----------------------------------------------------
CREATE TABLE ejercicio (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    rutina_id     INT NOT NULL,
    nombre        VARCHAR(100) NOT NULL,
    series        INT NOT NULL,
    repeticiones  INT NOT NULL,
    descanso      INT COMMENT 'segundos de descanso',
    CONSTRAINT fk_ejercicio_rutina
        FOREIGN KEY (rutina_id) REFERENCES rutina(id)
        ON DELETE CASCADE
);

-- -----------------------------------------------------
-- asistencia (muchas por cliente y por entrenador)
-- -----------------------------------------------------
CREATE TABLE asistencia (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id     INT NOT NULL,
    entrenador_id  INT NOT NULL,
    fecha          DATE NOT NULL,
    hora           TIME NOT NULL,
    turno          ENUM('mañana', 'noche') NOT NULL,
    CONSTRAINT fk_asistencia_cliente
        FOREIGN KEY (cliente_id) REFERENCES cliente(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_asistencia_entrenador
        FOREIGN KEY (entrenador_id) REFERENCES entrenador(id)
        ON DELETE RESTRICT
);

-- -----------------------------------------------------
-- usuario (quienes pueden iniciar sesion en el programa)
-- Las contrasenas NO se guardan en texto plano: se guarda
-- SHA-256(sal + contrasena).
-- Roles: 'admin' (todo), 'recepcion' (clientes, membresias,
-- rutinas, asistencia, registro fisico) y 'entrenador'
-- (rutinas, asistencia, registro fisico; vinculado a un
-- registro de la tabla entrenador).
-- -----------------------------------------------------
CREATE TABLE usuario (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo  VARCHAR(100) NOT NULL,
    nombre_usuario   VARCHAR(50)  NOT NULL,
    salt             VARCHAR(32)  NOT NULL,
    password_hash    CHAR(64)     NOT NULL,
    rol              ENUM('admin', 'recepcion', 'entrenador') NOT NULL DEFAULT 'recepcion',
    entrenador_id    INT NULL,
    CONSTRAINT uq_usuario_nombre UNIQUE (nombre_usuario),
    CONSTRAINT uq_usuario_entrenador UNIQUE (entrenador_id),
    CONSTRAINT fk_usuario_entrenador
        FOREIGN KEY (entrenador_id) REFERENCES entrenador(id)
        ON DELETE CASCADE
);

-- Usuario inicial: admin / admin123  (cambiar la contrasena desde el programa)
INSERT INTO usuario (nombre_completo, nombre_usuario, salt, password_hash, rol)
VALUES ('Administrador', 'admin', 'lc_salt_admin', SHA2(CONCAT('lc_salt_admin', 'admin123'), 256), 'admin');
