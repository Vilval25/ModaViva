// Pruebas de firestore.rules (HT-02 CA-04). Se ejecutan contra el emulador:
//   firebase emulators:exec --only firestore,storage "npm --prefix rules-tests test"

import { after, before, beforeEach, describe, test } from "node:test";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  collection,
  serverTimestamp,
  setDoc,
  updateDoc,
} from "firebase/firestore";

const ANA = "cli-ana";
const BETO = "cli-beto";

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-modaviva",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});

after(() => env.cleanup());

beforeEach(async () => {
  await env.clearFirestore();
  // Datos de partida escritos sin reglas, como lo haría el script de carga.
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "clientes", ANA), { nombres: "Ana", email: "ana@test.pe" });
    await setDoc(doc(db, "clientes", BETO), { nombres: "Beto", email: "beto@test.pe" });
    await setDoc(doc(db, "clientes", BETO, "favoritos", "BL-1002"), { agregadoEn: new Date() });
    await setDoc(doc(db, "clientes", BETO, "carrito", "BL-1002_M"), {
      prendaId: "BL-1002", talla: "M", cantidad: 1, agregadoEn: new Date(),
    });
    await setDoc(doc(db, "prendas", "BL-1002"), { nombre: "Blusa", precio: 89.9 });
    await setDoc(doc(db, "cupones", "BIENVENIDA"), { tipo: "porcentaje", valor: 10 });
    await setDoc(doc(db, "documentos", "40124685"), { authUid: BETO });
  });
});

const guest = () => env.unauthenticatedContext().firestore();
const as = (uid) => env.authenticatedContext(uid).firestore();

describe("clientes", () => {
  test("un invitado no puede leer ni escribir perfiles", async () => {
    await assertFails(getDoc(doc(guest(), "clientes", ANA)));
    await assertFails(setDoc(doc(guest(), "clientes", ANA), { nombres: "X" }));
  });

  test("un cliente lee y edita su propio perfil", async () => {
    await assertSucceeds(getDoc(doc(as(ANA), "clientes", ANA)));
    await assertSucceeds(updateDoc(doc(as(ANA), "clientes", ANA), { nombres: "Ana María" }));
  });

  test("un cliente nuevo crea su propio perfil", async () => {
    await assertSucceeds(setDoc(doc(as("cli-nuevo"), "clientes", "cli-nuevo"), { nombres: "Nuevo" }));
  });

  test("un cliente no puede leer ni escribir el perfil de otro", async () => {
    await assertFails(getDoc(doc(as(ANA), "clientes", BETO)));
    await assertFails(updateDoc(doc(as(ANA), "clientes", BETO), { nombres: "Hackeado" }));
    await assertFails(setDoc(doc(as(ANA), "clientes", "cli-otro"), { nombres: "Otro" }));
  });

  test("no se puede listar la colección de clientes", async () => {
    await assertFails(getDocs(collection(as(ANA), "clientes")));
  });

  test("nadie borra un perfil desde la app", async () => {
    await assertFails(deleteDoc(doc(as(ANA), "clientes", ANA)));
  });
});

describe("favoritos", () => {
  test("un cliente agrega y quita sus favoritos", async () => {
    const ref = doc(as(ANA), "clientes", ANA, "favoritos", "BL-1002");
    await assertSucceeds(setDoc(ref, { agregadoEn: serverTimestamp() }));
    await assertSucceeds(deleteDoc(ref));
  });

  test("un favorito solo acepta agregadoEn", async () => {
    const ref = doc(as(ANA), "clientes", ANA, "favoritos", "BL-1002");
    await assertFails(setDoc(ref, { agregadoEn: serverTimestamp(), precio: 1 }));
  });

  test("un cliente no ve ni toca los favoritos de otro", async () => {
    await assertFails(getDocs(collection(as(ANA), "clientes", BETO, "favoritos")));
    await assertFails(deleteDoc(doc(as(ANA), "clientes", BETO, "favoritos", "BL-1002")));
  });
});

