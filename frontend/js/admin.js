/* ============================================================================
 * admin.js — Vista ADMINISTRADOR.
 * CRUD completo de inventario: formulario alta/edición + tabla de gestión.
 * Consume datos EXCLUSIVAMENTE a través de la capa api.js (mock o API real).
 * ========================================================================== */

import {
  getProductos,
  getCategorias,
  crearProducto,
  actualizarProducto,
  eliminarProducto,
} from "./api.js";

const $form = document.getElementById("form-producto");
const $formTitulo = document.getElementById("form-titulo");
const $btnGuardar = document.getElementById("btn-guardar");
const $btnCancelar = document.getElementById("btn-cancelar");
const $formMensaje = document.getElementById("form-mensaje");
const $selectCatForm = document.querySelector('#form-producto select[name="categoriaId"]');
const $tabla = document.getElementById("tabla-productos");
const $buscadorAdmin = document.getElementById("buscador-admin");
const $btnResetMock = document.getElementById("btn-reset-mock");

let idEditando = null; // null = modo creación; número = modo edición
let cacheCategorias = [];

/* ------------------- Guardia de acceso (rol exclusivo) -------------------- */
/**
 * El panel admin es EXCLUSIVO del rol ADMIN. La validación fuerte vive en
 * el backend (Spring Security responde 403 a no-admins), esta guardia solo
 * oculta la interfaz a usuarios sin permiso.
 */
const sesionActual = Sesion.obtener();
if (!sesionActual || sesionActual.rol !== "ADMIN") {
  document.body.innerHTML = `
    <main class="min-h-screen flex items-center justify-center bg-slate-100 px-4">
      <div class="bg-white rounded-2xl shadow-lg p-10 text-center max-w-md">
        <p class="text-5xl mb-4">🔒</p>
        <h1 class="text-xl font-bold text-slate-800 mb-2">Acceso restringido</h1>
        <p class="text-sm text-slate-500 mb-6">
          ${sesionActual
            ? `Tu rol <b>${sesionActual.rol}</b> no tiene permisos para este panel.`
            : "No has iniciado sesión."}
          Esta sección requiere el rol <b>ADMIN</b>.
        </p>
        <a href="login.html" class="inline-block bg-indigo-600 hover:bg-indigo-700 text-white font-semibold px-5 py-2 rounded-lg">
          Ir a iniciar sesión
        </a>
      </div>
    </main>`;
  throw new Error("403: vista admin requiere rol ADMIN");
}

/** Barra de sesión del navbar del panel. */
function renderSesionAdmin() {
  const $zona = document.getElementById("zona-sesion");
  $zona.innerHTML = `
    <span class="text-xs font-semibold bg-amber-400 text-amber-950 px-2 py-1 rounded-full">${sesionActual.username}</span>
    <span class="text-[10px] uppercase tracking-wide text-slate-300">ADMIN</span>
    <a href="index.html" class="text-sm underline decoration-dotted hover:text-slate-300">Catálogo</a>
    <button id="btn-logout" class="text-xs underline decoration-dotted hover:text-slate-300">Cerrar sesión</button>`;
  document.getElementById("btn-logout").addEventListener("click", () => Sesion.cerrar());
}

/* ------------------------------- Utilidades ------------------------------- */

function badgeStock(p) {
  if (p.stock === 0) return `<span class="badge badge-agotado">Agotado</span>`;
  if (p.stock <= p.stockMinimo) return `<span class="badge badge-bajo">Bajo (${p.stock})</span>`;
  return `<span class="badge badge-ok">OK (${p.stock})</span>`;
}

/** Iniciales del SKU para el avatar circular de cada fila. */
function inicialesDe(p) {
  return p.sku.split("-")[0].slice(0, 2).toUpperCase();
}

function mostrarMensaje(texto, esError = false) {
  $formMensaje.textContent = texto;
  $formMensaje.className = `text-center text-sm ${esError ? "text-red-600" : "text-emerald-700"}`;
  setTimeout(() => ($formMensaje.textContent = ""), 3500);
}

