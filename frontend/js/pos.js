/* ============================================================================
 * pos.js — PUNTO DE VENTA (POS) para el rol CAJERO (Entregable 2).
 *
 * Flujo de cobro en caja:
 *  1. El cajero toca productos (o busca por SKU) para armar el ticket.
 *  2. Selecciona cliente/empresa -> la tasa de IVA se recalcula EN VIVO con
 *     la parametrización fiscal real (api.calcularImpuestos).
 *  3. "COBRAR" llama a api.crearCobroCaja: valida/descuenta stock y registra
 *     la orden con canal CAJA dentro de una transacción del backend.
 * ========================================================================== */

import {
  getProductos, getCategorias, getEmpresas, calcularImpuestos, crearCobroCaja,
  getProveedores, registrarMovimiento, getMovimientos,
  crearIncidencia, getIncidencias, cambiarEstadoIncidencia,
  getReporteSemanal, getOrdenes, getCaja, registrarMovimientoCaja,
  getSedeActual, getSedes,
} from "./api.js";

/* ------------------- Guardia de acceso CAJERO / ADMIN --------------------- */
const sesion = Sesion.obtener();
if (!sesion || !["CAJERO", "ADMIN"].includes(sesion.rol)) {
  document.body.innerHTML = `
    <main class="min-h-screen bg-slate-900 flex items-center justify-center px-4">
      <div class="bg-white rounded-3xl shadow-2xl p-10 text-center max-w-md">
        <p class="text-5xl mb-4">🔒</p>
        <h1 class="text-xl font-extrabold text-slate-800 mb-2">Acceso restringido</h1>
        <p class="text-sm text-slate-500 mb-6">
          ${sesion ? `Tu rol <b>${sesion.rol}</b> no puede operar la caja.` : "No has iniciado sesión."}
          Se requiere rol <b>CAJERO</b> o <b>ADMIN</b>.
        </p>
        <a href="login.html" class="inline-block bg-emerald-600 hover:bg-emerald-700 text-white font-semibold px-5 py-2.5 rounded-xl transition">
          Ir a iniciar sesión
        </a>
      </div>
    </main>`;
  throw new Error("403: POS requiere rol CAJERO o ADMIN");
}

const $grid = document.getElementById("pos-grid");
const $buscador = document.getElementById("pos-buscador");
const $selectCat = document.getElementById("pos-categoria");
const $selectCliente = document.getElementById("pos-cliente");
const $lineas = document.getElementById("pos-lineas");
const $vacioTicket = document.getElementById("pos-vacio-ticket");
const $btnCobrar = document.getElementById("btn-cobrar");

const fmt = (n) => "S/ " + Number(n).toLocaleString("es-PE", { minimumFractionDigits: 2 });
// Catálogo enfocado en ELECTRÓNICA: un emoji representativo por categoría.
const EMOJIS = {
  Audio: ["🎧", "🎙️", "🔊", "🎵"],
  Computacion: ["💻", "🖥️", "⌨️", "🖱️"],
  Smartphones: ["📱", "📲", "🔌", "🛰️"],
  Gaming: ["🎮", "🕹️", "👾", "🏆"],
  Accesorios: ["🔋", "🧵", "📡", "🧰"],
};
const emojiDe = (p) => (EMOJIS[p.categoriaNombre] ?? ["📦"])[p.idProducto % 4];

let catalogo = [];            // productos activos disponibles para vender
let ticket = [];              // líneas actuales [{productoId, cantidad, nombre, precioBase}]
let metodoPago = "EFECTIVO";  // método seleccionado
let cajaHabilitada = false;   // ¿la caja ya fue habilitada con fondo inicial?

const $posCajero = document.getElementById("pos-cajero");
const $posSede = document.getElementById("pos-sede");
const $posCajaNum = document.getElementById("pos-caja-num");

// Mostrar info del cajero, sede y número de caja
$posCajero.textContent = `Cajero: ${sesion.username}`;
if (sesion.sedeNombre) {
  $posSede.textContent = `📍 ${sesion.sedeNombre}`;
  $posSede.classList.remove("hidden");
}
if (sesion.cajaNumero != null) {
  $posCajaNum.textContent = `🧾 Caja #${sesion.cajaNumero}`;
  $posCajaNum.classList.remove("hidden");
}

// Reloj de caja (ambiente POS).
setInterval(() => {
  document.getElementById("pos-reloj").textContent =
    new Date().toLocaleString("es-PE", { dateStyle: "medium", timeStyle: "short" });
}, 1000);

/* ------------------------------ Ticket ------------------------------------ */

function agregarAlTicket(producto) {
  const existente = ticket.find((t) => t.productoId === producto.idProducto);
  if (existente) {
    if (existente.cantidad >= producto.stock)
      return mostrarToast(`Sin más stock de "${producto.nombre}"`, "error");
    existente.cantidad++;
  } else {
    if (producto.stock < 1) return mostrarToast(`"${producto.nombre}" agotado`, "error");
    ticket.push({
      productoId: producto.idProducto,
      cantidad: 1,
      nombre: producto.nombre,
      sku: producto.sku,
      precioBase: Number(producto.precioBase),
      emoji: emojiDe(producto),
      stockMax: producto.stock,
    });
  }
  renderTicket();
}

