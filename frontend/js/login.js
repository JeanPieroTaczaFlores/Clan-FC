/* ============================================================================
 * login.js — Pantalla de autenticación.
 * Valida las credenciales (mock o API Java según CONFIG.USE_API) y guarda
 * la sesión en sessionStorage antes de redirigir al catálogo.
 * ========================================================================== */

import { validarLogin } from "./api.js";

const $form = document.getElementById("form-login");
const $mensaje = document.getElementById("mensaje");
const $subtitulo = document.getElementById("subtitulo");
const $btn = $form.querySelector('button[type="submit"]');

/* --------------------------- Utilidades visuales -------------------------- */

function mostrarMensaje(texto, esError) {
  $mensaje.textContent = texto;
  $mensaje.className = `text-sm text-center ${esError ? "text-red-600" : "text-emerald-700"}`;
}

function setCargando(cargando) {
  $btn.disabled = cargando;
  $btn.textContent = cargando ? "Validando..." : "Ingresar";
}

/* ------------------------------ Envío del form ---------------------------- */

const DESTINO_POR_ROL = { ADMIN: "admin.html", CAJERO: "pos.html", CLIENTE: "index.html" };

$form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const { username, password } = Object.fromEntries(new FormData($form).entries());
  setCargando(true);
  $mensaje.classList.add("hidden");

  try {
    // validarLogin devuelve { username, rol, credencialBase64 }
    const sesion = await validarLogin(username.trim(), password);
    Sesion.guardar(sesion);

    mostrarMensaje(`✔ Bienvenido ${sesion.username} (${sesion.rol})`, false);

    // Cada rol aterriza en su vista: admin->panel, cajero->POS, cliente->tienda.
    setTimeout(() => {
      window.location.href = DESTINO_POR_ROL[sesion.rol] ?? "index.html";
    }, 400);
  } catch (err) {
    console.error(err);
    mostrarMensaje(err.message || "Error de autenticación", true);
    setCargando(false);
  }
});

/* ------------------------------ Inicialización ---------------------------- */

(async function init() {
  // Si ya hay sesión activa, no pedimos login otra vez.
  const activa = Sesion.obtener();
  if (activa) {
    $subtitulo.textContent = `Ya tienes sesión activa como ${activa.username} (${activa.rol})`;
    setTimeout(() => {
      window.location.href = DESTINO_POR_ROL[activa.rol] ?? "index.html";
    }, 600);
  }

  document.getElementById("subtitulo").textContent += CONFIG.USE_API
    ? " · API REST Spring Boot"
    : " · modo mock";
})();
