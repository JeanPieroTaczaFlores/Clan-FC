# 🛒 TiendaMenos — E-commerce con Java (Spring Boot) + PostgreSQL

Proyecto educativo construido por entregables con estrategia **"Frontend First"**:
primero la interfaz funciona con datos simulados (`mock_data.json` + Live Server),
luego se conecta a la API REST Java.

## Estructura del proyecto

```
Tiendamenos/
├── frontend/                  # HTML5 + TailwindCSS(CDN) + Vanilla JS
│   ├── index.html             # Vista CLIENTE: catálogo, buscador, filtros, badges stock
│   ├── admin.html             # Vista ADMIN: CRUD inventario (form + tabla)
│   ├── css/styles.css         # Estilos propios (badges de stock)
│   ├── js/config.js           # ⭐ SWITCH mock/API  (CONFIG.USE_API)
│   ├── js/api.js              # Capa de datos: mock_data.json <-> fetch() API REST
│   ├── js/catalogo.js         # Lógica vista cliente
│   ├── js/admin.js            # Lógica vista admin
│   └── data/mock_data.json    # Datos simulados (productos/categorías/perfiles)
├── backend/                   # Spring Boot 4 (Java 17+) con Maven Wrapper
│   └── src/main/java/com/tienda/
│       ├── model/             # Entidades JPA (@Entity) con Lombok
│       ├── repository/        # Repositorios Spring Data JPA
│       ├── dto/               # Records de entrada/salida (validación @Valid)
│       ├── service/           # Lógica de negocio (SKU único, reglas inventario)
│       ├── controller/        # @RestController (productos, categorías)
│       ├── config/            # SecurityConfig (roles) + CORS para Live Server
│       ├── security/          # UserDetailsService contra tabla usuarios
│       ├── exception/         # Manejo global de errores REST
│       └── bootstrap/         # DataLoader: siembra roles + usuarios demo (BCrypt)
└── database/
    └── schema.sql             # DDL PostgreSQL + índices + seed
```

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 17+ (probado con 21) | `java -version` |
| PostgreSQL | 14+ | Solo para conectar backend |
| VS Code | — | Con extensiones de abajo |
| Maven | ❌ no necesario | El proyecto incluye **Maven Wrapper** (`mvnw.cmd`) |

## Extensiones VS Code recomendadas

1. **Extension Pack for Java** (Microsoft) — lenguaje, depuración, Maven.
2. **Spring Boot Extension Pack** (Microsoft/VMware) — soporte Spring.
3. **Live Server** (Ritwick Dey) — servidor local del frontend.
4. *(Opcional)* **PostgreSQL** (Chris Kolkman) — ejecutar SQL desde VS Code.

## FASE A — Ejecutar solo el frontend (modo mock)

1. Abre la carpeta `frontend/` en VS Code (o el proyecto raíz).
2. Click derecho en `index.html` → **"Open with Live Server"**.
3. Se abre `http://127.0.0.1:5500/index.html`:
   - **Vista Cliente:** buscador, filtro por categoría, badges de stock
     (🟢 Disponible / 🟡 Stock bajo / 🔴 Agotado).
   - **Vista Admin:** `admin.html` → formulario alta/edición + tabla inventario.
   - Los cambios del admin se guardan en `localStorage`; botón "↺ Reset datos mock"
     restaura los datos originales.
   - Selector "Rol:" simula sesión hasta implementar login.

## FASE B — Base de datos PostgreSQL

1. Instala PostgreSQL y verifica el servicio:
   ```powershell
   winget install PostgreSQL.PostgreSQL.16
   psql --version
   ```
2. Crea la BD y ejecuta el DDL:
   ```powershell
   psql -U postgres -c "CREATE DATABASE tiendamenos;"
   psql -U postgres -d tiendamenos -f database/schema.sql
   ```
3. El script crea tablas (`roles`, `usuarios`, `empresas_clientes`, `categorias`,
   `productos`), restricciones (UNIQUE/CHECK/FK), índices y datos semilla.
   > Los usuarios los crea el backend al arrancar (`DataLoader`) con BCrypt.

## FASE C — Ejecutar el backend

1. Edita `backend/src/main/resources/application.properties` con tu usuario/contraseña de PostgreSQL.
2. Desde el terminal integrado de VS Code (`Ctrl+ñ` / `` Ctrl+` ``):
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run     # primera vez descarga dependencias (~2-5 min)
   ```
3. Verifica que levantó en `http://localhost:8080`.

### Probar la API (PowerShell)

```powershell
# Catálogo público (GET libre)
curl http://localhost:8080/api/productos

# Crear producto como ADMIN (Basic Auth)
curl -u admin:admin123 -H "Content-Type: application/json" `
  -d '{"sku":"TEST-001","nombre":"Producto Test","precioBase":100,"stock":10,"stockMinimo":2,"categoriaId":1}' `
  http://localhost:8080/api/productos

# Intentar crear como CLIENTE -> debe responder 403 Forbidden
curl -u cliente:cliente123 -X POST -H "Content-Type: application/json" `
  -d '{}' http://localhost:8080/api/productos
```

## FASE D — Conectar frontend ⇄ backend

1. Backend corriendo (Fase C).
2. En `frontend/js/config.js` cambia:
   ```js
   USE_API: true,
   ```
3. Recarga con Live Server: el catálogo y el panel admin ahora consumen la API Java.
   Las credenciales Basic por rol están en `CONFIG.AUTH`.

### Usuarios demo

| Usuario  | Contraseña  | Rol     | Puede                          |
|----------|-------------|---------|--------------------------------|
| admin    | admin123    | ADMIN   | CRUD productos/categorías      |
| cajero   | cajero123   | CAJERO  | POS (Entregable 2)             |
| cliente  | cliente123  | CLIENTE | Catálogo y checkout (Entregable 2) |

## Roadmap de entregables

- [x] **Entregable 1** — Catálogo + BD + CRUD REST + seguridad por roles.
- [ ] **Entregable 2** — Carrito, checkout Cliente, POS Cajero, IVA parametrizable
      (`TaxCalculationService`, checkout transaccional sobre `ordenes`/`detalle_ordenes`).
- [ ] **Entregable 3** — Dashboard Admin, comprobantes PDF (iText), pruebas finales.
