/* ============================================================================
 * tienda-tarjetas.js — Render visual compartido de tarjetas de producto.
 *
 * Usado por el HOME (catalogo.js: "Lo más popular") y por el CATÁLOGO
 * completo (tienda-catalogo.js). Un solo lugar para cambiar cómo se ve
 * una tarjeta en toda la tienda.
 * ========================================================================== */

const ICONOS_CATEGORIA = {
  Audio: ["🎧", "🎙️", "🔊", "🎵"],
  Computacion: ["💻", "🖥️", "⌨️", "🖱️"],
  Smartphones: ["📱", "📲", "🔌", "🛰️"],
  Gaming: ["🎮", "🕹️", "👾", "🏆"],
  Accesorios: ["🔋", "🧵", "📡", "🧰"],
};

export const EMOJIS_GRANDES = {
  Audio: "🎧",
  Computacion: "💻",
  Smartphones: "📱",
  Gaming: "🎮",
  Accesorios: "🔌",
};

const GRADIENTES = [
  "from-sky-100 to-blue-200",
  "from-emerald-100 to-teal-200",
  "from-amber-100 to-orange-200",
  "from-violet-100 to-purple-200",
];

export function emojiDe(p) {
  const lista = ICONOS_CATEGORIA[p.categoriaNombre] || ["📦"];
  return lista[p.idProducto % lista.length];
}

function gradienteDe(p) {
  return GRADIENTES[p.idProducto % GRADIENTES.length];
}

/** Estrellas determinísticas por producto (decorativo, 3.5–5). */
export function estrellasDe(p) {
  const n = 35 + ((p.idProducto * 7) % 16); // 35..50 => 3.5..5.0
  const llenas = Math.floor(n / 10);
  const media = (n % 10) >= 5;
  return "★".repeat(llenas) + (media ? "⯨" : "") + "☆".repeat(5 - llenas - (media ? 1 : 0));
}

export function badgeStock(p) {
  if (p.stock === 0)
    return `<span class="badge badge-agotado absolute top-3 left-3">AGOTADO</span>`;
  if (p.stock <= p.stockMinimo)
    return `<span class="badge badge-bajo absolute top-3 left-3">¡ÚLTIMAS ${p.stock}!</span>`;
  return `<span class="badge badge-ok absolute top-3 left-3">DISPONIBLE</span>`;
}

/** Chip "X vendidos" solo cuando hay ventas registradas. */
export function chipPopular(p) {
  return p.unidadesVendidas > 0
    ? `<span class="absolute bottom-3 left-3 bg-white/90 backdrop-blur text-[10px] font-bold text-rose-600 px-2 py-1 rounded-full shadow">
         🔥 ${p.unidadesVendidas} vendido${p.unidadesVendidas === 1 ? "" : "s"}
       </span>`
    : "";
}

/** Tarjeta estándar de producto para la tienda web. */
export function tarjetaProducto(p) {
  const agotado = p.stock === 0;
  return `
  <article class="card-producto group bg-white rounded-2xl shadow-sm hover:shadow-xl border border-slate-100 overflow-hidden flex flex-col fade-in">
    <div class="relative cursor-pointer" data-ver="${p.idProducto}">
      <div class="h-44 bg-gradient-to-br ${gradienteDe(p)} flex items-center justify-center text-6xl group-hover:scale-105 transition-transform duration-300">
        <span>${emojiDe(p)}</span>
      </div>
      ${badgeStock(p)}
      ${chipPopular(p)}
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
      ${
        p.garantiaMeses
          ? `<p class="text-[10px] text-emerald-600 font-semibold select-none">🛡️ ${p.garantiaMeses} meses de garantía</p>`
          : ""
      }

      <div class="flex items-end justify-between mt-auto pt-2">
        <div>
          <p class="text-lg font-extrabold text-slate-900">S/ ${Number(p.precioBase).toLocaleString("es-PE", { minimumFractionDigits: 2 })}</p>
          <p class="text-[10px] text-slate-400">12 MSI de S/ ${(Number(p.precioBase) / 12).toFixed(2)}</p>
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

/** Tarjeta pequeña de categoría con conteo (enlaza al catálogo filtrado). */
export function tarjetaCategoria(c, total) {
  return `
  <a href="catalogo.html?cat=${c.idCategoria}"
     class="card-producto group bg-white rounded-2xl border border-slate-100 shadow-sm p-5 text-left hover:border-indigo-300 block">
    <span class="inline-flex w-12 h-12 rounded-xl bg-gradient-to-br ${gradienteDe(c)} items-center justify-center text-2xl mb-3">
      ${EMOJIS_GRANDES[c.nombre] ?? "🛍️"}
    </span>
    <p class="font-bold text-slate-800 group-hover:text-indigo-600 transition">${c.nombre}</p>
    <p class="text-[11px] text-slate-400">${total} producto(s)</p>
  </a>`;
}
