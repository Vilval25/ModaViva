import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { getApps, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

if (!getApps().length) {
  initializeApp();
}

const groqApiKey = defineSecret("GROQ_API_KEY");

const GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";

const GROQ_MODELS: Array<{ id: string; reasoning: boolean }> = [
  { id: "qwen/qwen3.8-27b", reasoning: false },
  { id: "openai/gpt-oss-20b", reasoning: true },
  { id: "openai/gpt-oss-120b", reasoning: true },
];

interface ChatTurnInput {
  role?: string;
  content?: string;
}

interface PrendaCatalogoResumen {
  codigo: string;
  nombre: string;
  marca: string;
  categoria: string;
  subcategoria: string;
  color: string;
  precio: number;
  precioPromocional: number | null;
  tallas: string[];
  stockTotal: number;
  fotoUrl: string;
  descripcion: string;
  medidas?: Record<string, unknown>;
}

interface GroqResponse {
  choices?: Array<{
    message?: {
      content?: string;
    };
  }>;
  error?: {
    message?: string;
  };
}

function normalizar(texto: string): string {
  return texto
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function precioVigente(p: PrendaCatalogoResumen): number {
  if (p.precioPromocional !== null && p.precioPromocional > 0 && p.precioPromocional < p.precio) {
    return p.precioPromocional;
  }
  return p.precio;
}

function tieneOferta(p: PrendaCatalogoResumen): boolean {
  return p.precioPromocional !== null && p.precioPromocional > 0 && p.precioPromocional < p.precio;
}

function seleccionarPrendasRelevantes(
  catalogo: PrendaCatalogoResumen[],
  consulta: string,
  prendaActual?: PrendaCatalogoResumen,
  limite = 12
): PrendaCatalogoResumen[] {
  const q = normalizar(consulta);
  const tokens = q.split(/[^a-z0-9]+/).filter((t) => t.length >= 3);
  const pideOfertas = /\b(oferta|ofertas|descuento|promo|promocion|rebaja|barato|economico)\b/.test(q);

  const puntuadas = catalogo.map((p) => {
    let score = 0;
    if (prendaActual && p.codigo === prendaActual.codigo) score += 100;
    if (pideOfertas && tieneOferta(p)) score += 10;
    if (p.stockTotal > 0) score += 1;

    const haystack = normalizar(
      `${p.codigo} ${p.nombre} ${p.marca} ${p.categoria} ${p.subcategoria} ${p.color} ${p.descripcion}`
    );
    for (const tok of tokens) {
      if (haystack.includes(tok)) score += 4;
    }
    return { p, score };
  });

  puntuadas.sort((a, b) => b.score - a.score);
  return puntuadas.slice(0, limite).map((item) => item.p);
}

/**
 * Asistente virtual de ModaViva impulsado por Groq.
 * Filtra el contexto relevante de Firestore por consulta para minimizar tokens
 * (TPM) y aplica respaldo automático entre modelos ante límites 429.
 */
export const chatAsistente = onCall(
  {
    secrets: [groqApiKey],
    timeoutSeconds: 45,
    memory: "256MiB",
  },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError(
        "unauthenticated",
        "Inicia sesión para conversar con el asistente de ModaViva."
      );
    }

    const { mensaje, historial = [], prendaCodigo } = (request.data ?? {}) as {
      mensaje?: string;
      historial?: ChatTurnInput[];
      prendaCodigo?: string;
    };

    const textoUsuario = typeof mensaje === "string" ? mensaje.trim() : "";
    if (!textoUsuario) {
      throw new HttpsError("invalid-argument", "Escribe un mensaje para el asistente.");
    }
    if (textoUsuario.length > 1000) {
      throw new HttpsError(
        "invalid-argument",
        "El mensaje es demasiado largo (máximo 1000 caracteres)."
      );
    }

    const db = getFirestore();

    const [prendasSnap, tarifasSnap, clienteSnap, pedidosSnap] = await Promise.all([
      db.collection("prendas").limit(60).get(),
      db.collection("tarifasEnvio").get(),
      db.collection("clientes").doc(uid).get(),
      db.collection("clientes").doc(uid).collection("pedidos").limit(10).get(),
    ]);

    const catalogo: PrendaCatalogoResumen[] = prendasSnap.docs.map((doc) => {
      const d = doc.data() ?? {};
      const colorObj = d.color as { nombre?: string } | undefined;
      const fotos = Array.isArray(d.fotos)
        ? d.fotos.filter((f): f is string => typeof f === "string" && f.length > 0)
        : [];
      return {
        codigo: String(d.codigo || doc.id),
        nombre: String(d.nombre || ""),
        marca: String(d.marca || ""),
        categoria: String(d.categoria || ""),
        subcategoria: String(d.subcategoria || ""),
        color: String(colorObj?.nombre || ""),
        precio: Number(d.precio || 0),
        precioPromocional: d.precioPromocional != null ? Number(d.precioPromocional) : null,
        tallas: Array.isArray(d.tallas) ? d.tallas.map(String) : [],
        stockTotal: Number(d.stockTotal || 0),
        fotoUrl: fotos[0] || "",
        descripcion: String(d.descripcion || ""),
        medidas: d.medidas as Record<string, unknown> | undefined,
      };
    });

    const prendaActual = prendaCodigo
      ? catalogo.find((p) => p.codigo.toLowerCase() === prendaCodigo.toLowerCase())
      : undefined;

    const historialSeguro = Array.isArray(historial) ? historial.slice(-4) : [];
    const textoContextoConsulta = [
      textoUsuario,
      ...historialSeguro.slice(-2).map((h) => String(h.content || "")),
    ].join(" ");

    const prendasSeleccionadas = seleccionarPrendasRelevantes(
      catalogo,
      textoContextoConsulta,
      prendaActual,
      12
    );

    const resumenCatalogo = prendasSeleccionadas
      .map((p) => {
        const pv = precioVigente(p);
        const promo = tieneOferta(p) ? ` (oferta, antes S/${p.precio.toFixed(2)})` : "";
        return `[${p.codigo}] ${p.nombre} | ${p.categoria}/${p.subcategoria} | ${p.color} | S/${pv.toFixed(2)}${promo} | Tallas:${p.tallas.join(",")} | Stock:${p.stockTotal}`;
      })
      .join("\n");

    const distritosConCobertura: string[] = [];
    const distritosSinCobertura: string[] = [];
    const nombresDistritosNorm: string[] = [];
    for (const doc of tarifasSnap.docs) {
      const d = doc.data() ?? {};
      const nombre = String(d.nombre || d.distrito || doc.id);
      const precio = Number(d.precio ?? d.costo ?? 0);
      const activo = d.activo !== false;
      nombresDistritosNorm.push(normalizar(nombre));
      if (activo && precio > 0) {
        distritosConCobertura.push(`${nombre}: S/${precio.toFixed(2)}`);
      } else {
        distritosSinCobertura.push(nombre);
      }
    }

    const qNorm = normalizar(textoContextoConsulta);
    const mencionaEnvio =
      /\b(envio|envios|delivery|distrito|distritos|lima|recojo|tienda|costo|tarifa|cobertura|llega|llegan)\b/.test(qNorm) ||
      nombresDistritosNorm.some((d) => d.length >= 4 && qNorm.includes(d));

    const seccionEnvios = mencionaEnvio
      ? `ENVÍOS LIMA (Recojo en tienda GRATIS): Con cobertura: ${distritosConCobertura.join(", ")}. Sin cobertura delivery: ${distritosSinCobertura.join(", ") || "ninguno"}.`
      : "ENVÍOS: Recojo gratis en tienda; delivery en distritos habilitados de Lima.";

    const mencionaPedidos = /\b(pedido|pedidos|compra|compras|orden|estado|enviado|entregado)\b/.test(qNorm);
    const resumenPedidos = !mencionaPedidos || pedidosSnap.empty
      ? ""
      : "\nPEDIDOS RECIENTES:\n" +
        pedidosSnap.docs
          .map((doc) => doc.data() ?? {})
          .sort((a, b) => Number(b.creadoEnMillis || 0) - Number(a.creadoEnMillis || 0))
          .slice(0, 3)
          .map((p) => `- ${p.numeroPedido || "?"}: ${p.estado || "?"}, ${p.tipoEntrega || "?"}, S/${Number(p.total || 0).toFixed(2)}`)
          .join("\n");

    const clienteData = clienteSnap.data() ?? {};
    const nombreCliente = String(clienteData.nombres || "").trim();
    const alturaCm = clienteData.alturaCm ? `${clienteData.alturaCm}cm` : "no reg.";
    const tieneFotoProbador = Boolean(
      (clienteData.probador as { fotoUrl?: string } | undefined)?.fotoUrl ||
        clienteData.fotoProbadorUrl
    );

    const contextoPrendaActual = prendaActual
      ? `\nPRENDA ACTUAL: [${prendaActual.codigo}] ${prendaActual.nombre} (${prendaActual.marca}) - ${prendaActual.descripcion} | S/${precioVigente(prendaActual).toFixed(2)} | Tallas: ${prendaActual.tallas.join(",")}` +
        (prendaActual.medidas ? ` | Medidas: ${JSON.stringify(prendaActual.medidas)}` : "")
      : "";

    const systemPrompt = `Eres el Asistente Virtual de ModaViva (Lima, Perú). Responde en español, breve y práctico (máx 120 palabras).
REGLAS: Usa SOLO datos del catálogo/envíos abajo; nunca inventes precios ni prendas. Al recomendar una prenda escribe su código entre corchetes ej. [MV-101]. Si un distrito no tiene cobertura o es S/0, indica que no hay delivery y sugiere recojo gratis en tienda. Para probar ropa, menciona el botón "Probarme" (requiere foto en Probador).
CLIENTE: ${nombreCliente || "Cliente"} | Altura: ${alturaCm} | Foto probador: ${tieneFotoProbador ? "Sí" : "No"}${resumenPedidos}
${seccionEnvios}${contextoPrendaActual}
CATÁLOGO RELEVANTE (${prendasSeleccionadas.length} de ${catalogo.length}):
${resumenCatalogo}`;

    const mensajesApi: Array<{ role: "system" | "user" | "assistant"; content: string }> = [
      { role: "system", content: systemPrompt },
    ];

    for (const turn of historialSeguro) {
      const content = typeof turn.content === "string" ? turn.content.trim() : "";
      if (!content) continue;
      const role = turn.role === "assistant" ? "assistant" : "user";
      mensajesApi.push({ role, content: content.slice(0, 400) });
    }
    mensajesApi.push({ role: "user", content: textoUsuario });

    const apiKey = groqApiKey.value().trim();
    if (!apiKey) {
      throw new HttpsError(
        "failed-precondition",
        "Falta configurar la clave de Groq en el servidor."
      );
    }

    async function llamarGroq(modelo: { id: string; reasoning: boolean }): Promise<Response> {
      const payload: Record<string, unknown> = {
        model: modelo.id,
        messages: mensajesApi,
        temperature: 0.35,
        max_tokens: modelo.reasoning ? 800 : 400,
      };
      if (modelo.reasoning) {
        payload.reasoning_effort = "low";
      }
      return fetch(GROQ_CHAT_URL, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${apiKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });
    }

    let groqRes: Response | null = null;
    for (const modelo of GROQ_MODELS) {
      const res = await llamarGroq(modelo);
      groqRes = res;
      if (res.ok) break;
      if (res.status !== 400 && res.status !== 404 && res.status !== 429 && res.status !== 413) {
        break;
      }
    }

    if (!groqRes) {
      throw new HttpsError("internal", "No se pudo contactar con el asistente.");
    }

    const groqJson = (await groqRes.json().catch(() => ({}))) as GroqResponse;
    if (!groqRes.ok) {
      if (groqRes.status === 429) {
        throw new HttpsError(
          "resource-exhausted",
          "Límite temporal de tokens alcanzado. Espera unos segundos e intenta de nuevo."
        );
      }
      throw new HttpsError(
        "internal",
        groqJson.error?.message || `Error al consultar el asistente (${groqRes.status}).`
      );
    }

    const rawRespuesta = groqJson.choices?.[0]?.message?.content ?? "";
    const respuesta =
      rawRespuesta.replace(/<think>[\s\S]*?<\/think>/gi, "").trim() ||
      "Lo siento, no pude generar una respuesta en este momento.";

    const prendasSugeridas = catalogo
      .filter((p) => respuesta.toUpperCase().includes(p.codigo.toUpperCase()))
      .slice(0, 3)
      .map((p) => ({
        codigo: p.codigo,
        nombre: p.nombre,
        precio: precioVigente(p),
        fotoUrl: p.fotoUrl,
      }));

    return {
      ok: true,
      respuesta,
      prendasSugeridas,
    };
  }
);
