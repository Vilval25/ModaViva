import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
import { getApps, initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getRemoteConfig } from "firebase-admin/remote-config";
import { getStorage } from "firebase-admin/storage";
import { randomUUID } from "node:crypto";

if (!getApps().length) {
  initializeApp();
}

const genlookApiKey = defineSecret("GENLOOK_API_KEY");

const GENLOOK_SYNC_URL = "https://api.genlook.app/tryon/v1/try-on/sync";
/** HU-12 CA-07: corte de la llamada a la IA. */
const TIEMPO_MAXIMO_IA_MS = 60_000;
/** Pruebas nuevas por cliente y día si Remote Config no define `probador_limite_diario`. */
const LIMITE_DIARIO_POR_DEFECTO = 5;

const DEFAULT_DISABLED_STRING =
  "ropa-interior,lenceria,trajes-de-bano,banadores,bikinis,ropa-de-bano,accesorios,calzado";

interface PrendaInfo {
  id: string;
  codigo: string;
  nombre: string;
  descripcion: string;
  categoria: string;
  subcategoria: string;
  fotos: string[];
}

interface GenlookSyncResponse {
  generationId?: string;
  status?: string;
  resultImageUrl?: string;
  code?: string;
  message?: string;
}

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

function extraerFotos(raw: unknown): string[] {
  if (!Array.isArray(raw)) return [];
  return raw.filter((f): f is string => typeof f === "string" && f.length > 0);
}

async function obtenerCategoriasDeshabilitadas(): Promise<Set<string>> {
  try {
    const template = await getRemoteConfig().getTemplate();
    const param = template.parameters["probador_categorias_deshabilitadas"];
    const val = param?.defaultValue;
    if (val && "value" in val && typeof val.value === "string") {
      return parseCategorias(val.value);
    }
  } catch {
    // Fallback local si Remote Config no responde en tiempo real
  }
  return parseCategorias(DEFAULT_DISABLED_STRING);
}

async function obtenerLimiteDiario(): Promise<number> {
  try {
    const template = await getRemoteConfig().getTemplate();
    const val = template.parameters["probador_limite_diario"]?.defaultValue;
    if (val && "value" in val) {
      const n = Number(val.value);
      if (Number.isFinite(n) && n > 0) return Math.floor(n);
    }
  } catch {
    // Sin Remote Config se usa el valor por defecto.
  }
  return LIMITE_DIARIO_POR_DEFECTO;
}

/** Día actual en Lima (AAAA-MM-DD): el límite diario se renueva a medianoche de Perú. */
function diaLima(): string {
  return new Date().toLocaleDateString("en-CA", { timeZone: "America/Lima" });
}

/**
 * HU-12 CA-07: reserva una prueba del límite diario antes de llamar a la IA.
 * Si la generación falla o tarda más de 60 s, se libera con [liberarPrueba],
 * así ese intento no se descuenta. Devuelve cuántas quedan tras esta prueba.
 */
async function reservarPrueba(uid: string, limite: number): Promise<number> {
  const db = getFirestore();
  const usoRef = db.collection("clientes").doc(uid).collection("probadorUso").doc(diaLima());
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(usoRef);
    const usadas = typeof snap.get("usadas") === "number" ? (snap.get("usadas") as number) : 0;
    if (usadas >= limite) {
      throw new HttpsError(
        "resource-exhausted",
        `Usaste tus ${limite} pruebas virtuales de hoy. Se renuevan a medianoche.`
      );
    }
    tx.set(
      usoRef,
      { usadas: usadas + 1, limite, actualizadoEn: FieldValue.serverTimestamp() },
      { merge: true }
    );
    return limite - usadas - 1;
  });
}

async function liberarPrueba(uid: string): Promise<void> {
  const usoRef = getFirestore().collection("clientes").doc(uid).collection("probadorUso").doc(diaLima());
  await usoRef.update({ usadas: FieldValue.increment(-1) }).catch(() => undefined);
}