function renderTicket() {
  $vacioTicket.classList.toggle("hidden", ticket.length > 0);
  $btnCobrar.disabled = ticket.length === 0;

  $lineas.innerHTML = ticket.map((t) => `
    <li class="px-4 py-2.5 flex items-center gap-3 hover:bg-slate-50">
      <span class="text-xl">${t.emoji}</span>
      <div class="flex-1 min-w-0">
        <p class="font-semibold truncate text-[13px]">${t.nombre}</p>
        <p class="text-[11px] text-slate-400">${fmt(t.precioBase)} c/u</p>
      </div>
      <div class="flex items-center gap-1">
        <button data-restar="${t.productoId}" class="w-6 h-6 rounded-full border border-slate-300 text-slate-600 font-bold hover:bg-slate-100 transition">−</button>
        <span class="w-7 text-center font-bold">${t.cantidad}</span>
        <button data-sumar="${t.productoId}" class="w-6 h-6 rounded-full border border-slate-300 text-slate-600 font-bold hover:bg-slate-100 transition">+</button>
      </div>
      <span class="w-20 text-right font-bold text-[13px]">${fmt(t.precioBase * t.cantidad)}</span>
    </li>`).join("");

  recalcularTotales();
}

async function recalcularTotales() {
  const base = ticket.reduce((s, t) => s + t.precioBase * t.cantidad, 0);
  const empresaId = $selectCliente.value || null;
  // IVA dinámico según parametrización del cliente (mock o backend Java).
  const t = await calcularImpuestos(base, empresaId);

  document.getElementById("pos-subtotal").textContent = fmt(t.subtotal);
  document.getElementById("pos-tasa").textContent = Number(t.tasaIva).toFixed(2);
  document.getElementById("pos-iva").textContent = fmt(t.iva);
  document.getElementById("pos-total").textContent = fmt(t.total);
  // País + régimen aplicados (con bandera) bajo los totales.
  document.getElementById("pos-regimen").textContent =
    `${t.banderaEmoji ?? ""} ${t.paisNombre ?? ""} · ${t.regimenFiscal}`.replace(/\s+/g, " ").trim();
}

/* ------------------------------ Catálogo rápido --------------------------- */

function renderCatalogo() {
  const q = $buscador.value.trim().toLowerCase();
  let lista = catalogo.filter((p) => p.stock > 0); // en caja solo se vende con stock
  if ($selectCat.value) lista = lista.filter((p) => p.categoriaId === Number($selectCat.value));
  if (q) lista = lista.filter((p) => p.nombre.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q));

  $grid.innerHTML = lista.map((p) => `
    <button data-agregar="${p.idProducto}"
            class="bg-white text-slate-800 rounded-xl p-3 text-left hover:ring-2 hover:ring-emerald-500 active:scale-95 transition shadow-sm">
      <span class="text-3xl block mb-1">${emojiDe(p)}</span>
      <p class="text-[11px] font-semibold leading-tight line-clamp-2 min-h-[2rem]">${p.nombre}</p>
      <p class="mt-1 flex justify-between items-baseline">
        <b class="text-emerald-600 text-sm">${fmt(p.precioBase)}</b>
        <span class="text-[9px] text-slate-400">${p.stock} u.</span>
      </p>
    </button>`).join("");
}

/* -------------------------------- Cobro ---------------------------------- */

async function cobrar() {
  if (!ticket.length) return;
  if (!cajaHabilitada) {
    document.getElementById("modal-habilitar").classList.remove("hidden");
    document.getElementById("hab-sede").textContent = sesion.sedeNombre || "Sin sede";
    document.getElementById("hab-caja").textContent = sesion.cajaNumero != null ? `#${sesion.cajaNumero}` : "Sin asignar";
    document.getElementById("hab-cajero").textContent = `${sesion.username} (${sesion.rol})`;
    return mostrarToast("Debes habilitar la caja antes de cobrar", "error");
  }

  // Pago móvil (Yape): sin captura del comprobante no se permite cobrar.
  if (metodoPago === "YAPE" && !yapeComprobante)
    return mostrarToast("Adjunta la captura del comprobante Yape", "error");

  $btnCobrar.disabled = true;
  $btnCobrar.textContent = "PROCESANDO…";

  try {
    const payload = {
      items: ticket.map((t) => ({ productoId: t.productoId, cantidad: t.cantidad })),
      empresaClienteId: $selectCliente.value ? Number($selectCliente.value) : null,
      metodoPago,
    };
    if (metodoPago === "YAPE") {
      payload.yapeComprobante = yapeComprobante;
      payload.yapeOperacion =
        document.getElementById("yape-operacion").value.trim() || null;
    }
    const orden = await crearCobroCaja(payload); // transacción canal CAJA

    document.getElementById("exito-folio").textContent = orden.folio;
    document.getElementById("exito-cliente").textContent =
      `${orden.banderaEmoji ?? ""} ${orden.empresaNombre}`.trim();
    document.getElementById("exito-iva").textContent = fmt(orden.iva);
    const $exitoTotal = document.getElementById("exito-total");
    $exitoTotal.textContent = fmt(orden.total);
    // Aviso de cuánto entra en efectivo a la caja física.
    if (orden.metodoPago === "EFECTIVO")
      mostrarToast(`💵 +${fmt(orden.total)} entraron a la caja`, "ok");

    document.getElementById("pos-modal-exito").classList.remove("hidden");

    ticket = [];
    limpiarYape();
    await cargarProductos(); // refresca stocks tras descontar
    renderTicket();
    refrescarChipCaja();
  } catch (err) {
    console.error(err);
    mostrarToast(err.message.replace(/^API \d+:.*"error":"|".*$/s, "") || err.message, "error");
  } finally {
    $btnCobrar.disabled = false;
    $btnCobrar.textContent = "COBRAR";
  }
}

