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
  getPaises,
  getEmpresas,
  crearEmpresa,
  getUsuarios,
  crearUsuario,
  getProveedores,
  crearProveedor,
  getSedes,
  getComparacionSedes,
  getAlertasStock,
  getResumenFinanciero,
} from "./api.js";
import { initReportesAdmin } from "./reportes-admin.js";

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
    garantiaMeses: Number(data.garantiaMeses || 12),
    proveedorId: data.proveedorId ? Number(data.proveedorId) : null,
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
            <p class="font-bold text-slate-800">S/ ${Number(p.precioBase).toLocaleString("es-PE", { minimumFractionDigits: 2 })}</p>
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

/** Calcula y pinta las tarjetas de métricas del panel. Usa stock por sede. */
function renderStats(productos) {
  const sedeId = Number(localStorage.getItem("tm_sede_actual")) || 1;
  const total = productos.length;
  const valor = productos.reduce((sum, p) => {
    const stockSede = (p.sedeStock && p.sedeStock[String(sedeId)] != null) ? p.sedeStock[String(sedeId)] : p.stock;
    return sum + Number(p.precioBase) * stockSede;
  }, 0);
  const bajos = productos.filter((p) => {
    const stockSede = (p.sedeStock && p.sedeStock[String(sedeId)] != null) ? p.sedeStock[String(sedeId)] : p.stock;
    return stockSede <= p.stockMinimo;
  }).length;

  document.getElementById("stat-total").textContent = total;
  document.getElementById("stat-valor").textContent =
    "S/ " + valor.toLocaleString("es-PE", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  document.getElementById("stat-bajo").textContent = bajos;
  document.getElementById("stat-categorias").textContent =
    cacheCategorias.filter((c) => c.activa !== false).length;

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
  $form.garantiaMeses.value = producto.garantiaMeses ?? 12;
  $form.proveedorId.value = producto.proveedorId ? String(producto.proveedorId) : "";
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
  localStorage.removeItem("tm_mock_db_v3"); // clave usada por MockDB en api.js
  location.reload();
});

/* ------------------------------ Inicialización ---------------------------- */

(async function init() {
  renderSesionAdmin();
  await cargarCategoriasEnSelect();
  await initProveedores();
  await renderTabla();
  await initEmpresas();
  await initReportesAdmin();
  document.getElementById("rep-generado").textContent = sesionActual?.username ? `Generado por ${sesionActual.username}` : ``;
  await refreshDashboard();
})();

/* ------------------- Escuchar cambio de sede para recargar datos ----------- */
window.addEventListener("sedeCambiada", async () => {
  await renderTabla();
  await initReportesAdmin();
  document.getElementById("rep-generado").textContent = sesionActual?.username ? `Generado por ${sesionActual.username}` : ``;
  await refreshDashboard();
});

/* ============================================================================
 * PROVEEDORES — cadena de suministro: alta y catálogo (solo ADMIN).
 * ========================================================================== */

const $formProveedor = document.getElementById("form-proveedor");
const $tablaProveedores = document.getElementById("tabla-proveedores");
const $proveedoresContador = document.getElementById("proveedores-contador");
const $selectProvForm = document.querySelector('#form-producto select[name="proveedorId"]');

/** Pinta la tabla de proveedores y llena el select del formulario producto. */
async function initProveedores() {
  try {
    const proveedores = await getProveedores();

    $proveedoresContador.textContent = `${proveedores.length} activo(s)`;
    $tablaProveedores.innerHTML = proveedores.length
      ? proveedores
          .map(
            (prov) => `
        <tr class="hover:bg-indigo-50/40 transition">
          <td class="px-5 py-3">
            <p class="font-semibold text-slate-800">${prov.nombre}</p>
            <p class="text-[11px] text-slate-400">ID #${prov.idProveedor}</p>
          </td>
          <td class="px-4 py-3 text-slate-600">${prov.contactoNombre || "—"}</td>
          <td class="px-4 py-3 font-mono text-xs text-slate-600">${prov.telefono || "—"}</td>
          <td class="px-4 py-3 text-slate-600 truncate max-w-[180px]">${prov.email || "—"}</td>
        </tr>`
          )
          .join("")
      : `<tr><td colspan="4" class="px-4 py-8 text-center text-slate-400">Sin proveedores registrados.</td></tr>`;

    // Select del formulario de producto (sin duplicar opciones).
    $selectProvForm.innerHTML = `<option value="">— Sin proveedor asignado —</option>`;
    proveedores.forEach((prov) =>
      $selectProvForm.insertAdjacentHTML(
        "beforeend",
        `<option value="${prov.idProveedor}">${prov.nombre}</option>`
      )
    );
  } catch (err) {
    console.error(err);
  }
}

