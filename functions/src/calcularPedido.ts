import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

if (!admin.apps.length) {
  admin.initializeApp();
}

export interface ItemEntrada {
  prendaId: string;
  talla: string;
  cantidad: number;
  precioVisto?: number;
}

export interface SolicitudCalculoPedido {
  items: ItemEntrada[];
  tipoEntrega: "recojo" | "delivery";
  distritoSlug?: string;
  cupones?: string[];
}

export interface ItemCalculado {
  prendaId: string;
  codigo: string;
  nombre: string;
  fotoUrl: string;
  talla: string;
  cantidadOriginal: number;
  cantidadAprobada: number;
  precioUnitario: number;
  subtotal: number;
  agotado: boolean;
  cambioPrecio: boolean;
  precioAnterior?: number;
}

export interface CuponInfo {
  codigo: string;
  tipo: "porcentaje" | "monto";
  valor: number;
  descuentoCalculado: number;
}

function redondear2(val: number): number {
  return Math.round((val + Number.EPSILON) * 100) / 100;
}

function normalizarSlug(texto: string): string {
  return texto
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[\s_]+/g, "-");
}

/**
 * HU-19 — Cálculo del pedido antes de pagar.
 * 
 * Criterios cubiertos:
 * - CA-01: Revalida stock y precio de cada ítem desde Firestore.
 * - CA-02: Retorna subtotal, descuento, envío y total (total = subtotal - descuento + envío).
 * - CA-03: Costo de envío según tarifa del distrito o S/ 0 si es recojo.
 * - CA-04: Validación de cupones (existencia, vigencia, monto mínimo) con motivo específico de rechazo.
 * - CA-05: Rechazo de múltiples cupones si alguno no es combinable.
 * - CA-06: Notificación de ítems agotados o con precio modificado, recalculando el total.
 */
