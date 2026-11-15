import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions";
import { getApps, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

if (!getApps().length) {
  initializeApp();
}

/** Errores de FCM que indican que el token ya no sirve y se puede borrar. */
const TOKENS_INVALIDOS = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument",
]);

/**
 * HU-18 CA-05: cada aviso nuevo en clientes/{uid}/notificaciones se envía
 * como push a los dispositivos del cliente (campo fcmTokens de su perfil).
 *
 * Se envía un mensaje solo de datos para que la app arme la notificación y,
 * al tocarla, abra la pantalla del aviso. Si el cliente no dio permiso, el
 * aviso igual queda en la bandeja. Los tokens vencidos se eliminan.
 */
export const enviarPush = onDocumentCreated(
  "clientes/{uid}/notificaciones/{avisoId}",
  async (event) => {
    const aviso = event.data?.data();
    if (!aviso) return;

    const { uid, avisoId } = event.params;
    const clienteRef = getFirestore().collection("clientes").doc(uid);
    const cliente = await clienteRef.get();

    const tokens = (Array.isArray(cliente.get("fcmTokens")) ? cliente.get("fcmTokens") : [])
      .filter((t: unknown): t is string => typeof t === "string" && t.length > 0);

    if (tokens.length === 0) {
      logger.info("Aviso sin dispositivos para push", { uid, avisoId });
      return;
    }

    const respuesta = await getMessaging().sendEachForMulticast({
      tokens,
      data: {
        avisoId,
        tipo: String(aviso.tipo ?? "GENERAL"),
        titulo: String(aviso.titulo ?? "ModaViva"),
        texto: String(aviso.texto ?? ""),
        refId: String(aviso.refId ?? ""),
      },
      android: { priority: "high" },
    });

    const vencidos = respuesta.responses
      .map((r, i) => (!r.success && TOKENS_INVALIDOS.has(r.error?.code ?? "") ? tokens[i] : null))
      .filter((t): t is string => t !== null);

    if (vencidos.length > 0) {
      await clienteRef.update({ fcmTokens: FieldValue.arrayRemove(...vencidos) });
    }

    logger.info("Push de aviso enviado", {
      uid,
      avisoId,
      enviados: respuesta.successCount,
      fallidos: respuesta.failureCount,
      tokensEliminados: vencidos.length,
    });
  }
);