describe("carrito", () => {
  const item = (cantidad) => ({
    prendaId: "BL-1002", talla: "M", cantidad, agregadoEn: serverTimestamp(),
  });

  test("un cliente agrega un ítem válido a su carrito", async () => {
    await assertSucceeds(setDoc(doc(as(ANA), "clientes", ANA, "carrito", "BL-1002_M"), item(2)));
  });

  test("se rechazan cantidades fuera de 1–99 o no enteras", async () => {
    const ref = doc(as(ANA), "clientes", ANA, "carrito", "BL-1002_M");
    await assertFails(setDoc(ref, item(0)));
    await assertFails(setDoc(ref, item(100)));
    await assertFails(setDoc(ref, item(1.5)));
  });

  test("el carrito no acepta campos extra como el precio o el color", async () => {
    const ref = doc(as(ANA), "clientes", ANA, "carrito", "BL-1002_M");
    await assertFails(setDoc(ref, { ...item(1), precio: 1 }));
    await assertFails(setDoc(ref, { ...item(1), color: "blanco" }));
  });

  test("un cliente no ve ni toca el carrito de otro", async () => {
    await assertFails(getDoc(doc(as(ANA), "clientes", BETO, "carrito", "BL-1002_M")));
    await assertFails(setDoc(doc(as(ANA), "clientes", BETO, "carrito", "X_M"), item(1)));
  });
});

describe("notificaciones (HU-18)", () => {
  const aviso = (extra = {}) => ({
    tipo: "PEDIDO",
    titulo: "Pedido aprobado",
    texto: "Tu pago fue confirmado.",
    creadaEn: serverTimestamp(),
    leida: false,
    refId: "ped-1",
    ...extra,
  });

  beforeEach(async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "clientes", ANA, "notificaciones", "n1"), {
        tipo: "PEDIDO", titulo: "Aviso", texto: "", creadaEn: new Date(), leida: false,
      });
    });
  });

  test("un cliente lee y crea sus propios avisos", async () => {
    await assertSucceeds(getDocs(collection(as(ANA), "clientes", ANA, "notificaciones")));
    await assertSucceeds(setDoc(doc(as(ANA), "clientes", ANA, "notificaciones", "n2"), aviso()));
  });

  test("un aviso con tipo, campos extra o ya leído se rechaza", async () => {
    const ref = (id) => doc(as(ANA), "clientes", ANA, "notificaciones", id);
    await assertFails(setDoc(ref("x1"), aviso({ tipo: "SPAM" })));
    await assertFails(setDoc(ref("x2"), aviso({ urgente: true })));
    await assertFails(setDoc(ref("x3"), aviso({ leida: true })));
    await assertFails(setDoc(ref("x4"), aviso({ creadaEn: new Date(2020, 0, 1) })));
  });

  test("solo se puede cambiar el campo leida", async () => {
    const ref = doc(as(ANA), "clientes", ANA, "notificaciones", "n1");
    await assertSucceeds(updateDoc(ref, { leida: true }));
    await assertFails(updateDoc(ref, { titulo: "Otro" }));
  });

  test("un cliente no ve ni toca los avisos de otro", async () => {
    await assertFails(getDocs(collection(as(BETO), "clientes", ANA, "notificaciones")));
    await assertFails(updateDoc(doc(as(BETO), "clientes", ANA, "notificaciones", "n1"), { leida: true }));
    await assertFails(setDoc(doc(as(BETO), "clientes", ANA, "notificaciones", "n9"), aviso()));
    await assertFails(getDocs(collection(guest(), "clientes", ANA, "notificaciones")));
  });
});

