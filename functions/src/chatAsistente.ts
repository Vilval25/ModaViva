import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { getApps, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { cargarPoliticas, politicasParaConsulta } from "./politicasTienda.js";

if (!getApps().length) {
  initializeApp();
}

const groqApiKey = defineSecret("GROQ_API_KEY");

const GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";
/** HU-27 CA-05: si el modelo no responde en este tiempo se corta y la app ofrece Reintentar. */
const TIEMPO_MAXIMO_LLM_MS = 20_000;
/** Mensajes previos de la conversación que se envían al modelo. */
const TURNOS_HISTORIAL = 6;

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

function promocionVigente(d: FirebaseFirestore.DocumentData): number | null {
  const promo = d.precioPromo ?? d.precioPromocional;
  if (promo == null) return null;
  const hasta = d.promoHasta?.toDate?.() as Date | undefined;
  if (hasta && hasta.getTime() < Date.now()) return null;
  return Number(promo);
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

    const chatRef = db.collection("clientes").doc(uid).collection("chat");
    const [prendasSnap, tarifasSnap, clienteSnap, pedidosSnap, chatSnap, politicas] = await Promise.all([
      db.collection("prendas").limit(60).get(),
      db.collection("tarifasEnvio").get(),
      db.collection("clientes").doc(uid).get(),
      db.collection("clientes").doc(uid).collection("pedidos").limit(10).get(),
      chatRef.orderBy("creadoEnMillis", "desc").limit(TURNOS_HISTORIAL).get(),
      cargarPoliticas(),
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
        // El catálogo guarda la promoción en precioPromo con vigencia hasta promoHasta.
        precioPromocional: promocionVigente(d),
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

    // HU-27 CA-04: la conversación guardada en Firestore es la fuente del historial; el que
    // envía la app solo se usa si aún no hay nada guardado.
    const historialGuardado: ChatTurnInput[] = chatSnap.docs
      .map((doc) => ({
        role: doc.get("esUsuario") === true ? "user" : "assistant",
        content: String(doc.get("texto") ?? ""),
      }))
      .reverse();
    const historialSeguro =
      historialGuardado.length > 0
        ? historialGuardado
        : Array.isArray(historial)
          ? historial.slice(-4)
          : [];
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
      const activo = d.activo !== false && d.cobertura !== false;
      const plazo = typeof d.plazoDias === "number" ? ` (${d.plazoDias + 2}-${d.plazoDias + 3} días hábiles)` : "";
      nombresDistritosNorm.push(normalizar(nombre));
      if (activo && precio > 0) {
        distritosConCobertura.push(`${nombre}: S/${precio.toFixed(2)}${plazo}`);
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

    // Solo van completas las políticas del tema de la pregunta (mismo criterio que el catálogo).
    const seccionPoliticas = politicasParaConsulta(politicas, qNorm);

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
TEMAS: tallas, envíos y recojo, cambios y devoluciones, pagos, el probador virtual, prendas del catálogo y los pedidos del cliente.
REGLAS:
- Responde SOLO con la información de POLÍTICAS, ENVÍOS, PEDIDOS y CATÁLOGO de abajo; nunca inventes precios, stock, prendas, pedidos, plazos ni políticas.
- Si preguntan algo fuera de esos temas, di amablemente que solo puedes ayudar con compras en ModaViva y menciona los temas.
- Si el dato no está abajo (por ejemplo, un pedido que no aparece o un distrito sin tarifa), di claramente que no tienes esa información; no la supongas.
- Al recomendar una prenda escribe su código entre corchetes ej. [MV-101].
- Si un distrito no tiene cobertura o es S/0, indica que no hay delivery y sugiere el recojo gratis en tienda.
POLÍTICAS:
${seccionPoliticas}
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
      const corte = new AbortController();
      const temporizador = setTimeout(() => corte.abort(), TIEMPO_MAXIMO_LLM_MS);
      try {
        return await fetch(GROQ_CHAT_URL, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${apiKey}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
          signal: corte.signal,
        });
      } catch {
        throw new HttpsError(
          "unavailable",
          "El asistente no respondió a tiempo. Toca Reintentar para volver a enviar tu mensaje."
        );
      } finally {
        clearTimeout(temporizador);
      }
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

    // HU-27 CA-04: se guardan la pregunta y la respuesta juntas solo si hubo respuesta; si
    // falló, la app conserva el mensaje para reintentar.
    const ahora = Date.now();
    const lote = db.batch();
    lote.set(chatRef.doc(), {
      texto: textoUsuario,
      esUsuario: true,
      creadoEnMillis: ahora,
      creadoEn: FieldValue.serverTimestamp(),
    });
    lote.set(chatRef.doc(), {
      texto: respuesta,
      esUsuario: false,
      prendasSugeridas,
      creadoEnMillis: ahora + 1,
      creadoEn: FieldValue.serverTimestamp(),
    });
    await lote.commit();

    return {
      ok: true,
      respuesta,
      prendasSugeridas,
    };
  }
);
