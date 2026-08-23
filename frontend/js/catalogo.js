/* ============================================================================
 * catalogo.js — Tienda (vista CLIENTE / invitados).
 *
 * Flujo tipo e-commerce real:
 *  - CUALQUIER visitante navega catálogo, ofertas y categorías sin login.
 *  - El login solo se exige para acciones que requieren identidad
 *    (checkout → Entregable 2) o para el panel ADMIN.
 *
 * Consume datos EXCLUSIVAMENTE a través de api.js (mock o API REST Java).
 * ========================================================================== */

import { getProductos, getCategorias } from "./api.js";

const $buscador = document.getElementById("buscador");
const $buscadorMovil = document.getElementById("buscador-movil");
const $grid = document.getElementById("grid-productos");
const $vacio = document.getElementById("mensaje-vacio");
const $contador = document.getElementById("contador-resultados");
const $ordenar = document.getElementById("ordenar");
const $chips = document.getElementById("chips-categorias");
const $gridCategorias = document.getElementById("grid-categorias");
const $modoDatos = null; // el indicador de modo vive ahora en el footer del login

let categoriasCache = [];
let categoriaActiva = null;   // null = todas
let filtroStockBajo = false;  // banner "últimas piezas"

/* --------------------------- Decoración visual ---------------------------- */

const ICONOS_CATEGORIA = {
  "Electrónica": ["🎧", "⌨️", "🖱️", "📺", "📱", "🔊"],
  "Hogar":       ["☕", "🛏️", "🥛", "🍳", "🧴", "🕯️"],
  "Oficina":     ["💼", "📓", "✏️", "🖨️", "🗂️", "📎"],
  "Deportes":    ["🏋️", "🧘", "⚽", "🚴", "🏀", "🎾"],
};

const GRADIENTES = [
  "from-sky-100 to-blue-200",
  "from-emerald-100 to-teal-200",
  "from-amber-100 to-orange-200",
  "from-violet-100 to-purple-200",
];

function emojiDe(p) {
  const lista = ICONOS_CATEGORIA[p.categoriaNombre] || ["📦"];
  return lista[p.idProducto % lista.length];
}

function gradienteDe(p) {
  return GRADIENTES[p.idProducto % GRADIENTES.length];
}

/** Estrellas determinísticas por producto (decorativo, 3.5–5). */
function estrellasDe(p) {
  const n = 35 + ((p.idProducto * 7) % 16); // 35..50 => 3.5..5.0
  const llenas = Math.floor(n / 10);
  const media = (n % 10) >= 5;
  return "★".repeat(llenas) + (media ? "⯨" : "") + "☆".repeat(5 - llenas - (media ? 1 : 0));
}

/* ------------------------------ Badges ------------------------------------ */

function badgeStock(p) {
  if (p.stock === 0)
    return `<span class="badge badge-agotado absolute top-3 left-3">AGOTADO</span>`;
  if (p.stock <= p.stockMinimo)
    return `<span class="badge badge-bajo absolute top-3 left-3">¡ÚLTIMAS ${p.stock}!</span>`;
  return `<span class="badge badge-ok absolute top-3 left-3">DISPONIBLE</span>`;
}

/* --------------------------- Tarjeta de producto -------------------------- */