describe("metodosPago (HU-21)", () => {
  const tarjeta = (extra = {}) => ({
    marca: "Visa",
    ultimos4: "1111",
    vencimiento: "08/29",
    titular: "ANA",
    tipo: "CREDITO",
    tokenPasarela: "tok_sandbox_abc",
    esPredeterminada: true,
    ...extra,
  });

  beforeEach(async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "clientes", ANA, "metodosPago", "card_visa_1111"), tarjeta());
    });
  });

  test("un cliente lee, elimina y cambia la predeterminada de sus tarjetas", async () => {
    const ref = doc(as(ANA), "clientes", ANA, "metodosPago", "card_visa_1111");
    await assertSucceeds(getDocs(collection(as(ANA), "clientes", ANA, "metodosPago")));
    await assertSucceeds(updateDoc(ref, { esPredeterminada: false }));
    await assertFails(updateDoc(ref, { ultimos4: "2222" }));
    await assertSucceeds(deleteDoc(ref));
  });

  test("nunca se guarda el número completo ni el CVV", async () => {
    const ref = (id) => doc(as(ANA), "clientes", ANA, "metodosPago", id);
    await assertSucceeds(setDoc(ref("demo"), tarjeta()));
    await assertFails(setDoc(ref("x1"), tarjeta({ numero: "4111111111111111" })));
    await assertFails(setDoc(ref("x2"), tarjeta({ cvv: "123" })));
    await assertFails(setDoc(ref("x3"), tarjeta({ ultimos4: "4111111111111111" })));
    await assertFails(setDoc(ref("x4"), tarjeta({ tokenPasarela: "4111111111111111" })));
  });

  test("un cliente no ve las tarjetas de otro", async () => {
    await assertFails(getDocs(collection(as(BETO), "clientes", ANA, "metodosPago")));
    await assertFails(deleteDoc(doc(as(BETO), "clientes", ANA, "metodosPago", "card_visa_1111")));
  });

  test("operaciones de pago y contador de pedidos son solo del servidor", async () => {
    await assertFails(getDoc(doc(as(ANA), "clientes", ANA, "operacionesPago", "pay_1")));
    await assertFails(setDoc(doc(as(ANA), "clientes", ANA, "operacionesPago", "pay_1"), { x: 1 }));
    await assertFails(getDoc(doc(as(ANA), "contadores", "pedidos")));
    await assertFails(setDoc(doc(as(ANA), "contadores", "pedidos"), { ultimo: 1 }));
  });
});

describe("solicitudes de cambio o devolución (HU-24)", () => {
  const solicitud = (extra = {}) => ({
    numeroSolicitud: "SOL-000001-1",
    pedidoId: "ped_entregado",
    numeroPedido: "MV-000001",
    prendaCodigo: "BL-1002",
    prendaNombre: "Blusa",
    prendaFotoUrl: "",
    talla: "M",
    tipo: "CAMBIO",
    motivo: "La talla no me quedó",
    detalleOtro: null,
    fotosUrls: [],
    tuvoPruebaVirtual: false,
    estado: "EN_EVALUACION",
    creadaEnMillis: 1,
    creadaEn: serverTimestamp(),
    ...extra,
  });
  const ref = (id, uid = ANA) => doc(as(uid), "clientes", uid, "solicitudes", id);

  beforeEach(async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, "clientes", ANA, "pedidos", "ped_entregado"), { estado: "ENTREGADO" });
      await setDoc(doc(db, "clientes", ANA, "pedidos", "ped_aprobado"), { estado: "APROBADO" });
      await setDoc(doc(db, "clientes", ANA, "solicitudes", "s1"), { ...solicitud(), creadaEn: new Date() });
    });
  });

  test("un cliente crea una solicitud 'En evaluación' de un pedido entregado y la lee", async () => {
    await assertSucceeds(setDoc(ref("s2"), solicitud()));
    await assertSucceeds(getDoc(ref("s1")));
  });

  test("motivo 'Otro' exige detalle y se aceptan como máximo 3 fotos", async () => {
    await assertFails(setDoc(ref("x1"), solicitud({ motivo: "Otro" })));
    await assertFails(setDoc(ref("x2"), solicitud({ motivo: "Otro", detalleOtro: "no" })));
    await assertSucceeds(setDoc(ref("x3"), solicitud({ motivo: "Otro", detalleOtro: "Se descosió" })));
    await assertFails(setDoc(ref("x4"), solicitud({ fotosUrls: ["a", "b", "c", "d"] })));
  });

  test("la app no puede aprobar, resolver ni borrar solicitudes", async () => {
    await assertFails(setDoc(ref("x5"), solicitud({ estado: "APROBADA" })));
    await assertFails(setDoc(ref("x6"), solicitud({ resolucion: "Aprobada" })));
    await assertFails(updateDoc(ref("s1"), { estado: "APROBADA" }));
    await assertFails(deleteDoc(ref("s1")));
  });

  test("solo para pedidos entregados y propios", async () => {
    await assertFails(setDoc(ref("x7"), solicitud({ pedidoId: "ped_aprobado" })));
    await assertFails(setDoc(ref("x8"), solicitud({ pedidoId: "no_existe" })));
    await assertFails(getDoc(doc(as(BETO), "clientes", ANA, "solicitudes", "s1")));
    await assertFails(setDoc(doc(as(BETO), "clientes", ANA, "solicitudes", "x9"), solicitud()));
  });
});

