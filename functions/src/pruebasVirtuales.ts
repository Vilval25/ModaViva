import { HttpsError, onCall } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";
import { getApps, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";

if (!getApps().length) {
  initializeApp();
}

/**
 * HU-12 CA-06 — Guardar o descartar el resultado de una prueba virtual.
 *
 * - guardar: la imagen queda en el historial del cliente.
 * - descartar: se borran la imagen de Storage, la caché del resultado y el
 *   registro más reciente de esa prenda en el historial de pruebas.
 */
export const gestionarPruebaVirtual = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Inicia sesión para continuar.");

  const { prendaId, accion } = (request.data ?? {}) as { prendaId?: string; accion?: string };
  if (!prendaId || (accion !== "guardar" && accion !== "descartar")) {
    throw new HttpsError("invalid-argument", "Indica la prenda y la acción (guardar o descartar).");
  }

  const db = getFirestore();
  const safeCode = prendaId.replace(/[^a-zA-Z0-9_-]/g, "_");
  const clienteRef = db.collection("clientes").doc(uid);
  const cacheRef = clienteRef.collection("probadorCache").doc(safeCode);

  if (accion === "guardar") {
    const cache = await cacheRef.get();
    if (!cache.exists) throw new HttpsError("not-found", "Esta prueba ya no está disponible.");
    await cacheRef.update({ guardada: true, guardadaEn: FieldValue.serverTimestamp() });
    return { ok: true, accion };
  }

  // Descartar: imagen en Storage (cualquier extensión), caché e historial.
  const bucket = getStorage().bucket();
  const [archivos] = await bucket.getFiles({ prefix: `clientes/${uid}/probador/resultados/${safeCode}.` });
  await Promise.all(archivos.map((f) => f.delete({ ignoreNotFound: true })));
  await cacheRef.delete();

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(clienteRef);
    const historial = Array.isArray(snap.get("pruebasVirtuales"))
      ? [...(snap.get("pruebasVirtuales") as Array<Record<string, unknown>>)]
      : [];
    let indice = -1;
    historial.forEach((p, i) => {
      if (p?.prendaCodigo !== prendaId) return;
      const fecha = typeof p.fechaEpochMillis === "number" ? p.fechaEpochMillis : 0;
      const actual = indice >= 0 ? Number(historial[indice].fechaEpochMillis ?? 0) : -1;
      if (fecha >= actual) indice = i;
    });
    if (indice >= 0) {
      historial.splice(indice, 1);
      tx.update(clienteRef, { pruebasVirtuales: historial });
    }
  });

  logger.info("Prueba virtual descartada", { uid, prendaId, archivos: archivos.length });
  return { ok: true, accion };
});