/* -------------------------------- Eventos --------------------------------- */

$grid.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-agregar]");
  if (!btn) return;
  const producto = catalogo.find((p) => String(p.idProducto) === btn.dataset.agregar);
  if (producto) agregarAlTicket(producto);
});

$lineas.addEventListener("click", (e) => {
  const sumar = e.target.closest("[data-sumar]");
  const restar = e.target.closest("[data-restar]");
  if (!sumar && !restar) return;

  const id = Number((sumar ?? restar).dataset.sumar ?? restar.dataset.restar);
  const linea = ticket.find((t) => t.productoId === id);
  linea.cantidad += sumar ? 1 : -1;
  if (linea.cantidad <= 0) ticket = ticket.filter((t) => t !== linea);
  renderTicket();
});

let timerBusqueda;
$buscador.addEventListener("input", () => {
  clearTimeout(timerBusqueda);
  timerBusqueda = setTimeout(renderCatalogo, 200);
});
$selectCat.addEventListener("change", renderCatalogo);
$selectCliente.addEventListener("change", recalcularTotales);

// Selector visual de método de pago.
const $metodos = document.querySelectorAll(".metodo-btn");
function pintarMetodos() {
  $metodos.forEach((b) => {
    const activo = b.dataset.metodo === metodoPago;
    b.className = `metodo-btn py-2 rounded-xl border-2 transition ${
      activo ? "border-emerald-600 bg-emerald-50 text-emerald-700"
             : "border-slate-200 text-slate-500 hover:border-slate-400"}`;
  });
  // El panel de comprobante solo aplica a pagos móviles (Yape).
  document.getElementById("panel-yape").classList.toggle("hidden", metodoPago !== "YAPE");
}
$metodos.forEach((b) =>
  b.addEventListener("click", () => { metodoPago = b.dataset.metodo; pintarMetodos(); })
);

/* --------------------- Comprobante Yape (captura del QR) ------------------ */

let yapeComprobante = null; // dataURL comprimido de la captura

document.getElementById("yape-captura").addEventListener("change", async (e) => {
  const archivo = e.target.files?.[0];
  if (!archivo) return;
  try {
    yapeComprobante = await comprimirImagen(archivo, 520, 0.72);
    const $prev = document.getElementById("yape-preview");
    $prev.src = yapeComprobante;
    $prev.classList.remove("hidden");
    mostrarToast("Captura adjuntada ✔", "ok");
  } catch (err) {
    console.error(err);
    mostrarToast("No se pudo leer la imagen", "error");
  }
});

/** Reduce la foto/captura a un JPEG pequeño para guardarla con la venta. */
function comprimirImagen(archivo, maxLado, calidad) {
  return new Promise((resolve, reject) => {
    const lector = new FileReader();
    lector.onerror = reject;
    lector.onload = () => {
      const img = new Image();
      img.onerror = reject;
      img.onload = () => {
        const escala = Math.min(1, maxLado / Math.max(img.width, img.height));
        const canvas = document.createElement("canvas");
        canvas.width = Math.round(img.width * escala);
        canvas.height = Math.round(img.height * escala);
        canvas.getContext("2d").drawImage(img, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL("image/jpeg", calidad));
      };
      img.src = lector.result;
    };
    lector.readAsDataURL(archivo);
  });
}

function limpiarYape() {
  yapeComprobante = null;
  document.getElementById("yape-captura").value = "";
  document.getElementById("yape-operacion").value = "";
  document.getElementById("yape-preview").classList.add("hidden");
}

document.getElementById("pos-limpiar").addEventListener("click", () => {
  ticket = [];
  renderTicket();
});

$btnCobrar.addEventListener("click", cobrar);

document.getElementById("btn-nueva-venta").addEventListener("click", () => {
  document.getElementById("pos-modal-exito").classList.add("hidden");
});

/* ================= PESTAÑAS: VENTA · ALMACÉN · INCIDENCIAS · REPORTE ====== */

const $paneles = {
  venta: document.getElementById("panel-venta"),
  almacen: document.getElementById("panel-almacen"),
  incidencias: document.getElementById("panel-incidencias"),
  reporte: document.getElementById("panel-reporte"),
};

