/* ============================================================================
 * carrito.js — Página de carrito + CHECKOUT del CLIENTE (Entregable 2).
 *
 * Flujo:
 *  1. Pinta las líneas guardadas en carrito-store.
 *  2. Al elegir régimen fiscal (empresa B2B o consumidor final), recalcula
 *     EN VIVO subtotal / IVA / total vía api.calcularImpuestos — que usa la
 *     parametrización real de la empresa (mock o backend Java).
 *  3. "Confirmar pedido" exige sesión y llama a api.crearCheckout, que
 *     valida/descuenta stock y registra la orden transaccionalmente.
 * ========================================================================== */

import { getEmpresas, calcularImpuestos, crearCheckout } from "./api.js";
import { Carrito, actualizarBadgeCarrito } from "./carrito-store.js";

const $vistaCarrito = document.getElementById("vista-carrito");
const $vistaVacia = document.getElementById("vista-vacia");
const $vistaExito = document.getElementById("vista-exito");
const $lista = document.getElementById("lista-items");
const $selectRegimen = document.getElementById("select-regimen");
const $metodoPago = document.getElementById("metodo-pago");
const $btnConfirmar = document.getElementById("btn-confirmar");

const fmt = (n) => "S/ " + Number(n).toLocaleString("es-PE", { minimumFractionDigits: 2 });

/* ------------------------------ Render líneas ----------------------------- */

function renderItems() {
  const items = Carrito.obtener();

  if (!items.length) {
    $vistaCarrito.classList.add("hidden");
    $vistaVacia.classList.remove("hidden");
    return;
  }

  const EMOJIS = ["🛍️", "📦"];
  $lista.innerHTML = items.map((i) => `
    <li class="px-6 py-4 flex items-center gap-4">
      <span class="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-100 to-violet-200 flex items-center justify-center text-2xl shrink-0">${i.emoji || EMOJIS[i.productoId % 2]}</span>
      <div class="flex-1 min-w-0">
        <p class="font-semibold text-slate-800 truncate">${i.nombre}</p>
        <p class="text-[11px] font-mono text-slate-400">${i.sku} · ${fmt(i.precioBase)} c/u</p>
      </div>
      <div class="flex items-center gap-1.5">
        <button data-restar="${i.productoId}" class="w-7 h-7 rounded-full border border-slate-300 hover:bg-slate-100 font-bold text-slate-600 transition">−</button>
        <span class="w-8 text-center font-bold text-slate-800 text-sm">${i.cantidad}</span>
        <button data-sumar="${i.productoId}" class="w-7 h-7 rounded-full border border-slate-300 hover:bg-slate-100 font-bold text-slate-600 transition">+</button>
      </div>
      <div class="w-24 text-right">
        <p class="font-extrabold text-slate-800 text-sm">${fmt(i.precioBase * i.cantidad)}</p>
        <button data-quitar="${i.productoId}" class="text-[10px] text-red-400 hover:text-red-600 underline">quitar</button>
      </div>
    </li>`).join("");

  // Deshabilita confirmar mientras se recalcula; el estado real lo fija recalcular().
}

/* ------------------------- Cálculo de impuestos --------------------------- */

async function recalcular() {
  const base = Carrito.subtotalBase();
  const empresaId = $selectRegimen.value || null;

  // El cálculo SIEMPRE pasa por la capa de negocio (mock o API Java):
  // el frontend nunca inventa la tasa fiscal.
  const t = await calcularImpuestos(base, empresaId);

  document.getElementById("res-subtotal").textContent = fmt(t.subtotal);
  document.getElementById("res-tasa").textContent = Number(t.tasaIva).toFixed(2);
  document.getElementById("res-iva").textContent = fmt(t.iva);
  document.getElementById("res-total").textContent = fmt(t.total);
  document.getElementById("res-regimen").textContent =
    `Régimen aplicado: ${t.banderaEmoji ?? ""} ${t.paisNombre ?? ""} · ${t.regimenFiscal}${empresaId ? "" : " · consumidor final"}`.replace(/\s+/g, " ");

  return t;
}

/* ------------------------------ Selector régimen -------------------------- */

async function cargarEmpresas() {
  const empresas = await getEmpresas();
  empresas.forEach((e) => {
    $selectRegimen.insertAdjacentHTML(
      "beforeend",
      `<option value="${e.idEmpresa}">
         ${e.banderaEmoji} ${e.razonSocial} · ${e.paisNombre ?? "s/país"} · ${e.regimenFiscal} (${Number(e.tasaIva).toFixed(0)}%)
       </option>`
    );
  });
}

/* ------------------------------- Checkout --------------------------------- */

