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
  // v3: catálogo 100% electrónica + proveedores/garantías/kardex.
  KEY: "tm_mock_db_v4",

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

/* ------------------------- SEDES (multi-sucursal) ------------------------- */

/** Obtiene el ID de la sede seleccionada actualmente (localStorage). */
export function getSedeActual() {
  return Number(localStorage.getItem("tm_sede_actual")) || 1;
}

/** Establece la sede activa y la guarda en localStorage. */
export function setSedeActual(idSede) {
  localStorage.setItem("tm_sede_actual", Number(idSede));
}

/** Lista todas las sedes activas. */
export async function getSedes() {
  if (CONFIG.USE_API) return apiFetch("/sedes");
  return MockDB.init().then((db) => (db.sedes ?? []).filter((s) => s.activa !== false));
}

/* ------------------------------ AUTENTICACIÓN ----------------------------- */

/**
 * Valida credenciales del formulario de login.
 *  - Modo API: prueba GET /api/auth/login con Basic Auth; Spring Security
 *    responde 401 si son incorrectas y { username, rol, país } si son válidas.
 *  - Modo mock: compara contra usuarios registrados + "perfiles" semilla.
 * @returns {Promise<{username, rol, paisCodigo?, paisNombre?, banderaEmoji?, credencialBase64}>}
 */
export async function validarLogin(username, password) {
  if (CONFIG.USE_API) {
    const credencialBase64 = btoa(`${username}:${password}`);
    const res = await fetch(`${CONFIG.BASE_URL}/auth/login`, {
      headers: { Authorization: `Basic ${credencialBase64}` },
    });
    if (!res.ok) throw new Error("Usuario o contraseña incorrectos");
    const datos = await res.json();
    return {
      username: datos.username,
      nombreCompleto: datos.nombreCompleto ?? null,
      rol: datos.rol,
      paisCodigo: datos.paisCodigo ?? null,
      paisNombre: datos.paisNombre ?? null,
      banderaEmoji: datos.banderaEmoji ?? "🌐",
      credencialBase64,
    };
  }

  // ---- Modo mock ----
  const db = await MockDB.init();
  // Cuentas registradas por el formulario de registro...
  let cuenta = (db.usuarios ?? []).find(
    (u) => u.username === username && u.password === password && u.activo !== false
  );
  // ...o perfiles semilla (admin/cajero/cliente demo).
  if (!cuenta) {
    const perfil = db.perfiles.find(
      (p) => p.username === username && p.password === password
    );
    if (!perfil) throw new Error("Usuario o contraseña incorrectos");
    cuenta = perfil;
  }
  const pais = (db.paises ?? []).find((p) => p.idPais === Number(cuenta.idPais));
  const sede = (db.sedes ?? []).find((s) => s.idSede === Number(cuenta.sedeId));
  // Establecer sede activa al iniciar sesión
  if (cuenta.sedeId) setSedeActual(cuenta.sedeId);
  return {
    username: cuenta.username,
    nombreCompleto: cuenta.nombreCompleto ?? null,
    rol: cuenta.rol,
    paisCodigo: pais ? pais.codigoIso2 : null,
    paisNombre: pais ? pais.nombre : null,
    banderaEmoji: banderaDe(pais ? pais.codigoIso2 : null),
    sedeId: cuenta.sedeId ?? null,
    sedeNombre: sede ? sede.nombre : null,
    cajaNumero: cuenta.cajaNumero ?? null,
    credencialBase64: btoa(`${username}:${password}`),
  };
}

/**
 * AUTOREGISTRO público: crea una cuenta de tipo CLIENTE con su país fiscal.
 * No requiere sesión (endpoint permitAll en el backend).
 */
export function registrarme(payload) {
  if (CONFIG.USE_API)
    return fetch(`${CONFIG.BASE_URL}/auth/registro`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    }).then(async (res) => {
      if (!res.ok) {
        const body = await res.text().catch(() => "");
        throw new Error(extraerMensaje(body, res.status));
      }
      return res.json();
    });

  // ---- Modo mock ----
  return (async () => {
    const db = await MockDB.init();
    const ocupado =
      (db.perfiles ?? []).some((p) => p.username.toLowerCase() === payload.username.toLowerCase()) ||
      (db.usuarios ?? []).some((u) => u.username.toLowerCase() === payload.username.toLowerCase());
    if (ocupado) throw new Error("Ese nombre de usuario ya está en uso");
    if ((db.usuarios ?? []).some((u) => u.email?.toLowerCase() === payload.email.toLowerCase()))
      throw new Error("Ese correo ya está registrado");

    const pais = (db.paises ?? []).find((p) => p.idPais === Number(payload.idPais));
    if (!pais) throw new Error("Selecciona un país válido");

    db.usuarios = db.usuarios ?? [];
    db.usuarios.push({
      idUsuario: Date.now(),
      username: payload.username,
      email: payload.email,
      password: payload.password, // solo para el mock; el backend real hashea BCrypt
      nombreCompleto: payload.nombreCompleto,
      rol: "CLIENTE", // el autoregistro SIEMPRE crea clientes
      idPais: pais.idPais,
      activo: true,
    });
    MockDB.save(db);
    return { ok: true };
  })();
}

/** Extrae un mensaje legible de un cuerpo de error JSON del backend. */
function extraerMensaje(body, status) {
  try {
    const json = JSON.parse(body);
    return typeof json.error === "string" ? json.error : json.message || `Error ${status}`;
  } catch {
    return body || `Error ${status}`;
  }
}

/* --------------------- USUARIOS (gestión exclusiva ADMIN) ------------------ */