function mostrarTab(nombre) {
  Object.entries($paneles).forEach(([clave, panel]) => {
    panel.classList.toggle("hidden", clave !== nombre);
    // "grid" se conserva; solo alternamos hidden.
  });
  document.querySelectorAll(".pos-tab").forEach((btn) => {
    const activo = btn.dataset.tab === nombre;
    btn.classList.toggle("border-emerald-400", activo);
    btn.classList.toggle("text-white", activo);
    btn.classList.toggle("border-transparent", !activo);
    btn.classList.toggle("text-slate-400", !activo);
    btn.classList.toggle("hover:text-slate-200", !activo);
  });

  // Carga perezosa de datos al entrar a cada pestaña.
  if (nombre === "almacen") refrescarKardex();
  if (nombre === "incidencias") refrescarIncidencias();
  if (nombre === "reporte") renderReporte();
}

document.querySelectorAll(".pos-tab").forEach((btn) =>
  btn.addEventListener("click", () => mostrarTab(btn.dataset.tab))
);

/* ------------------------------ ALMACÉN ----------------------------------- */

let proveedores = [];

async function llenarSelectsAlmacen() {
  const [proveedoresLista] = await Promise.all([getProveedores()]);
  proveedores = proveedoresLista;

  const $selProd = document.querySelector('#form-movimiento select[name="productoId"]');
  const $selInc = document.querySelector('#form-incidencia select[name="productoId"]');
  const opciones = catalogo
    .map((p) => `<option value="${p.idProducto}">${p.sku} · ${p.nombre} (stock ${p.stock})</option>`)
    .join("");
  // Idempotente: conserva solo la opción "Selecciona…" y vuelve a pintar.
  [$selProd, $selInc].forEach((sel) => {
    sel.innerHTML = sel.options[0]?.outerHTML ?? "";
    sel.insertAdjacentHTML("beforeend", opciones);
    sel.value = "";
  });

  const $selProv = document.querySelector('#form-movimiento select[name="proveedorId"]');
  proveedores.forEach((prov) =>
    $selProv.insertAdjacentHTML(
      "beforeend",
      `<option value="${prov.idProveedor}">${prov.nombre}</option>`
    )
  );
}

const ESTILO_MOV = {
  ENTRADA: "bg-emerald-500/15 text-emerald-400",
  DEVOLUCION: "bg-sky-500/15 text-sky-400",
  MERMA: "bg-rose-500/15 text-rose-400",
  AJUSTE: "bg-amber-500/15 text-amber-400",
  SALIDA_VENTA: "bg-slate-500/20 text-slate-300",
};
const ICONO_MOV = { ENTRADA: "📥", DEVOLUCION: "↩️", MERMA: "🗑️", AJUSTE: "🔧", SALIDA_VENTA: "🛒" };

async function refrescarKardex() {
  const movs = await getMovimientos();
  const tbody = document.getElementById("tbody-kardex");

  tbody.innerHTML = movs.length
    ? movs
        .map((m) => `
      <tr class="hover:bg-slate-700/30">
        <td class="px-3 py-2 whitespace-nowrap text-slate-400">${new Date(m.fecha).toLocaleString("es-PE", { dateStyle: "short", timeStyle: "short" })}</td>
        <td class="px-3 py-2">
          <span class="px-2 py-0.5 rounded-full font-bold ${ESTILO_MOV[m.tipo] ?? ""}">${ICONO_MOV[m.tipo] ?? ""} ${m.tipo}</span>
        </td>
        <td class="px-3 py-2"><span class="font-mono text-[10px] text-slate-500">${m.productoSku ?? ""}</span><br/>${m.productoNombre ?? ""}</td>
        <td class="px-3 py-2 text-right font-bold">${["ENTRADA", "DEVOLUCION"].includes(m.tipo) ? "+" : "−"}${m.cantidad}</td>
        <td class="px-3 py-2 text-right font-mono">${m.stockResultante}</td>
        <td class="px-3 py-2 font-mono text-[10px] text-slate-400">${m.referencia ?? "—"}</td>
        <td class="px-3 py-2 text-slate-400">${m.proveedorNombre ?? "—"}</td>
        <td class="px-3 py-2 text-slate-400">${m.usuarioNombre ?? "—"}</td>
      </tr>`)
        .join("")
    : `<tr><td colspan="8" class="text-center text-slate-500 py-6">Sin movimientos registrados todavía.</td></tr>`;
}

document.getElementById("form-movimiento").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  try {
    const mov = await registrarMovimiento({
      productoId: Number(fd.get("productoId")),
      tipo: fd.get("tipo"),
      cantidad: Number(fd.get("cantidad")),
      referencia: fd.get("referencia") || null,
      nota: fd.get("nota") || null,
      proveedorId: fd.get("proveedorId") || null,
    });
    mostrarToast(`${ICONO_MOV[mov.tipo]} Movimiento registrado: stock ahora ${mov.stockResultante}`, "ok");
    e.target.reset();

    await cargarProductos(); // stocks cambiaron -> catálogo y selects frescos
    renderCatalogo();
    await llenarSelectsAlmacen();
    refrescarKardex();
  } catch (err) {
    console.error(err);
    mostrarToast(err.message, "error");
  }
});

document.getElementById("btn-refresh-kardex").addEventListener("click", () => refrescarKardex());

/* ----------------------------- INCIDENCIAS -------------------------------- */

