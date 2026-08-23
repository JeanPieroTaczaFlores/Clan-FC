/* ============================================================================
 * CONFIG — Punto único de configuración del frontend TiendaMenos.
 * Cambia USE_API a true cuando el backend Spring Boot esté levantado
 * (http://localhost:8080) para consumir la API REST real en lugar del mock.
 * ========================================================================== */
const CONFIG = {
  USE_API: false, // false = login contra mock_data.json | true = API REST Java
  BASE_URL: "http://localhost:8080/api",
};

/* ============================================================================
 * SESIÓN — estado del usuario autenticado.
 * Se guarda en sessionStorage: sobrevive recargas pero muere al cerrar la
 * pestaña (comportamiento razonable para una demo sin tokens JWT).
 *
 * Estructura guardada:
 *   { username, rol, credencialBase64 }   <- Basic Auth lista para fetch()
 * ========================================================================== */
const Sesion = {
  KEY: "tm_sesion",

  /** Devuelve la sesión activa o null si no hay nadie logueado. */
  obtener() {
    try {
      return JSON.parse(sessionStorage.getItem(this.KEY));
    } catch {
      return null;
    }
  },

  guardar(sesion) {
    sessionStorage.setItem(this.KEY, JSON.stringify(sesion));
  },

  cerrar() {
    sessionStorage.removeItem(this.KEY);
    window.location.href = "login.html";
  },

  /** true si el usuario logueado tiene ALGUNO de los roles indicados. */
  esRol(...roles) {
    const s = this.obtener();
    return Boolean(s && roles.includes(s.rol));
  },
};

/* ============================================================================
 * TOAST — notificaciones flotantes globales (esquina inferior derecha).
 * Uso: mostrarToast("Producto agregado", "exito")
 * ========================================================================== */
function mostrarToast(mensaje, tipo = "info") {
  const colores = {
    info: "bg-slate-800",
    exito: "bg-emerald-600",
    error: "bg-red-600",
  };
  const $contenedor = document.getElementById("toast-container");
  if (!$contenedor) return alert(mensaje); // respaldo si la página no tiene contenedor

  const $toast = document.createElement("div");
  $toast.className =
    `${colores[tipo] || colores.info} text-white text-sm px-4 py-3 rounded-xl shadow-lg ` +
    `opacity-0 translate-y-2 transition-all duration-300 max-w-xs`;
  $toast.textContent = mensaje;
  $contenedor.appendChild($toast);

  requestAnimationFrame(() => $toast.classList.remove("opacity-0", "translate-y-2"));
  setTimeout(() => {
    $toast.classList.add("opacity-0", "translate-y-2");
    setTimeout(() => $toast.remove(), 300);
  }, 2800);
}
