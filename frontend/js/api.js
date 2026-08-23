/* ============================================================================
 * API — Capa de abstracción de datos de TiendaMenos.
 *
 * Estrategia "Frontend First":
 *  - CONFIG.USE_API = false -> lee/escribe sobre mock_data.json (persistido en
 *    localStorage para que el CRUD del Admin sobreviva recargas).
 *  - CONFIG.USE_API = true  -> consume la API REST del backend Spring Boot
 *    mediante fetch() con HTTP Basic Auth.
 *
 * Las vistas (catalogo.js / admin.js) SOLO usan estas funciones; nunca saben
 * si el dato viene del mock o del backend real.
 * ========================================================================== */

/* ------------------------- Utilidades internas ---------------------------- */

/** Cabeceras + URL base para peticiones a la API Java.
 *  Usa SIEMPRE la credencial de la sesión iniciada en login.html. */
function apiHeaders(auth = true) {
  const headers = { "Content-Type": "application/json" };
  if (auth) {
    const sesion = Sesion.obtener();
    if (!sesion || !sesion.credencialBase64) {
      throw new Error("Sin sesión activa: inicia sesión primero");
    }
    headers["Authorization"] = "Basic " + sesion.credencialBase64;
  }
  return headers;
}

async function apiFetch(path, options = {}) {
  const res = await fetch(`${CONFIG.BASE_URL}${path}`, {
    headers: apiHeaders(options.auth !== false),
    ...options,
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`API ${res.status}: ${body || res.statusText}`);
  }
  return res.status === 204 ? null : res.json();
}

/* -------------------- Modo MOCK: mini base de datos local ----------------- */

const MockDB = {
  KEY: "tm_mock_db",

  /** Carga el JSON semilla una sola vez y lo cachea en localStorage. */
  async init() {
    let db = localStorage.getItem(this.KEY);
    if (!db) {
      const seed = await fetch("./data/mock_data.json").then((r) => r.json());
      localStorage.setItem(this.KEY, JSON.stringify(seed));
      db = JSON.stringify(seed);
    }
    return JSON.parse(db);
  },

  save(db) {
    localStorage.setItem(this.KEY, JSON.stringify(db));
  },

  async reset() {
    localStorage.removeItem(this.KEY);
    return this.init();
  },

  nextId(coleccion) {
    const db = this._cache;
    return db[coleccion].reduce((max, x) => Math.max(max, x.idProducto ?? x.idCategoria ?? 0), 0) + 1;
  },
};

/* ------------------------------ AUTENTICACIÓN ----------------------------- */

/**
 * Valida credenciales del formulario de login.
 *  - Modo API: prueba GET /api/auth/login con Basic Auth; Spring Security
 *    responde 401 si son incorrectas y { username, rol } si son válidas.
 *  - Modo mock: compara contra los "perfiles" de mock_data.json.
 * @returns {Promise<{username: string, rol: string}>}
 */
export async function validarLogin(username, password) {
  if (CONFIG.USE_API) {
    const credencialBase64 = btoa(`${username}:${password}`);
    const res = await fetch(`${CONFIG.BASE_URL}/auth/login`, {
      headers: { Authorization: `Basic ${credencialBase64}` },
    });
    if (!res.ok) throw new Error("Usuario o contraseña incorrectos");
    const datos = await res.json();
    return { username: datos.username, rol: datos.rol, credencialBase64 };
  }

  // ---- Modo mock ----
  const db = await MockDB.init();
  const perfil = db.perfiles.find(
    (p) => p.username === username && p.password === password
  );
  if (!perfil) throw new Error("Usuario o contraseña incorrectos");
  return { username: perfil.username, rol: perfil.rol };
}

/* ------------------------------ CATEGORÍAS -------------------------------- */

export const getCategorias = () =>
  CONFIG.USE_API
    ? apiFetch("/categorias")
    : MockDB.init().then((db) => db.categorias.filter((c) => c.activa !== false));

export function crearCategoria(categoria) {
  if (CONFIG.USE_API) return apiFetch("/categorias", { method: "POST", body: JSON.stringify(categoria) });
  return MockDB.init().then((db) => {
    const nueva = { ...categoria, idCategoria: MockDB.nextId("categorias") };
    db.categorias.push(nueva);
    MockDB.save(db);
    return nueva;
  });
}

/* ------------------------------- PRODUCTOS -------------------------------- */

/**
 * Lista productos con filtros opcionales.
 * @param {{busqueda?: string, categoriaId?: number, incluirInactivos?: boolean}} filtros
 */
export async function getProductos(filtros = {}) {
  if (CONFIG.USE_API) {
    const params = new URLSearchParams();
    if (filtros.busqueda) params.set("busqueda", filtros.busqueda);
    if (filtros.categoriaId) params.set("categoriaId", filtros.categoriaId);
    if (filtros.incluirInactivos) params.set("incluirInactivos", "true");
    return apiFetch(`/productos?${params}`);
  }

  let productos = (await MockDB.init()).productos;

  if (!filtros.incluirInactivos) productos = productos.filter((p) => p.activo !== false);
  if (filtros.categoriaId) productos = productos.filter((p) => p.categoriaId === Number(filtros.categoriaId));
  if (filtros.busqueda) {
    const q = filtros.busqueda.toLowerCase();
    productos = productos.filter(
      (p) => p.nombre.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q)
    );
  }
  return structuredClone(productos);
}

export function getProducto(id) {
  if (CONFIG.USE_API) return apiFetch(`/productos/${id}`);
  return MockDB.init().then((db) => {
    const p = db.productos.find((x) => x.idProducto === Number(id));
    if (!p) throw new Error(`Producto ${id} no encontrado`);
    return structuredClone(p);
  });
}

export function crearProducto(producto) {
  if (CONFIG.USE_API) return apiFetch("/productos", { method: "POST", body: JSON.stringify(producto) });
  return MockDB.init().then((db) => {
    const cat = db.categorias.find((c) => c.idCategoria === Number(producto.categoriaId));
    const nuevo = {
      ...producto,
      categoriaNombre: cat ? cat.nombre : "",
      idProducto: MockDB.nextId("productos"),
    };
    db.productos.push(nuevo);
    MockDB.save(db);
    return structuredClone(nuevo);
  });
}

export function actualizarProducto(id, producto) {
  if (CONFIG.USE_API)
    return apiFetch(`/productos/${id}`, { method: "PUT", body: JSON.stringify(producto) });

  return MockDB.init().then((db) => {
    const idx = db.productos.findIndex((x) => x.idProducto === Number(id));
    if (idx < 0) throw new Error(`Producto ${id} no encontrado`);
    const cat = db.categorias.find((c) => c.idCategoria === Number(producto.categoriaId));
    db.productos[idx] = { ...db.productos[idx], ...producto, categoriaNombre: cat ? cat.nombre : "" };
    MockDB.save(db);
    return structuredClone(db.productos[idx]);
  });
}

export function eliminarProducto(id) {
  if (CONFIG.USE_API) return apiFetch(`/productos/${id}`, { method: "DELETE" }).then(() => true);
  return MockDB.init().then((db) => {
    db.productos = db.productos.filter((x) => x.idProducto !== Number(id));
    MockDB.save(db);
    return true;
  });
}