const ESTILO_ESTADO = {
  REPORTADA: "bg-rose-500/15 text-rose-400",
  EN_REVISION: "bg-amber-500/15 text-amber-400",
  RESUELTA: "bg-emerald-500/15 text-emerald-400",
  CANCELADA: "bg-slate-500/20 text-slate-400",
};
const TIPO_INC_ICONO = { DEVOLUCION: "↩️ Devolución", DEFECTO: "🧯 Defecto", GARANTIA: "🛡️ Garantía" };

async function refrescarIncidencias() {
  const lista = await getIncidencias();
  const ul = document.getElementById("lista-incidencias");

  // Badge en la pestaña con incidencias pendientes.
  const pendientes = lista.filter((i) => ["REPORTADA", "EN_REVISION"].includes(i.estado)).length;
  const badge = document.getElementById("badge-incidencias");
  badge.textContent = pendientes;
  badge.classList.toggle("hidden", pendientes === 0);

  ul.innerHTML = lista.length
    ? lista
        .map((i) => `
      <li class="p-4 hover:bg-slate-700/30">
        <div class="flex flex-wrap items-center gap-2 mb-1">
          <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-700">${TIPO_INC_ICONO[i.tipo] ?? i.tipo}</span>
          <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${ESTILO_ESTADO[i.estado] ?? ""}">${i.estado}</span>
          <b class="text-sm">${i.productoNombre}</b>
          <span class="font-mono text-[10px] text-slate-500">${i.productoSku}</span>
          <span class="text-xs text-slate-500">× ${i.cantidad} u.</span>
          ${i.garantiaMeses != null ? `<span class="text-[10px] text-sky-400">🛡️ ${i.garantiaMeses} meses garantía</span>` : ""}
        </div>
        <p class="text-xs text-slate-300">${i.descripcion}</p>
        <p class="text-[10px] text-slate-500 mt-1">
          Reportó <b>${i.reportadoPor ?? "—"}</b> · ${new Date(i.fechaReporte).toLocaleString("es-PE")}
          ${i.resolucion ? `· Resolución: ${i.resolucion}` : ""}
        </p>
        ${
          ["REPORTADA", "EN_REVISION"].includes(i.estado)
            ? `<div class="flex flex-wrap gap-1.5 mt-2">
                 ${i.estado === "REPORTADA" ? `<button data-inc-id="${i.idIncidencia}" data-inc-estado="EN_REVISION" class="btn-inc px-2.5 py-1 rounded-lg text-[11px] font-bold bg-amber-500/15 text-amber-400 hover:bg-amber-500/25 transition">🔍 En revisión</button>` : ""}
                 <button data-inc-id="${i.idIncidencia}" data-inc-estado="RESUELTA" class="btn-inc px-2.5 py-1 rounded-lg text-[11px] font-bold bg-emerald-500/15 text-emerald-400 hover:bg-emerald-500/25 transition">✅ Resolver${["GARANTIA", "DEFECTO"].includes(i.tipo) ? " (retira como merma)" : ""}</button>
                 <button data-inc-id="${i.idIncidencia}" data-inc-estado="CANCELADA" class="btn-inc px-2.5 py-1 rounded-lg text-[11px] font-bold bg-slate-600/40 text-slate-400 hover:bg-slate-600/60 transition">Cancelar</button>
               </div>`
            : ""
        }
      </li>`)
        .join("")
    : `<li class="text-center text-slate-500 py-6">Sin incidencias — todo el inventario está bien 🎉</li>`;
}

document.getElementById("lista-incidencias").addEventListener("click", async (e) => {
  const btn = e.target.closest(".btn-inc");
  if (!btn) return;
  try {
    await cambiarEstadoIncidencia(btn.dataset.incId, btn.dataset.incEstado);
    mostrarToast(`Incidencia marcada como ${btn.dataset.incEstado}`, "ok");
    refrescarIncidencias();
  } catch (err) {
    console.error(err);
    mostrarToast(err.message, "error");
  }
});

document.getElementById("form-incidencia").addEventListener("submit", async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  try {
    await crearIncidencia({
      productoId: Number(fd.get("productoId")),
      ordenId: fd.get("ordenId") || null,
      tipo: fd.get("tipo"),
      cantidad: Number(fd.get("cantidad")),
      descripcion: fd.get("descripcion"),
    });
    mostrarToast("Incidencia reportada correctamente", "ok");
    e.target.reset();

    await cargarProductos(); // una DEVOLUCION puede haber sumado stock
    renderCatalogo();
    await llenarSelectsAlmacen();
    refrescarIncidencias();
  } catch (err) {
    console.error(err);
    mostrarToast(err.message, "error");
  }
});

document.getElementById("btn-refresh-incidencias").addEventListener("click", () => refrescarIncidencias());

/* ---------------------------- REPORTE SEMANAL ----------------------------- */

