/* ============================================================================
 * registro.js — AUTOREGISTRO público de cuentas CLIENTE (Entregable 2).
 *
 * Reglas de negocio:
 *  - Cualquiera puede registrarse; el rol asignado SIEMPRE es CLIENTE.
 *    Los roles internos (CAJERO/ADMIN) solo los crea un ADMIN en su panel.
 *  - El usuario elige su PAÍS fiscal: ese país define su IVA de consumidor
 *    final en carrito y checkout.
 *  - Tras el registro exitoso se inicia sesión automáticamente.
 * ========================================================================== */

import { getPaises, registrarme, validarLogin } from "./api.js";

const $form = document.getElementById("form-registro");
const $mensaje = document.getElementById("mensaje");
const $btn = $form.querySelector('button[type="submit"]');
const $selectPais = $form.elements["idPais"];

function mostrarMensaje(texto, esError) {
  $mensaje.textContent = texto;
  $mensaje.className = `text-sm text-center ${esError ? "text-red-600" : "text-emerald-700"}`;
}

/* ------------------------- Catálogo de países ----------------------------- */

(async function cargarPaises() {
  try {
    const paises = await getPaises();
    // Orden alfabético con bandera: "🇦🇷 Argentina".
    paises
      .sort((a, b) => String(a.nombre).localeCompare(String(b.nombre)))
      .forEach((p) =>
        $selectPais.insertAdjacentHTML(
          "beforeend",
          `<option value="${p.idPais}">${banderaDe(p.codigoIso2)} ${p.nombre} · IVA ${Number(p.tasaIvaGeneral).toFixed(0)}%</option>`
        )
      );
  } catch (err) {
    console.error(err);
    $selectPais.innerHTML = '<option value="">No se pudieron cargar los países</option>';
  }
})();

/* ------------------------------ Envío del form ---------------------------- */

$form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const datos = Object.fromEntries(new FormData($form).entries());

  if (!$selectPais.value) return mostrarMensaje("Selecciona tu país", true);
  if (datos.password !== datos.password2)
    return mostrarMensaje("Las contraseñas no coinciden", true);

  $btn.disabled = true;
  $btn.textContent = "Creando cuenta…";
  $mensaje.classList.add("hidden");

  try {
    await registrarme({
      username: datos.username.trim(),
      email: datos.email.trim(),
      password: datos.password,
      nombreCompleto: datos.nombreCompleto.trim(),
      idPais: Number($selectPais.value),
    });

    // Auto-login inmediato para una experiencia fluida.
    const sesion = await validarLogin(datos.username.trim(), datos.password);
    Sesion.guardar(sesion);

    mostrarToast(`✔ ¡Bienvenido/a ${sesion.banderaEmoji ?? ""} ${sesion.username}!`, "exito");
    setTimeout(() => (window.location.href = "index.html"), 800);
  } catch (err) {
    console.error(err);
    mostrarMensaje(err.message || "No se pudo crear la cuenta", true);
    $btn.disabled = false;
    $btn.textContent = "Crear mi cuenta";
  }
});

document.getElementById("modo-etiqueta").textContent = CONFIG.USE_API
  ? "API REST Spring Boot"
  : "modo mock";
