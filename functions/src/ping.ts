import { onCall } from "firebase-functions/v2/https";

/**
 * Prueba de humo del backend (HT-02 CA-05): la app la llama y comprueba que
 * responde. No lee ni escribe datos.
 */
export const ping = onCall(() => ({
  ok: true,
  message: "pong",
  serverTime: new Date().toISOString(),
}));