async function renderReporte() {
  let rep;
  try {
    rep = await getReporteSemanal();
  } catch (err) {
    console.error(err);
    return mostrarToast("No se pudo cargar el reporte", "error");
  }

  document.getElementById("rep-total").textContent = fmt(rep.totalVentas);
  document.getElementById("rep-ordenes").textContent = rep.numeroOrdenes;
  document.getElementById("rep-promedio").textContent = fmt(rep.ticketPromedio);
  document.getElementById("rep-bajo").textContent = rep.bajoStock.length;

  // Ventas de HOY (del reporte: el último día es siempre hoy).
  const hoy = rep.dias[rep.dias.length - 1];
  if (hoy) {
    document.getElementById("rep-hoy").textContent = fmt(hoy.total);
    document.getElementById("rep-hoy-ordenes").textContent =
      `${hoy.ordenes} cobro${hoy.ordenes === 1 ? "" : "s"}`;
  }

  await renderHistorial();
  await renderCaja();

  // Gráfica de barras CSS proporcional al mejor día.
  const maxDia = Math.max(...rep.dias.map((d) => d.total), 1);
  document.getElementById("rep-grafica").innerHTML = rep.dias
    .map((d) => {
      const alto = Math.max((d.total / maxDia) * 100, d.total > 0 ? 6 : 2);
      return `<div class="flex-1 h-full flex items-end" title="${d.fecha}: ${fmt(d.total)} (${d.ordenes} órdenes)">
                <div class="w-full rounded-t-lg ${d.total > 0 ? "bg-gradient-to-t from-emerald-700 to-emerald-400" : "bg-slate-700"}"
                     style="height:${alto}%"></div>
              </div>`;
    })
    .join("");
  document.getElementById("rep-grafica-labels").innerHTML = rep.dias
    .map((d) => `<span class="flex-1 text-center">${d.diaSemana}<br/><span class="text-slate-400">${d.total > 0 ? fmt(d.total) : "—"}</span></span>`)
    .join("");

  // Top productos.
  document.getElementById("rep-top").innerHTML = rep.topProductos.length
    ? rep.topProductos
        .map(
          (t, idx) => `
      <li class="flex items-center gap-3">
        <span class="w-6 h-6 grid place-items-center rounded-full text-[11px] font-extrabold ${
          idx === 0 ? "bg-yellow-400 text-slate-900" : idx === 1 ? "bg-slate-300 text-slate-900" : idx === 2 ? "bg-amber-600 text-white" : "bg-slate-700 text-slate-300"
        }">${idx + 1}</span>
        <span class="flex-1 min-w-0 truncate">${t.nombre}</span>
        <span class="text-xs text-slate-400 font-mono">${t.sku}</span>
        <b>${t.unidades} u.</b>
        <span class="text-emerald-400 w-20 text-right">${fmt(t.importe)}</span>
      </li>`
        )
        .join("")
    : `<li class="text-slate-500 text-sm">Aún no hay ventas esta semana.</li>`;

  // Bajo stock + acción rápida de entrada.
  document.getElementById("tbody-bajo-stock").innerHTML = rep.bajoStock.length
    ? rep.bajoStock
        .map((p) => `
      <tr class="hover:bg-slate-700/30">
        <td class="px-4 py-2 font-mono text-[10px] text-slate-500">${p.sku}</td>
        <td class="px-4 py-2">${p.nombre}</td>
        <td class="px-4 py-2 text-right"><b class="${p.stock === 0 ? "text-rose-400" : "text-amber-400"}">${p.stock}</b></td>
        <td class="px-4 py-2 text-right text-slate-400">${p.stockMinimo}</td>
        <td class="px-4 py-2 text-slate-400">${p.proveedorNombre ?? "—"}</td>
        <td class="px-4 py-2 text-center">
          <button data-reponer='${JSON.stringify({ id: p.idProducto, prov: p.proveedorNombre }).replace(/'/g, "&#39;")}'
                  class="btn-reponer px-3 py-1 rounded-lg text-[11px] font-bold bg-emerald-500/15 text-emerald-400 hover:bg-emerald-500/25 transition">
            📥 Entrada rápida
          </button>
        </td>
      </tr>`)
        .join("")
    : `<tr><td colspan="6" class="text-center text-slate-500 py-5">Ningún producto está bajo su mínimo 👍</td></tr>`;
}

// "Entrada rápida": lleva al cajero a Almacén con el producto y proveedor listos.
document.addEventListener("click", async (e) => {
  const btn = e.target.closest(".btn-reponer");
  if (!btn) return;
  const datos = JSON.parse(btn.dataset.reponer);

  mostrarTab("almacen");
  const $form = document.getElementById("form-movimiento");
  $form.elements.productoId.value = String(datos.id);
  $form.elements.tipo.value = "ENTRADA";
  if (datos.prov) {
    const opt = [...$form.elements.proveedorId.options].find((o) => o.textContent === datos.prov);
    if (opt) $form.elements.proveedorId.value = opt.value;
  }
  $form.elements.cantidad.focus();
  mostrarToast(`Producto listo para ENTRADA — escribe la cantidad recibida`, "ok");
});

document.getElementById("btn-refresh-reporte").addEventListener("click", () => renderReporte());

/* ------------------------------ CAJA FÍSICA ------------------------------- */

const HAB_KEY = `tm_caja_habilitada_${sesion.username}_${getSedeActual()}`;

