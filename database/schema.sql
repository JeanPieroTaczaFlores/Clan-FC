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
    id_pais        BIGINT        REFERENCES paises (id_pais),     -- país fiscal del usuario (IVA)
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_usuarios_id_rol ON usuarios (id_rol);
CREATE INDEX IF NOT EXISTS idx_usuarios_activo ON usuarios (activo) WHERE activo;

/* ----------------------------------------------------------------------------
 * 3) PAISES — origen fiscal del cliente: el IVA depende del país.
 *    codigo_iso2 permite generar la bandera (emoji) en el frontend.
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS paises (
    id_pais            BIGSERIAL      PRIMARY KEY,
    codigo_iso2        VARCHAR(2)     NOT NULL UNIQUE
                       CHECK (codigo_iso2 ~ '^[A-Z]{2}$'),            -- ISO 3166-1 alpha-2
    nombre             VARCHAR(60)    NOT NULL UNIQUE,
    tasa_iva_general   NUMERIC(5,2)   NOT NULL DEFAULT 16.00
                       CHECK (tasa_iva_general >= 0 AND tasa_iva_general <= 100),
    tasa_iva_reducido  NUMERIC(5,2)   NOT NULL DEFAULT 8.00
                       CHECK (tasa_iva_reducido >= 0 AND tasa_iva_reducido <= 100),
    activo             BOOLEAN        NOT NULL DEFAULT TRUE
);

/* ----------------------------------------------------------------------------
 * 4) EMPRESAS_CLIENTES — clientes B2B con país y régimen fiscal.
 *    La tasa efectiva se resuelve: PAÍS + RÉGIMEN (dinámico) y la columna
 *    tasa_iva guarda el snapshot aplicado al momento del registro.
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS empresas_clientes (
    id_empresa      BIGSERIAL     PRIMARY KEY,
    razon_social    VARCHAR(150)  NOT NULL,
    rfc             VARCHAR(13)   NOT NULL UNIQUE
                    CHECK (char_length(rfc) BETWEEN 10 AND 13),     -- RFC/NIT/identificador fiscal
    id_pais         BIGINT        REFERENCES paises (id_pais),      -- país fiscal (IVA por país)
    regimen_fiscal  VARCHAR(20)   NOT NULL
                    CHECK (regimen_fiscal IN ('EXENTO', 'GENERAL', 'REDUCIDO')),
    tasa_iva        NUMERIC(5,2)  NOT NULL DEFAULT 16.00
                    CHECK (tasa_iva >= 0 AND tasa_iva <= 100),      -- snapshot / override manual
    contacto_email  VARCHAR(100),
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_email_contacto CHECK (
        contacto_email IS NULL OR contacto_email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'
    )
);

CREATE INDEX IF NOT EXISTS idx_empresas_regimen ON empresas_clientes (regimen_fiscal);
CREATE INDEX IF NOT EXISTS idx_empresas_pais    ON empresas_clientes (id_pais);

/* ----------------------------------------------------------------------------
 * 5) CATEGORIAS
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS categorias (
    id_categoria   BIGSERIAL     PRIMARY KEY,
    nombre         VARCHAR(80)   NOT NULL UNIQUE,
    descripcion    VARCHAR(255),
    activa         BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ   NOT NULL DEFAULT now()
);

/* ----------------------------------------------------------------------------
 * 6bis) PROVEEDORES — quién surte la mercancía electrónica.
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS proveedores (
    id_proveedor     BIGSERIAL     PRIMARY KEY,
    nombre           VARCHAR(120)  NOT NULL UNIQUE,
    contacto_nombre  VARCHAR(120),
    telefono         VARCHAR(30),
    email            VARCHAR(100)
                     CHECK (email IS NULL OR email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

/* ----------------------------------------------------------------------------
 * 6) PRODUCTOS — inventario del catálogo (tienda enfocada en ELECTRÓNICA).
 *    precio_base es SIN IVA; cada producto declara su garantía en meses y
 *    su proveedor para trazabilidad de compras y reclamos.
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS productos (
    id_producto        BIGSERIAL       PRIMARY KEY,
    id_categoria       BIGINT          NOT NULL REFERENCES categorias (id_categoria),
    id_proveedor       BIGINT          REFERENCES proveedores (id_proveedor),
    sku                VARCHAR(40)     NOT NULL UNIQUE,
    nombre             VARCHAR(120)    NOT NULL,
    descripcion        VARCHAR(500),
    precio_base        NUMERIC(12,2)   NOT NULL CHECK (precio_base >= 0),
    stock              INTEGER         NOT NULL DEFAULT 0 CHECK (stock >= 0),
    stock_minimo       INTEGER         NOT NULL DEFAULT 5 CHECK (stock_minimo >= 0),
    garantia_meses     INTEGER         NOT NULL DEFAULT 12
                       CHECK (garantia_meses BETWEEN 0 AND 60),      -- póliza del fabricante
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

-- Catálogo de países: cada país define sus tasas de IVA (general y reducida).
INSERT INTO paises (codigo_iso2, nombre, tasa_iva_general, tasa_iva_reducido) VALUES
    ('MX', 'México',         16.00,  8.00),   -- IVA frontera norte
    ('CO', 'Colombia',       19.00,  5.00),
    ('PE', 'Perú',           18.00, 18.00),
    ('AR', 'Argentina',      21.00, 10.50),
    ('CL', 'Chile',          19.00, 19.00),
    ('EC', 'Ecuador',        15.00, 15.00),
    ('ES', 'España',         21.00, 10.00),
    ('US', 'Estados Unidos',  0.00,  0.00)    -- sin IVA federal (sales tax estatal)
ON CONFLICT (codigo_iso2) DO NOTHING;

-- Empresas B2B de ejemplo con su país fiscal.
INSERT INTO empresas_clientes (razon_social, rfc, id_pais, regimen_fiscal, tasa_iva, contacto_email) VALUES
    ('Comercializadora del Norte SA de CV', 'CON850101AB1', (SELECT id_pais FROM paises WHERE codigo_iso2='MX'), 'GENERAL',  16.00, 'compras@comer-norte.mx'),
    ('Fundación Educativa Alianza AC',      'FEA120314QP9', (SELECT id_pais FROM paises WHERE codigo_iso2='MX'), 'EXENTO',    0.00, 'admin@fundalianza.org'),
    ('Talleres Mecánicos Ríos SRL',         'TMR180622R4A', (SELECT id_pais FROM paises WHERE codigo_iso2='MX'), 'REDUCIDO', 8.00, 'pagos@talleresrios.mx'),
    ('Cafés de Bogotá SAS',                 'CB901234567',  (SELECT id_pais FROM paises WHERE codigo_iso2='CO'), 'GENERAL', 19.00, 'finanzas@cafesbogota.co')
ON CONFLICT (rfc) DO NOTHING;

INSERT INTO categorias (nombre, descripcion) VALUES
    ('Audio',        'Audífonos, bocinas y micrófonos'),
    ('Computación',  'Teclados, mouses, monitores y laptops'),
    ('Smartphones',  'Celulares, cargadores y power banks'),
    ('Gaming',       'Consolas, controles y accesorios gamer'),
    ('Accesorios',   'Cables, hubs, soportes y fundas')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO productos (id_categoria, id_proveedor, sku, nombre, descripcion, precio_base, stock, stock_minimo, garantia_meses) VALUES
    (1, 1, 'AUD-001', 'Audífonos OneOdio Pro-10 Cable', 'Over-ear 50mm sonido HiFi, micrófono, rosa',   549.00, 25, 5, 12),
    (1, 1, 'AUD-002', 'Audífonos Inalámbricos TWS',     'Bluetooth 5.3, estuche de carga, IPX5',         699.00, 18, 6, 12),
    (1, 3, 'BOC-001', 'Bocina Bluetooth Portátil',      '10W resistente al agua, 12h batería',           459.00, 14, 5,  6),
    (1, 3, 'MIC-001', 'Micrófono Condensador USB',      'Para streaming y grabación, brazo articulado', 1299.00,  8, 3, 12),
    (2, 2, 'TEC-001', 'Teclado Mecánico RGB 60%',       'Switches rojos hot-swap, layout ES',           1150.00, 10, 4, 12),
    (2, 2, 'RAT-001', 'Mouse Inalámbrico Ergonómico',   'Sensor 4000 DPI silencioso, recargable',        349.00, 20, 6, 12),
    (2, 2, 'MON-001', 'Monitor LED 27" 144Hz',          'Full HD, FreeSync Premium, HDMI+DP',           3899.00,  6, 2, 24),
    (2, 2, 'LAP-001', 'Laptop 14" Ryzen 5 16GB',        'SSD 512GB, Windows 11, aluminio',             12499.00,  4, 2, 24),
    (3, 3, 'CEL-001', 'Smartphone 128GB 6.7"',          'Pantalla AMOLED 120Hz, cámara triple',         8999.00,  7, 2, 24),
    (3, 3, 'CAR-001', 'Power Bank 20000mAh',            'Carga rápida 22.5W, doble USB + USB-C',         429.00, 22, 8,  6),
    (3, 3, 'CAR-002', 'Cargador Rápido 65W GaN',        '3 puertos USB-C/USB-A, compacto',               389.00, 16, 6, 12),
    (4, 2, 'GAM-001', 'Control Inalámbrico Pro',        'Gatillos hall effect, RGB, PC/Switch',         1499.00,  9, 3, 12),
    (4, 2, 'GAM-002', 'Audífonos Gaming 7.1',            'Micrófono abatible, luces RGB, USB',            799.00, 11, 4, 12),
    (5, 1, 'ACC-001', 'Hub USB-C 7 en 1',               'HDMI 4K, SD/microSD, 100W PD',                  549.00, 15, 5, 12),
    (5, 1, 'ACC-002', 'Cable HDMI 2.1 4K 2m',           '8K@60Hz certificado, trenzado',                 189.00, 30, 10, 6),
    (5, 1, 'ACC-003', 'Soporte Laptop Ajustable',       'Aluminio, 6 niveles, antideslizante',           299.00, 12, 4,  6)
ON CONFLICT (sku) DO NOTHING;

/* ----------------------------------------------------------------------------
 * 7) ORDENES — cabecera de cada venta (WEB de Cliente o CAJA del Cajero).
 *    Guarda snapshot de país/régimen/tasa aplicados para trazabilidad fiscal.
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS ordenes (
    id_orden           BIGSERIAL       PRIMARY KEY,
    folio              VARCHAR(30)     NOT NULL UNIQUE,                 -- ej. ORD-20260823-153045-K7X
    canal              VARCHAR(10)     NOT NULL
                       CHECK (canal IN ('WEB', 'CAJA')),
    id_usuario         BIGINT          NOT NULL REFERENCES usuarios (id_usuario),
    id_empresa_cliente BIGINT          REFERENCES empresas_clientes (id_empresa),
    id_pais            BIGINT          REFERENCES paises (id_pais),     -- país fiscal de la venta (reportes)
    regimen_fiscal     VARCHAR(20)     NOT NULL
                       CHECK (regimen_fiscal IN ('EXENTO', 'GENERAL', 'REDUCIDO')),
    tasa_iva           NUMERIC(5,2)    NOT NULL CHECK (tasa_iva >= 0 AND tasa_iva <= 100),
    subtotal           NUMERIC(12,2)   NOT NULL CHECK (subtotal >= 0),  -- suma precios base
    iva                NUMERIC(12,2)   NOT NULL CHECK (iva >= 0),
    total              NUMERIC(12,2)   NOT NULL CHECK (total >= subtotal),
    metodo_pago        VARCHAR(20)     NOT NULL DEFAULT 'EFECTIVO'
                       CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    estado             VARCHAR(15)     NOT NULL DEFAULT 'PAGADA'
                       CHECK (estado IN ('PAGADA', 'CANCELADA')),
    fecha_creacion     TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ordenes_fecha   ON ordenes (fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_ordenes_canal   ON ordenes (canal);
CREATE INDEX IF NOT EXISTS idx_ordenes_usuario ON ordenes (id_usuario);
CREATE INDEX IF NOT EXISTS idx_ordenes_empresa ON ordenes (id_empresa_cliente);

/* ----------------------------------------------------------------------------
 * 8) DETALLE_ORDENES — líneas vendidas. Snapshot de SKU/nombre para que el
 *    comprobante siga siendo válido aunque el producto se edite o borre.
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS detalle_ordenes (
    id_detalle      BIGSERIAL      PRIMARY KEY,
    id_orden        BIGINT         NOT NULL REFERENCES ordenes (id_orden),
    id_producto     BIGINT         NOT NULL REFERENCES productos (id_producto),
    sku             VARCHAR(40)    NOT NULL,
    nombre_producto VARCHAR(120)   NOT NULL,
    cantidad        INTEGER        NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12,2)  NOT NULL CHECK (precio_unitario >= 0),
    iva_linea       NUMERIC(12,2)  NOT NULL DEFAULT 0 CHECK (iva_linea >= 0),
    subtotal_linea  NUMERIC(12,2)  NOT NULL CHECK (subtotal_linea >= 0)
);

CREATE INDEX IF NOT EXISTS idx_detalle_orden    ON detalle_ordenes (id_orden);
CREATE INDEX IF NOT EXISTS idx_detalle_producto ON detalle_ordenes (id_producto);

/* ----------------------------------------------------------------------------
 * 9) MOVIMIENTOS_ALMACEN — kardex: toda entrada/salida queda registrada.
 *    ENTRADA = compra a proveedor | SALIDA_VENTA = venta (auto por checkout)
 *    DEVOLUCION = cliente devuelve y vuelve al stock | MERMA = dañado/roto
 *    AJUSTE = corrección manual de inventario.
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS movimientos_almacen (
    id_movimiento   BIGSERIAL      PRIMARY KEY,
    tipo            VARCHAR(15)    NOT NULL
                    CHECK (tipo IN ('ENTRADA', 'SALIDA_VENTA', 'DEVOLUCION', 'MERMA', 'AJUSTE')),
    id_producto     BIGINT         NOT NULL REFERENCES productos (id_producto),
    cantidad        INTEGER        NOT NULL CHECK (cantidad > 0),
    stock_resultante INTEGER       NOT NULL,                       -- auditoría: cómo quedó
    referencia      VARCHAR(60),                                    -- folio de orden / factura proveedor
    nota            VARCHAR(255),
    id_proveedor    BIGINT         REFERENCES proveedores (id_proveedor),
    id_usuario      BIGINT         NOT NULL REFERENCES usuarios (id_usuario), -- quién lo registró
    fecha           TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mov_producto ON movimientos_almacen (id_producto);
CREATE INDEX IF NOT EXISTS idx_mov_fecha    ON movimientos_almacen (fecha);
CREATE INDEX IF NOT EXISTS idx_mov_tipo     ON movimientos_almacen (tipo);

/* ----------------------------------------------------------------------------
 * 10) INCIDENCIAS — productos devueltos, defectuosos o en garantía.
 * -------------------------------------------------------------------------- */
