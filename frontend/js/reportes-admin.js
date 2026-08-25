/* ============================================================================
 * reportes-admin.js — Sección "Reportes y gráficas" del panel ADMIN.
 * Renderiza KPIs + gráficas (Chart.js) del reporte semanal y exporta el
 * reporte a Excel (.xlsx), Word (.doc) y PDF usando librerías por CDN.
 * Los datos salen SIEMPRE de api.js (funciona en modo mock y API real).
 * ========================================================================== */

import { getReporteSemanal, getProductos } from "./api.js";

/* ------------------------------- Estado ---------------------------------- */

let cacheReporte = null; // resultado de getReporteSemanal()
const charts = { ventas: null, top: null, categorias: null };

const PALETA = [
  "#6366f1", "#10b981", "#f59e0b", "#ef4444",
  "#8b5cf6", "#0ea5e9", "#84cc16", "#ec4899",
];

/* ------------------------------ Utilidades ------------------------------- */

function fmtMoney(n) {
  return "S/ " + Number(n || 0).toLocaleString("es-PE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

/** Nombre de archivo estándar para los tres formatos. */
function nombreArchivo(ext) {
  return `reporte-tiendamenos-${new Date().toISOString().slice(0, 10)}.${ext}`;
}

/** Dispara la descarga de un Blob en el navegador. */
function descargarBlob(blob, nombre) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = nombre;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

/** Corta textos largos para las etiquetas de las gráficas. */
function recortar(t, max = 18) {
  return t.length > max ? t.slice(0, max - 1) + "…" : t;
}

/* ------------------------------- Carga ----------------------------------- */

async function cargarReporte() {
  const btn = document.getElementById("btn-rep-refresh");
  if (btn) btn.textContent = "Cargando…";
  try {
    const [reporte, productos] = await Promise.all([
      getReporteSemanal(),
      getProductos({ incluirInactivos: false }),
    ]);
    cacheReporte = { ...reporte, _productos: productos };
    renderKpis(reporte);
    renderGraficas(reporte, productos);
  } catch (err) {
    console.error(err);
    mostrarToast("No se pudo cargar el reporte", "error");
  } finally {
    if (btn) btn.textContent = "Actualizar ↻";
  }

  // --- Actualizar período y sparkline ---
  const inicio = new Date(cacheReporte.dias[6].fecha);
  const fin = new Date(cacheReporte.dias[0].fecha);
  const inicioForm = inicio.toLocaleDateString("es-PE", { day: "numeric", month: "short" });
  const finForm = fin.toLocaleDateString("es-PE", { day: "numeric", month: "short" });
  document.getElementById("rep-periodo").textContent = `${inicioForm} al ${finForm}`;

  const sparklineData = cacheReporte.dias.map((d) => d.total);
  const ctx = document.getElementById("chart-spark").getContext("2d");
  const sparkGradient = ctx.createLinearGradient(0, 0, 0, 140);
  sparkGradient.addColorStop(0, "rgba(99, 102, 241, 0.4)");
  sparkGradient.addColorStop(1, "rgba(99, 102, 241, 0)");
  new Chart(document.getElementById("chart-spark"), {
    type: "line",
    data: {
      labels: cacheReporte.dias.map((d) => d.diaSemana),
      datasets: [{
        data: sparklineData,
        tension: 0.4,
        fill: true,
        backgroundColor: sparkGradient,
        borderColor: "#6366f1",
        pointBackgroundColor: "#fff",
        pointBorderColor: "#6366f1",
        pointRadius: 3,
        pointHoverRadius: 5,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } },
    },
  });
}

/* ---------------------------- KPIs + Gráficas ----------------------------- */

function renderKpis(rep) {
  document.getElementById("rep-kpi-ventas").textContent = fmtMoney(rep.totalVentas);
  document.getElementById("rep-kpi-ordenes").textContent = rep.numeroOrdenes;
  document.getElementById("rep-kpi-ticket").textContent = fmtMoney(rep.ticketPromedio);
}

function destruirGraficas() {
  Object.keys(charts).forEach((k) => {
    if (charts[k]) {
      charts[k].destroy();
      charts[k] = null;
    }
  });
}

function renderGraficas(rep, productos) {
  if (typeof Chart === "undefined") {
    mostrarToast("Chart.js no disponible (revisa tu conexión)", "error");
    return;
  }
  destruirGraficas();

  // 1) Ventas por día (barras)
  charts.ventas = new Chart(document.getElementById("chart-ventas"), {
    type: "bar",
    data: {
      labels: rep.dias.map((d) => d.diaSemana),
      datasets: [{
        label: "Venta del día",
        data: rep.dias.map((d) => Number(d.total)),
        backgroundColor: "#6366f1",
        borderRadius: 8,
        maxBarThickness: 38,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } },
    },
  });

  // 2) Top productos vendidos (barras horizontales)
  const top = rep.topProductos ?? [];
  charts.top = new Chart(document.getElementById("chart-top"), {
    type: "bar",
    data: {
      labels: top.map((t) => recortar(t.nombre)),
      datasets: [{
        label: "Unidades",
        data: top.map((t) => t.unidades),
        backgroundColor: PALETA,
        borderRadius: 8,
      }],
    },
    options: {
      indexAxis: "y",
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { x: { beginAtZero: true, ticks: { precision: 0 } } },
    },
  });

  // 3) Stock actual por categoría (dona)
  const porCategoria = {};
  for (const p of productos) {
    const cat = p.categoriaNombre ?? "Sin categoría";
    porCategoria[cat] = (porCategoria[cat] ?? 0) + Number(p.stock || 0);
  }
  const cats = Object.entries(porCategoria).sort((a, b) => b[1] - a[1]);
  charts.categorias = new Chart(document.getElementById("chart-categorias"), {
    type: "doughnut",
    data: {
      labels: cats.map(([c]) => c),
      datasets: [{
        data: cats.map(([, v]) => v),
        backgroundColor: PALETA,
        borderWidth: 2,
        borderColor: "#ffffff",
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: "55%",
      plugins: { legend: { position: "right", labels: { boxWidth: 12, font: { size: 10 } } } },
    },
  });
}

/* ------------------------- Exportación a EXCEL --------------------------- */

/**
 * Libro .xlsx con 4 hojas: Resumen, Ventas por día, Top productos y
 * Reposición sugerida. Usa SheetJS (window.XLSX cargado por CDN).
 */
function exportarExcel() {
  if (!cacheReporte) return mostrarToast("Aún no hay datos del reporte", "error");
  if (typeof XLSX === "undefined") return mostrarToast("SheetJS no disponible", "error");

  const rep = cacheReporte;
  const wb = XLSX.utils.book_new();

  XLSX.utils.book_append_sheet(
    wb,
    XLSX.utils.aoa_to_sheet([
      ["TiendaMenos — Reporte semanal"],
      ["Generado", new Date().toLocaleString("es-PE")],
      [],
      ["Concepto", "Valor"],
      ["Ventas últimos 7 días", Number(rep.totalVentas)],
      ["Órdenes pagadas", rep.numeroOrdenes],
      ["Ticket promedio", Number(rep.ticketPromedio)],
    ]),
    "Resumen"
  );

  XLSX.utils.book_append_sheet(
    wb,
    XLSX.utils.aoa_to_sheet([
      ["Fecha", "Día", "Total", "Órdenes"],
      ...rep.dias.map((d) => [d.fecha, d.diaSemana, Number(d.total), d.ordenes]),
    ]),
    "Ventas por dia"
  );

  XLSX.utils.book_append_sheet(
    wb,
    XLSX.utils.aoa_to_sheet([
      ["SKU", "Producto", "Unidades", "Importe"],
      ...(rep.topProductos ?? []).map((t) => [t.sku, t.nombre, t.unidades, Number(t.importe)]),
    ]),
    "Top productos"
  );

  XLSX.utils.book_append_sheet(
    wb,
    XLSX.utils.aoa_to_sheet([
      ["SKU", "Producto", "Stock", "Stock mínimo", "Proveedor"],
      ...(rep.bajoStock ?? []).map((p) => [
        p.sku, p.nombre, p.stock, p.stockMinimo, p.proveedorNombre ?? "—",
      ]),
    ]),
    "Reposicion"
  );

  XLSX.writeFile(wb, nombreArchivo("xlsx"));
  mostrarToast("✔ Reporte Excel descargado", "exito");
}

/* -------------------------- Exportación a WORD --------------------------- */

/**
 * Documento .doc generado desde HTML (formato nativo que Word abre
 * directamente, sin librerías externas).
 */
function exportarWord() {
  if (!cacheReporte) return mostrarToast("Aún no hay datos del reporte", "error");

  const rep = cacheReporte;
  const fila = (celdas) => `<tr>${celdas.map((c) => `<td>${c}</td>`).join("")}</tr>`;
  const tabla = (titulo, encabezados, filas) => `
    <h2 style="color:#374151;font-size:13pt;margin:18px 0 6px;">${titulo}</h2>
    <table border="1" cellspacing="0" cellpadding="5"
           style="border-collapse:collapse;width:100%;font-size:10pt;">
      <thead><tr style="background:#eef2ff;">
        ${encabezados.map((h) => `<th align="left">${h}</th>`).join("")}
      </tr></thead>
      <tbody>${filas.join("")}</tbody>
    </table>`;

  const html = `
    <html xmlns:w="urn:schemas-microsoft-com:office:word">
    <head><meta charset="utf-8"><title>Reporte semanal</title></head>
    <body style="font-family:Calibri,Arial,sans-serif;color:#1f2937;">
      <h1 style="color:#4338ca;">TiendaMenos — Reporte semanal</h1>
      <p style="font-size:10pt;color:#6b7280;">
        Generado el ${new Date().toLocaleString("es-PE")}
      </p>
      <table border="1" cellspacing="0" cellpadding="6"
             style="border-collapse:collapse;font-size:11pt;margin-top:10px;">
        ${fila(["<b>Ventas 7 días</b>", "<b>Órdenes pagadas</b>", "<b>Ticket promedio</b>"])}
        ${fila([fmtMoney(rep.totalVentas), rep.numeroOrdenes, fmtMoney(rep.ticketPromedio)])}
      </table>
      ${tabla(
        "Ventas por día",
        ["Fecha", "Día", "Total", "Órdenes"],
        rep.dias.map((d) =>
          fila([d.fecha, d.diaSemana, fmtMoney(d.total), d.ordenes]))
      )}
      ${tabla(
        "Top productos vendidos",
        ["SKU", "Producto", "Unidades", "Importe"],
        (rep.topProductos ?? []).map((t) =>
          fila([t.sku, t.nombre, t.unidades, fmtMoney(t.importe)]))
      )}
      ${tabla(
        "Reposición sugerida (stock bajo)",
        ["SKU", "Producto", "Stock", "Mínimo", "Proveedor"],
        (rep.bajoStock ?? []).map((p) =>
          fila([p.sku, p.nombre, p.stock, p.stockMinimo, p.proveedorNombre ?? "—"]))
      )}
    </body></html>`;

  // \ufeff (BOM) evita que Word muestre caracteres raros con acentos.
  descargarBlob(new Blob(["\ufeff" + html], { type: "application/msword" }), nombreArchivo("doc"));
  mostrarToast("✔ Reporte Word descargado", "exito");
}

/* --------------------------- Exportación a PDF ---------------------------- */

/**
 * PDF con encabezado, KPIs y tablas automáticas (jsPDF + AutoTable por CDN).
 * Las imágenes de las gráficas se incrustan como PNG desde los canvas.
 */
function exportarPdf() {
  if (!cacheReporte) return mostrarToast("Aún no hay datos del reporte", "error");
  const JsPdf = window.jspdf && window.jspdf.jsPDF;
  if (!JsPdf) return mostrarToast("jsPDF no disponible", "error");

  const rep = cacheReporte;
  const doc = new JsPdf({ unit: "pt", format: "a4" });
  const ancho = doc.internal.pageSize.getWidth();

  doc.setFontSize(16);
  doc.setTextColor(67, 56, 202);
  doc.text("TiendaMenos — Reporte semanal", 40, 46);
  doc.setFontSize(9);
  doc.setTextColor(107, 114, 128);
  doc.text(`Generado el ${new Date().toLocaleString("es-PE")}`, 40, 62);

  doc.setFontSize(11);
  doc.setTextColor(31, 41, 55);
  doc.text(
    `Ventas 7 días: ${fmtMoney(rep.totalVentas)}   |   ` +
    `Órdenes: ${rep.numeroOrdenes}   |   Ticket promedio: ${fmtMoney(rep.ticketPromedio)}`,
    40, 84
  );

  doc.autoTable({
    startY: 100,
    head: [["Fecha", "Día", "Total", "Órdenes"]],
    body: rep.dias.map((d) => [d.fecha, d.diaSemana, fmtMoney(d.total), String(d.ordenes)]),
    theme: "grid",
    headStyles: { fillColor: [99, 102, 241] },
    styles: { fontSize: 9 },
  });

  doc.autoTable({
    startY: doc.lastAutoTable.finalY + 20,
    head: [["SKU", "Producto", "Unidades", "Importe"]],
    body: (rep.topProductos ?? []).map((t) => [
      t.sku, t.nombre, String(t.unidades), fmtMoney(t.importe),
    ]),
    theme: "grid",
    headStyles: { fillColor: [16, 185, 129] },
    styles: { fontSize: 9 },
  });

  doc.autoTable({
    startY: doc.lastAutoTable.finalY + 20,
    head: [["SKU", "Producto", "Stock", "Mínimo", "Proveedor"]],
    body: (rep.bajoStock ?? []).map((p) => [
      p.sku, p.nombre, String(p.stock), String(p.stockMinimo), p.proveedorNombre ?? "—",
    ]),
    theme: "grid",
    headStyles: { fillColor: [245, 158, 11] },
    styles: { fontSize: 9 },
    didDrawPage: () => {},
  });

  // Gráfica de ventas por día incrustada debajo de las tablas.
  const canvasVentas = document.getElementById("chart-ventas");
  if (canvasVentas && doc.lastAutoTable.finalY < 700) {
    try {
      doc.addImage(canvasVentas.toDataURL("image/png", 1.0), "PNG", 40,
        doc.lastAutoTable.finalY + 24, 320, 180);
      doc.setFontSize(8);
      doc.setTextColor(107, 114, 128);
      doc.text("Ventas por día (S/)", 40, doc.lastAutoTable.finalY + 218);
    } catch { /* canvas no disponible: se omite la imagen */ }
  }

  doc.save(nombreArchivo("pdf"));
  mostrarToast("✔ Reporte PDF descargado", "exito");
}

/* ----------------------------- Inicialización ----------------------------- */

/** Punto de entrada llamado desde admin.js cuando el panel ya validó rol. */
export async function initReportesAdmin() {
  document.getElementById("btn-rep-excel").addEventListener("click", exportarExcel);
  document.getElementById("btn-rep-word").addEventListener("click", exportarWord);
  document.getElementById("btn-rep-pdf").addEventListener("click", exportarPdf);
  document.getElementById("btn-rep-refresh").addEventListener("click", cargarReporte);
  await cargarReporte();
}