function verificarHabilitacion() {
  const estado = localStorage.getItem(HAB_KEY);
  if (estado === "true") {
    cajaHabilitada = true;
    document.getElementById("modal-habilitar").classList.add("hidden");
  } else {
    cajaHabilitada = false;
    document.getElementById("modal-habilitar").classList.remove("hidden");
    // Llenar info del modal
    document.getElementById("hab-sede").textContent = sesion.sedeNombre || "Sin sede";
    document.getElementById("hab-caja").textContent = sesion.cajaNumero != null ? `#${sesion.cajaNumero}` : "Sin asignar";
    document.getElementById("hab-cajero").textContent = `${sesion.username} (${sesion.rol})`;
  }
}

document.getElementById("btn-habilitar").addEventListener("click", async () => {
  const monto = Number(document.getElementById("hab-monto").value);
  if (!monto || monto <= 0) return mostrarToast("Ingresa un monto válido para el fondo inicial", "error");

  try {
    await registrarMovimientoCaja("FONDO", monto, "Fondo inicial de apertura de caja");
    localStorage.setItem(HAB_KEY, "true");
    cajaHabilitada = true;
    document.getElementById("modal-habilitar").classList.add("hidden");
    mostrarToast(`✅ Caja habilitada con fondo de ${fmt(monto)}`, "exito");
    refrescarChipCaja();
  } catch (err) {
    console.error(err);
    mostrarToast(err.message, "error");
  }
});

async function refrescarChipCaja() {
  try {
    const { efectivo } = await getCaja();
    document.getElementById("chip-caja").textContent = `💵 ${fmt(efectivo)}`;
  } catch { /* silencioso: es solo indicativo */ }
}

const ICONO_CAJA = { VENTA: "🛒", FONDO: "➕", RETIRO: "➖" };
const COLOR_CAJA = {
  VENTA: "text-emerald-400",
  FONDO: "text-sky-400",
  RETIRO: "text-rose-400",
};

/** Pinta el panel grande de caja del reporte (saldo + movimientos). */
async function renderCaja() {
  const { efectivo, movimientos } = await getCaja();
  document.getElementById("caja-efectivo").textContent = fmt(efectivo);

  document.getElementById("lista-caja").innerHTML = movimientos.length
    ? movimientos
        .map((m) => `
      <li class="px-4 py-2.5 flex items-center gap-3">
        <span class="w-6 text-center">${ICONO_CAJA[m.tipo] ?? "•"}</span>
        <div class="flex-1 min-w-0">
          <p class="font-bold ${COLOR_CAJA[m.tipo] ?? ""}">${m.tipo}</p>
          <p class="text-slate-500 truncate">${m.nota ?? ""} · ${m.usuarioNombre ?? "—"}</p>
        </div>
        <span class="text-right shrink-0">
          <b class="${COLOR_CAJA[m.tipo] ?? ""}">${m.tipo === "RETIRO" ? "−" : "+"}${fmt(m.monto)}</b>
          <br/><span class="text-[9px] text-slate-500">${new Date(m.fecha).toLocaleString("es-PE", { dateStyle: "short", timeStyle: "short" })}</span>
        </span>
      </li>`)
        .join("")
    : `<li class="text-center text-slate-500 py-6">Sin movimientos de caja todavía.</li>`;
}

async function moverCaja(tipo) {
  const $monto = document.getElementById("caja-monto");
  const monto = Number($monto.value);
  if (!monto || monto <= 0)
    return mostrarToast("Escribe un monto válido primero", "error");

  try {
    await registrarMovimientoCaja(tipo, monto, document.getElementById("caja-nota").value);
    mostrarToast(
      tipo === "FONDO"
        ? `➕ Fondo de ${fmt(monto)} agregado a la caja`
        : `➖ Retiro de ${fmt(monto)} registrado`,
      "ok"
    );
    document.getElementById("caja-nota").value = "";
    $monto.value = "";
    await renderCaja();
    refrescarChipCaja();
  } catch (err) {
    console.error(err);
    mostrarToast(err.message, "error");
  }
}

document.getElementById("btn-caja-fondo").addEventListener("click", () => moverCaja("FONDO"));
document.getElementById("btn-caja-retiro").addEventListener("click", () => moverCaja("RETIRO"));

// El chip del ticket lleva directo al control de caja.
document.getElementById("chip-caja").addEventListener("click", () => {
  mostrarTab("reporte");
  setTimeout(() =>
    document.querySelector("#caja-efectivo")?.scrollIntoView({ behavior: "smooth", block: "center" }), 80);
});

/* --------------------------- HISTORIAL DE VENTAS -------------------------- */

let cacheOrdenes = [];