/** Lista cuentas del sistema (sin contraseñas) con rol y bandera de país. */
export async function getUsuarios() {
  if (CONFIG.USE_API) return apiFetch("/usuarios");

  const db = await MockDB.init();
  const desdePerfiles = (db.perfiles ?? []).map((p) => ({
    idUsuario: null,
    username: p.username,
    email: `${p.username}@tiendamenos.demo`,
    nombreCompleto: p.nombreCompleto,
    rol: p.rol,
    idPais: p.idPais ?? null,
    sedeId: p.sedeId ?? null,
    cajaNumero: p.cajaNumero ?? null,
    activo: true,
  }));
  const desdeUsuarios = (db.usuarios ?? []).map((u) => ({
    idUsuario: u.idUsuario,
    username: u.username,
    email: u.email,
    nombreCompleto: u.nombreCompleto,
    rol: u.rol,
    idPais: u.idPais ?? null,
    sedeId: u.sedeId ?? null,
    cajaNumero: u.cajaNumero ?? null,
    activo: u.activo !== false,
  }));
  // Enriquecer con país/bandera/sede antes de devolver.
  const sedes = db.sedes ?? [];
  return [...desdePerfiles, ...desdeUsuarios].map((u) => {
    const pais = (db.paises ?? []).find((p) => p.idPais === Number(u.idPais));
    const sede = sedes.find((s) => s.idSede === Number(u.sedeId));
    return {
      ...u,
      paisNombre: pais ? pais.nombre : null,
      banderaEmoji: banderaDe(pais ? pais.codigoIso2 : null),
      sedeNombre: sede ? sede.nombre : null,
    };
  });
}

/**
 * Alta de usuario con rol específico (CAJERO/ADMIN/CLIENTE).
 * Solo accesible para ADMIN; el backend valida la contraseña/unicidad.
 */
export function crearUsuario(payload, rol) {
  if (CONFIG.USE_API)
    return apiFetch(`/usuarios?rol=${encodeURIComponent(rol)}`, {
      method: "POST",
      body: JSON.stringify(payload),
    });

  return (async () => {
    const db = await MockDB.init();
    const ocupado =
      (db.perfiles ?? []).some((p) => p.username.toLowerCase() === payload.username.toLowerCase()) ||
      (db.usuarios ?? []).some((u) => u.username.toLowerCase() === payload.username.toLowerCase());
    if (ocupado) throw new Error("Ese nombre de usuario ya está en uso");

    const pais = (db.paises ?? []).find((p) => p.idPais === Number(payload.idPais));
    if (!pais) throw new Error("Selecciona un país válido");
    if (!["CLIENTE", "CAJERO", "ADMIN"].includes(rol)) throw new Error("Rol inválido");

    db.usuarios = db.usuarios ?? [];
    db.usuarios.push({
      ...payload,
      idPais: pais.idPais,
      sedeId: payload.sedeId ? Number(payload.sedeId) : null,
      cajaNumero: payload.cajaNumero ? Number(payload.cajaNumero) : null,
      rol,
      activo: true,
      idUsuario: Date.now(),
    });
    MockDB.save(db);
    return { ok: true };
  })();
}

/* ------------------------- PAÍSES / EMPRESAS / FISCAL --------------------- */

/** Catálogo de países activos (selector con banderas en registro de empresas). */
export const getPaises = () =>
  CONFIG.USE_API
    ? apiFetch("/paises")
    : MockDB.init().then((db) => db.paises ?? []);

/**
 * Tasa de IVA de una empresa según su PAÍS + RÉGIMEN (mock del
 * TaxCalculationService). Precedencia: país dinámico > override > defaults.
 */
function resolverTasaMock(db, empresa) {
  const regimen = empresa?.regimenFiscal ?? "GENERAL";
  if (regimen === "EXENTO") return { tasa: 0 };

  const pais = empresa
    ? (db.paises ?? []).find((p) => p.idPais === Number(empresa.idPais))
    : null;
  if (pais) {
    return {
      tasa: regimen === "REDUCIDO" ? Number(pais.tasaIvaReducido) : Number(pais.tasaIvaGeneral),
      pais,
    };
  }
  // Sin país vinculado: override manual guardado o tasas por defecto.
  if (empresa?.tasaIva != null) return { tasa: Number(empresa.tasaIva) };
  return { tasa: TASAS_DEFECTO[regimen] ?? TASAS_DEFECTO.GENERAL };
}

/**
 * Empresas clientes B2B activas, enriquecidas con país y bandera para los
 * selectores del carrito/POS ("🇨🇴 Cafés de Bogotá SAS · GENERAL 19%").
 */
export async function getEmpresas() {
  if (CONFIG.USE_API) return apiFetch("/empresas");

  const db = await MockDB.init();
  return (db.empresas ?? [])
    .map((e) => {
      const { tasa, pais } = resolverTasaMock(db, e);
      return {
        ...e,
        tasaIva: tasa,
        paisCodigo: pais ? pais.codigoIso2 : null,
        paisNombre: pais ? pais.nombre : null,
        banderaEmoji: banderaDe(pais ? pais.codigoIso2 : null),
      };
    })
    .sort((a, b) => String(a.razonSocial).localeCompare(String(b.razonSocial)));
}

/**
 * Registra una empresa cliente B2B con país fiscal (solo ADMIN).
 * La tasa se DERIVA del país elegido — nunca la manda el cliente.
 */
export function crearEmpresa(payload) {
  if (CONFIG.USE_API)
    return apiFetch("/empresas", { method: "POST", body: JSON.stringify(payload) });

  return (async () => {
    const db = await MockDB.init();
    if ((db.empresas ?? []).some((e) => e.rfc.toUpperCase() === payload.rfc.trim().toUpperCase()))
      throw new Error("Ya existe una empresa registrada con ese RFC/NIT");

    const pais = (db.paises ?? []).find((p) => p.idPais === Number(payload.idPais));
    if (!pais) throw new Error("Selecciona un país válido");

    const regimen = payload.regimenFiscal || "GENERAL";
    let tasa;
    if (regimen === "EXENTO") tasa = 0;
    else tasa = regimen === "REDUCIDO" ? Number(pais.tasaIvaReducido) : Number(pais.tasaIvaGeneral);

    const nueva = {
      idEmpresa: Date.now(),
      razonSocial: payload.razonSocial.trim(),
      rfc: payload.rfc.trim().toUpperCase(),
      idPais: pais.idPais,
      regimenFiscal: regimen,
      tasaIva: tasa, // snapshot derivado (respaldo); el país manda dinámicamente
      contactoEmail: payload.contactoEmail || "",
      activo: true,
    };
    db.empresas.push(nueva);
    MockDB.save(db);
    return { ...nueva, paisCodigo: pais.codigoIso2, paisNombre: pais.nombre, banderaEmoji: banderaDe(pais.codigoIso2) };
  })();
}