function tarjetaProducto(p) {
  const agotado = p.stock === 0;
  return `
  <article class="card-producto group bg-white rounded-2xl shadow-sm hover:shadow-xl border border-slate-100 overflow-hidden flex flex-col fade-in">
    <div class="relative cursor-pointer" data-ver="${p.idProducto}">
      <div class="h-44 bg-gradient-to-br ${gradienteDe(p)} flex items-center justify-center text-6xl group-hover:scale-105 transition-transform duration-300">
        <span>${emojiDe(p)}</span>
      </div>
      ${badgeStock(p)}
      <button data-favorito="${p.idProducto}" title="Guardar en favoritos"
              class="absolute top-2.5 right-2.5 w-8 h-8 rounded-full bg-white/90 backdrop-blur shadow flex items-center justify-center text-sm opacity-0 group-hover:opacity-100 transition hover:scale-110">
        ♡
      </button>
    </div>

    <div class="p-4 flex flex-col gap-1.5 flex-1">
      <span class="text-[10px] font-bold uppercase tracking-widest text-slate-400">${p.categoriaNombre ?? "General"}</span>
      <h3 class="font-semibold text-slate-800 leading-snug line-clamp-2 min-h-[2.6rem] cursor-pointer hover:text-indigo-600 transition" data-ver="${p.idProducto}">
        ${p.nombre}
      </h3>
      <p class="text-xs text-amber-500 select-none" title="Valoración de clientes">
        ${estrellasDe(p)} <span class="text-slate-400 text-[11px]">(${((p.idProducto * 13) % 120) + 8})</span>
      </p>

      <div class="flex items-end justify-between mt-auto pt-2">
        <div>
          <p class="text-lg font-extrabold text-slate-900">$${Number(p.precioBase).toLocaleString("es-MX", { minimumFractionDigits: 2 })}</p>
          <p class="text-[10px] text-slate-400">12 MSI de $${(Number(p.precioBase) / 12).toFixed(2)}</p>
          <p class="text-[9px] uppercase tracking-wide text-slate-300 mt-0.5">IVA no incluido*</p>
        </div>
        <button data-agregar="${p.idProducto}" ${agotado ? "disabled" : ""}
                title="${agotado ? "Sin stock" : "Agregar al carrito"}"
                class="${agotado
                  ? "bg-slate-200 text-slate-400 cursor-not-allowed"
                  : "bg-indigo-600 hover:bg-indigo-700 hover:scale-110 active:scale-95"} 
                  text-white rounded-full w-10 h-10 flex items-center justify-center shadow-md transition text-xl font-bold">
          ${agotado ? "–" : "+"}
        </button>
      </div>
    </div>
  </article>`;
}

/* ------------------------ Categorías y chips ------------------------------ */

const EMOJIS_GRANDES = { "Electrónica": "📱", "Hogar": "🏠", "Oficina": "💼", "Deportes": "⚽" };

function renderCategorias(productos) {
  // Tarjetas grandes con conteo de productos por categoría.
  $gridCategorias.innerHTML = categoriasCache.map((c) => {
    const total = productos.filter((p) => p.categoriaId === c.idCategoria).length;
    return `
    <button data-filtro-categoria="${c.idCategoria}"
            class="card-producto group bg-white rounded-2xl border border-slate-100 shadow-sm p-5 text-left hover:border-indigo-300">
      <span class="inline-flex w-12 h-12 rounded-xl bg-gradient-to-br ${gradienteDe(c)} items-center justify-center text-2xl mb-3">
        ${EMOJIS_GRANDES[c.nombre] ?? "🛍️"}
      </span>
      <p class="font-bold text-slate-800 group-hover:text-indigo-600 transition">${c.nombre}</p>
      <p class="text-[11px] text-slate-400">${total} producto(s)</p>
    </button>`;
  }).join("");

  renderChips();
}

function renderChips() {
  const chipBase = "chip-cat text-xs font-semibold px-4 py-1.5 rounded-full border";
  const activo = "bg-indigo-600 text-white border-indigo-600 shadow";
  const inactivo = "bg-white text-slate-600 border-slate-300 hover:border-indigo-400 hover:text-indigo-600";

  $chips.innerHTML =
    `<button data-chip="" class="${categoriaActiva === null && !filtroStockBajo ? activo : inactivo}">Todas</button>` +
    categoriasCache.map(
      (c) => `<button data-chip="${c.idCategoria}" class="${String(categoriaActiva) === String(c.idCategoria) ? activo : inactivo}">${c.nombre}</button>`
    ).join("");
}

/* ------------------------------ Renderizado ------------------------------- */

function aplicarOrden(lista) {
  const copia = [...lista];
  switch ($ordenar.value) {
    case "precioAsc":  return copia.sort((a, b) => a.precioBase - b.precioBase);
    case "precioDesc": return copia.sort((a, b) => b.precioBase - a.precioBase);
    case "nombreAZ":   return copia.sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));
    default:           return copia;
  }
}

async function renderProductos() {
  try {
    let productos = await getProductos({
      busqueda: ($buscador.value || $buscadorMovil.value || "").trim(),
      categoriaId: categoriaActiva,
    });

    if (filtroStockBajo) productos = productos.filter((p) => p.stock > 0 && p.stock <= p.stockMinimo);

    const finales = aplicarOrden(productos);
    $grid.innerHTML = finales.map(tarjetaProducto).join("");
    $vacio.classList.toggle("hidden", finales.length > 0);
    $contador.textContent = `${finales.length} producto(s) encontrado(s)`;
  } catch (err) {
    console.error(err);
    mostrarToast("Error cargando productos: " + err.message, "error");
  }
}