$formProveedor.addEventListener("submit", async (e) => {
  e.preventDefault();
  const data = Object.fromEntries(new FormData($formProveedor).entries());
  const $msg = document.getElementById("form-proveedor-mensaje");

  try {
    await crearProveedor({
      nombre: data.nombre.trim(),
      contactoNombre: (data.contactoNombre || "").trim() || null,
      telefono: (data.telefono || "").trim() || null,
      email: (data.email || "").trim() || null,
    });
    mostrarToast(`Proveedor "${data.nombre}" registrado`, "exito");
    $msg.textContent = "✔ Proveedor registrado";
    setTimeout(() => ($msg.textContent = ""), 3000);
    $formProveedor.reset();
    await initProveedores();
  } catch (err) {
    console.error(err);
    $msg.textContent = `✖ ${err.message}`;
    $msg.className = "text-center text-xs text-red-500";
    setTimeout(() => {
      $msg.textContent = "";
      $msg.className = "text-center text-xs";
    }, 4000);
  }
});

/* ============================================================================
 * EMPRESAS CLIENTES B2B — registro por país (el IVA depende del país).
 * ========================================================================== */

const $formEmpresa = document.getElementById("form-empresa");
const $selectPais = $formEmpresa.elements["idPais"];
const $tablaEmpresas = document.getElementById("tabla-empresas");

/** Llena los selectores de países (empresas y usuarios) con su bandera. */
async function cargarPaises() {
  const paises = await getPaises();
  const opciones =
    '<option value="">Selecciona país…</option>' +
    paises
      .map(
        (p) => `<option value="${p.idPais}">
                  ${banderaDe(p.codigoIso2)} ${p.nombre} · IVA ${Number(p.tasaIvaGeneral).toFixed(0)}%
                </option>`
      )
      .join("");
  $selectPais.innerHTML = opciones; // formulario de empresas
  $selectPaisUsuario.innerHTML = opciones.replace(
    "IVA", "· IVA"); // mismo catálogo para el alta de usuarios
}

function badgeRegimen(regimen) {
  const estilos = {
    GENERAL: "bg-indigo-100 text-indigo-700",
    REDUCIDO: "bg-amber-100 text-amber-700",
    EXENTO: "bg-emerald-100 text-emerald-700",
  };
  return `<span class="text-[11px] font-bold px-2.5 py-1 rounded-full ${estilos[regimen] ?? "bg-slate-100 text-slate-600"}">${regimen}</span>`;
}

async function renderEmpresas() {
  const empresas = await getEmpresas();
  document.getElementById("empresas-contador").textContent = `${empresas.length} registrada(s)`;

  if (!empresas.length) {
    $tablaEmpresas.innerHTML = `<tr><td colspan="5" class="px-4 py-8 text-center text-slate-400">
      Aún no hay empresas clientes registradas.</td></tr>`;
    return;
  }

  $tablaEmpresas.innerHTML = empresas
    .map(
      (e) => `
      <tr class="hover:bg-emerald-50/40 transition">
        <td class="px-5 py-3">
          <p class="font-semibold text-slate-800">${e.banderaEmoji} ${e.razonSocial}</p>
          ${e.contactoEmail ? `<p class="text-[11px] text-slate-400">${e.contactoEmail}</p>` : ""}
        </td>
        <td class="px-4 py-3 text-slate-600">${e.paisNombre ?? "🌐 s/país"}</td>
        <td class="px-4 py-3 font-mono text-xs text-slate-500">${e.rfc}</td>
        <td class="px-4 py-3 text-center">${badgeRegimen(e.regimenFiscal)}</td>
        <td class="px-5 py-3 text-right">
          <b class="text-slate-800">${Number(e.tasaIva).toFixed(2)}%</b>
          <p class="text-[10px] text-slate-400">según su país</p>
        </td>
      </tr>`
    )
    .join("");
}