describe("catálogo", () => {
  for (const coleccion of ["categorias", "prendas", "stock", "tiendas", "tarifasEnvio", "inicio"]) {
    test(`${coleccion}: lectura pública y sin escritura desde la app`, async () => {
      await assertSucceeds(getDocs(collection(guest(), coleccion)));
      await assertSucceeds(getDocs(collection(as(ANA), coleccion)));
      await assertFails(setDoc(doc(as(ANA), coleccion, "nuevo"), { x: 1 }));
      await assertFails(setDoc(doc(guest(), coleccion, "nuevo"), { x: 1 }));
    });
  }

  test("nadie cambia el precio de una prenda desde la app", async () => {
    await assertFails(updateDoc(doc(as(ANA), "prendas", "BL-1002"), { precio: 1 }));
  });
});

describe("cupones", () => {
  test("ni invitados ni clientes pueden leer cupones", async () => {
    await assertFails(getDoc(doc(guest(), "cupones", "BIENVENIDA")));
    await assertFails(getDoc(doc(as(ANA), "cupones", "BIENVENIDA")));
    await assertFails(getDocs(collection(as(ANA), "cupones")));
  });
});

describe("documentos (temporal hasta rehacer HU-01)", () => {
  test("se puede consultar si un DNI existe", async () => {
    await assertSucceeds(getDoc(doc(guest(), "documentos", "40124685")));
  });

  test("un cliente reserva un DNI para su propia cuenta", async () => {
    await assertSucceeds(setDoc(doc(as(ANA), "documentos", "11111111"), { authUid: ANA }));
  });

  test("no se puede reservar un DNI a nombre de otro ni sin sesión", async () => {
    await assertFails(setDoc(doc(as(ANA), "documentos", "22222222"), { authUid: BETO }));
    await assertFails(setDoc(doc(guest(), "documentos", "33333333"), { authUid: ANA }));
  });

  test("no se puede listar, cambiar ni liberar un DNI", async () => {
    await assertFails(getDocs(collection(as(ANA), "documentos")));
    await assertFails(updateDoc(doc(as(BETO), "documentos", "40124685"), { authUid: BETO }));
    await assertFails(deleteDoc(doc(as(BETO), "documentos", "40124685")));
  });
});

describe("colecciones no declaradas", () => {
  test("se rechaza cualquier colección que no esté en las reglas", async () => {
    await assertFails(getDoc(doc(as(ANA), "pedidos", "x")));
    await assertFails(setDoc(doc(as(ANA), "pedidos", "x"), { total: 1 }));
  });
});
