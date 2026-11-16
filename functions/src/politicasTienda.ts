import { getFirestore } from "firebase-admin/firestore";

/**
 * HU-27 CA-02 — Políticas de la tienda que el asistente usa como contexto.
 *
 * Los textos por defecto describen lo que la app hace hoy. El back-office
 * puede reemplazar cualquiera creando un documento en la colección
 * `politicasTienda` con el mismo ID (tallas, envios, devoluciones, pagos,
 * probador) y el campo `texto`.
 */
export const POLITICAS_POR_DEFECTO: Record<string, { titulo: string; texto: string }> = {
  tallas: {
    titulo: "TALLAS",
    texto:
      "Cada prenda tiene su tabla de medidas en cm (pecho, cintura, cadera, etc.) en su ficha, botón 'Tabla de medidas'. " +
      "Si el cliente registró su altura y una foto de cuerpo completo, la app le recomienda una talla por prenda; " +
      "si queda entre dos tallas muestra ambas. Es una recomendación aproximada, no garantiza el ajuste exacto. " +
      "Tabla genérica (cuerpo, cm): XS pecho 78-82 cintura 60-64 cadera 84-88; S 83-87/65-69/89-93; " +
      "M 88-92/70-74/94-98; L 93-98/75-80/99-104; XL 99-105/81-87/105-111. " +
      "Si la talla no queda, se puede pedir cambio de talla dentro de los 7 días desde la entrega.",
  },
  envios: {
    titulo: "ENVÍOS Y RECOJO",
    texto:
      "Recojo en tienda gratis en cualquiera de las 5 tiendas de Lima; suele estar listo en 24 a 48 h según el stock de la tienda. " +
      "Envío a domicilio solo en los distritos de Lima Metropolitana con cobertura; el costo y el plazo dependen del distrito " +
      "(plazo de la tarifa más 2 a 3 días hábiles). El costo exacto se calcula en el checkout al elegir la dirección.",
  },
  devoluciones: {
    titulo: "CAMBIOS Y DEVOLUCIONES",
    texto:
      "Plazo: hasta 7 días calendario después de que el pedido pasa a 'Entregado'. Se pide desde Mis pedidos > detalle del pedido > " +
      "'Solicitar cambio o devolución': se elige la prenda, cambio o devolución, el motivo y se pueden adjuntar hasta 3 fotos. " +
      "La prenda debe estar sin uso y con sus etiquetas. La solicitud queda 'En evaluación' y el equipo de ModaViva la aprueba " +
      "o rechaza; el cliente recibe un aviso y ve la resolución en Perfil > Mis solicitudes.",
  },
  pagos: {
    titulo: "PAGOS",
    texto:
      "Medios: tarjeta de crédito, tarjeta de débito, billetera electrónica (Yape/Plin) y pago contra entrega / reserva. " +
      "Con contra entrega el stock queda reservado 30 minutos para completar el pago. Las tarjetas se pueden guardar en " +
      "Perfil > Métodos de pago (solo se guardan marca, últimos 4 dígitos y vencimiento). Al aprobarse el pago se puede " +
      "descargar la boleta en PDF.",
  },
  probador: {
    titulo: "PROBADOR VIRTUAL",
    texto:
      "El botón 'Probarme' genera con IA una imagen de la prenda sobre la foto del cliente. Requiere sesión, aceptar el " +
      "consentimiento de uso de la foto y registrar una foto de cuerpo completo en la pestaña Probador. Hay un límite de " +
      "pruebas por día. El resultado es una simulación referencial.",
  },
};

/** Textos de las políticas: los de Firestore reemplazan a los de por defecto. */
export async function cargarPoliticas(): Promise<string> {
  const politicas = { ...POLITICAS_POR_DEFECTO };
  try {
    const snap = await getFirestore().collection("politicasTienda").get();
    for (const doc of snap.docs) {
      const texto = doc.get("texto");
      if (typeof texto === "string" && texto.trim().length > 0) {
        politicas[doc.id] = {
          titulo: String(doc.get("titulo") ?? politicas[doc.id]?.titulo ?? doc.id.toUpperCase()),
          texto: texto.trim(),
        };
      }
    }
  } catch {
    // Sin acceso a Firestore se usan las políticas por defecto.
  }
  return Object.values(politicas)
    .map((p) => `${p.titulo}: ${p.texto}`)
    .join("\n");
}