/** Serializa el formulario a un objeto plano listo para la capa api.js. */
function leerFormulario() {
  const data = Object.fromEntries(new FormData($form).entries());
  return {
    sku: data.sku.trim().toUpperCase(),
    nombre: data.nombre.trim(),
    descripcion: (data.descripcion || "").trim(),
    categoriaId: Number(data.categoriaId),
    precioBase: Number(data.precioBase),
    stock: Number(data.stock),
    stockMinimo: Number(data.stockMinimo),
    imagenUrl: (data.imagenUrl || "").trim(),
    activo: $form.elements["activo"].checked,
  };
}

/* ------------------------------ Render tabla ------------------------------ */

async function renderTabla() {
  try {
    const productos = await getProductos({
      busqueda: $buscadorAdmin.value.trim(),
      incluirInactivos: true, // el admin ve también productos desactivados
    });

    renderStats(productos);

    if (!productos.length) {
      $tabla.innerHTML = `<tr><td colspan="6" class="px-4 py-10 text-center text-slate-400">
        Sin productos registrados.</td></tr>`;
      return;
    }

    $tabla.innerHTML = productos
      .map(
        (p) => `
        <tr class="hover:bg-indigo-50/40 transition">
          <td class="px-5 py-3">
            <div class="flex items-center gap-3">
              <span class="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-100 to-violet-200 flex items-center justify-center text-[10px] font-extrabold text-indigo-700 shrink-0">
                ${inicialesDe(p)}
              </span>
              <div class="min-w-0">
                <p class="font-semibold text-slate-800 truncate max-w-[220px]">${p.nombre}</p>
                <p class="text-[11px] font-mono text-slate-400">${p.sku} · #${p.idProducto}</p>
              </div>
            </div>
          </td>
          <td class="px-4 py-3">
            <span class="text-xs bg-slate-100 text-slate-600 px-2.5 py-1 rounded-full">${p.categoriaNombre ?? "-"}</span>
          </td>
          <td class="px-4 py-3 text-right">
            <p class="font-bold text-slate-800">$${Number(p.precioBase).toLocaleString("es-MX", { minimumFractionDigits: 2 })}</p>
            <p class="text-[10px] text-slate-400">sin IVA</p>
          </td>
          <td class="px-4 py-3 text-center font-semibold">${p.stock}</td>
          <td class="px-4 py-3 text-center">
            ${badgeStock(p)}
            ${p.activo === false ? '<p class="text-[10px] text-slate-400 mt-1">(inactivo)</p>' : ""}
          </td>
          <td class="px-5 py-3 text-center whitespace-nowrap">
            <button data-editar="${p.idProducto}"
                    class="bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-semibold px-3 py-1.5 rounded-lg mr-2 transition">✎ Editar</button>
            <button data-eliminar="${p.idProducto}"
                    class="bg-red-50 hover:bg-red-100 text-red-600 text-xs font-semibold px-3 py-1.5 rounded-lg transition">🗑</button>
          </td>
        </tr>`
      )
      .join("");
  } catch (err) {
    console.error(err);
    mostrarMensaje("Error cargando inventario", true);
  }
}

/** Calcula y pinta las tarjetas de métricas del panel. */
function renderStats(productos) {
  const total = productos.length;
  const valor = productos.reduce((sum, p) => sum + Number(p.precioBase) * p.stock, 0);
  const bajos = productos.filter((p) => p.stock <= p.stockMinimo).length;

  document.getElementById("stat-total").textContent = total;
  document.getElementById("stat-valor").textContent =
    "$" + valor.toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  document.getElementById("stat-bajo").textContent = bajos;
  document.getElementById("stat-categorias").textContent =
    cacheCategorias.filter((c) => c.activa !== false).length;

  // Aviso proactivo si hay faltantes (anticipa alertas del dashboard, Entregable 3).
  if (bajos > 0 && !renderStats._avisado) {
    renderStats._avisado = true;
    mostrarToast(`⚠️ ${bajos} producto(s) con stock bajo requieren reposición`, "info");
  }
}