/* --------------------------- IMPUESTOS EN VIVO ---------------------------- */

/** Tasas por defecto para consumidor final (igual que application.properties). */
const TASAS_DEFECTO = { EXENTO: 0, REDUCIDO: 8, GENERAL: 16 };

/**
 * Calcula el desglose subtotal/IVA/total según PAÍS + RÉGIMEN fiscal.
 * @param {number} subtotalBase suma de precios SIN IVA
 * @param {number|null} empresaClienteId empresa compradora (null = consumidor final)
 * @returns {Promise<{subtotal, tasaIva, regimenFiscal, iva, total,
 *                    paisCodigo, paisNombre, banderaEmoji}>}
 */
/**
 * País fiscal del usuario logueado según la sesión (para consumidor final).
 */
function paisDeSesion(db) {
  const sesion = Sesion.obtener();
  if (!sesion?.paisCodigo) return null;
  return (db.paises ?? []).find((p) => p.codigoIso2 === sesion.paisCodigo) ?? null;
}

export async function calcularImpuestos(subtotalBase, empresaClienteId = null) {
  if (CONFIG.USE_API) {
    return apiFetch("/impuestos/preview", {
      method: "POST",
      body: JSON.stringify({ subtotalBase, empresaClienteId }),
    });
  }

  // ---- Modo mock ----
  const db = await MockDB.init();
  const empresa = (db.empresas ?? []).find((e) => e.idEmpresa === Number(empresaClienteId));
  // Consumidor final: usa el país del usuario logueado si lo tiene.
  const paisSinEmpresa = empresa ? null : paisDeSesion(db);
  let tasaIva;
  let pais;
  if (empresa) ({ tasa: tasaIva, pais } = resolverTasaMock(db, empresa));
  else {
    pais = paisSinEmpresa;
    tasaIva = pais ? Number(pais.tasaIvaGeneral) : TASAS_DEFECTO.GENERAL;
  }
  const regimenFiscal = empresa ? empresa.regimenFiscal : "GENERAL";
  const subtotal = Math.round(subtotalBase * 100) / 100;
  const iva = Math.round(((subtotal * tasaIva) / 100) * 100) / 100;
  return {
    subtotal,
    tasaIva,
    regimenFiscal,
    iva,
    total: Math.round((subtotal + iva) * 100) / 100,
    paisCodigo: pais ? pais.codigoIso2 : null,
    paisNombre: pais ? pais.nombre : null,
    banderaEmoji: banderaDe(pais ? pais.codigoIso2 : null),
  };
}

/* --------------------- CHECKOUT WEB Y COBRO EN CAJA ----------------------- */

/**
 * Genera un folio tipo ORD-yyyyMMdd-HHmmss-XX igual al del backend.
 */
function folioMock() {
  const f = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  const sufijo = Math.random().toString(36).slice(2, 4).toUpperCase();
  return `ORD-${f.getFullYear()}${pad(f.getMonth() + 1)}${pad(f.getDate())}-${pad(f.getHours())}${pad(f.getMinutes())}${pad(f.getSeconds())}-${sufijo}`;
}

/**
 * Procesa la venta (checkout web o cobro de caja).
 * Modo mock simula la transacción completa: valida y descuenta stock,
 * registra la orden en localStorage y devuelve el ticket.
 */