export const calcularPedido = onCall(async (request) => {
  const data = (request.data || {}) as SolicitudCalculoPedido;
  const db = admin.firestore();
  const ahora = new Date();

  const itemsInput = Array.isArray(data.items) ? data.items : [];
  const tipoEntrega = data.tipoEntrega === "recojo" ? "recojo" : "delivery";
  const distritoSlug = data.distritoSlug ? normalizarSlug(data.distritoSlug) : "";
  const cuponesSolicitados = (data.cupones || [])
    .map((c) => c.trim().toUpperCase())
    .filter((c) => c.length > 0);

  const itemsCalculados: ItemCalculado[] = [];
  const alertas: string[] = [];

  let subtotal = 0;

  // 1. Revalidación de cada ítem (CA-01, CA-06)
  for (const item of itemsInput) {
    if (!item.prendaId || !item.talla || item.cantidad <= 0) continue;

    // Buscar la prenda
    let prendaDoc = await db.collection("prendas").doc(item.prendaId).get();
    if (!prendaDoc.exists) {
      const q = await db
        .collection("prendas")
        .where("codigo", "==", item.prendaId)
        .limit(1)
        .get();
      if (!q.empty) prendaDoc = q.docs[0];
    }

    if (!prendaDoc.exists) {
      alertas.push(`La prenda ${item.prendaId} ya no está disponible y fue retirada.`);
      continue;
    }

    const pData = prendaDoc.data() || {};
    const nombre = pData.nombre || item.prendaId;
    const codigo = pData.codigo || prendaDoc.id;
    const fotos = Array.isArray(pData.fotos) ? pData.fotos : [];
    const fotoUrl = fotos.length > 0 ? fotos[0] : "";

    // Determinar precio actual según vigencia de promoción
    let precioActual = typeof pData.precio === "number" ? pData.precio : 0;
    if (typeof pData.precioPromo === "number") {
      const promoHasta = pData.promoHasta ? pData.promoHasta.toDate() : null;
      if (!promoHasta || ahora <= promoHasta) {
        precioActual = pData.precioPromo;
      }
    }
    precioActual = redondear2(precioActual);

    // Comprobar cambio de precio (CA-06)
    let cambioPrecio = false;
    let precioAnterior: number | undefined;
    if (typeof item.precioVisto === "number" && Math.abs(item.precioVisto - precioActual) > 0.009) {
      cambioPrecio = true;
      precioAnterior = item.precioVisto;
      alertas.push(
        `El precio de '${nombre}' cambió de S/ ${precioAnterior.toFixed(2)} a S/ ${precioActual.toFixed(2)}.`
      );
    }

    // Verificar stock de la variante {prendaId}_{talla}
    const varianteId = `${codigo}_${item.talla}`;
    const stockDoc = await db.collection("stock").doc(varianteId).get();
    let stockTotal = 0;
    if (stockDoc.exists) {
      const sData = stockDoc.data() || {};
      stockTotal = typeof sData.stockTotal === "number" ? sData.stockTotal : 0;
    }

    let cantidadAprobada = item.cantidad;
    let agotado = false;

    if (stockTotal <= 0) {
      agotado = true;
      cantidadAprobada = 0;
      alertas.push(`'${nombre}' (talla ${item.talla}) está agotado y se retiró del total.`);
    } else if (stockTotal < item.cantidad) {
      cantidadAprobada = stockTotal;
      alertas.push(
        `Solo quedan ${stockTotal} unidad(es) de '${nombre}' (talla ${item.talla}). Se ajustó la cantidad.`
      );
    }

    const subtotalLinea = redondear2(precioActual * cantidadAprobada);
    if (!agotado && cantidadAprobada > 0) {
      subtotal += subtotalLinea;
    }

    itemsCalculados.push({
      prendaId: codigo,
      codigo,
      nombre,
      fotoUrl,
      talla: item.talla,
      cantidadOriginal: item.cantidad,
      cantidadAprobada,
      precioUnitario: precioActual,
      subtotal: subtotalLinea,
      agotado,
      cambioPrecio,
      precioAnterior,
    });
  }

  subtotal = redondear2(subtotal);

  // 2. Validación de cupones (CA-04, CA-05)
  const cuponesAplicados: CuponInfo[] = [];
  let descuentoTotal = 0;
  const erroresCupon: string[] = [];

  if (cuponesSolicitados.length > 0) {
    const cuponesDocs: { codigo: string; data: FirebaseFirestore.DocumentData }[] = [];

    for (const codigo of cuponesSolicitados) {
      const cDoc = await db.collection("cupones").doc(codigo).get();
      if (!cDoc.exists) {
        erroresCupon.push(`El cupón '${codigo}' no existe.`);
        continue;
      }
      const cData = cDoc.data() || {};
      if (cData.activo === false) {
        erroresCupon.push(`El cupón '${codigo}' ya no está activo.`);
        continue;
      }

      // Validar vigencia
      const desde = cData.vigenteDesde ? cData.vigenteDesde.toDate() : null;
      const hasta = cData.vigenteHasta ? cData.vigenteHasta.toDate() : null;
      if (desde && ahora < desde) {
        erroresCupon.push(`El cupón '${codigo}' aún no está vigente.`);
        continue;
      }
      if (hasta && ahora > hasta) {
        erroresCupon.push(`El cupón '${codigo}' ha vencido.`);
        continue;
      }

      // Validar monto mínimo
      const montoMinimo = typeof cData.montoMinimo === "number" ? cData.montoMinimo : 0;
      if (subtotal < montoMinimo) {
        erroresCupon.push(
          `El cupón '${codigo}' requiere una compra mínima de S/ ${montoMinimo.toFixed(2)}.`
        );
        continue;
      }

      cuponesDocs.push({ codigo, data: cData });
    }

    // CA-05: Si hay más de un cupón, todos deben ser combinables
    if (cuponesDocs.length > 1) {
      const algunNoCombinable = cuponesDocs.some((c) => !c.data.combinable);
      if (algunNoCombinable) {
        erroresCupon.push(
          "No es posible combinar estos cupones. Uno o más de ellos son exclusivos de campaña."
        );
        // Dejar solo el primer cupón válido si no son combinables
        cuponesDocs.splice(1);
      }
    }

    // Calcular montos de descuento
    let subtotalRestante = subtotal;
    for (const { codigo, data: cData } of cuponesDocs) {
      let desc = 0;
      const tipo = cData.tipo === "porcentaje" ? "porcentaje" : "monto";
      const valor = typeof cData.valor === "number" ? cData.valor : 0;

      if (tipo === "porcentaje") {
        desc = redondear2(subtotalRestante * (valor / 100));
      } else {
        desc = redondear2(Math.min(subtotalRestante, valor));
      }

      desc = Math.min(desc, subtotalRestante);
      subtotalRestante = redondear2(subtotalRestante - desc);
      descuentoTotal = redondear2(descuentoTotal + desc);

      cuponesAplicados.push({
        codigo,
        tipo,
        valor,
        descuentoCalculado: desc,
      });
    }
  }

  // 3. Tarifa de envío (CA-03)
  let costoEnvio = 0;
  let nombreDistrito = "";
  let diasEntrega = 0;

  if (tipoEntrega === "delivery") {
    if (distritoSlug) {
      const tarifaDoc = await db.collection("tarifasEnvio").doc(distritoSlug).get();
      if (tarifaDoc.exists) {
        const tData = tarifaDoc.data() || {};
        nombreDistrito = tData.distrito || distritoSlug;
        if (tData.cobertura === false) {
          alertas.push(`No hay cobertura de entrega para el distrito de ${nombreDistrito}.`);
          costoEnvio = 0;
        } else {
          costoEnvio = typeof tData.costo === "number" ? redondear2(tData.costo) : 0;
          diasEntrega = typeof tData.plazoDias === "number" ? tData.plazoDias : 3;
        }
      } else {
        alertas.push(`No se encontró tarifa registrada para el distrito '${distritoSlug}'.`);
      }
    }
  } else {
    // Recojo en tienda (S/ 0)
    costoEnvio = 0;
  }

  // 4. Total final (CA-02: total = subtotal - descuento + envío)
  const total = redondear2(Math.max(0, subtotal - descuentoTotal + costoEnvio));

  return {
    ok: true,
    subtotal,
    descuentoTotal,
    costoEnvio,
    total,
    tipoEntrega,
    distrito: nombreDistrito,
    plazoDias: diasEntrega,
    items: itemsCalculados,
    cuponesAplicados,
    erroresCupon,
    alertas,
    totalPrendasAprobadas: itemsCalculados.reduce((sum, i) => sum + i.cantidadAprobada, 0),
  };
});
