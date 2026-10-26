import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

if (!admin.apps.length) {
  admin.initializeApp();
}

const DEFAULT_DISABLED_STRING =
  "ropa-interior,lenceria,trajes-de-bano,banadores,bikinis,ropa-de-bano,accesorios,calzado";

function normalizar(texto: string): string {
  return texto
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[\s_]+/g, "-");
}

function parseCategorias(raw: string): Set<string> {
  return new Set(
    raw
      .split(/[,;\n]/)
      .map(normalizar)
      .filter((s) => s.length > 0)
  );
}

/**
 * HU-16 CA-05: Dado que una versión desactualizada de la app envía una solicitud
 * de prueba para una prenda no habilitada, cuando llega al servidor, entonces la
 * Cloud Function la rechaza y no se genera imagen.
 */
export const probarPrenda = onCall(async (request) => {
  const { prendaId, categoria: categoriaInput } = request.data ?? {};

  if (!prendaId && !categoriaInput) {
    throw new HttpsError(
      "invalid-argument",
      "Se requiere 'prendaId' o 'categoria' para validar la prueba virtual."
    );
  }

  // 1. Obtener lista de categorías inhabilitadas desde Remote Config con fallback
  let disabledCategories = parseCategorias(DEFAULT_DISABLED_STRING);
  try {
    const template = await admin.remoteConfig().getServerTemplate();
    const param = template.parameters["probador_categorias_deshabilitadas"];
    const val = param?.defaultValue;
    if (val && "value" in val && typeof val.value === "string") {
      disabledCategories = parseCategorias(val.value);
    }
  } catch {
    // Si no se puede contactar Remote Config en tiempo real, usar fallback por defecto
    disabledCategories = parseCategorias(DEFAULT_DISABLED_STRING);
  }

  // 2. Determinar categoría y subcategoría de la prenda
  let categoria = categoriaInput ? normalizar(categoriaInput) : "";
  let subcategoria = "";

  if (prendaId) {
    const db = admin.firestore();
    const docSnap = await db.collection("prendas").doc(prendaId).get();
    if (docSnap.exists) {
      const data = docSnap.data();
      if (data?.categoria) categoria = normalizar(data.categoria);
      if (data?.subcategoria) subcategoria = normalizar(data.subcategoria);
    } else {
      // Buscar por código
      const querySnap = await db
        .collection("prendas")
        .where("codigo", "==", prendaId)
        .limit(1)
        .get();
      if (!querySnap.empty) {
        const data = querySnap.docs[0].data();
        if (data?.categoria) categoria = normalizar(data.categoria);
        if (data?.subcategoria) subcategoria = normalizar(data.subcategoria);
      }
    }
  }

  // 3. Validar si la categoría está deshabilitada
  const estaDeshabilitada = Array.from(disabledCategories).some((dis) => {
    return (
      categoria === dis ||
      categoria.includes(dis) ||
      (subcategoria && (subcategoria === dis || subcategoria.includes(dis)))
    );
  });

  if (estaDeshabilitada) {
    throw new HttpsError(
      "failed-precondition",
      "Esta prenda no está disponible en el probador virtual por motivos de higiene y política de la tienda."
    );
  }

  return {
    ok: true,
    habilitada: true,
    mensaje: "Prenda verificada y habilitada para el probador virtual.",
  };
});