async function buscarPrenda(prendaId: string): Promise<PrendaInfo | null> {
  const db = getFirestore();
  const docSnap = await db.collection("prendas").doc(prendaId).get();
  if (docSnap.exists) {
    const data = docSnap.data() ?? {};
    return {
      id: docSnap.id,
      codigo: String(data.codigo || docSnap.id),
      nombre: String(data.nombre || prendaId),
      descripcion: String(data.descripcion || ""),
      categoria: String(data.categoria || ""),
      subcategoria: String(data.subcategoria || ""),
      fotos: extraerFotos(data.fotos),
    };
  }

  const querySnap = await db
    .collection("prendas")
    .where("codigo", "==", prendaId)
    .limit(1)
    .get();
  if (!querySnap.empty) {
    const doc = querySnap.docs[0];
    const data = doc.data() ?? {};
    return {
      id: doc.id,
      codigo: String(data.codigo || doc.id),
      nombre: String(data.nombre || prendaId),
      descripcion: String(data.descripcion || ""),
      categoria: String(data.categoria || ""),
      subcategoria: String(data.subcategoria || ""),
      fotos: extraerFotos(data.fotos),
    };
  }

  return null;
}

async function guardarImagenPermanente(
  uid: string,
  codigoPrenda: string,
  sourceUrl: string
): Promise<string> {
  const imageResponse = await fetch(sourceUrl);
  if (!imageResponse.ok) {
    throw new Error(`No se pudo descargar la imagen generada (${imageResponse.status})`);
  }

  const contentType = imageResponse.headers.get("content-type") || "image/jpeg";
  const ext = contentType.includes("webp")
    ? "webp"
    : contentType.includes("png")
      ? "png"
      : "jpg";
  const buffer = Buffer.from(await imageResponse.arrayBuffer());

  const bucket = getStorage().bucket();
  const safeCode = codigoPrenda.replace(/[^a-zA-Z0-9_-]/g, "_");
  const filePath = `clientes/${uid}/probador/resultados/${safeCode}.${ext}`;
  const downloadToken = randomUUID();

  await bucket.file(filePath).save(buffer, {
    metadata: {
      contentType,
      metadata: {
        firebaseStorageDownloadTokens: downloadToken,
        subidoPor: uid,
        origen: "genlook",
      },
    },
  });

  return (
    `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/` +
    `${encodeURIComponent(filePath)}?alt=media&token=${downloadToken}`
  );
}

async function registrarPruebaEnHistorial(
  uid: string,
  prenda: PrendaInfo
): Promise<void> {
  const db = getFirestore();
  const clienteRef = db.collection("clientes").doc(uid);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(clienteRef);
    const data = snap.data() ?? {};
    const existentes = Array.isArray(data.pruebasVirtuales)
      ? [...(data.pruebasVirtuales as Array<Record<string, unknown>>)]
      : [];

    const ahoraMs = Date.now();
    const seisHorasMs = 6 * 60 * 60 * 1000;
    const yaReciente = existentes.some(
      (item) =>
        item?.prendaCodigo === prenda.codigo &&
        typeof item?.fechaEpochMillis === "number" &&
        ahoraMs - item.fechaEpochMillis < seisHorasMs
    );

    if (!yaReciente) {
      existentes.push({
        prendaCodigo: prenda.codigo,
        prendaNombre: prenda.nombre,
        categoria: prenda.categoria,
        fechaEpochMillis: ahoraMs,
        comprada: false,
      });
      tx.set(
        clienteRef,
        {
          pruebasVirtuales: existentes,
          actualizadoEn: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    }
  });
}

/**
 * HU-16 CA-05 + Probador Virtual con Genlook:
 * 1. Rechaza categorías deshabilitadas (Remote Config).
 * 2. Verifica sesión, consentimiento Ley N° 29733 y foto registrada.
 * 3. Reutiliza caché en Firestore/Storage para no consumir créditos repetidos.
 * 4. Llama a Genlook /try-on/sync y guarda la imagen resultante en Storage.
 */
