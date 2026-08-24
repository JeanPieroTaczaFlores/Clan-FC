/* ============================================================================
 * carrito-store.js — Estado global del carrito (módulo compartido).
 *
 * Persiste en localStorage para sobrevivir recargas. Cada item guarda un
 * SNAPSHOT del producto (nombre/precio/emoji) para pintar rápido; el stock
 * real se valida de nuevo al confirmar el checkout.
 *
 * Emite el evento "carrito:cambio" en document para que las páginas
 * sincronicen badges y vistas.
 * ========================================================================== */

const KEY = "tm_carrito";

export const Carrito = {
  /** Items actuales: [{productoId, cantidad, nombre, sku, precioBase, emoji}] */
  obtener() {
    try {
      return JSON.parse(localStorage.getItem(KEY)) ?? [];
    } catch {
      return [];
    }
  },

  guardar(items) {
    localStorage.setItem(KEY, JSON.stringify(items));
    document.dispatchEvent(new CustomEvent("carrito:cambio", { detail: items.length }));
  },

  /** Agrega unidades respetando el stock disponible del snapshot. */
  agregar(producto, cantidad = 1) {
    const items = this.obtener();
    const existente = items.find((i) => i.productoId === producto.idProducto);
    const stockMax = Number(producto.stock) || 0;

    if (existente) {
      if (existente.cantidad >= stockMax) {
        throw new Error(`Solo hay ${stockMax} unidad(es) de "${producto.nombre}"`);
      }
      existente.cantidad = Math.min(existente.cantidad + cantidad, stockMax);
    } else {
      if (stockMax < 1) throw new Error(`"${producto.nombre}" está agotado`);
      items.push({
        productoId: producto.idProducto,
        cantidad: Math.min(cantidad, stockMax),
        nombre: producto.nombre,
        sku: producto.sku,
        precioBase: Number(producto.precioBase),
        emoji: producto._emoji ?? "🛍️",
        stockMax, // techo de unidades para los botones +/−
      });
    }
    this.guardar(items);
  },

  cambiarCantidad(productoId, nuevaCantidad) {
    let items = this.obtener();
    const item = items.find((i) => i.productoId === Number(productoId));
    if (!item) return;

    if (nuevaCantidad <= 0) {
      items = items.filter((i) => i !== item);
    } else {
      // Nunca excede el stock capturado al momento de agregar.
      item.cantidad = Math.min(nuevaCantidad, item.stockMax ?? 99);
    }
    this.guardar(items);
  },

  eliminar(productoId) {
    this.guardar(this.obtener().filter((i) => i.productoId !== Number(productoId)));
  },

  vaciar() {
    this.guardar([]);
  },

  /** Subtotal SIN IVA (el impuesto se calcula con la capa api.js). */
  subtotalBase() {
    return this.obtener().reduce((s, i) => s + i.precioBase * i.cantidad, 0);
  },

  /** Unidades totales (para el badge del navbar). */
  contarUnidades() {
    return this.obtener().reduce((s, i) => s + i.cantidad, 0);
  },
};

/** Sincroniza el badge 🛒 del navbar con el estado actual del carrito. */
export function actualizarBadgeCarrito() {
  const $badge = document.getElementById("carrito-badge");
  if (!$badge) return;
  const total = Carrito.contarUnidades();
  $badge.textContent = String(total);
  $badge.classList.toggle("hidden", total === 0);
  $badge.classList.toggle("flex", total > 0);
}

// Escucha los cambios para mantener cualquier página sincronizada.
document.addEventListener("carrito:cambio", actualizarBadgeCarrito);
