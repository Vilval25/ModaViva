import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions";
import { getApps, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";

if (!getApps().length) {
  initializeApp();
}

function sumar(porTienda: unknown): number {
  if (!porTienda || typeof porTienda !== "object") return 0;
  return Object.values(porTienda as Record<string, unknown>).reduce<number>(
    (acc, v) => acc + (typeof v === "number" && v > 0 ? v : 0),
    0
  );
}

function mismoPorTienda(a: unknown, b: unknown): boolean {
  const norm = (x: unknown) =>
    JSON.stringify(Object.entries((x as Record<string, unknown>) ?? {}).sort(([k1], [k2]) => k1.localeCompare(k2)));
  return norm(a) === norm(b);
}

/**
 * HU-05 CA-02 y CA-03 — Mantiene coherente el stock cuando el back-office lo
 * edita (por ejemplo, desde la consola):
 *
 * 1. `total` = suma de `porTienda` (el stock por tienda es la fuente de verdad).
 * 2. `actualizadoEn` se marca en cada cambio, para que la app sincronice solo
 *    lo que cambió (cada 15 min con WorkManager).
 * 3. `prendas/{prendaId}.stockTotal` = suma de todas sus tallas, así el
 *    catálogo muestra 'Agotado' al instante.
 *
 * Solo escribe si algo es distinto, por lo que su propia escritura no genera
 * un ciclo.
 */
export const sincronizarStock = onDocumentWritten("stock/{varianteId}", async (event) => {
  const despues = event.data?.after;
  if (!despues?.exists) return;
  const antes = event.data?.before;
  const d = despues.data() ?? {};
  const a = antes?.exists ? antes.data() ?? {} : null;

  const cambioStock = !a || !mismoPorTienda(a.porTienda, d.porTienda) || a.total !== d.total;
  if (!cambioStock) return;

  const suma = sumar(d.porTienda);
  const cambios: Record<string, unknown> = {};
  if (d.total !== suma) cambios.total = suma;
  const fechaAntes = a?.actualizadoEn as Timestamp | undefined;
  const fechaDespues = d.actualizadoEn as Timestamp | undefined;
  const fechaSinCambio = !fechaDespues || (fechaAntes && fechaDespues && fechaAntes.isEqual(fechaDespues));
  if (fechaSinCambio) cambios.actualizadoEn = FieldValue.serverTimestamp();
  if (Object.keys(cambios).length > 0) await despues.ref.update(cambios);

  const prendaId = typeof d.prendaId === "string" ? d.prendaId : event.params.varianteId.split("_")[0];
  const db = getFirestore();
  const variantes = await db.collection("stock").where("prendaId", "==", prendaId).get();
  const stockTotal = variantes.docs.reduce(
    (acc, doc) => acc + (doc.id === despues.id ? suma : typeof doc.get("total") === "number" ? doc.get("total") : 0),
    0
  );

  const prendaRef = db.collection("prendas").doc(prendaId);
  const prenda = await prendaRef.get();
  if (prenda.exists && prenda.get("stockTotal") !== stockTotal) {
    await prendaRef.update({ stockTotal });
    logger.info("stockTotal de prenda actualizado", { prendaId, stockTotal });
  }
});