export const probarPrenda = onCall(
  {
    secrets: [genlookApiKey],
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (request) => {
    const {
      prendaId,
      categoria: categoriaInput,
      mock = false,
      forzarRegeneracion = false,
      talla = "",
      color = "",
      soloHistorial = false,
    } = (request.data ?? {}) as {
      prendaId?: string;
      categoria?: string;
      mock?: boolean;
      forzarRegeneracion?: boolean;
      /** HU-12 CA-02: talla y color elegidos por el cliente. */
      talla?: string;
      color?: string;
      /** HU-12 CA-04: abrir el resultado desde un aviso sin generar uno nuevo. */
      soloHistorial?: boolean;
    };
    const tallaElegida = String(talla).trim().slice(0, 10);
    const colorElegido = String(color).trim().slice(0, 40);

    if (!prendaId && !categoriaInput) {
      throw new HttpsError(
        "invalid-argument",
        "Se requiere 'prendaId' o 'categoria' para validar la prueba virtual."
      );
    }

    // 1. Validar política de categorías permitidas (HU-16)
    const disabledCategories = await obtenerCategoriasDeshabilitadas();
    const prenda = prendaId ? await buscarPrenda(prendaId) : null;

    const categoria = prenda?.categoria
      ? normalizar(prenda.categoria)
      : categoriaInput
        ? normalizar(categoriaInput)
        : "";
    const subcategoria = prenda?.subcategoria ? normalizar(prenda.subcategoria) : "";

    const estaDeshabilitada = Array.from(disabledCategories).some(
      (dis) =>
        categoria === dis ||
        categoria.includes(dis) ||
        (subcategoria.length > 0 && (subcategoria === dis || subcategoria.includes(dis)))
    );

    if (estaDeshabilitada) {
      throw new HttpsError(
        "failed-precondition",
        "Esta prenda no está disponible en el probador virtual por motivos de higiene y política de la tienda."
      );
    }

    if (!prendaId) {
      return {
        ok: true,
        habilitada: true,
        mensaje: "Categoría habilitada para el probador virtual.",
      };
    }

    if (!prenda) {
      throw new HttpsError("not-found", "No se encontró la prenda solicitada.");
    }

    // 2. Validar autenticación del usuario
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError(
        "unauthenticated",
        "Debes iniciar sesión para usar el probador virtual."
      );
    }

    // 3. Validar consentimiento y foto registrada del cliente
    const db = getFirestore();
    const clienteSnap = await db.collection("clientes").doc(uid).get();
    const clienteData = clienteSnap.data() ?? {};

    const consentimiento = clienteData.consentimientoProbador as
      | { aceptado?: boolean }
      | undefined;
    if (!consentimiento?.aceptado) {
      throw new HttpsError(
        "failed-precondition",
        "Debes aceptar el consentimiento de fotografía (Ley N° 29733) en la pestaña Probador."
      );
    }

    const probadorData = clienteData.probador as { fotoUrl?: string } | undefined;
    const fotoProbadorUrl =
      (typeof probadorData?.fotoUrl === "string" && probadorData.fotoUrl) ||
      (typeof clienteData.fotoProbadorUrl === "string" && clienteData.fotoProbadorUrl) ||
      "";

    if (!fotoProbadorUrl) {
      throw new HttpsError(
        "failed-precondition",
        "Registra primero tu fotografía de cuerpo completo en la pestaña Probador."
      );
    }

    const fotoPrendaUrl = prenda.fotos[0];
    if (!fotoPrendaUrl) {
      throw new HttpsError(
        "failed-precondition",
        "La prenda seleccionada no cuenta con una fotografía válida."
      );
    }

    // 4. Caché por usuario + prenda + foto actual para cuidar créditos
    const safeCode = prenda.codigo.replace(/[^a-zA-Z0-9_-]/g, "_");
    const cacheRef = db
      .collection("clientes")
      .doc(uid)
      .collection("probadorCache")
      .doc(safeCode);

    if (!mock && !forzarRegeneracion) {
      const cacheSnap = await cacheRef.get();
      const cacheData = cacheSnap.data();
      if (
        cacheSnap.exists &&
        cacheData?.fotoUsuarioUrl === fotoProbadorUrl &&
        typeof cacheData?.resultadoUrl === "string" &&
        cacheData.resultadoUrl.length > 0
      ) {
        return {
          ok: true,
          habilitada: true,
          resultadoUrl: cacheData.resultadoUrl,
          fotoOriginalUrl: fotoProbadorUrl,
          generationId: String(cacheData.generationId || ""),
          cached: true,
          isMock: false,
          // Al abrir el resultado desde un aviso no llegan talla ni color: se usan los de la generación.
          talla: tallaElegida || String(cacheData.talla ?? ""),
          color: colorElegido || String(cacheData.color ?? ""),
          // Las pruebas anteriores a Guardar/Descartar (HU-12 CA-06) se consideran guardadas.
          guardada: cacheData.guardada !== false,
          mensaje: "Prueba virtual recuperada de tu historial guardado.",
        };
      }
    }

    if (soloHistorial) {
      throw new HttpsError("not-found", "Esta prueba virtual ya no está disponible.");
    }

    // 5. Reservar una prueba del límite diario (las del historial y las de demostración no cuentan).
    const limiteDiario = await obtenerLimiteDiario();
    const restantesHoy = mock ? limiteDiario : await reservarPrueba(uid, limiteDiario);
    const inicio = Date.now();

    const generarConIa = async () => {
      // 6. Llamar a Genlook /tryon/v1/try-on/sync
      const apiKey = genlookApiKey.value().trim();
      if (!apiKey) {
        throw new HttpsError(
          "failed-precondition",
          "Falta configurar la clave de Genlook en el servidor."
        );
      }

      const requestBody: Record<string, unknown> = {
        person: {
          image: { source: { url: fotoProbadorUrl } },
        },
        products: [
          {
            externalId: prenda.codigo,
            title: prenda.nombre,
            description: [
              prenda.descripcion || `${prenda.categoria} ${prenda.subcategoria}`.trim(),
              colorElegido ? `Color: ${colorElegido}.` : "",
              tallaElegida ? `Talla: ${tallaElegida}.` : "",
            ].filter((t) => t.length > 0).join(" "),
            images: [{ source: { url: fotoPrendaUrl } }],
          },
        ],
      };
      if (mock) {
        requestBody.mock = true;
      }

      // HU-12 CA-07: si la IA no responde en 60 s se corta el intento.
      const corte = new AbortController();
      const temporizador = setTimeout(() => corte.abort(), TIEMPO_MAXIMO_IA_MS);
      let genlookRes: Response;
      try {
        genlookRes = await fetch(GENLOOK_SYNC_URL, {
          method: "POST",
          headers: {
            "x-api-key": apiKey,
            "Content-Type": "application/json",
          },
          body: JSON.stringify(requestBody),
          signal: corte.signal,
        });
      } catch (e) {
        if (corte.signal.aborted) {
          throw new HttpsError(
            "deadline-exceeded",
            "La IA tardó más de 60 segundos en responder. Este intento no se descontó de tus pruebas de hoy."
          );
        }
        throw new HttpsError("unavailable", "No se pudo conectar con el servicio de IA. Intenta de nuevo.");
      } finally {
        clearTimeout(temporizador);
      }

      const genlookJson = (await genlookRes.json().catch(() => ({}))) as GenlookSyncResponse;

      if (!genlookRes.ok) {
        if (genlookRes.status === 402 || genlookJson.code === "INSUFFICIENT_CREDITS") {
          throw new HttpsError(
            "resource-exhausted",
            "Se alcanzó el límite de créditos disponibles del probador virtual."
          );
        }
        if (genlookRes.status === 429 || genlookJson.code === "RATE_LIMITED") {
          throw new HttpsError(
            "resource-exhausted",
            "El probador virtual está procesando muchas solicitudes. Intenta en unos segundos."
          );
        }
        throw new HttpsError(
          "internal",
          genlookJson.message || `Error al generar la prueba virtual (${genlookRes.status}).`
        );
      }

      const tempResultUrl = genlookJson.resultImageUrl;
      if (!tempResultUrl) {
        throw new HttpsError(
          "internal",
          "Genlook no devolvió la URL de la imagen generada."
        );
      }

      // 6. En modo real guardamos permanentemente en Firebase Storage + caché + historial
      let finalUrl = tempResultUrl;
      if (!mock) {
        finalUrl = await guardarImagenPermanente(uid, prenda.codigo, tempResultUrl);

        await cacheRef.set({
          prendaCodigo: prenda.codigo,
          prendaNombre: prenda.nombre,
          categoria: prenda.categoria,
          fotoUsuarioUrl: fotoProbadorUrl,
          resultadoUrl: finalUrl,
          generationId: genlookJson.generationId || "",
          talla: tallaElegida,
          color: colorElegido,
          // HU-12 CA-06: queda pendiente hasta que el cliente elija Guardar o Descartar.
          guardada: false,
          generadoEn: FieldValue.serverTimestamp(),
        });

        await registrarPruebaEnHistorial(uid, prenda);
      }

      const duracionMs = Date.now() - inicio;
      logger.info("Prueba virtual generada", { uid, prenda: prenda.codigo, duracionMs, mock });

      return {
        ok: true,
        habilitada: true,
        resultadoUrl: finalUrl,
        fotoOriginalUrl: fotoProbadorUrl,
        generationId: genlookJson.generationId || "",
        cached: false,
        isMock: Boolean(mock),
        talla: tallaElegida,
        color: colorElegido,
        guardada: false,
        restantesHoy,
        limiteDiario,
        duracionMs,
        mensaje: mock
          ? "Vista previa generada en modo de prueba gratuito."
          : "Prueba virtual generada correctamente.",
      };
    };

    try {
      return await generarConIa();
    } catch (e) {
      // HU-12 CA-07: un intento fallido o cortado no se descuenta del límite diario.
      if (!mock) await liberarPrueba(uid);
      throw e;
    }
  }
);
