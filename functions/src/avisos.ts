import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions";
import { getApps, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";

if (!getApps().length) {
  initializeApp();
}

const ETIQUETAS: Record<string, string> = {
  EN_EVALUACION: "En evaluación",
  APROBADA: "Aprobada",
  RECHAZADA: "Rechazada",
};

/**
 * HU-24 + HU-18 — Cuando el back-office cambia el estado de una solicitud de
 * cambio o devolución, se crea un aviso en la bandeja del cliente (y
 * enviarPush lo manda como push). Al resolverla se registra la fecha.
 *
 * Solo reacciona a cambios del campo `estado`; las demás ediciones (por
 * ejemplo, corregir el texto de la resolución) no generan avisos.
 */
export const avisarCambioSolicitud = onDocumentUpdated(
  "clientes/{uid}/solicitudes/{solicitudId}",
  async (event) => {
    const antes = event.data?.before.data();
    const despues = event.data?.after.data();
    if (!antes || !despues || antes.estado === despues.estado) return;

    const { uid, solicitudId } = event.params;
    const estado = String(despues.estado ?? "");
    const etiqueta = ETIQUETAS[estado] ?? estado;
    const numero = String(despues.numeroSolicitud ?? solicitudId);
    const resolucion = typeof despues.resolucion === "string" ? despues.resolucion.trim() : "";

    const texto =
      estado === "APROBADA"
        ? `Tu solicitud de ${String(despues.prendaNombre ?? "la prenda")} fue aprobada.`
        : estado === "RECHAZADA"
          ? `Tu solicitud de ${String(despues.prendaNombre ?? "la prenda")} fue rechazada.`
          : `Tu solicitud ahora está ${etiqueta}.`;

    const db = getFirestore();
    const lote = db.batch();

    lote.set(db.collection("clientes").doc(uid).collection("notificaciones").doc(), {
      tipo: "SOLICITUD",
      titulo: `Solicitud ${numero}: ${etiqueta}`,
      texto: (resolucion ? `${texto} ${resolucion}` : texto).slice(0, 500),
      creadaEn: FieldValue.serverTimestamp(),
      leida: false,
      refId: solicitudId,
    });

    if ((estado === "APROBADA" || estado === "RECHAZADA") && despues.resueltaEnMillis == null) {
      lote.update(event.data!.after.ref, { resueltaEnMillis: Date.now() });
    }

    await lote.commit();
    logger.info("Aviso de cambio de estado de solicitud", { uid, solicitudId, estado });
  }
);
