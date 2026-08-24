/* ============================================================================
 * catalogo.js — HOME de la tienda (vista CLIENTE / invitados).
 *
 * El inicio muestra SOLO lo más popular (lo más vendido) + categorías;
 * el catálogo completo con buscador y filtros vive en catalogo.html.
 * Consume datos EXCLUSIVAMENTE a través de api.js (mock o API REST Java).
 * ========================================================================== */

import { getProductos, getCategorias, getProductosPopulares } from "./api.js";
import { Carrito, actualizarBadgeCarrito } from "./carrito-store.js";
import { emojiDe, tarjetaProducto, tarjetaCategoria } from "./tienda-tarjetas.js";

const $gridPopulares = document.getElementById("grid-populares");
const $popularesVacio = document.getElementById("populares-vacio");
const $buscador = document.getElementById("buscador");
const $buscadorMovil = document.getElementById("buscador-movil");
const $gridCategorias = document.getElementById("grid-categorias");

let productosCache = []; // populares renderizados (para agregar al carrito)
let categoriasCache = [];

/* ------------------------- Barra de sesión -------------------------------- */

/** Invitados navegan libremente; login solo se exige al pagar/administrar. */
function renderSesion() {
  const sesion = Sesion.obtener();
  const $zona = document.getElementById("zona-sesion");
  const $linkAdmin = document.getElementById("link-admin");
  const $linkPos = document.getElementById("link-pos");

  if (sesion) {
    // Enlaces según rol: ADMIN ve todo, CAJERO ve la caja, CLIENTE solo tienda.
    $linkAdmin.classList.toggle("hidden", sesion.rol !== "ADMIN");
    $linkPos.classList.toggle("hidden", !(sesion.rol === "CAJERO" || sesion.rol === "ADMIN"));
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
    if ($linkPos) $linkPos.classList.add("hidden");
    $zona.innerHTML = `
      <a href="login.html" class="text-sm font-semibold text-slate-700 hover:text-indigo-600 transition">Iniciar sesión</a>
      <a href="login.html" class="hidden sm:inline-block text-sm font-semibold bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-1.5 rounded-full transition">
        Crear cuenta
      </a>`;
  }
}

/* -------------------------------- Eventos --------------------------------- */

// Buscadores del navbar: Enter -> catálogo completo con la búsqueda.
function irAlCatalogo(q) {
  const valor = q.trim();
  location.href = valor ? `catalogo.html?q=${encodeURIComponent(valor)}` : "catalogo.html";
}
[$buscador, $buscadorMovil].forEach(($input) =>
  $input.addEventListener("keydown", (e) => {
    if (e.key === "Enter") irAlCatalogo($input.value);
  })
);

document.body.addEventListener("click", (e) => {
  // Agregar al carrito desde las tarjetas populares.
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

  // Tap en tarjeta popular -> catálogo con ese producto ya buscado.
  const ver = e.target.closest("[data-ver]");
  if (ver) {
    const producto = productosCache.find((p) => String(p.idProducto) === ver.dataset.ver);
    location.href = producto
      ? `catalogo.html?q=${encodeURIComponent(producto.nombre)}`
      : "catalogo.html";
    return;
  }

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
  const todos = await getProductos({});
  $gridCategorias.innerHTML = categoriasCache
    .map((c) => tarjetaCategoria(c, todos.filter((p) => p.categoriaId === c.idCategoria).length))
    .join("");

  try {
    const populares = await getProductosPopulares(8);
    productosCache = populares;
    $gridPopulares.innerHTML = populares.map(tarjetaProducto).join("");
    $popularesVacio.classList.toggle("hidden", populares.length > 0);
  } catch (err) {
    console.error(err);
    $popularesVacio.classList.remove("hidden");
  }
})();