$formEmpresa.addEventListener("submit", async (e) => {
  e.preventDefault();

  // El frontend NO envía la tasa: el backend/mock la deriva del país elegido.
  const datos = Object.fromEntries(new FormData($formEmpresa).entries());
  try {
    await crearEmpresa({
      razonSocial: datos.razonSocial,
      rfc: datos.rfc,
      idPais: Number(datos.idPais),
      regimenFiscal: datos.regimenFiscal,
      contactoEmail: datos.contactoEmail || null,
    });

    mostrarToast(`✔ ${datos.razonSocial} registrada`, "exito");
    $formEmpresa.reset();
    await cargarPaises(); // repoblar select tras reset()
    await renderEmpresas();
  } catch (err) {
    console.error(err);
    mostrarToast(err.message || "No se pudo registrar la empresa", "error");
  }
});

async function initEmpresas() {
  await cargarPaises();
  await renderEmpresas();
}

/* ============================================================================
 * USUARIOS — alta de personal (CAJERO/ADMIN) y listado con rol + país.
 * ========================================================================== */

const $formUsuario = document.getElementById("form-usuario");
const $tablaUsuarios = document.getElementById("tabla-usuarios");
const $selectPaisUsuario = $formUsuario.elements["idPais"];

function badgeRol(rol) {
  const estilos = {
    ADMIN: "bg-amber-100 text-amber-800",
    CAJERO: "bg-sky-100 text-sky-700",
    CLIENTE: "bg-emerald-100 text-emerald-700",
  };
  return `<span class="text-[11px] font-bold px-2.5 py-1 rounded-full ${estilos[rol] ?? "bg-slate-100 text-slate-600"}">${rol}</span>`;
}

async function renderUsuarios() {
  const usuarios = await getUsuarios();
  document.getElementById("usuarios-contador").textContent = `${usuarios.length} cuenta(s)`;

  $tablaUsuarios.innerHTML = usuarios
    .map(
      (u) => `
      <tr class="hover:bg-indigo-50/40 transition">
        <td class="px-5 py-3">
          <p class="font-semibold text-slate-800">
            ${u.banderaEmoji ?? ""} ${u.nombreCompleto ?? u.username}
          </p>
          <p class="text-[11px] font-mono text-slate-400">@${u.username}</p>
        </td>
        <td class="px-4 py-3 text-center">${badgeRol(u.rol)}</td>
        <td class="px-4 py-3 text-xs text-slate-500">${u.sedeNombre ?? "—"}</td>
        <td class="px-4 py-3 text-center text-xs font-mono text-slate-600">${u.cajaNumero != null ? "Caja #" + u.cajaNumero : "—"}</td>
        <td class="px-4 py-3 text-xs text-slate-500">${u.email ?? "-"}</td>
        <td class="px-5 py-3 text-right text-slate-600">${u.paisNombre ?? "🌐 s/país"}</td>
      </tr>`
    )
    .join("");
}

$formUsuario.addEventListener("submit", async (e) => {
  e.preventDefault();

  const datos = Object.fromEntries(new FormData($formUsuario).entries());
  try {
    await crearUsuario(
      {
        username: datos.username.trim(),
        email: datos.email.trim(),
        password: datos.password,
        nombreCompleto: datos.nombreCompleto.trim(),
        idPais: Number(datos.idPais),
        sedeId: datos.sedeId ? Number(datos.sedeId) : null,
        cajaNumero: datos.cajaNumero ? Number(datos.cajaNumero) : null,
      },
      datos.rol
    );

    mostrarToast(`✔ Usuario @${datos.username} creado como ${datos.rol}`, "exito");
    $formUsuario.reset();
    await cargarPaises();
    await renderUsuarios();
  } catch (err) {
    console.error(err);
    mostrarToast(err.message || "No se pudo crear el usuario", "error");
  }
});

