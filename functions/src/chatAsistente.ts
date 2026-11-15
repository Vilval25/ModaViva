import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { getApps, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

if (!getApps().length) {
  initializeApp();
}

const groqApiKey = defineSecret("GROQ_API_KEY");

const GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";
const GROQ_PRIMARY_MODEL = "qwen/qwen3.8-27b";
const GROQ_FALLBACK_MODEL = "openai/gpt-oss-20b";

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

function precioVigente(p: PrendaCatalogoResumen): number {
  if (p.precioPromocional !== null && p.precioPromocional > 0 && p.precioPromocional < p.precio) {
    return p.precioPromocional;
  }
  return p.precio;
}

/**
 * Asistente virtual de ModaViva impulsado por Groq (Llama 3.3 70B).
 * Inyecta catálogo vivo de Firestore, tarifas de envío en Lima y perfil del
 * cliente para responder sobre tallas, outfits, precios y cobertura.
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
      db.collection("prendas").limit(40).get(),
      db.collection("tarifasEnvio").get(),
      db.collection("clientes").doc(uid).get(),
      db.collection("clientes").doc(uid).collection("pedidos").limit(20).get(),
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

    const resumenCatalogo = catalogo
      .map((p) => {
        const pv = precioVigente(p);
        const promoTag =
          p.precioPromocional && p.precioPromocional < p.precio
            ? ` (antes S/ ${p.precio.toFixed(2)}, en oferta)`
            : "";
        return `- [${p.codigo}] ${p.nombre} | ${p.marca} | ${p.categoria}/${p.subcategoria} | Color: ${p.color} | S/ ${pv.toFixed(2)}${promoTag} | Tallas: ${p.tallas.join(", ")} | Stock: ${p.stockTotal}`;
      })
      .join("\n");

    const distritosConCobertura: string[] = [];
    const distritosSinCobertura: string[] = [];
    for (const doc of tarifasSnap.docs) {
      const d = doc.data() ?? {};
      const nombre = String(d.nombre || d.distrito || doc.id);
      const precio = Number(d.precio ?? d.costo ?? 0);
      const activo = d.activo !== false;
      if (activo && precio > 0) {
        distritosConCobertura.push(`${nombre}: S/ ${precio.toFixed(2)}`);
      } else {
        distritosSinCobertura.push(nombre);
      }
    }

    const clienteData = clienteSnap.data() ?? {};
    const nombreCliente = String(clienteData.nombres || "").trim();
    const alturaCm = clienteData.alturaCm ? `${clienteData.alturaCm} cm` : "no registrada";
    const tieneFotoProbador = Boolean(
      (clienteData.probador as { fotoUrl?: string } | undefined)?.fotoUrl ||
        clienteData.fotoProbadorUrl
    );

    const resumenPedidos = pedidosSnap.empty
      ? "Sin pedidos recientes."
      : pedidosSnap.docs
          .map((doc) => doc.data() ?? {})
          .sort((a, b) => Number(b.creadoEnMillis || 0) - Number(a.creadoEnMillis || 0))
          .slice(0, 3)
          .map((p) => {
            return `- Pedido ${p.numeroPedido || "?"}: estado=${p.estado || "?"}, entrega=${p.tipoEntrega || "?"}, total=S/ ${Number(p.total || 0).toFixed(2)}`;
          })
          .join("\n");

    const prendaActual = prendaCodigo
      ? catalogo.find((p) => p.codigo.toLowerCase() === prendaCodigo.toLowerCase())
      : undefined;

    const contextoPrendaActual = prendaActual
      ? `\nPRENDA QUE EL CLIENTE ESTÁ VIENDO AHORA:\n` +
        `Código: ${prendaActual.codigo}\n` +
        `Nombre: ${prendaActual.nombre} (${prendaActual.marca})\n` +
        `Descripción: ${prendaActual.descripcion}\n` +
        `Precio vigente: S/ ${precioVigente(prendaActual).toFixed(2)}\n` +
        `Tallas: ${prendaActual.tallas.join(", ")} | Stock total: ${prendaActual.stockTotal}\n` +
        (prendaActual.medidas ? `Medidas (cm): ${JSON.stringify(prendaActual.medidas)}\n` : "")
      : "";

    const systemPrompt = `Eres el Asistente Virtual oficial de ModaViva (tienda de moda en Lima, Perú).
Responde siempre en español claro, cordial, breve y práctico (máximo 140 palabras salvo que te pidan comparar varias opciones).

REGLAS ESTRICTAS:
1. Usa ÚNICAMENTE los productos, códigos, precios (en S/), tallas y distritos del contexto real provisto abajo. Nunca inventes prendas, precios ni coberturas.
2. Cuando recomiendes una prenda del catálogo, incluye su código entre corchetes tal como aparece en la lista, por ejemplo [MV-101], para que la app muestre su tarjeta directa.
3. Si preguntan por un distrito sin cobertura o con tarifa S/ 0.00, indica claramente que no hay cobertura de envío a domicilio para ese distrito y sugiere recojo gratis en tienda.
4. Si preguntan cómo ver cómo les queda una prenda, indícales que pueden usar el botón "Probarme" con el Probador Virtual IA (requiere aceptar privacidad y subir su foto de cuerpo completo en la pestaña Probador).

PERFIL DEL CLIENTE:
- Nombre: ${nombreCliente || "Cliente"}
- Estatura registrada: ${alturaCm}
- Foto en Probador Virtual: ${tieneFotoProbador ? "Sí registrada" : "Aún no registrada"}

PEDIDOS RECIENTES DEL CLIENTE:
${resumenPedidos}

ENVÍOS EN LIMA (Recojo en tienda es GRATIS):
- Con cobertura: ${distritosConCobertura.join(" | ") || "Consultar en Checkout"}
- Sin cobertura de delivery: ${distritosSinCobertura.join(", ") || "Ninguno"}
${contextoPrendaActual}
CATÁLOGO ACTUAL DE MODAVIVA:
${resumenCatalogo}`;

    const mensajesApi: Array<{ role: "system" | "user" | "assistant"; content: string }> = [
      { role: "system", content: systemPrompt },
    ];

    const historialSeguro = Array.isArray(historial) ? historial.slice(-8) : [];
    for (const turn of historialSeguro) {
      const content = typeof turn.content === "string" ? turn.content.trim() : "";
      if (!content) continue;
      const role = turn.role === "assistant" ? "assistant" : "user";
      mensajesApi.push({ role, content: content.slice(0, 1000) });
    }
    mensajesApi.push({ role: "user", content: textoUsuario });

    const apiKey = groqApiKey.value().trim();
    if (!apiKey) {
      throw new HttpsError(
        "failed-precondition",
        "Falta configurar la clave de Groq en el servidor."
      );
    }

    async function llamarGroq(modelo: string, esReasoning: boolean): Promise<Response> {
      const payload: Record<string, unknown> = {
        model: modelo,
        messages: mensajesApi,
        temperature: 0.35,
        max_tokens: esReasoning ? 1200 : 650,
      };
      if (esReasoning) {
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

    let groqRes = await llamarGroq(GROQ_PRIMARY_MODEL, false);
    if (groqRes.status === 400 || groqRes.status === 404) {
      groqRes = await llamarGroq(GROQ_FALLBACK_MODEL, true);
    }

    const groqJson = (await groqRes.json().catch(() => ({}))) as GroqResponse;
    if (!groqRes.ok) {
      if (groqRes.status === 429) {
        throw new HttpsError(
          "resource-exhausted",
          "El asistente está atendiendo muchas consultas. Intenta en unos segundos."
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
