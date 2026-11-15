import { HttpsError, onCall } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";
import { getApps, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { randomBytes } from "node:crypto";
import { calcularResumen, ItemEntrada } from "./calcularPedido.js";
import { DatosTarjeta, guardarTarjetaTokenizada, procesarTarjeta, TarjetaTokenizada } from "./pasarela.js";

if (!getApps().length) {
  initializeApp();
}

type MedioPago = "TARJETA_CREDITO" | "TARJETA_DEBITO" | "BILLETERA_ELECTRONICA" | "CONTRA_ENTREGA";
const MEDIOS: MedioPago[] = ["TARJETA_CREDITO", "TARJETA_DEBITO", "BILLETERA_ELECTRONICA", "CONTRA_ENTREGA"];

interface SolicitudPedido {
  idempotencyKey: string;
  items: ItemEntrada[];
  tipoEntrega: "recojo" | "delivery";
  distritoSlug?: string;
  cupones?: string[];
  entrega?: {
    direccion?: string;
    referencia?: string;
    distrito?: string;
    tiendaRecojoId?: string;
    destinatario?: string;
    telefono?: string;
  };
  medioPago: MedioPago;
  pago?: {
    tarjetaId?: string;
    tarjetaNueva?: DatosTarjeta;
    guardarTarjeta?: boolean;
  };
  prendasProbadas?: string[];
  /** Total que el cliente vio en el botón 'Pagar S/ X'. */
  totalEsperado?: number;
}

interface ResultadoPedido {
  estado: "APROBADO" | "PENDIENTE" | "RECHAZADO";
  pedidoId?: string;
  numeroPedido?: string;
  motivo?: string;
}

interface Asignacion {
  varianteId: string;
  tiendaId: string;
  cantidad: number;
}

const DURACION_RESERVA_MS = 30 * 60 * 1000; // HU-22

/**
 * HU-21 — Pago y creación del pedido en el servidor.
 *
 * - CA-03: la tarjeta nueva se procesa en la pasarela del servidor; solo se
 *   guardan marca, últimos 4, vencimiento y token.
 * - CA-07: con el pago aprobado, en una sola transacción se asigna el número
 *   correlativo (MV-000123), se descuenta el stock por tienda y talla y se
 *   crea el pedido 'Aprobado'.
 * - CA-08: se crea el aviso del pedido, que enviarPush manda como push.
 * - CA-09: si la pasarela rechaza, se devuelve el motivo y no se toca nada.
 * - CA-10: la idempotencyKey que genera el celular por intento de pago hace
 *   que un doble toque o un reintento tras un corte devuelva el mismo
 *   resultado: un solo cobro y un solo pedido.
 */
export const crearPedido = onCall(async (request): Promise<ResultadoPedido> => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Inicia sesión para pagar.");

  const data = (request.data || {}) as SolicitudPedido;
  const key = String(data.idempotencyKey ?? "");
  if (!/^[A-Za-z0-9_-]{8,100}$/.test(key)) {
    throw new HttpsError("invalid-argument", "Falta el identificador del intento de pago.");
  }
  if (!MEDIOS.includes(data.medioPago)) {
    throw new HttpsError("invalid-argument", "Elige un medio de pago válido.");
  }

  const db = getFirestore();
  const clienteRef = db.collection("clientes").doc(uid);
  const operacionRef = clienteRef.collection("operacionesPago").doc(key);

  // Reintento de una operación ya resuelta: se devuelve lo mismo (CA-10).
  const previa = await operacionRef.get();
  if (previa.exists) {
    return previa.get("resultado") as ResultadoPedido;
  }

  // 1. Total recalculado en el servidor (nunca se confía en el del celular).
  const tipoEntrega = data.tipoEntrega === "recojo" ? "recojo" : "delivery";
  const resumen = await calcularResumen({
    items: Array.isArray(data.items) ? data.items : [],
    tipoEntrega,
    distritoSlug: data.distritoSlug,
    cupones: data.cupones,
  });

  const itemsCobrables = resumen.items.filter((i) => i.cantidadAprobada > 0);
  if (itemsCobrables.length === 0) {
    throw new HttpsError("failed-precondition", "Tu carrito no tiene prendas disponibles para pagar.");
  }
  const ajustado = resumen.items.find((i) => i.cantidadAprobada < i.cantidadOriginal);
  if (ajustado) {
    throw new HttpsError(
      "failed-precondition",
      `El stock de '${ajustado.nombre}' (talla ${ajustado.talla}) cambió. Vuelve al resumen para revisar tu pedido.`
    );
  }

  if (typeof data.totalEsperado === "number" && Math.abs(data.totalEsperado - resumen.total) > 0.009) {
    throw new HttpsError(
      "failed-precondition",
      `El total de tu pedido cambió a S/ ${resumen.total.toFixed(2)}. Vuelve al resumen para revisarlo.`
    );
  }

  const tiendaId = tipoEntrega === "recojo" ? data.entrega?.tiendaRecojoId : undefined;
  if (tipoEntrega === "recojo" && !tiendaId) {
    throw new HttpsError("invalid-argument", "Elige la tienda donde recogerás tu pedido.");
  }
  const tienda = tiendaId ? (await db.collection("tiendas").doc(tiendaId).get()).data() : undefined;

  // 2. Cobro en la pasarela sandbox.
  const medio = data.medioPago;
  let resumenMedio = "";
  let tarjetaNueva: TarjetaTokenizada | null = null;
  let tokenCobro: string | null = null;

  if (medio === "TARJETA_CREDITO" || medio === "TARJETA_DEBITO") {
    if (data.pago?.tarjetaId) {
      const guardada = await clienteRef.collection("metodosPago").doc(data.pago.tarjetaId).get();
      if (!guardada.exists) {
        throw new HttpsError("not-found", "La tarjeta elegida ya no está guardada. Elige otra.");
      }
      resumenMedio = `${guardada.get("marca")} •••• ${guardada.get("ultimos4")}`;
      tokenCobro = String(guardada.get("tokenPasarela") ?? "");
    } else if (data.pago?.tarjetaNueva) {
      const r = procesarTarjeta({
        ...data.pago.tarjetaNueva,
        tipo: medio === "TARJETA_DEBITO" ? "DEBITO" : "CREDITO",
      });
      if (!r.aprobado) {
        const rechazo: ResultadoPedido = { estado: "RECHAZADO", motivo: r.motivo };
        await operacionRef.set({ resultado: rechazo, creadaEn: FieldValue.serverTimestamp() });
        logger.info("Pago rechazado por la pasarela", { uid, key });
        return rechazo;
      }
      tarjetaNueva = r.tarjeta;
      resumenMedio = `${r.tarjeta.marca} •••• ${r.tarjeta.ultimos4}`;
      tokenCobro = r.tarjeta.tokenPasarela;
    } else {
      throw new HttpsError("invalid-argument", "Elige una tarjeta o ingresa una nueva.");
    }
  } else if (medio === "BILLETERA_ELECTRONICA") {
    resumenMedio = "Billetera electrónica (Yape / Plin)";
  } else {
    resumenMedio = "Pago contra entrega / Reserva 30 min";
  }

  const aprobado = medio !== "CONTRA_ENTREGA";
  const codigoAutorizacion = aprobado ? randomBytes(4).toString("hex").toUpperCase() : null;

  // 3. Transacción: correlativo + stock + pedido + operación + aviso.
  const probadas = new Set(Array.isArray(data.prendasProbadas) ? data.prendasProbadas : []);
  const contadorRef = db.collection("contadores").doc("pedidos");

  const resultado = await db.runTransaction(async (tx) => {
    const repetida = await tx.get(operacionRef);
    if (repetida.exists) return repetida.get("resultado") as ResultadoPedido;

    const contador = await tx.get(contadorRef);
    const stockRefs = itemsCobrables.map((i) => db.collection("stock").doc(`${i.codigo}_${i.talla}`));
    const stockDocs = await tx.getAll(...stockRefs);

    // Asignación de stock por tienda (todas las lecturas antes de escribir).
    const asignaciones: Asignacion[] = [];
    const nuevosStocks = stockDocs.map((doc, idx) => {
      const item = itemsCobrables[idx];
      const porTienda: Record<string, number> = { ...(doc.get("porTienda") ?? {}) };
      let falta = item.cantidadAprobada;

      const tiendasOrdenadas = tiendaId
        ? [tiendaId]
        : Object.keys(porTienda).sort((a, b) => (porTienda[b] ?? 0) - (porTienda[a] ?? 0));

      for (const t of tiendasOrdenadas) {
        if (falta === 0) break;
        const toma = Math.min(falta, porTienda[t] ?? 0);
        if (toma > 0) {
          porTienda[t] -= toma;
          falta -= toma;
          asignaciones.push({ varianteId: doc.id, tiendaId: t, cantidad: toma });
        }
      }
      if (falta > 0) {
        throw new HttpsError(
          "failed-precondition",
          tiendaId
            ? `No hay stock suficiente de '${item.nombre}' (talla ${item.talla}) en la tienda elegida.`
            : `No hay stock suficiente de '${item.nombre}' (talla ${item.talla}).`
        );
      }
      const total = Object.values(porTienda).reduce((a, b) => a + b, 0);
      return { ref: doc.ref, porTienda, total, codigo: item.codigo, cantidad: item.cantidadAprobada };
    });

    const numero = (typeof contador.get("ultimo") === "number" ? contador.get("ultimo") : 0) + 1;
    const numeroPedido = `MV-${String(numero).padStart(6, "0")}`;
    const pedidoId = `ped_${numeroPedido.toLowerCase()}`;
    const pedidoRef = clienteRef.collection("pedidos").doc(pedidoId);
    const ahora = Date.now();

    tx.set(contadorRef, { ultimo: numero, actualizadoEn: FieldValue.serverTimestamp() }, { merge: true });

    for (const s of nuevosStocks) {
      tx.update(s.ref, { porTienda: s.porTienda, total: s.total, actualizadoEn: FieldValue.serverTimestamp() });
      tx.update(db.collection("prendas").doc(s.codigo), { stockTotal: FieldValue.increment(-s.cantidad) });
    }

    tx.set(pedidoRef, {
      id: pedidoId,
      numeroPedido,
      uidCliente: uid,
      estado: aprobado ? "APROBADO" : "PENDIENTE",
      tipoEntrega,
      direccionEntrega: tipoEntrega === "recojo" ? tienda?.direccion ?? "" : data.entrega?.direccion ?? "",
      referenciaEntrega: tipoEntrega === "recojo" ? null : data.entrega?.referencia ?? null,
      distrito: tipoEntrega === "recojo" ? tienda?.distrito ?? "" : data.entrega?.distrito ?? resumen.distrito,
      distritoSlug: tipoEntrega === "recojo" ? "" : data.distritoSlug ?? "",
      tiendaRecojoId: tiendaId ?? null,
      tiendaRecojoNombre: tienda?.nombre ?? null,
      tiendaRecojoDireccion: tienda?.direccion ?? null,
      tiendaRecojoHorario: tienda?.horario ?? null,
      destinatario: String(data.entrega?.destinatario ?? ""),
      telefonoContacto: String(data.entrega?.telefono ?? ""),
      medioPago: medio,
      resumenMedioPago: resumenMedio,
      subtotal: resumen.subtotal,
      descuentoTotal: resumen.descuentoTotal,
      costoEnvio: resumen.costoEnvio,
      total: resumen.total,
      cuponAplicado: resumen.cuponesAplicados[0]?.codigo ?? null,
      creadoEnMillis: ahora,
      actualizadoEnMillis: ahora,
      venceEnMillis: aprobado ? null : ahora + DURACION_RESERVA_MS,
      entregadoEnMillis: null,
      empresaTransporte: null,
      numeroSeguimiento: null,
      listoParaRecoger: false,
      items: itemsCobrables.map((i) => ({
        prendaId: i.prendaId,
        codigo: i.codigo,
        nombre: i.nombre,
        fotoUrl: i.fotoUrl,
        talla: i.talla,
        cantidad: i.cantidadAprobada,
        precioUnitario: i.precioUnitario,
        subtotal: i.subtotal,
        usoProbadorVirtual: probadas.has(i.codigo),
      })),
      hitos: [
        aprobado
          ? {
              estado: "APROBADO",
              titulo: "Pago aprobado",
              descripcion: `Pago confirmado mediante ${resumenMedio}. El stock ha sido reservado.`,
              fechaEpochMillis: ahora,
            }
          : {
              estado: "PENDIENTE",
              titulo: "Reserva creada (30 min)",
              descripcion: "Stock reservado temporalmente por 30 minutos mientras completas el pago.",
              fechaEpochMillis: ahora,
            },
      ],
      solicitudPostventa: null,
      // Datos solo del servidor: trazabilidad del cobro y del stock reservado.
      idempotencyKey: key,
      stockReservado: asignaciones,
      pago: {
        estado: aprobado ? "APROBADO" : "PENDIENTE",
        codigoAutorizacion,
        tokenPasarela: tokenCobro,
        monto: resumen.total,
      },
    });

    const res: ResultadoPedido = {
      estado: aprobado ? "APROBADO" : "PENDIENTE",
      pedidoId,
      numeroPedido,
    };
    tx.set(operacionRef, { resultado: res, creadaEn: FieldValue.serverTimestamp() });

    // Aviso en la bandeja (HU-18); enviarPush lo manda como push (CA-08).
    tx.set(clienteRef.collection("notificaciones").doc(), {
      tipo: "PEDIDO",
      titulo: aprobado ? `Pedido ${numeroPedido} aprobado` : `Pedido ${numeroPedido} reservado`,
      texto: aprobado
        ? `Tu pago de S/ ${resumen.total.toFixed(2)} fue confirmado. Te avisaremos cuando cambie el estado de tu pedido.`
        : "Tienes 30 minutos para completar el pago antes de que se libere la reserva.",
      creadaEn: FieldValue.serverTimestamp(),
      leida: false,
      refId: pedidoId,
    });

    return res;
  });

  // 4. Tarjeta nueva aprobada y el cliente pidió guardarla (CA-02).
  if (tarjetaNueva && data.pago?.guardarTarjeta && resultado.estado === "APROBADO") {
    await guardarTarjetaTokenizada(uid, tarjetaNueva).catch((e) =>
      logger.warn("No se pudo guardar la tarjeta tras el pago", { uid, error: String(e) })
    );
  }

  logger.info("Pedido procesado", { uid, key, ...resultado });
  return resultado;
});