(async function initUsuarios() {
  // Cargar sedes en el select
  try {
    const sedes = await getSedes();
    const $selectSede = $formUsuario.elements["sedeId"];
    if ($selectSede) {
      sedes.forEach((s) =>
        $selectSede.insertAdjacentHTML("beforeend", `<option value="${s.idSede}">${s.nombre}</option>`)
      );
    }
  } catch (err) { console.error(err); }
  await renderUsuarios();
})();

/* ============================================================================
 * DASHBOARD EJECUTIVO — Comparación de sedes, alertas, finanzas.
 * ========================================================================== */

/** Renderiza tarjetas comparativas de cada sede. */
async function renderComparacionSedes() {
  try {
    const sedes = await getComparacionSedes();
    const grid = document.getElementById("sede-comparacion-grid");
    if (!grid) return;

    const colores = {
      1: { gradient: "from-rose-500 to-pink-600", ring: "ring-rose-200" },
      2: { gradient: "from-sky-500 to-blue-600", ring: "ring-sky-200" },
      3: { gradient: "from-emerald-500 to-teal-600", ring: "ring-emerald-200" },
      4: { gradient: "from-amber-500 to-orange-600", ring: "ring-amber-200" },
    };

    grid.innerHTML = sedes.map((s) => {
      const c = colores[s.idSede] || colores[1];
      const isActive = Number(localStorage.getItem("tm_sede_actual")) === s.idSede;
      return `
        <div class="relative overflow-hidden rounded-2xl bg-white border ${isActive ? "ring-2 ring-indigo-400 shadow-lg shadow-indigo-200/40" : "border-slate-100"} p-5 transition-all hover:shadow-lg cursor-pointer"
             onclick="window.cambiarSede('${s.idSede}')">
          <div class="absolute -right-6 -top-6 w-24 h-24 rounded-full bg-gradient-to-br ${c.gradient} opacity-10 blur-xl"></div>
          <div class="flex items-center gap-2 mb-3">
            <span class="w-8 h-8 rounded-lg bg-gradient-to-br ${c.gradient} flex items-center justify-center text-white text-[10px] font-black shadow">T${s.idSede}</span>
            <div>
              <h3 class="font-bold text-slate-800 text-sm">${s.nombre}</h3>
              <p class="text-[10px] text-slate-400">${s.ordenesPagadas} órdenes · ${s.incidenciasAbiertas + s.stockBajo} alertas</p>
            </div>
            ${isActive ? '<span class="ml-auto text-[9px] font-bold bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded-full">ACTIVA</span>' : ""}
          </div>
          <div class="grid grid-cols-2 gap-3 text-center">
            <div class="bg-slate-50 rounded-xl p-2.5">
              <p class="text-[10px] text-slate-400 font-semibold uppercase">Ventas</p>
              <p class="text-base font-black text-emerald-600">S/ ${s.totalVentas.toLocaleString("es-PE", { maximumFractionDigits: 0 })}</p>
            </div>
            <div class="bg-slate-50 rounded-xl p-2.5">
              <p class="text-[10px] text-slate-400 font-semibold uppercase">Caja</p>
              <p class="text-base font-black text-indigo-600">S/ ${s.efectivo.toLocaleString("es-PE", { maximumFractionDigits: 0 })}</p>
            </div>
            <div class="bg-slate-50 rounded-xl p-2.5">
              <p class="text-[10px] text-slate-400 font-semibold uppercase">Ticket</p>
              <p class="text-sm font-bold text-slate-700">S/ ${s.ticketPromedio.toLocaleString("es-PE", { maximumFractionDigits: 0 })}</p>
            </div>
            <div class="bg-slate-50 rounded-xl p-2.5">
              <p class="text-[10px] text-slate-400 font-semibold uppercase">Alertas</p>
              <p class="text-sm font-bold ${s.incidenciasAbiertas > 0 ? "text-red-500" : "text-emerald-500"}">${s.incidenciasAbiertas > 0 ? "⚠️ " + s.incidenciasAbiertas : "✅ 0"}</p>
            </div>
          </div>
          <div class="mt-3 flex flex-wrap gap-1.5">
            ${Object.entries(s.metodosPago).map(([met, cnt]) =>
              `<span class="text-[9px] bg-slate-100 text-slate-500 px-2 py-0.5 rounded-full font-semibold">${met}: ${cnt}</span>`
            ).join("")}
          </div>
        </div>`;
    }).join("");
  } catch (err) {
    console.error("Error renderizando comparación de sedes:", err);
  }
}

