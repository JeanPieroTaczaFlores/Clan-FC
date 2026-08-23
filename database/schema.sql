-- ============================================================================
-- TiendaMenos — Script DDL para PostgreSQL
-- Entregable 1: roles, usuarios, empresas_clientes, categorias, productos.
--
-- Uso en VS Code:
--   1) Crear la base de datos:        CREATE DATABASE tiendamenos;
--   2) Ejecutar este script conectado a "tiendamenos":
--        psql -U postgres -d tiendamenos -f database/schema.sql
--      o pegarlo en la extensión "PostgreSQL" de VS Code.
--
-- NOTA: los usuarios (tabla usuarios) se siembran desde Java (DataLoader)
--       porque las contraseñas requieren hash BCrypt generado en runtime.
-- ============================================================================

BEGIN; -- Transacción única: todo o nada

/* ----------------------------------------------------------------------------
 * 1) ROLES — catálogo fijo para Spring Security (prefijo ROLE_ se agrega en Java)
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS roles (
    id_rol       BIGSERIAL    PRIMARY KEY,
    nombre       VARCHAR(30)  NOT NULL UNIQUE
                 CHECK (nombre IN ('ADMIN', 'CAJERO', 'CLIENTE')),
    descripcion  VARCHAR(200)
);

/* ----------------------------------------------------------------------------
 * 2) USUARIOS — credenciales del sistema (login + autorización por rol)
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario     BIGSERIAL     PRIMARY KEY,
    id_rol         BIGINT        NOT NULL REFERENCES roles (id_rol),
    username       VARCHAR(50)   NOT NULL UNIQUE,
    email          VARCHAR(100)  NOT NULL UNIQUE
                   CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),  -- formato email básico
    password_hash  VARCHAR(100)  NOT NULL,                        -- BCrypt (~60 chars)
    nombre_completo VARCHAR(120) NOT NULL,
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_usuarios_id_rol ON usuarios (id_rol);
CREATE INDEX IF NOT EXISTS idx_usuarios_activo ON usuarios (activo) WHERE activo;

/* ----------------------------------------------------------------------------
 * 3) EMPRESAS_CLIENTES — clientes B2B con régimen fiscal parametrizable.
 *    La tasa de IVA aplicable depende del régimen (lógica Entregable 2):
 *      EXENTO -> 0% | REDUCIDO -> tasa reducida | GENERAL -> tasa general
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS empresas_clientes (
    id_empresa      BIGSERIAL     PRIMARY KEY,
    razon_social    VARCHAR(150)  NOT NULL,
    rfc             VARCHAR(13)   NOT NULL UNIQUE
                    CHECK (char_length(rfc) BETWEEN 10 AND 13),     -- RFC México (PF/PM)
    regimen_fiscal  VARCHAR(20)   NOT NULL
                    CHECK (regimen_fiscal IN ('EXENTO', 'GENERAL', 'REDUCIDO')),
    tasa_iva        NUMERIC(5,2)  NOT NULL DEFAULT 16.00
                    CHECK (tasa_iva >= 0 AND tasa_iva <= 100),      -- % IVA parametrizable
    contacto_email  VARCHAR(100),
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_email_contacto CHECK (
        contacto_email IS NULL OR contacto_email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'
    )
);

CREATE INDEX IF NOT EXISTS idx_empresas_regimen ON empresas_clientes (regimen_fiscal);

/* ----------------------------------------------------------------------------
 * 4) CATEGORIAS
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS categorias (
    id_categoria   BIGSERIAL     PRIMARY KEY,
    nombre         VARCHAR(80)   NOT NULL UNIQUE,
    descripcion    VARCHAR(255),
    activa         BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ   NOT NULL DEFAULT now()
);

/* ----------------------------------------------------------------------------
 * 5) PRODUCTOS — inventario del catálogo.
 *    precio_base es SIN IVA; el impuesto se calcula según el régimen fiscal
 *    del comprador (cliente web o venta de mostrador).
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS productos (
    id_producto        BIGSERIAL       PRIMARY KEY,
    id_categoria       BIGINT          NOT NULL REFERENCES categorias (id_categoria),
    sku                VARCHAR(40)     NOT NULL UNIQUE,
    nombre             VARCHAR(120)    NOT NULL,
    descripcion        VARCHAR(500),
    precio_base        NUMERIC(12,2)   NOT NULL CHECK (precio_base >= 0),
    stock              INTEGER         NOT NULL DEFAULT 0 CHECK (stock >= 0),
    stock_minimo       INTEGER         NOT NULL DEFAULT 5 CHECK (stock_minimo >= 0),
    imagen_url         VARCHAR(300),
    activo             BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion     TIMESTAMPTZ     NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_productos_categoria ON productos (id_categoria);
CREATE INDEX IF NOT EXISTS idx_productos_nombre    ON productos (lower(nombre)); -- búsquedas case-insensitive
CREATE INDEX IF NOT EXISTS idx_productos_activo    ON productos (activo) WHERE activo;
CREATE INDEX IF NOT EXISTS idx_productos_stock_bajo ON productos (stock)
    WHERE stock <= stock_minimo; -- alertas de reposición (dashboard Entregable 3)

/* ============================================================================
 * DATOS SEMILLA (DML mínimo para pruebas)
 * ========================================================================== */

INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMIN',   'Administrador: gestión total, inventario y usuarios'),
    ('CAJERO',  'Cajero: punto de venta y emisión de comprobantes'),
    ('CLIENTE', 'Cliente: compra web, carrito y checkout')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO empresas_clientes (razon_social, rfc, regimen_fiscal, tasa_iva, contacto_email) VALUES
    ('Comercializadora del Norte SA de CV', 'CON850101AB1', 'GENERAL',  16.00, 'compras@comer-norte.mx'),
    ('Fundación Educativa Alianza AC',      'FEA120314QP9', 'EXENTO',    0.00,  'admin@fundalianza.org'),
    ('Talleres Mecánicos Ríos SRL',         'TMR180622R4A', 'REDUCIDO', 8.00,  'pagos@talleresrios.mx')
ON CONFLICT (rfc) DO NOTHING;

INSERT INTO categorias (nombre, descripcion) VALUES
    ('Electrónica', 'Dispositivos y accesorios electrónicos'),
    ('Hogar',       'Artículos para el hogar y cocina'),
    ('Oficina',     'Papelería y equipo de trabajo'),
    ('Deportes',    'Equipamiento y ropa deportiva')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO productos (id_categoria, sku, nombre, descripcion, precio_base, stock, stock_minimo) VALUES
    (1, 'ELEC-001', 'Audífonos Bluetooth Pro',        'Cancelación de ruido activa, 30h batería',        899.00, 25,  5),
    (1, 'ELEC-002', 'Mouse Inalámbrico Ergo',         'Sensor 4000 DPI, silencioso',                     249.50,  3,  5),
    (1, 'ELEC-003', 'Teclado Mecánico RGB',           'Switches rojos, layout ES',                      1150.00,  0,  4),
    (2, 'HOG-001',  'Cafetera Espresso 15 Bar',       'Vaporizador de leche incluido',                  2399.00, 12,  3),
    (2, 'HOG-002',  'Juego de Sábanas Queen',         'Algodón egipcio 400 hilos',                       649.90,  4,  5),
    (2, 'HOG-003',  'Set de 6 Vasos Termoresistentes','Aptos para microondas',                           189.00, 40, 10),
    (3, 'OFI-001',  'Silla de Oficina Lumbar+',       'Soporte lumbar ajustable, ruedas silenciosas',   3150.00,  7,  2),
    (3, 'OFI-002',  'Cuaderno Profesional x5',        '100 hojas cada uno, cuadro chico',                 95.00,120, 20),
    (4, 'DEP-001',  'Mancuernas Ajustables 20kg',     'Par con sistema de disco rápido',                1899.00,  6,  3),
    (4, 'DEP-002',  'Tapete de Yoga Antideslizante',  '6mm, incluye correa de transporte',               320.00, 18,  6)
ON CONFLICT (sku) DO NOTHING;

COMMIT;

-- Verificación rápida:
SELECT 'roles' AS tabla, count(*) FROM roles
UNION ALL SELECT 'empresas_clientes', count(*) FROM empresas_clientes
UNION ALL SELECT 'categorias', count(*) FROM categorias
UNION ALL SELECT 'productos', count(*) FROM productos;