CREATE TABLE IF NOT EXISTS incidencias (
    id_incidencia   BIGSERIAL      PRIMARY KEY,
    tipo            VARCHAR(15)    NOT NULL
                    CHECK (tipo IN ('DEVOLUCION', 'DEFECTO', 'GARANTIA')),
    estado          VARCHAR(15)    NOT NULL DEFAULT 'REPORTADA'
                    CHECK (estado IN ('REPORTADA', 'EN_REVISION', 'RESUELTA', 'CANCELADA')),
    id_producto     BIGINT         NOT NULL REFERENCES productos (id_producto),
    id_orden        BIGINT         REFERENCES ordenes (id_orden),  -- si viene de una venta
    cantidad        INTEGER        NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    descripcion     VARCHAR(500)   NOT NULL,
    reportado_por   BIGINT         NOT NULL REFERENCES usuarios (id_usuario),
    fecha_reporte   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    resolucion      VARCHAR(500)                                   -- qué se hizo al resolverla
);

CREATE INDEX IF NOT EXISTS idx_inc_estado   ON incidencias (estado);
CREATE INDEX IF NOT EXISTS idx_inc_producto ON incidencias (id_producto);

/* ============================================================================
 * MIGRACIÓN para bases creadas antes del cambio "IVA por país".
 * Idempotente: si la BD ya está al día, no altera nada.
 * ========================================================================== */