/** Renderiza alertas de stock e incidencias. */
async function renderAlertas() {
  try {
    const alertas = await getAlertasStock();
    const grid = document.getElementById("alertas-grid");
    const contador = document.getElementById("alertas-contador");
    const badge = document.getElementById("notif-badge");
    if (!grid) return;

    const criticas = alertas.filter((a) => a.severidad === "CRITICA");
    const alertasNorm = alertas.filter((a) => a.severidad === "ALERTA");
    if (contador) contador.textContent = `${alertas.length} alerta${alertas.length !== 1 ? "s" : ""}`;
    if (badge) {
      if (criticas.length > 0) {
        badge.textContent = criticas.length;
        badge.classList.remove("hidden");
      } else {
        badge.classList.add("hidden");
      }
    }

    if (!alertas.length) {
      grid.innerHTML = `<div class="bg-emerald-50 border border-emerald-100 rounded-2xl p-6 text-center">
        <p class="text-3xl mb-2">✅</p>
        <p class="text-sm font-semibold text-emerald-700">Sin alertas activas</p>
        <p class="text-xs text-emerald-500 mt-1">Todos los stocks están en niveles normales.</p>
      </div>`;
      return;
    }

    const severidadEstilo = {
      CRITICA: { bg: "bg-red-50", border: "border-red-200", badge: "bg-red-100 text-red-700", icon: "🔴" },
      ALERTA: { bg: "bg-amber-50", border: "border-amber-200", badge: "bg-amber-100 text-amber-700", icon: "🟡" },
      INFO: { bg: "bg-sky-50", border: "border-sky-200", badge: "bg-sky-100 text-sky-700", icon: "🔵" },
    };

    grid.innerHTML = alertas.slice(0, 20).map((a) => {
      const e = severidadEstilo[a.severidad] || severidadEstilo.INFO;
      return `
        <div class="flex items-start gap-3 ${e.bg} border ${e.border} rounded-xl p-3.5 transition hover:shadow-sm">
          <span class="text-lg mt-0.5">${e.icon}</span>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-semibold text-slate-800 truncate">${a.mensaje}</p>
            <div class="flex items-center gap-2 mt-1 flex-wrap">
              <span class="text-[10px] font-bold ${e.badge} px-2 py-0.5 rounded-full">${a.severidad}</span>
              <span class="text-[10px] text-slate-400">SKU: ${a.sku}</span>
              <span class="text-[10px] text-slate-400">Stock: ${a.stock}/${a.stockMinimo}</span>
            </div>
          </div>
          <button onclick="window.cambiarSede('${a.sedeId}')" title="Ir a ${a.sede}"
                  class="text-[10px] font-bold text-indigo-500 hover:text-indigo-700 whitespace-nowrap">
            ${a.sede} →
          </button>
        </div>`;
    }).join("");
  } catch (err) {
    console.error("Error renderizando alertas:", err);
  }
}