/* ------------------------- Barra de sesión -------------------------------- */

/** Invitados navegan libremente; login solo se exige al pagar/administrar. */
function renderSesion() {
  const sesion = Sesion.obtener();
  const $zona = document.getElementById("zona-sesion");
  const $linkAdmin = document.getElementById("link-admin");

  if (sesion) {
    $linkAdmin.classList.toggle("hidden", sesion.rol !== "ADMIN");
    const colorBadge =
      sesion.rol === "ADMIN" ? "bg-amber-100 text-amber-800" :
      sesion.rol === "CAJERO" ? "bg-emerald-100 text-emerald-700" :
      "bg-indigo-100 text-indigo-700";
    $zona.innerHTML = `
      <span class="hidden sm:inline-flex items-center gap-1.5 text-xs px-2.5 py-1.5 rounded-full ${colorBadge} font-semibold">
        ${sesion.username} · ${sesion.rol}
      </span>
      <button id="btn-logout" title="Cerrar sesión"
              class="text-xs text-slate-500 hover:text-rose-600 underline decoration-dotted">Salir</button>`;
    document.getElementById("btn-logout").addEventListener("click", () => {
      mostrarToast("Sesión cerrada. ¡Vuelve pronto!", "info");
      setTimeout(() => Sesion.cerrar(), 600);
    });
  } else {
    $linkAdmin.classList.add("hidden");
    $zona.innerHTML = `
      <a href="login.html" class="text-sm font-semibold text-slate-700 hover:text-indigo-600 transition">Iniciar sesión</a>
      <a href="login.html" class="hidden sm:inline-block text-sm font-semibold bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-1.5 rounded-full transition">
        Crear cuenta
      </a>`;
  }
}

/* -------------------------------- Eventos --------------------------------- */

let timerBusqueda;
function alEscribir() {
  clearTimeout(timerBusqueda);
  timerBusqueda = setTimeout(renderProductos, 250);
}
$buscador.addEventListener("input", () => { $buscadorMovil.value = $buscador.value; alEscribir(); });
$buscadorMovil.addEventListener("input", () => { $buscador.value = $buscadorMovil.value; alEscribir(); });

$ordenar.addEventListener("change", renderProductos);

// Chips de categoría
$chips.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-chip]");
  if (!btn) return;
  categoriaActiva = btn.dataset.chip ? Number(btn.dataset.chip) : null;
  filtroStockBajo = false;
  renderChips();
  renderProductos();
});

// Banners promocionales + tarjetas de categoría -> filtran el catálogo
document.body.addEventListener("click", (e) => {
  const filtroCat = e.target.closest("[data-filtro-categoria]");
  if (filtroCat) {
    categoriaActiva = Number(filtroCat.dataset.filtroCategoria);
    filtroStockBajo = false;
    renderChips();
    renderProductos();
    document.getElementById("catalogo").scrollIntoView({ behavior: "smooth" });
    return;
  }
  if (e.target.closest("[data-filtro-stock]")) {
    categoriaActiva = null;
    filtroStockBajo = true;
    renderChips();
    renderProductos();
    document.getElementById("catalogo").scrollIntoView({ behavior: "smooth" });
    return;
  }

  // Agregar al carrito (placeholder hasta Entregable 2)
  const agregar = e.target.closest("[data-agregar]");
  if (agregar && !agregar.disabled) {
    mostrarToast("🛒 El carrito llega con el Entregable 2 — ¡ya casi!", "info");
    return;
  }

  // Favoritos (visual)
  const fav = e.target.closest("[data-favorito]");
  if (fav) {
    fav.textContent = fav.textContent === "♡" ? "❤️" : "♡";
    return;
  }
});

document.getElementById("btn-carrito").addEventListener("click", () =>
  mostrarToast("🛒 El carrito se implementa en el Entregable 2", "info")
);

/* ------------------------------ Inicialización ---------------------------- */

(async function init() {
  renderSesion();
  categoriasCache = await getCategorias();
  await renderCategorias(await getProductos({}));
  await renderProductos();
})();
