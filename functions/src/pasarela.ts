import { HttpsError, onCall } from "firebase-functions/v2/https";
import { getApps, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { randomBytes } from "node:crypto";

if (!getApps().length) {
  initializeApp();
}

/**
 * Pasarela de pago en modo prueba (sandbox) que corre en el servidor (HU-21).
 *
 * Los datos completos de la tarjeta solo viajan del celular a esta función y
 * nunca se guardan: en Firestore quedan marca, últimos 4 dígitos, vencimiento
 * y el token que emite la pasarela (CA-03, CA-04).
 *
 * Tarjetas de prueba para la demo (todas pasan la validación de Luhn):
 *  - 4111 1111 1111 1111 / 5555 5555 5555 4444 → aprobadas
 *  - 4200 0000 0000 0000 o CVV 000 → rechazada por fondos insuficientes
 *  - 4000 0000 0000 0002           → rechazada, tarjeta restringida por el banco
 *  - vencimiento pasado        → rechazada, tarjeta vencida
 *  - cualquier otra válida     → aprobada
 */

export interface DatosTarjeta {
  numero: string;
  vencimiento: string; // MM/AA
  cvv: string;
  titular: string;
  tipo?: string; // CREDITO | DEBITO
}

export interface TarjetaTokenizada {
  marca: string;
  ultimos4: string;
  vencimiento: string;
  titular: string;
  tipo: string;
  tokenPasarela: string;
}

export type ResultadoPasarela =
  | { aprobado: true; tarjeta: TarjetaTokenizada }
  | { aprobado: false; motivo: string };

function detectarMarca(numero: string): string {
  if (numero.startsWith("34") || numero.startsWith("37")) return "Amex";
  if (numero.startsWith("36") || numero.startsWith("38")) return "Diners";
  if (numero.startsWith("4")) return "Visa";
  if (numero.startsWith("5") || numero.startsWith("2")) return "Mastercard";
  return "Visa";
}

/** Algoritmo de Luhn: descarta números mal digitados antes de "ir al banco". */
function pasaLuhn(numero: string): boolean {
  let suma = 0;
  let doblar = false;
  for (let i = numero.length - 1; i >= 0; i--) {
    let d = Number(numero[i]);
    if (doblar) {
      d *= 2;
      if (d > 9) d -= 9;
    }
    suma += d;
    doblar = !doblar;
  }
  return suma % 10 === 0;
}

/** Errores de formato: se muestran como dato inválido, no como rechazo del banco. */
function validarFormato(d: DatosTarjeta): string | null {
  const numero = String(d.numero ?? "").replace(/\D/g, "");
  if (numero.length < 15 || numero.length > 16) {
    return "Ingresa un número de tarjeta válido de 15 o 16 dígitos.";
  }
  if (!pasaLuhn(numero)) {
    return "El número de tarjeta no es válido. Revisa los dígitos.";
  }
  if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(String(d.vencimiento ?? "").trim())) {
    return "Ingresa la fecha de vencimiento en formato MM/AA (ej. 08/28).";
  }
  const cvv = String(d.cvv ?? "").replace(/\D/g, "");
  if (cvv.length < 3 || cvv.length > 4) {
    return "Ingresa un código CVV válido de 3 o 4 dígitos.";
  }
  if (String(d.titular ?? "").trim().length < 3) {
    return "Ingresa el nombre del titular de la tarjeta.";
  }
  return null;
}

function estaVencida(vencimiento: string, ahora: Date): boolean {
  const [mm, aa] = vencimiento.split("/").map(Number);
  // La tarjeta es válida hasta el último día de su mes de vencimiento.
  const finDeMes = new Date(2000 + aa, mm, 1);
  return finDeMes <= ahora;
}

/**
 * Valida la tarjeta y simula la respuesta del banco emisor. Lanza HttpsError
 * si el formato es inválido; devuelve aprobado/rechazado con el motivo.
 */
export function procesarTarjeta(d: DatosTarjeta): ResultadoPasarela {
  const errorFormato = validarFormato(d);
  if (errorFormato) {
    throw new HttpsError("invalid-argument", errorFormato);
  }

  const numero = d.numero.replace(/\D/g, "");
  const cvv = d.cvv.replace(/\D/g, "");
  const vencimiento = d.vencimiento.trim();

  if (numero.endsWith("0000") || cvv === "000") {
    return { aprobado: false, motivo: "Fondos insuficientes. El banco emisor rechazó la operación." };
  }
  if (numero.endsWith("0002")) {
    return { aprobado: false, motivo: "Tarjeta restringida por el banco emisor. Comunícate con tu banco." };
  }
  if (estaVencida(vencimiento, new Date())) {
    return { aprobado: false, motivo: "La tarjeta está vencida." };
  }

  const marca = detectarMarca(numero);
  const ultimos4 = numero.slice(-4);
  return {
    aprobado: true,
    tarjeta: {
      marca,
      ultimos4,
      vencimiento,
      titular: String(d.titular).trim().toUpperCase(),
      tipo: d.tipo === "DEBITO" ? "DEBITO" : "CREDITO",
      tokenPasarela: `tok_sandbox_${randomBytes(12).toString("hex")}`,
    },
  };
}

/**
 * Guarda una tarjeta aprobada en clientes/{uid}/metodosPago. Si es la primera,
 * queda como predeterminada. Devuelve el id del documento.
 */
export async function guardarTarjetaTokenizada(
  uid: string,
  tarjeta: TarjetaTokenizada
): Promise<string> {
  const col = getFirestore().collection("clientes").doc(uid).collection("metodosPago");
  const id = `card_${tarjeta.marca.toLowerCase()}_${tarjeta.ultimos4}`;
  const existentes = await col.limit(2).get();
  const otras = existentes.docs.filter((doc) => doc.id !== id);

  await col.doc(id).set(
    {
      ...tarjeta,
      esPredeterminada: otras.length === 0,
      actualizadoEn: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );
  return id;
}

/**
 * Perfil > Métodos de pago > Agregar tarjeta (HU-21 CA-04, CA-05).
 * Si la pasarela la rechaza, devuelve el motivo y no guarda nada.
 */
export const registrarTarjeta = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Inicia sesión para guardar tarjetas.");

  const resultado = procesarTarjeta((request.data || {}) as DatosTarjeta);
  if (!resultado.aprobado) {
    return { ok: false, motivo: resultado.motivo };
  }

  const id = await guardarTarjetaTokenizada(uid, resultado.tarjeta);
  const { tokenPasarela: _token, ...visible } = resultado.tarjeta;
  return { ok: true, tarjeta: { id, ...visible } };
});