async function procesarVenta(payload, canal, endpointApi) {
  if (CONFIG.USE_API) {
    return apiFetch(endpointApi, { method: "POST", body: JSON.stringify(payload) });
  }

  // ---- Modo mock: transacción simulada sobre MockDB ----
  const sesion = Sesion.obtener();
  if (!sesion) throw new Error("Debes iniciar sesión para completar la compra");

  const db = await MockDB.init();
  const empresa = payload.empresaClienteId
    ? (db.empresas ?? []).find((e) => e.idEmpresa === Number(payload.empresaClienteId))
    : null;
  if (payload.empresaClienteId && !empresa)
    throw new Error("Empresa cliente no encontrada");

  // IVA por país: empresa, o el país del usuario logueado (consumidor final).
  let tasaIva;
  let pais;
  if (empresa) ({ tasa: tasaIva, pais } = resolverTasaMock(db, empresa));
  else {
    pais = paisDeSesion(db);
    tasaIva = pais ? Number(pais.tasaIvaGeneral) : TASAS_DEFECTO.GENERAL;
  }

  let acumuladoBase = 0;
  const items = [];
  const sedeId = getSedeActual();

  for (const item of payload.items) {
    const producto = db.productos.find((p) => p.idProducto === Number(item.productoId));
    if (!producto) throw new Error(`Producto ${item.productoId} no encontrado`);
    const cantidad = Number(item.cantidad);

    // Stock real de la sede actual
    const stockSede = (producto.sedeStock && producto.sedeStock[String(sedeId)] != null)
      ? producto.sedeStock[String(sedeId)]
      : producto.stock;
    if (stockSede < cantidad)
      throw new Error(`Stock insuficiente para "${producto.nombre}" (disponible: ${stockSede})`);

    // Descontar del sedeStock y del stock global
    if (producto.sedeStock) {
      producto.sedeStock[String(sedeId)] = stockSede - cantidad;
    }
    producto.stock -= cantidad;

    const precioUnitario = Number(producto.precioBase);
    const subtotalLinea = Math.round(precioUnitario * cantidad * 100) / 100;
    const ivaLinea = Math.round(((subtotalLinea * tasaIva) / 100) * 100) / 100;

    items.push({
      sku: producto.sku,
      nombreProducto: producto.nombre,
      cantidad,
      precioUnitario,
      ivaLinea,
      subtotalLinea,
    });
    acumuladoBase += subtotalLinea;

    // Kardex: cada venta deja su SALIDA_VENTA (igual que AlmacenService).
    db.movimientos = db.movimientos ?? [];
    db.movimientos.push({
      idMovimiento: Date.now() + Math.floor(Math.random() * 1000),
      tipo: "SALIDA_VENTA",
      productoId: producto.idProducto,
      cantidad,
      stockResultante: producto.stock,
      referencia: "VENTA", // el folio se agrega abajo, cuando existe la orden
      nota: null,
      proveedorId: producto.proveedorId ?? null,
      usuarioNombre: sesion.username,
      fecha: new Date().toISOString(),
    });
  }

  const subtotal = Math.round(acumuladoBase * 100) / 100;
  const iva = Math.round(items.reduce((s, i) => s + i.ivaLinea, 0) * 100) / 100;

  const orden = {
    idOrden: Date.now(),
    folio: folioMock(),
    canal,
    estado: "PAGADA",
    sedeId,
    usuarioNombre: sesion.username,
    empresaId: empresa ? empresa.idEmpresa : null,
    empresaNombre: empresa ? empresa.razonSocial : "Consumidor final",
    paisNombre: pais ? pais.nombre : null,
    banderaEmoji: banderaDe(pais ? pais.codigoIso2 : null),
    regimenFiscal: empresa ? empresa.regimenFiscal : "GENERAL",
    tasaIva,
    subtotal,
    iva,
    total: Math.round((subtotal + iva) * 100) / 100,
    metodoPago: (payload.metodoPago || "EFECTIVO").toUpperCase(),
    // Comprobante de pago móvil (Yape/Plin): captura comprimida + N° de operación.
    yapeComprobante: payload.yapeComprobante || null,
    yapeOperacion: payload.yapeOperacion || null,
    fechaCreacion: new Date().toISOString(),
    items,
  };

  // Persistencia mock de la orden + stocks descontados.
  db.ordenes = db.ordenes ?? [];
  db.ordenes.push(orden);
  // Etiquetar los movimientos de esta venta con su folio real.
  const nuevosMovs = db.movimientos.slice(-items.length);
  nuevosMovs.forEach((m) => (m.referencia = orden.folio));

  // Caja física: los pagos en EFECTIVO engrosan el dinero en caja de la sede.
  if (orden.metodoPago === "EFECTIVO") {
    const key = String(sedeId);
    db.cajas = db.cajas ?? {};
    db.cajas[key] = db.cajas[key] ?? { efectivo: 0 };
    db.cajas[key].efectivo = Math.round((Number(db.cajas[key].efectivo) + orden.total) * 100) / 100;
    db.cajaMovimientos = db.cajaMovimientos ?? [];
    db.cajaMovimientos.push({
      tipo: "VENTA",
      sedeId,
      monto: orden.total,
      nota: `Venta ${orden.folio}`,
      usuarioNombre: sesion.username,
      fecha: new Date().toISOString(),
    });
  }

  MockDB.save(db);
  return orden;
}

/** Checkout del cliente web (canal WEB). */
export function crearCheckout(payload) {
  return procesarVenta(payload, "WEB", "/api/checkout");
}

/** Cobro en punto de venta del cajero (canal CAJA). */
export function crearCobroCaja(payload) {
  return procesarVenta(payload, "CAJA", "/api/pos/cobros");
}

/** Órdenes visibles según rol (ADMIN todas · CAJERO caja · CLIENTE propias). */
export function getOrdenes() {
  if (CONFIG.USE_API) return apiFetch("/ordenes");
  return MockDB.init().then((db) => {
    const sesion = Sesion.obtener();
    const sedeId = getSedeActual();
    const ordenes = db.ordenes ?? [];
    if (!sesion) return [];
    if (sesion.rol === "ADMIN") return [...ordenes].filter((o) => o.sedeId === sedeId || o.sedeId == null).reverse();
    if (sesion.rol === "CAJERO") return [...ordenes].reverse().filter((o) => o.canal === "CAJA" && (o.sedeId === sedeId || o.sedeId == null));
    return [...ordenes].reverse().filter((o) => o.usuarioNombre === sesion.username);
  });
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
  // Aplicar stock de la sede actual
  const sedeId = getSedeActual();
  productos = productos.map((p) => {
    const clone = structuredClone(p);
    if (p.sedeStock && p.sedeStock[String(sedeId)] != null) {
      clone.stock = p.sedeStock[String(sedeId)];
    }
    return clone;
  });
  return productos;
}