function mostrarExito(orden) {
  Carrito.vaciar();
  actualizarBadgeCarrito();

  $vistaCarrito.classList.add("hidden");
  $vistaVacia.classList.add("hidden");
  $vistaExito.classList.remove("hidden");
  $vistaExito.scrollIntoView({ behavior: "smooth" });

  document.getElementById("exito-folio").textContent = orden.folio;
  document.getElementById("exito-regimen").textContent =
    `${orden.banderaEmoji ?? ""} ${orden.regimenFiscal} · ${orden.paisNombre ?? "Consumidor final"} (${Number(orden.tasaIva).toFixed(0)}%)`;
  document.getElementById("exito-subtotal").textContent = fmt(orden.subtotal);
  document.getElementById("exito-iva").textContent = fmt(orden.iva);
  document.getElementById("exito-total").textContent = fmt(orden.total);
}

async function confirmarPedido() {
  const sesion = Sesion.obtener();
  if (!sesion) {
    mostrarToast("Inicia sesión para completar tu compra 🔒", "error");
    setTimeout(() => (window.location.href = "login.html"), 1200);
    return;
  }
  if (!Carrito.contarUnidades()) return;

  $btnConfirmar.disabled = true;
  $btnConfirmar.textContent = "Procesando...";

  try {
    const payload = {
      items: Carrito.obtener().map((i) => ({ productoId: i.productoId, cantidad: i.cantidad })),
      empresaClienteId: $selectRegimen.value ? Number($selectRegimen.value) : null,
      metodoPago: $metodoPago.value,
    };
    const orden = await crearCheckout(payload); // transacción: stock + orden
    mostrarToast(`✔ Pedido ${orden.folio} registrado`, "exito");
    setTimeout(() => mostrarExito(orden), 700);
  } catch (err) {
    console.error(err);
    mostrarToast(err.message.replace(/^API \d+:.*"error":"|".*$/s, "") || err.message, "error");
    await recalcular(); // por si cambió algo en el servidor
    $btnConfirmar.disabled = false;
    $btnConfirmar.textContent = "Confirmar pedido";
  }
}

/* -------------------------------- Eventos --------------------------------- */

$lista.addEventListener("click", async (e) => {
  const sumar = e.target.closest("[data-sumar]");
  const restar = e.target.closest("[data-restar]");
  const quitar = e.target.closest("[data-quitar]");
  if (!sumar && !restar && !quitar) return;

  const items = Carrito.obtener();
  if (sumar) {
    const it = items.find((i) => i.productoId === Number(sumar.dataset.sumar));
    Carrito.cambiarCantidad(it.productoId, it.cantidad + 1); // respeta stockMax
  } else if (restar) {
    const it = items.find((i) => i.productoId === Number(restar.dataset.restar));
    Carrito.cambiarCantidad(restar.dataset.restar, it.cantidad - 1);
  } else {
    Carrito.eliminar(quitar.dataset.quitar);
  }

  renderItems();
  await recalcular();
});

document.getElementById("btn-vaciar").addEventListener("click", () => {
  Carrito.vaciar();
  renderItems();
});

$selectRegimen.addEventListener("change", recalcular); // recalcula IVA al cambiar régimen

$btnConfirmar.addEventListener("click", confirmarPedido);

/* ------------------------------ Inicialización ---------------------------- */

(async function init() {
  // Barra de sesión compacta para esta página.
  const sesion = Sesion.obtener();
  const $zona = document.getElementById("zona-sesion");
  $zona.innerHTML = sesion
    ? `<span class="text-xs px-2.5 py-1.5 rounded-full bg-indigo-100 text-indigo-700 font-semibold">${sesion.banderaEmoji ?? ""} ${sesion.username}</span>
       <button id="btn-salir" class="text-xs text-slate-500 hover:text-rose-600 underline">Salir</button>`
    : `<a href="index.html" class="text-sm text-slate-600 hover:text-indigo-600">← Tienda</a>
       <a href="login.html" class="text-xs font-semibold bg-indigo-600 text-white px-3 py-1.5 rounded-full">Iniciar sesión</a>`;
  const btnSalir = document.getElementById("btn-salir");
  if (btnSalir) btnSalir.addEventListener("click", () => Sesion.cerrar());

  document.getElementById("nota-login").classList.toggle("hidden", Boolean(sesion));

  // La opción "Consumidor final" refleja el país del usuario logueado.
  if (sesion?.banderaEmoji) {
    $selectRegimen.options[0].textContent =
      `${sesion.banderaEmoji} ${sesion.paisNombre ?? "Consumidor final"} · consumidor final`;
  }

  await cargarEmpresas();
  renderItems();
  await recalcular();
  actualizarBadgeCarrito();
})();