/** Renderiza resumen financiero consolidado. */
async function renderFinanzas() {
  try {
    const data = await getResumenFinanciero();
    const grid = document.getElementById("finanzas-grid");
    const sedesGrid = document.getElementById("finanzas-sedes");
    if (!grid) return;

    const c = data.consolidado;
    grid.innerHTML = `
      <div class="rounded-2xl bg-gradient-to-br from-emerald-50 to-emerald-100/50 border border-emerald-200 p-5">
        <p class="text-[10px] uppercase tracking-widest font-bold text-emerald-500">Ingresos totales</p>
        <p class="text-2xl font-black text-emerald-700 mt-1">S/ ${c.ingresosTotales.toLocaleString("es-PE", { minimumFractionDigits: 2 })}</p>
        <p class="text-[10px] text-emerald-500 mt-1">Todas las sedes · solo PAGADAS</p>
      </div>
      <div class="rounded-2xl bg-gradient-to-br from-indigo-50 to-indigo-100/50 border border-indigo-200 p-5">
        <p class="text-[10px] uppercase tracking-widest font-bold text-indigo-500">IVA recaudado</p>
        <p class="text-2xl font-black text-indigo-700 mt-1">S/ ${c.ivaTotal.toLocaleString("es-PE", { minimumFractionDigits: 2 })}</p>
        <p class="text-[10px] text-indigo-500 mt-1">18% IGV · Perú</p>
      </div>
      <div class="rounded-2xl bg-gradient-to-br from-violet-50 to-violet-100/50 border border-violet-200 p-5">
        <p class="text-[10px] uppercase tracking-widest font-bold text-violet-500">Efectivo en cajas</p>
        <p class="text-2xl font-black text-violet-700 mt-1">S/ ${c.efectivoTotal.toLocaleString("es-PE", { minimumFractionDigits: 2 })}</p>
        <p class="text-[10px] text-violet-500 mt-1">Consolidado 3 sedes</p>
      </div>
      <div class="rounded-2xl bg-gradient-to-br from-amber-50 to-amber-100/50 border border-amber-200 p-5">
        <p class="text-[10px] uppercase tracking-widest font-bold text-amber-500">Retiros</p>
        <p class="text-2xl font-black text-amber-700 mt-1">S/ ${c.retirosTotales.toLocaleString("es-PE", { minimumFractionDigits: 2 })}</p>
        <p class="text-[10px] text-amber-500 mt-1">Bóveda / depósitos</p>
      </div>`;

    if (sedesGrid) {
      sedesGrid.innerHTML = data.sedes.map((s) => `
        <div class="bg-white border border-slate-100 rounded-2xl p-5 hover:shadow-md transition">
          <h4 class="font-bold text-slate-800 text-sm mb-3">${s.nombre}</h4>
          <div class="space-y-2 text-xs">
            <div class="flex justify-between"><span class="text-slate-500">Ingresos</span><b class="text-emerald-600">S/ ${s.ingresos.toLocaleString("es-PE", { minimumFractionDigits: 2 })}</b></div>
            <div class="flex justify-between"><span class="text-slate-500">IVA recaudado</span><b class="text-indigo-600">S/ ${s.ivaRecaudado.toLocaleString("es-PE", { minimumFractionDigits: 2 })}</b></div>
            <div class="flex justify-between"><span class="text-slate-500">Efectivo actual</span><b class="text-slate-800">S/ ${s.efectivoActual.toLocaleString("es-PE", { minimumFractionDigits: 2 })}</b></div>
            <div class="flex justify-between"><span class="text-slate-500">Fondos recibidos</span><span class="text-slate-600">S/ ${s.fondosRecibidos.toLocaleString("es-PE", { minimumFractionDigits: 2 })}</span></div>
            <div class="flex justify-between"><span class="text-slate-500">Retiros</span><span class="text-red-500">S/ ${s.retirosRealizados.toLocaleString("es-PE", { minimumFractionDigits: 2 })}</span></div>
            <div class="border-t border-slate-100 pt-2 flex justify-between"><span class="text-slate-500">Movimientos</span><span class="font-semibold text-slate-700">${s.movimientosTotales}</span></div>
          </div>
        </div>`).join("");
    }
  } catch (err) {
    console.error("Error renderizando finanzas:", err);
  }
}

/** Actualiza las secciones del dashboard ejecutivo. */
async function refreshDashboard() {
  await Promise.all([
    renderComparacionSedes(),
    renderAlertas(),
    renderFinanzas(),
  ]);
}