async function renderHistorial() {
  try {
    cacheOrdenes = await getOrdenes();
  } catch (err) {
    console.error(err);
    return;
  }

  const soloHoy = document.getElementById("chk-todas-fechas")?.checked ?? true;
  const hoyISO = new Date().toISOString().slice(0, 10);

  const lista = cacheOrdenes
    .filter((o) => (soloHoy ? String(o.fechaCreacion).slice(0, 10) === hoyISO : true))
    .slice(0, 50); // las más recientes primero (getOrdenes ya invierte)

  const METODO_ICONO = { EFECTIVO: "💵", TARJETA: "💳", TRANSFERENCIA: "🏦" };

  document.getElementById("tbody-historial").innerHTML = lista.length
    ? lista
        .map((o) => `
      <tr class="hover:bg-slate-700/30">
        <td class="px-4 py-2 font-mono text-[10px] text-emerald-400">${o.folio}</td>
        <td class="px-4 py-2 whitespace-nowrap text-slate-300">${new Date(o.fechaCreacion).toLocaleString("es-PE", { dateStyle: "short", timeStyle: "short" })}</td>
        <td class="px-4 py-2 text-slate-400 max-w-[160px] truncate">${o.banderaEmoji ?? ""} ${o.empresaNombre ?? "—"}</td>
        <td class="px-4 py-2 text-center">${(o.items ?? []).reduce((s, i) => s + i.cantidad, 0)}</td>
        <td class="px-4 py-2 text-slate-400">${METODO_ICONO[o.metodoPago] ?? ""} ${o.metodoPago ?? ""}</td>
        <td class="px-4 py-2 text-right font-bold">${fmt(o.total)}</td>
        <td class="px-4 py-2 text-center">
          <button data-ver-orden="${o.idOrden}" class="text-[11px] font-bold text-sky-400 hover:text-sky-300 underline decoration-dotted">Ver</button>
        </td>
      </tr>`)
        .join("")
    : `<tr><td colspan="7" class="text-center text-slate-500 py-6">
         ${soloHoy ? "Sin ventas registradas hoy." : "Sin ventas registradas."}
       </td></tr>`;
}

document.getElementById("chk-todas-fechas").addEventListener("change", () => renderHistorial());

// Detalle de una venta del historial.
document.getElementById("tbody-historial").addEventListener("click", (e) => {
  const btn = e.target.closest("[data-ver-orden]");
  if (!btn) return;
  const orden = cacheOrdenes.find((o) => String(o.idOrden) === btn.dataset.verOrden);
  if (!orden) return;

  document.getElementById("detalle-folio").textContent = orden.folio;
  document.getElementById("detalle-meta").textContent =
    `${new Date(orden.fechaCreacion).toLocaleString("es-PE")} · ${orden.canal} · ${orden.metodoPago}` +
    (orden.paisNombre ? ` · ${orden.banderaEmoji ?? ""} ${orden.paisNombre}` : "");
  document.getElementById("detalle-lineas").innerHTML = (orden.items ?? [])
    .map(
      (i) => `
    <li class="flex items-center gap-3 px-4 py-2">
      <b class="w-8 text-center bg-slate-100 rounded-lg py-0.5">${i.cantidad}</b>
      <div class="flex-1 min-w-0">
        <p class="font-semibold truncate text-[13px]">${i.nombreProducto}</p>
        <p class="text-[11px] font-mono text-slate-400">${i.sku}</p>
      </div>
      <span class="text-slate-500 text-xs">${fmt(i.precioUnitario)} c/u</span>
      <b class="w-24 text-right">${fmt(i.subtotalLinea)}</b>
    </li>`
    )
    .join("");
  document.getElementById("detalle-subtotal").textContent = fmt(orden.subtotal);
  document.getElementById("detalle-iva").textContent = fmt(orden.iva);
  document.getElementById("detalle-total").textContent = fmt(orden.total);

  // Comprobante Yape adjunto (si la venta fue con ese método).
  const $yape = document.getElementById("detalle-yape");
  if (orden.yapeComprobante) {
    document.getElementById("detalle-yape-img").src = orden.yapeComprobante;
    document.getElementById("detalle-yape-link").href = orden.yapeComprobante;
    document.getElementById("detalle-yape-op").textContent =
      orden.yapeOperacion ? `Operación: ${orden.yapeOperacion}` : "Sin N° de operación";
    $yape.classList.remove("hidden");
  } else {
    $yape.classList.add("hidden");
  }

  document.getElementById("modal-detalle").classList.remove("hidden");
});

document.getElementById("btn-cerrar-detalle").addEventListener("click", () =>
  document.getElementById("modal-detalle").classList.add("hidden")
);
document.getElementById("modal-detalle").addEventListener("click", (e) => {
  if (e.target.id === "modal-detalle")
    document.getElementById("modal-detalle").classList.add("hidden");
});

/* ------------------------------ Inicialización ---------------------------- */

async function cargarProductos() {
  catalogo = await getProductos({});
}

(async function init() {
  pintarMetodos();
  renderTicket();
  verificarHabilitacion();

  const [categorias, empresas] = await Promise.all([getCategorias(), getEmpresas()]);
  categorias.forEach((c) =>
    $selectCat.insertAdjacentHTML("beforeend", `<option value="${c.idCategoria}">${c.nombre}</option>`)
  );
  empresas.forEach((e) =>
    $selectCliente.insertAdjacentHTML(
      "beforeend",
      `<option value="${e.idEmpresa}">${e.banderaEmoji} ${e.razonSocial} · ${e.regimenFiscal} (${Number(e.tasaIva).toFixed(0)}%)</option>`
    )
  );

  await cargarProductos();
  renderCatalogo();
  await llenarSelectsAlmacen();
  refrescarIncidencias(); // pinta badge de pendientes desde el arranque
  refrescarChipCaja();
})();