ALTER TABLE empresas_clientes ADD COLUMN IF NOT EXISTS id_pais BIGINT REFERENCES paises (id_pais);
UPDATE empresas_clientes SET id_pais = (SELECT id_pais FROM paises WHERE codigo_iso2 = 'MX')
WHERE id_pais IS NULL; -- default México para registros previos
ALTER TABLE ordenes ADD COLUMN IF NOT EXISTS id_pais BIGINT REFERENCES paises (id_pais);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS id_pais BIGINT REFERENCES paises (id_pais);
ALTER TABLE productos ADD COLUMN IF NOT EXISTS id_proveedor BIGINT REFERENCES proveedores (id_proveedor);
ALTER TABLE productos ADD COLUMN IF NOT EXISTS garantia_meses INTEGER NOT NULL DEFAULT 12;

-- Catálogo de proveedores electrónicos semilla.
INSERT INTO proveedores (nombre, contacto_nombre, telefono, email) VALUES
    ('AudioMax Distribución', 'Raúl Soto',    '55-1234-5601', 'ventas@audiomax.mx'),
    ('TechSupply Global',     'Lucía Fernández','55-1234-5602', 'pedidos@techsupply.com'),
    ('Importadora ElectroNorte','Mario Cano', '55-1234-5603', 'compras@electronorte.mx')
ON CONFLICT (nombre) DO NOTHING;

COMMIT;

-- Verificación rápida:
SELECT 'roles' AS tabla, count(*) FROM roles
UNION ALL SELECT 'paises',            count(*) FROM paises
UNION ALL SELECT 'proveedores',       count(*) FROM proveedores
UNION ALL SELECT 'empresas_clientes', count(*) FROM empresas_clientes
UNION ALL SELECT 'categorias',        count(*) FROM categorias
UNION ALL SELECT 'productos',         count(*) FROM productos;
