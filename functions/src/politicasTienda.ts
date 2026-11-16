import { getFirestore } from "firebase-admin/firestore";

interface Politica {
  titulo: string;
  /** Una línea con lo esencial; viaja siempre para que el asistente sepa qué temas existen. */
  resumen: string;
  /** Texto completo; solo viaja si la pregunta trata de este tema (ahorra tokens). */
  texto: string;
  /** Raíces de palabras (sin tildes, en minúsculas) que indican que la pregunta es de este tema. */
  claves: string[];
}

/**
 * HU-27 CA-02 — Políticas de la tienda que el asistente usa como contexto.
 *
 * Los textos por defecto describen lo que la app hace hoy. El back-office
 * puede reemplazar cualquiera creando un documento en la colección
 * `politicasTienda` con el mismo ID (tallas, envios, devoluciones, pagos,
 * probador) y los campos `texto` y, opcionalmente, `resumen`.
 */
export const POLITICAS_POR_DEFECTO: Record<string, Politica> = {
  tallas: {
    titulo: "TALLAS",
    resumen: "tabla de medidas en la ficha y talla recomendada aproximada con altura y foto.",
    texto:
      "Cada prenda tiene su tabla de medidas en cm (pecho, cintura, cadera, etc.) en su ficha, botón 'Tabla de medidas'. " +
      "Si el cliente registró su altura y una foto de cuerpo completo, la app le recomienda una talla por prenda; " +
      "si queda entre dos tallas muestra ambas. Es una recomendación aproximada, no garantiza el ajuste exacto. " +
      "Tabla genérica (cuerpo, cm): XS pecho 78-82 cintura 60-64 cadera 84-88; S 83-87/65-69/89-93; " +
      "M 88-92/70-74/94-98; L 93-98/75-80/99-104; XL 99-105/81-87/105-111. " +
      "Si la talla no queda, se puede pedir cambio de talla dentro de los 7 días desde la entrega.",
    claves: ["talla", "medida", "pecho", "cintura", "cadera", "ajust", "grande", "pequen", "chic", "holgad", "apretad", "queda"],
  },
  envios: {
    titulo: "ENVÍOS Y RECOJO",
    resumen: "recojo gratis en 5 tiendas (24-48 h); delivery en distritos de Lima con cobertura.",
    texto:
      "Recojo en tienda gratis en cualquiera de las 5 tiendas de Lima; suele estar listo en 24 a 48 h según el stock de la tienda. " +
      "Envío a domicilio solo en los distritos de Lima Metropolitana con cobertura; el costo y el plazo dependen del distrito " +
      "(plazo de la tarifa más 2 a 3 días hábiles). El costo exacto se calcula en el checkout al elegir la dirección.",
    claves: ["envio", "enviar", "delivery", "despach", "distrito", "recojo", "recoger", "tienda", "llega", "demora", "tarda", "domicilio", "lima"],
  },
  devoluciones: {
    titulo: "CAMBIOS Y DEVOLUCIONES",
    resumen: "hasta 7 días calendario desde la entrega, desde Mis pedidos.",
    texto:
      "Plazo: hasta 7 días calendario después de que el pedido pasa a 'Entregado'. Se pide desde Mis pedidos > detalle del pedido > " +
      "'Solicitar cambio o devolución': se elige la prenda, cambio o devolución, el motivo y se pueden adjuntar hasta 3 fotos. " +
      "La prenda debe estar sin uso y con sus etiquetas. La solicitud queda 'En evaluación' y el equipo de ModaViva la aprueba " +
      "o rechaza; el cliente recibe un aviso y ve la resolución en Perfil > Mis solicitudes.",
    claves: ["devol", "devuelv", "cambi", "reembols", "solicitud", "garanti", "defect", "falla", "usad", "etiqueta"],
  },
  pagos: {
    titulo: "PAGOS",
    resumen: "tarjeta de crédito, débito, Yape/Plin o contra entrega (reserva de 30 min).",
    texto:
      "Medios: tarjeta de crédito, tarjeta de débito, billetera electrónica (Yape/Plin) y pago contra entrega / reserva. " +
      "Con contra entrega el stock queda reservado 30 minutos para completar el pago. Las tarjetas se pueden guardar en " +
      "Perfil > Métodos de pago (solo se guardan marca, últimos 4 dígitos y vencimiento). Al aprobarse el pago se puede " +
      "descargar la boleta en PDF.",
    claves: ["pag", "tarjeta", "yape", "plin", "billetera", "contra entrega", "efectivo", "boleta", "factura", "reserva", "cobr"],
  },
  probador: {
    titulo: "PROBADOR VIRTUAL",
    resumen: "botón 'Probarme' con IA; requiere consentimiento y foto de cuerpo completo.",
    texto:
      "El botón 'Probarme' genera con IA una imagen de la prenda sobre la foto del cliente. Requiere sesión, aceptar el " +
      "consentimiento de uso de la foto y registrar una foto de cuerpo completo en la pestaña Probador. Hay un límite de " +
      "pruebas por día. El resultado es una simulación referencial.",
    claves: ["probador", "probarme", "probar", "foto", "virtual", "imagen", "consentimiento"],
  },
};

/** Políticas vigentes: las de Firestore reemplazan a las de por defecto. */
export async function cargarPoliticas(): Promise<Record<string, Politica>> {
  const politicas: Record<string, Politica> = { ...POLITICAS_POR_DEFECTO };
  try {
    const snap = await getFirestore().collection("politicasTienda").get();
    for (const doc of snap.docs) {
      const texto = doc.get("texto");
      if (typeof texto !== "string" || texto.trim().length === 0) continue;
      const base = politicas[doc.id];
      politicas[doc.id] = {
        titulo: String(doc.get("titulo") ?? base?.titulo ?? doc.id.toUpperCase()),
        resumen: String(doc.get("resumen") ?? base?.resumen ?? texto.trim().slice(0, 120)),
        texto: texto.trim(),
        claves: base?.claves ?? [doc.id.toLowerCase()],
      };
    }
  } catch {
    // Sin acceso a Firestore se usan las políticas por defecto.
  }
  return politicas;
}

/**
 * Arma la sección de políticas para una consulta ya normalizada (sin tildes, en minúsculas):
 * van completas solo las del tema de la pregunta y, del resto, su resumen de una línea. Así se
 * mantiene el ahorro de tokens del filtrado por relevancia del catálogo.
 */
export function politicasParaConsulta(politicas: Record<string, Politica>, consultaNorm: string): string {
  const lista = Object.values(politicas);
  const relevantes = lista.filter((p) => p.claves.some((clave) => consultaNorm.includes(clave)));
  const otras = lista.filter((p) => !relevantes.includes(p));
  const lineas = relevantes.map((p) => `${p.titulo}: ${p.texto}`);
  if (otras.length > 0) {
    lineas.push("OTROS TEMAS (resumen): " + otras.map((p) => `${p.titulo}: ${p.resumen}`).join(" | "));
  }
  return lineas.join("\n");
}