/* --------------------------- Formulario (CRUD) ---------------------------- */

async function cargarCategoriasEnSelect() {
  cacheCategorias = await getCategorias();
  cacheCategorias.forEach((c) => {
    $selectCatForm.insertAdjacentHTML(
      "beforeend",
      `<option value="${c.idCategoria}">${c.nombre}</option>`
    );
  });
}

function entrarModoEdicion(producto) {
  idEditando = producto.idProducto;
  $formTitulo.textContent = `✎ Editando #${producto.idProducto}`;
  $btnGuardar.textContent = "Actualizar";
  $btnCancelar.classList.remove("hidden");

  $form.sku.value = producto.sku;
  $form.nombre.value = producto.nombre;
  $form.descripcion.value = producto.descripcion ?? "";
  $form.categoriaId.value = String(producto.categoriaId);
  $form.precioBase.value = producto.precioBase;
  $form.stock.value = producto.stock;
  $form.stockMinimo.value = producto.stockMinimo;
  $form.imagenUrl.value = producto.imagenUrl ?? "";
  $form.activo.checked = producto.activo !== false;

  window.scrollTo({ top: 0, behavior: "smooth" });
}

function salirModoEdicion() {
  idEditando = null;
  $form.reset();
  $formTitulo.textContent = "➕ Agregar producto";
  $btnGuardar.textContent = "Guardar";
  $btnCancelar.classList.add("hidden");
}

$form.addEventListener("submit", async (e) => {
  e.preventDefault();
  const datos = leerFormulario();

  // Validación mínima en cliente (la validación fuerte vive en el backend).
  if (!datos.categoriaId) return mostrarMensaje("Selecciona una categoría", true);

  try {
    if (idEditando === null) {
      await crearProducto(datos);
      mostrarMensaje("✔ Producto creado");
      mostrarToast(`"${datos.nombre}" agregado al inventario`, "exito");
    } else {
      await actualizarProducto(idEditando, datos);
      mostrarMensaje("✔ Producto actualizado");
      mostrarToast(`"${datos.nombre}" actualizado`, "exito");
    }
    salirModoEdicion();
    await renderTabla();
  } catch (err) {
    console.error(err);
    mostrarMensaje(`Error: ${err.message}`, true);
  }
});

$btnCancelar.addEventListener("click", salirModoEdicion);

/* ------------------------- Acciones de la tabla --------------------------- */

$tabla.addEventListener("click", async (e) => {
  const btnEditar = e.target.closest("[data-editar]");
  const btnEliminar = e.target.closest("[data-eliminar]");
  if (!btnEditar && !btnEliminar) return;

  try {
    if (btnEditar) {
      const id = btnEditar.dataset.editar;
      const todos = await getProductos({ incluirInactivos: true });
      const producto = todos.find((x) => String(x.idProducto) === String(id));
      if (producto) entrarModoEdicion(producto);
    } else if (btnEliminar) {
      const id = btnEliminar.dataset.eliminar;
      if (confirm(`¿Eliminar el producto #${id}? Esta acción no se puede deshacer.`)) {
        await eliminarProducto(id);
        if (String(idEditando) === String(id)) salirModoEdicion();
        mostrarMensaje("🗑 Producto eliminado");
        mostrarToast("Producto eliminado del inventario", "info");
        await renderTabla();
      }
    }
  } catch (err) {
    console.error(err);
    mostrarMensaje(`Error: ${err.message}`, true);
  }
});

$buscadorAdmin.addEventListener("input", () => renderTabla());

// Reset solo útil en modo mock: restaura los datos semilla originales.
$btnResetMock.addEventListener("click", () => {
  if (CONFIG.USE_API) return mostrarMensaje("Solo aplica en modo mock", true);
  localStorage.removeItem("tm_mock_db"); // clave usada por MockDB en api.js
  location.reload();
});

/* ------------------------------ Inicialización ---------------------------- */

(async function init() {
  renderSesionAdmin();
  await cargarCategoriasEnSelect();
  await renderTabla();
})();
