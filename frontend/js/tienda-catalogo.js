/* ============================================================================
 * tienda-catalogo.js — CATÁLOGO COMPLETO (catalogo.html).
 *
 * Apartado independiente de la tienda con:
 *  - Buscador grande (nombre o SKU), sincronizado con la URL (?q=...)
 *  - Filtro por categoría con chips (?cat=...)
 *  - Orden: más populares, precio, nombre
 *  - Acceso directo desde el home (?stock=bajo muestra últimas piezas)
 * ========================================================================== */

import { getProductos, getCategorias, getProductosPopulares } from "./api.js";
import { Carrito, actualizarBadgeCarrito } from "./carrito-store.js";
import { emojiDe, tarjetaProducto } from "./tienda-tarjetas.js";

const $buscador = document.getElementById("buscador-catalogo");
const $ordenar = document.getElementById("ordenar-catalogo");
const $chips = document.getElementById("chips-categorias");
const $grid = document.getElementById("grid-productos");
const $vacio = document.getElementById("mensaje-vacio");
const $contador = document.getElementById("contador-resultados");

let categoriasCache = [];
let productosCache = [];   // productos renderizados (para agregar al carrito)
let categoriaActiva = null; // null = todas
let filtroStockBajo = false;

/* --------------------------- Estado desde la URL -------------------------- */

function leerParams() {
  const params = new URLSearchParams(location.search);
  if (params.get("q")) $buscador.value = params.get("q");
  const cat = params.get("cat");
  if (cat) {
    categoriaActiva = Number(cat);
    // El select se aplica cuando existan opciones.
  }
  if (params.get("stock") === "bajo") filtroStockBajo = true;
}

/** Actualiza ?q= en la barra de direcciones sin recargar (historial limpio). */
function sincronizarUrl() {
  const params = new URLSearchParams();
  if ($buscador.value.trim()) params.set("q", $buscador.value.trim());
  if (categoriaActiva != null) params.set("cat", String(categoriaActiva));
  if (filtroStockBajo) params.set("stock", "bajo");
  const query = params.toString();
  history.replaceState(null, "", query ? `?${query}` : location.pathname);
}

/* ------------------------------- Chips ------------------------------------ */

function renderChips() {
  const chipBase = "chip-cat text-xs font-semibold px-4 py-1.5 rounded-full border";
  const activo = "bg-white text-indigo-700 border-white shadow font-bold";
  const inactivo = "bg-white/10 text-indigo-100 border-white/30 hover:bg-white/20";

  let html =
    `<button data-chip="" class="${categoriaActiva === null && !filtroStockBajo ? activo : inactivo}">Todas</button>` +
    categoriasCache.map(
      (c) =>
        `<button data-chip="${c.idCategoria}" class="${String(categoriaActiva) === String(c.idCategoria) && !filtroStockBajo ? activo : inactivo}">${c.nombre}</button>`
    ).join("");

  if (filtroStockBajo)
    html += `<button data-chip="stock" class="${activo}">⏰ Últimas piezas</button>`;

  $chips.innerHTML = html;
}

/* ------------------------------ Renderizado ------------------------------- */

function aplicarOrden(lista) {
  const copia = [...lista];
  switch ($ordenar.value) {
    case "precioAsc":  return copia.sort((a, b) => a.precioBase - b.precioBase);
    case "precioDesc": return copia.sort((a, b) => b.precioBase - a.precioBase);
    case "nombreAZ":   return copia.sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));
    default:
      // Más populares: primero unidades vendidas; desempate por stock disponible.
      return copia.sort((a, b) => (b.unidadesVendidas ?? 0) - (a.unidadesVendidas ?? 0));
  }
}

async function renderProductos() {
  try {
    let lista = await getProductos({
      busqueda: $buscador.value.trim(),
      categoriaId: categoriaActiva,
    });

    if (filtroStockBajo) lista = lista.filter((p) => p.stock > 0 && p.stock <= p.stockMinimo);

    // Unidades vendidas para ordenar por popularidad (mock: api lo calcula).
    const populares = await getProductosPopulares(999);
    const ventasPorId = new Map(populares.map((p) => [p.idProducto, p.unidadesVendidas ?? 0]));
    lista = lista.map((p) => ({ ...p, unidadesVendidas: ventasPorId.get(p.idProducto) ?? 0 }));

    const finales = aplicarOrden(lista);
    productosCache = finales;
    $grid.innerHTML = finales.map(tarjetaProducto).join("");
    $vacio.classList.toggle("hidden", finales.length > 0);
    $contador.textContent = `${finales.length} producto(s) encontrado(s)`;
    sincronizarUrl();
  } catch (err) {
    console.error(err);
    mostrarToast("Error cargando productos: " + err.message, "error");
  }
}

/* ------------------------- Barra de sesión -------------------------------- */

function renderSesion() {
  const sesion = Sesion.obtener();
  const $zona = document.getElementById("zona-sesion");

  if (sesion) {
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
    $zona.innerHTML = `
      <a href="login.html" class="text-sm font-semibold text-slate-700 hover:text-indigo-600 transition">Iniciar sesión</a>
      <a href="login.html" class="hidden sm:inline-block text-sm font-semibold bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-1.5 rounded-full transition">
        Crear cuenta
      </a>`;
  }
}

/* -------------------------------- Eventos --------------------------------- */

let timerBusqueda;
$buscador.addEventListener("input", () => {
  clearTimeout(timerBusqueda);
  timerBusqueda = setTimeout(renderProductos, 250);
});

$ordenar.addEventListener("change", renderProductos);

$chips.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-chip]");
  if (!btn) return;
  const chip = btn.dataset.chip;
  if (chip === "stock") {
    filtroStockBajo = true;
    categoriaActiva = null;
  } else {
    filtroStockBajo = false;
    categoriaActiva = chip ? Number(chip) : null;
  }
  renderChips();
  renderProductos();
});

document.body.addEventListener("click", (e) => {
  const agregar = e.target.closest("[data-agregar]");
  if (agregar && !agregar.disabled) {
    const producto = productosCache.find((p) => String(p.idProducto) === agregar.dataset.agregar);
    if (!producto) return;
    try {
      Carrito.agregar({ ...producto, _emoji: emojiDe(producto) });
      mostrarToast(`✔ "${producto.nombre}" agregado al carrito`, "exito");
    } catch (err) {
      mostrarToast(err.message, "error");
    }
    return;
  }

  // Favoritos (visual).
  const fav = e.target.closest("[data-favorito]");
  if (fav) fav.textContent = fav.textContent === "♡" ? "❤️" : "♡";
});

document.getElementById("btn-carrito").addEventListener("click", () => {
  if (Carrito.contarUnidades() === 0) mostrarToast("Tu carrito está vacío todavía 🛒", "info");
});

/* ------------------------------ Inicialización ---------------------------- */

(async function init() {
  renderSesion();
  actualizarBadgeCarrito();
  categoriasCache = await getCategorias();
  leerParams();
  renderChips();
  await renderProductos();
})();