export function getProducto(id) {
  if (CONFIG.USE_API) return apiFetch(`/productos/${id}`);
  return MockDB.init().then((db) => {
    const p = db.productos.find((x) => x.idProducto === Number(id));
    if (!p) throw new Error(`Producto ${id} no encontrado`);
    const clone = structuredClone(p);
    const sedeId = getSedeActual();
    if (p.sedeStock && p.sedeStock[String(sedeId)] != null) {
      clone.stock = p.sedeStock[String(sedeId)];
    }
    return clone;
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

/* ================= ALMACÉN · PROVEEDORES · INCIDENCIAS · REPORTES ========= */

/* ------------------------------ PROVEEDORES ------------------------------- */

/** Proveedores activos (para formularios y kardex). */
export const getProveedores = () =>
  CONFIG.USE_API
    ? apiFetch("/proveedores")
    : MockDB.init().then((db) => (db.proveedores ?? []).filter((p) => p.activo !== false));

/** Alta de proveedor (solo ADMIN). */
export function crearProveedor(payload) {
  if (CONFIG.USE_API)
    return apiFetch("/proveedores", { method: "POST", body: JSON.stringify(payload) });

  return MockDB.init().then((db) => {
    const nombre = payload.nombre.trim();
    if ((db.proveedores ?? []).some((p) => p.nombre.toLowerCase() === nombre.toLowerCase()))
      throw new Error("Ya existe un proveedor con ese nombre");
    const nuevo = {
      idProveedor: Date.now(),
      nombre,
      contactoNombre: payload.contactoNombre || "",
      telefono: payload.telefono || "",
      email: payload.email || "",
      activo: true,
    };
    db.proveedores = db.proveedores ?? [];
    db.proveedores.push(nuevo);
    MockDB.save(db);
    return nuevo;
  });
}

/* -------------------------------- ALMACÉN --------------------------------- */

const TIPOS_MOVIMIENTO = ["ENTRADA", "DEVOLUCION", "MERMA", "AJUSTE"];
const TIPOS_SUMAN = new Set(["ENTRADA", "DEVOLUCION"]);

/**
 * Registra un movimiento de almacén (kardex):
 *  - ENTRADA/DEVOLUCION suman stock (mercadería que llega / devolución del cliente)
 *  - MERMA/AJUSTE lo restan (producto dañado o corrección de inventario físico)
 * @returns {Promise<{idMovimiento, tipo, producto, cantidad, stockResultante, ...}>}
 */
export function registrarMovimiento(payload) {
  if (CONFIG.USE_API)
    return apiFetch("/almacen/movimientos", { method: "POST", body: JSON.stringify(payload) });

  return (async () => {
    const sesion = Sesion.obtener();
    if (!sesion) throw new Error("Debes iniciar sesión");

    const tipo = String(payload.tipo ?? "").toUpperCase();
    if (!TIPOS_MOVIMIENTO.includes(tipo))
      throw new Error("Tipo de movimiento inválido (ENTRADA, DEVOLUCION, MERMA o AJUSTE)");
    const cantidad = Number(payload.cantidad);
    if (!Number.isInteger(cantidad) || cantidad < 1) throw new Error("Cantidad inválida");

    const db = await MockDB.init();
    const producto = db.productos.find((p) => p.idProducto === Number(payload.productoId));
    if (!producto) throw new Error("Producto no encontrado");

    if (!TIPOS_SUMAN.has(tipo) && producto.stock < cantidad)
      throw new Error(`Stock insuficiente para descontar (${producto.stock} disponibles)`);

    producto.stock += TIPOS_SUMAN.has(tipo) ? cantidad : -cantidad;

    const mov = {
      idMovimiento: Date.now(),
      tipo,
      productoId: producto.idProducto,
      cantidad,
      stockResultante: producto.stock,
      referencia: payload.referencia?.trim() || null,
      nota: payload.nota?.trim() || null,
      proveedorId: payload.proveedorId ? Number(payload.proveedorId) : null,
      usuarioNombre: sesion.username,
      fecha: new Date().toISOString(),
    };
    db.movimientos = db.movimientos ?? [];
    db.movimientos.push(mov);
    MockDB.save(db);
    return {
      ...mov,
      productoSku: producto.sku,
      productoNombre: producto.nombre,
    };
  })();
}

/** Kardex reciente (últimos 100 movimientos), enriquecido con producto/proveedor. */
export async function getMovimientos() {
  if (CONFIG.USE_API) return apiFetch("/almacen/movimientos");

  const db = await MockDB.init();
  return [...(db.movimientos ?? [])]
    .sort((a, b) => new Date(b.fecha) - new Date(a.fecha))
    .slice(0, 100)
    .map((m) => {
      const p = db.productos.find((x) => x.idProducto === Number(m.productoId));
      const prov = db.proveedores?.find((x) => x.idProveedor === Number(m.proveedorId));
      return {
        ...m,
        productoSku: p ? p.sku : "?",
        productoNombre: p ? p.nombre : `Producto ${m.productoId}`,
        proveedorNombre: prov ? prov.nombre : null,
      };
    });
}

/* ------------------------------ INCIDENCIAS ------------------------------- */

const TIPOS_INCIDENCIA = ["DEVOLUCION", "DEFECTO", "GARANTIA"];

/**
 * Reporta una incidencia de producto:
 *  - DEVOLUCION: el cliente devolvió el producto -> reingresa al stock.
 *  - DEFECTO:    llegó dañado o falló -> se reporta para retiro (merma al resolver).
 *  - GARANTIA:   reclamo dentro del período de garantía -> reposición/merma.
 */
export function crearIncidencia(payload) {
  if (CONFIG.USE_API)
    return apiFetch("/incidencias", { method: "POST", body: JSON.stringify(payload) });

  return (async () => {
    const sesion = Sesion.obtener();
    if (!sesion) throw new Error("Debes iniciar sesión");
    const tipo = String(payload.tipo ?? "").toUpperCase();
    if (!TIPOS_INCIDENCIA.includes(tipo))
      throw new Error("Tipo inválido (DEVOLUCION, DEFECTO o GARANTIA)");
    const cantidad = Number(payload.cantidad);
    if (!Number.isInteger(cantidad) || cantidad < 1) throw new Error("Cantidad inválida");
    if (!payload.descripcion || payload.descripcion.trim().length < 5)
      throw new Error("Describe el problema (mínimo 5 caracteres)");

    const db = await MockDB.init();
    const producto = db.productos.find((p) => p.idProducto === Number(payload.productoId));
    if (!producto) throw new Error("Producto no encontrado");

    const inc = {
      idIncidencia: Date.now(),
      tipo,
      estado: "REPORTADA",
      productoId: producto.idProducto,
      ordenId: payload.ordenId ? Number(payload.ordenId) : null,
      cantidad,
      descripcion: payload.descripcion.trim(),
      resolucion: null,
      reportadoPor: sesion.username,
      fechaReporte: new Date().toISOString(),
      garantiaMeses: producto.garantiaMeses ?? 12,
      productoSku: producto.sku,
      productoNombre: producto.nombre,
    };

    // La DEVOLUCIÓN reingresa al stock con su movimiento de kardex.
    if (tipo === "DEVOLUCION") {
      producto.stock += cantidad;
      db.movimientos = db.movimientos ?? [];
      db.movimientos.push({
        idMovimiento: Date.now() + 1,
        tipo: "DEVOLUCION",
        productoId: producto.idProducto,
        cantidad,
        stockResultante: producto.stock,
        referencia: `INC-${inc.idIncidencia}`,
        nota: inc.descripcion.slice(0, 80),
        proveedorId: producto.proveedorId ?? null,
        usuarioNombre: sesion.username,
        fecha: new Date().toISOString(),
      });
    }

    db.incidencias = db.incidencias ?? [];
    db.incidencias.push(inc);
    MockDB.save(db);
    return inc;
  })();
}

/** Lista todas las incidencias (más recientes primero). */
export async function getIncidencias() {
  if (CONFIG.USE_API) return apiFetch("/incidencias");

  const db = await MockDB.init();
  return [...(db.incidencias ?? [])]
    .sort((a, b) => new Date(b.fechaReporte) - new Date(a.fechaReporte))
    .map((i) => {
      const p = db.productos.find((x) => x.idProducto === Number(i.productoId));
      return {
        ...i,
        productoSku: i.productoSku ?? (p ? p.sku : "?"),
        productoNombre: i.productoNombre ?? (p ? p.nombre : `Producto ${i.productoId}`),
      };
    });
}

/**
 * Cambia el estado de una incidencia (REPORTADA → EN_REVISION → RESUELTA/CANCELADA).
 * Al RESOLVER una GARANTIA o DEFECTO el producto sale del inventario como merma.
 */
export function cambiarEstadoIncidencia(id, estado, resolucion = "") {
  if (CONFIG.USE_API)
    return apiFetch(`/incidencias/${id}/estado`, {
      method: "PATCH",
      body: JSON.stringify({ estado, resolucion }),
    });

  return (async () => {
    const sesion = Sesion.obtener();
    const db = await MockDB.init();
    const inc = db.incidencias?.find((x) => x.idIncidencia === Number(id));
    if (!inc) throw new Error("Incidencia no encontrada");
    if (!["REPORTADA", "EN_REVISION", "RESUELTA", "CANCELADA"].includes(estado))
      throw new Error("Estado inválido");

    inc.estado = estado;
    inc.resolucion = resolucion?.trim() || inc.resolucion;

    // Resolver garantía/defecto => el producto NO vuelve a venta: merma.
    if (
      estado === "RESUELTA" &&
      ["GARANTIA", "DEFECTO"].includes(inc.tipo) &&
      !inc.mermaRegistrada
    ) {
      const producto = db.productos.find((p) => p.idProducto === Number(inc.productoId));
      if (producto && producto.stock >= inc.cantidad) {
        producto.stock -= inc.cantidad;
        db.movimientos = db.movimientos ?? [];
        db.movimientos.push({
          idMovimiento: Date.now() + 2,
          tipo: "MERMA",
          productoId: producto.idProducto,
          cantidad: inc.cantidad,
          stockResultante: producto.stock,
          referencia: `INC-${inc.idIncidencia}`,
          nota: `${inc.tipo} resuelta: ${inc.descripcion.slice(0, 60)}`,
          proveedorId: producto.proveedorId ?? null,
          usuarioNombre: sesion ? sesion.username : "sistema",
          fecha: new Date().toISOString(),
        });
      }
      inc.mermaRegistrada = true;
    }

    MockDB.save(db);
    return inc;
  })();
}

/* ---------------------------- REPORTE SEMANAL ----------------------------- */

const DIAS_SEMANA = ["dom", "lun", "mar", "mié", "jue", "vie", "sáb"];

/**
 * Reporte de los últimos 7 días: ventas por día, top productos vendidos
 * y productos en/bajo su stock mínimo (para surtir).
 */
export async function getReporteSemanal() {
  if (CONFIG.USE_API) return apiFetch("/reportes/semanal");

  const db = await MockDB.init();
  const sesion = Sesion.obtener();
  const sedeId = getSedeActual();

  // Admin (Dirección General) ve TODAS las sedes; cajero/cliente solo la suya.
  const esAdmin = sesion?.rol === "ADMIN";
  const ordenesFiltradas = (db.ordenes ?? []).filter((o) =>
    esAdmin ? true : (o.sedeId === sedeId || o.sedeId == null)
  );

  // --- Ventas por día (7 días, incluido hoy; días sin ventas = S/ 0) ---
  const hoy = new Date();
  hoy.setHours(23, 59, 59, 999);
  const dias = [];
  let totalVentas = 0;
  let numeroOrdenes = 0;

  for (let i = 6; i >= 0; i--) {
    const fin = new Date(hoy.getTime() - i * 24 * 3600 * 1000);
    const inicio = new Date(fin); inicio.setHours(0, 0, 0, 0);
    const ordenesDia = ordenesFiltradas.filter((o) => {
      const f = new Date(o.fechaCreacion);
      return o.estado === "PAGADA" && f >= inicio && f <= fin;
    });
    const totalDia = ordenesDia.reduce((s, o) => s + Number(o.total), 0);
    dias.push({
      fecha: fin.toISOString().slice(0, 10),
      diaSemana: DIAS_SEMANA[fin.getDay()],
      total: Math.round(totalDia * 100) / 100,
      ordenes: ordenesDia.length,
    });
    totalVentas += totalDia;
    numeroOrdenes += ordenesDia.length;
  }
  totalVentas = Math.round(totalVentas * 100) / 100;

  // --- Top productos por unidades vendidas ---
  const acumulado = {};
  for (const orden of ordenesFiltradas) {
    if (orden.estado !== "PAGADA") continue;
    for (const it of orden.items ?? []) {
      acumulado[it.sku] = acumulado[it.sku] ?? {
        sku: it.sku,
        nombre: it.nombreProducto,
        unidades: 0,
        importe: 0,
      };
      acumulado[it.sku].unidades += it.cantidad;
      acumulado[it.sku].importe += it.subtotalLinea;
    }
  }
  const topProductos = Object.values(acumulado)
    .map((t) => ({ ...t, importe: Math.round(t.importe * 100) / 100 }))
    .sort((a, b) => b.unidades - a.unidades)
    .slice(0, 5);

  // --- Bajo stock (en o debajo del mínimo): para pedir al proveedor ---
  const bajoStock = db.productos
    .filter((p) => {
      if (p.activo === false) return false;
      if (esAdmin) {
        // Admin ve si ALGUNA sede está baja
        return Object.keys(p.sedeStock ?? {}).some((sk) => (p.sedeStock[sk] ?? 0) <= p.stockMinimo);
      }
      const stockActual = (p.sedeStock && p.sedeStock[String(sedeId)] != null)
        ? p.sedeStock[String(sedeId)]
        : p.stock;
      return stockActual <= p.stockMinimo;
    })
    .map((p) => {
      if (esAdmin) {
        const sedesBajas = Object.entries(p.sedeStock ?? {})
          .filter(([sk, st]) => (st ?? 0) <= p.stockMinimo)
          .map(([sk]) => {
            const s = (db.sedes ?? []).find((sd) => String(sd.idSede) === sk);
            return s?.nombre ?? `Sede ${sk}`;
          });
        return {
          idProducto: p.idProducto,
          sku: p.sku,
          nombre: p.nombre,
          stock: Object.values(p.sedeStock ?? {}).reduce((a, b) => a + (b ?? 0), 0),
          stockMinimo: p.stockMinimo,
          proveedorNombre: p.proveedorNombre ?? null,
          sedesBajas: sedesBajas.join(", "),
        };
      }
      const stockActual = (p.sedeStock && p.sedeStock[String(sedeId)] != null)
        ? p.sedeStock[String(sedeId)]
        : p.stock;
      return {
        idProducto: p.idProducto,
        sku: p.sku,
        nombre: p.nombre,
        stock: stockActual,
        stockMinimo: p.stockMinimo,
        proveedorNombre: p.proveedorNombre ?? null,
      };
    });

  return {
    totalVentas,
    numeroOrdenes,
    ticketPromedio: numeroOrdenes ? Math.round((totalVentas / numeroOrdenes) * 100) / 100 : 0,
    dias,
    topProductos,
    bajoStock,
  };
}

/* --------------------------- CAJA FÍSICA (EFECTIVO) ----------------------- */

/** Estado del dinero en caja + últimos movimientos (ventas, fondos, retiros) para la sede actual. */
export async function getCaja() {
  const db = await MockDB.init();
  const sedeId = getSedeActual();
  const cajas = db.cajas ?? {};
  const caja = cajas[String(sedeId)] ?? { efectivo: 0 };
  return {
    efectivo: Number(caja.efectivo),
    movimientos: [...(db.cajaMovimientos ?? [])]
      .filter((m) => m.sedeId === sedeId || m.sedeId == null)
      .sort((a, b) => new Date(b.fecha) - new Date(a.fecha))
      .slice(0, 20),
  };
}

/**
 * Movimiento de caja manual (por sede):
 *  - FONDO: el cajero agrega efectivo (cambio para empezar el día, solicitar más).
 *  - RETIRO: saca dinero (a bóveda/depósito) — no puede dejar la caja negativa.
 */
export async function registrarMovimientoCaja(tipo, monto, nota) {
  const sesion = Sesion.obtener();
  if (!sesion) throw new Error("Debes iniciar sesión");
  if (!["FONDO", "RETIRO"].includes(tipo)) throw new Error("Tipo de movimiento inválido");
  const m = Math.round(Number(monto) * 100) / 100;
  if (!Number.isFinite(m) || m <= 0) throw new Error("Monto inválido");

  const sedeId = getSedeActual();
  const db = await MockDB.init();
  db.cajas = db.cajas ?? {};
  const key = String(sedeId);
  db.cajas[key] = db.cajas[key] ?? { efectivo: 0 };
  if (tipo === "RETIRO" && m > Number(db.cajas[key].efectivo))
    throw new Error(`No hay suficiente efectivo en caja (S/ ${Number(db.cajas[key].efectivo).toFixed(2)})`);

  db.cajas[key].efectivo = Math.round(
    (tipo === "FONDO" ? Number(db.cajas[key].efectivo) + m : Number(db.cajas[key].efectivo) - m) * 100
  ) / 100;

  db.cajaMovimientos = db.cajaMovimientos ?? [];
  db.cajaMovimientos.push({
    tipo,
    sedeId,
    monto: m,
    nota: (nota || "").trim() || null,
    usuarioNombre: sesion.username,
    fecha: new Date().toISOString(),
  });
  MockDB.save(db);
  return { efectivo: db.cajas[key].efectivo };
}

/* ---------------------------- LO MÁS POPULAR ------------------------------ */

/**
 * Productos más vendidos (por unidades, todas las órdenes PAGADAS).
 * Si aún no hay ventas, rellena con los primeros productos activos para que
 * la sección del inicio nunca se vea vacía.
 */
export async function getProductosPopulares(limite = 8) {
  const db = await MockDB.init();
  const sedeId = getSedeActual();

  const unidadesPorSku = {};
  for (const orden of (db.ordenes ?? []).filter((o) => o.sedeId === sedeId || o.sedeId == null)) {
    if (orden.estado !== "PAGADA") continue;
    for (const it of orden.items ?? []) {
      unidadesPorSku[it.sku] = (unidadesPorSku[it.sku] ?? 0) + it.cantidad;
    }
  }

  const populares = db.productos
    .filter((p) => {
      if (p.activo === false) return false;
      const stockActual = (p.sedeStock && p.sedeStock[String(sedeId)] != null)
        ? p.sedeStock[String(sedeId)]
        : p.stock;
      return stockActual > 0;
    })
    .map((p) => {
      const stockActual = (p.sedeStock && p.sedeStock[String(sedeId)] != null)
        ? p.sedeStock[String(sedeId)]
        : p.stock;
      return { ...p, stock: stockActual, unidadesVendidas: unidadesPorSku[p.sku] ?? 0 };
    })
    .sort((a, b) => b.unidadesVendidas - a.unidadesVendidas)
    .slice(0, limite);

  return structuredClone(populares);
}

/* ======================== DASHBOARD EJECUTIVO AVANZADO ===================== */

/**
 * Comparación consolidada de todas las sedes.
 * Devuelve un array con métricas por sede: ventas totales, número de órdenes,
 * ticket promedio, efectivo en caja, incidencias abiertas, y productos con stock bajo.
 */
export async function getComparacionSedes() {
  const db = await MockDB.init();
  const sedes = (db.sedes ?? []).filter((s) => s.activa !== false);
  const ordenes = db.ordenes ?? [];
  const incidencias = db.incidencias ?? [];
  const productos = db.productos ?? [];
  const cajas = db.cajas ?? {};

  return sedes.map((sede) => {
    const ordenesSede = ordenes.filter((o) => o.sedeId === sede.idSede);
    const pagadas = ordenesSede.filter((o) => o.estado === "PAGADA");
    const totalVentas = pagadas.reduce((sum, o) => sum + Number(o.total), 0);
    const ticketPromedio = pagadas.length ? Math.round((totalVentas / pagadas.length) * 100) / 100 : 0;
    const incidenciasAbiertas = incidencias.filter(
      (i) => i.sedeId === sede.idSede && i.estado !== "RESUELTA"
    );
    const stockBajo = productos.filter((p) => {
      const stock = p.sedeStock?.[String(sede.idSede)] ?? 0;
      return stock > 0 && stock <= (p.stockMinimo ?? 0);
    });
    const agotados = productos.filter((p) => {
      const stock = p.sedeStock?.[String(sede.idSede)] ?? 0;
      return stock === 0;
    });
    const efectivo = Number(cajas[String(sede.idSede)]?.efectivo ?? 0);

    // Métodos de pago
    const pagos = {};
    pagadas.forEach((o) => { pagos[o.metodoPago] = (pagos[o.metodoPago] ?? 0) + 1; });

    return {
      idSede: sede.idSede,
      nombre: sede.nombre,
      direccion: sede.direccion,
      telefono: sede.telefono,
      totalVentas: Math.round(totalVentas * 100) / 100,
      numOrdenes: ordenesSede.length,
      ordenesPagadas: pagadas.length,
      ticketPromedio,
      efectivo,
      incidenciasAbiertas: incidenciasAbiertas.length,
      incidenciasCriticas: incidenciasAbiertas.filter((i) => i.tipo === "DAÑO").length,
      stockBajo: stockBajo.length,
      agotados: agotados.length,
      metodosPago: pagos,
    };
  });
}

/**
 * Alertas de stock consolidadas de todas las sedes.
 * Devuelve un array de objetos con severidad: CRITICA, ALERTA, INFO.
 */
export async function getAlertasStock() {
  const db = await MockDB.init();
  const productos = db.productos ?? [];
  const sedes = (db.sedes ?? []).filter((s) => s.activa !== false);
  const alertas = [];

  for (const p of productos) {
    if (p.activo === false) continue;
    for (const sede of sedes) {
      const stock = p.sedeStock?.[String(sede.idSede)] ?? 0;
      if (stock === 0) {
        alertas.push({
          severidad: "CRITICA",
          producto: p.nombre,
          sku: p.sku,
          sede: sede.nombre,
          sedeId: sede.idSede,
          stock,
          stockMinimo: p.stockMinimo,
          mensaje: `"${p.nombre}" agotado en ${sede.nombre}`,
        });
      } else if (stock <= p.stockMinimo) {
        alertas.push({
          severidad: "ALERTA",
          producto: p.nombre,
          sku: p.sku,
          sede: sede.nombre,
          sedeId: sede.idSede,
          stock,
          stockMinimo: p.stockMinimo,
          mensaje: `"${p.nombre}" con stock bajo (${stock}/${p.stockMinimo}) en ${sede.nombre}`,
        });
      }
    }
  }

  // Agregar incidencias abiertas como alertas
  const incidencias = (db.incidencias ?? []).filter((i) => i.estado !== "RESUELTA");
  for (const inc of incidencias) {
    const sede = sedes.find((s) => s.idSede === inc.sedeId);
    alertas.push({
      severidad: inc.tipo === "DAÑO" ? "CRITICA" : "ALERTA",
      producto: inc.productoNombre ?? inc.sku,
      sku: inc.sku,
      sede: sede?.nombre ?? `Sede #${inc.sedeId}`,
      sedeId: inc.sedeId,
      stock: 0,
      stockMinimo: 0,
      mensaje: `[${inc.tipo}] ${inc.descripcion?.slice(0, 80) ?? inc.productoNombre} (${sede?.nombre ?? "?"})`,
    });
  }

  // Ordenar: CRITICA primero
  const orden = { CRITICA: 0, ALERTA: 1, INFO: 2 };
  return alertas.sort((a, b) => (orden[a.severidad] ?? 9) - (orden[b.severidad] ?? 9));
}

/**
 * Resumen financiero consolidado de todas las sedes.
 * Incluye ingresos por sede, movimientos de caja, y métricas derivadas.
 */
export async function getResumenFinanciero() {
  const db = await MockDB.init();
  const sedes = (db.sedes ?? []).filter((s) => s.activa !== false);
  const ordenes = db.ordenes ?? [];
  const movimientos = db.cajaMovimientos ?? [];
  const cajas = db.cajas ?? {};

  const porSede = sedes.map((sede) => {
    const ordenesSede = ordenes.filter((o) => o.sedeId === sede.idSede && o.estado === "PAGADA");
    const ingresos = ordenesSede.reduce((sum, o) => sum + Number(o.total), 0);
    const ivaRecaudado = ordenesSede.reduce((sum, o) => sum + Number(o.iva), 0);
    const movs = movimientos.filter((m) => m.sedeId === sede.idSede);
    const fondos = movs.filter((m) => m.tipo === "FONDO").reduce((s, m) => s + m.monto, 0);
    const retiros = movs.filter((m) => m.tipo === "RETIRO").reduce((s, m) => s + m.monto, 0);
    const ventasEfectivo = movs.filter((m) => m.tipo === "VENTA").reduce((s, m) => s + m.monto, 0);

    return {
      idSede: sede.idSede,
      nombre: sede.nombre,
      ingresos,
      ivaRecaudado,
      efectivoActual: Number(cajas[String(sede.idSede)]?.efectivo ?? 0),
      fondosRecibidos: fondos,
      retirosRealizados: retiros,
      ventasEfectivo,
      movimientosTotales: movs.length,
    };
  });

  return {
    sedes: porSede,
    consolidado: {
      ingresosTotales: porSede.reduce((s, p) => s + p.ingresos, 0),
      ivaTotal: porSede.reduce((s, p) => s + p.ivaRecaudado, 0),
      efectivoTotal: porSede.reduce((s, p) => s + p.efectivoActual, 0),
      retirosTotales: porSede.reduce((s, p) => s + p.retirosRealizados, 0),
    },
  };
}
