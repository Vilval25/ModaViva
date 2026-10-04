// Verifica la carga (HT-02 CA-01, CA-02, CA-03).
//
//   node verificar.mjs --emulador --fuente <ruta>
//   node verificar.mjs --produccion --fuente <ruta> --credenciales <ruta>
//
// Solo lee. Termina con código 1 si algo no cuadra.

import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { cert, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";

const args = process.argv.slice(2);
const iFuente = args.indexOf("--fuente");
const FUENTE = iFuente >= 0 ? args[iFuente + 1] : process.env.MODAVIVA_FUENTE;
const DATA = FUENTE && join(FUENTE, "datos");
if (!DATA || !existsSync(DATA)) {
  console.error("Indica la carpeta de datos con --fuente <ruta> o MODAVIVA_FUENTE.");
  process.exit(1);
}
const emulador = args.includes("--emulador");
const i = args.indexOf("--credenciales");
const credenciales = i >= 0 ? args[i + 1] : process.env.GOOGLE_APPLICATION_CREDENTIALS;

if (emulador) {
  process.env.FIRESTORE_EMULATOR_HOST ??= "127.0.0.1:8080";
  process.env.FIREBASE_AUTH_EMULATOR_HOST ??= "127.0.0.1:9099";
}
initializeApp({
  projectId: emulador ? "demo-modaviva" : "modaviva-dsm",
  ...(emulador ? {} : { credential: cert(credenciales) }),
});

const db = getFirestore();
const auth = getAuth();
const leer = (f) => JSON.parse(readFileSync(join(DATA, f), "utf8"));
const fallas = [];
const check = (ok, mensaje) => {
  console.log(`${ok ? "OK   " : "FALLA"} ${mensaje}`);
  if (!ok) fallas.push(mensaje);
};

const docs = async (coleccion) => (await db.collection(coleccion).get()).docs;

// CA-01: colecciones creadas y con la cantidad esperada
const prendasData = leer("prendas.json").prendas;
const esperado = {
  categorias: leer("categorias.json").length,
  prendas: prendasData.length,
  tiendas: 5,
  tarifasEnvio: leer("tarifasEnvio.json").length,
  cupones: leer("cupones.json").cupones.length,
  stock: prendasData.reduce((n, p) => n + p.tallas.length, 0),
};
const leidos = {};
for (const [coleccion, cantidad] of Object.entries(esperado)) {
  leidos[coleccion] = await docs(coleccion);
  check(leidos[coleccion].length === cantidad, `${coleccion}: ${leidos[coleccion].length} documentos (esperados ${cantidad})`);
}

// CA-01: datos consistentes entre sí
const prendas = new Map(leidos.prendas.map((d) => [d.id, d.data()]));
const tiendas = new Set(leidos.tiendas.map((d) => d.id));
const categorias = new Map(leidos.categorias.map((d) => [d.id, d.data()]));
const sumaPorPrenda = new Map();
let stockHuerfano = 0;
let tiendaDesconocida = 0;
for (const d of leidos.stock) {
  const s = d.data();
  const prenda = prendas.get(s.prendaId);
  if (!prenda || !prenda.tallas.includes(s.talla) || d.id !== `${s.prendaId}_${s.talla}`) stockHuerfano++;
  if (Object.keys(s.porTienda).some((t) => !tiendas.has(t))) tiendaDesconocida++;
  sumaPorPrenda.set(s.prendaId, (sumaPorPrenda.get(s.prendaId) ?? 0) + s.total);
}
check(stockHuerfano === 0, `stock: cada documento corresponde a una talla de su prenda`);
check(tiendaDesconocida === 0, `stock: solo usa las 5 tiendas existentes`);
const totalesMal = [...prendas].filter(([id, p]) => p.stockTotal !== (sumaPorPrenda.get(id) ?? 0)).map(([id]) => id);
check(totalesMal.length === 0, `prendas: stockTotal coincide con la suma de su stock${totalesMal.length ? " (no: " + totalesMal + ")" : ""}`);
const categoriaMal = [...prendas].filter(
  ([, p]) => !categorias.get(p.categoria)?.subcategorias.some((s) => s.slug === p.subcategoria),
);
check(categoriaMal.length === 0, `prendas: todas apuntan a una categoría y subcategoría existentes`);
check([...prendas.values()].every((p) => p.fotos?.length > 0), `prendas: todas tienen al menos una foto`);

// CA-02: cada cliente de la tienda está en Auth y en Firestore con el mismo UID
const clientesData = [...leer("clientes.json").clientes];
let sinAuth = 0;
let sinPerfil = 0;
let correoDistinto = 0;
for (const c of clientesData) {
  const usuario = await auth.getUser(c.uid).catch(() => null);
  const perfil = await db.doc(`clientes/${c.uid}`).get();
  if (!usuario) sinAuth++;
  if (!perfil.exists) sinPerfil++;
  if (usuario && perfil.exists && usuario.email !== perfil.data().email) correoDistinto++;
}
check(sinAuth === 0, `clientes: los ${clientesData.length} existen en Firebase Auth`);
check(sinPerfil === 0, `clientes: los ${clientesData.length} tienen perfil en clientes/{uid}`);
check(correoDistinto === 0, `clientes: el correo de Auth y el del perfil coinciden`);

// CA-03: sin duplicados (un correo = una cuenta)
const correosAuth = [];
let pagina;
do {
  const r = await auth.listUsers(1000, pagina);
  correosAuth.push(...r.users.map((u) => u.email?.toLowerCase()).filter(Boolean));
  pagina = r.pageToken;
} while (pagina);
check(new Set(correosAuth).size === correosAuth.length, `auth: ningún correo repetido (${correosAuth.length} cuentas)`);

console.log(fallas.length ? `\n${fallas.length} verificaciones fallaron.` : "\nTodo correcto.");
process.exit(fallas.length ? 1 : 0);
