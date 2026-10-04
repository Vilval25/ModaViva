// Carga de datos de la tienda en Firebase (HT-02 CA-01, CA-02, CA-03).
//
// Uso:
//   node cargar.mjs --emulador --fuente <ruta>
//   node cargar.mjs --produccion --fuente <ruta> --credenciales <clave.json>        (vista previa)
//   node cargar.mjs --produccion --fuente <ruta> --credenciales <clave.json> --si   (carga)
//
// Opciones:
//   --fuente <ruta>      carpeta con los datos del negocio (o MODAVIVA_FUENTE):
//                          <fuente>/datos/        tiendas, tarifas, cupones, clientes y catálogo
//                          <fuente>/repositorio/  dataset de prendas (info.json y fotos)
//                        Nada de esto está en el repositorio: ver docs/carga-de-datos.md.
//   --credenciales <ruta> clave de la cuenta de servicio (o GOOGLE_APPLICATION_CREDENTIALS),
//                        solo para producción y siempre fuera del repositorio.
//   --limpiar            además, borra del catálogo (categorias, prendas, stock y sus
//                        fotos) lo que ya no está en los datos. Antes lista qué borrará.
//
// Se puede ejecutar varias veces: todos los documentos usan IDs fijos y los
// clientes se crean o actualizan, nunca se duplican (CA-03). Sin --limpiar no
// borra nada.

import { createHash } from "node:crypto";
import { existsSync, readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { cert, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, GeoPoint, getFirestore, Timestamp } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";


// Fecha de referencia del catálogo: creadaEn = FECHA_BASE - diasDesdeCreacion.
// Es fija para que volver a cargar no cambie el orden de "novedades".
const FECHA_BASE = new Date("2026-10-01T12:00:00-05:00");
const ZONA_LIMA = "-05:00";

// Carpeta del dataset con las fotos grandes de cada prenda.
const CARPETA_FOTOS = "principal_1094x1238";

// Nombres de las medidas del dataset en el vocabulario del modelo.
const NOMBRES_MEDIDAS = {
  "length": "largo",
  "chest circumference": "pecho",
  "waist circumference": "cintura",
  "hip circumference": "cadera",
  "thigh circumference": "muslo",
  "inseam": "entrepierna",
  "rise": "tiro",
  "shoulder": "hombro",
  "sleeve": "manga",
};

// --- Argumentos ------------------------------------------------------------

const args = process.argv.slice(2);
const flag = (nombre) => args.includes(nombre);
const valor = (nombre) => {
  const i = args.indexOf(nombre);
  return i >= 0 ? args[i + 1] : undefined;
};

const emulador = flag("--emulador");
const produccion = flag("--produccion");
const limpiar = flag("--limpiar");
const confirmado = emulador || flag("--si");
if (emulador === produccion) salir("Indica el destino: --emulador o --produccion.");

const FUENTE = valor("--fuente") ?? process.env.MODAVIVA_FUENTE;
const DATA = FUENTE && join(FUENTE, "datos");
const DATASET = FUENTE && join(FUENTE, "repositorio");
if (!FUENTE || !existsSync(DATA) || !existsSync(DATASET)) {
  salir(
    "Indica la carpeta de datos con --fuente <ruta> o MODAVIVA_FUENTE. Debe contener " +
      "las subcarpetas datos/ y repositorio/ (ver docs/carga-de-datos.md).",
  );
}

const destino = emulador
  ? {
      projectId: "demo-modaviva",
      bucket: "demo-modaviva.appspot.com",
      // 10.0.2.2 es la máquina anfitriona vista desde el emulador de Android.
      hostFotos: valor("--host-fotos") ?? "http://10.0.2.2:9199",
    }
  : {
      projectId: "modaviva-dsm",
      bucket: "modaviva-dsm.firebasestorage.app",
      hostFotos: "https://firebasestorage.googleapis.com",
    };

if (emulador) {
  process.env.FIRESTORE_EMULATOR_HOST ??= "127.0.0.1:8080";
  process.env.FIREBASE_AUTH_EMULATOR_HOST ??= "127.0.0.1:9099";
  process.env.FIREBASE_STORAGE_EMULATOR_HOST ??= "127.0.0.1:9199";
}

// --- Datos -----------------------------------------------------------------

const leer = (nombre) => JSON.parse(readFileSync(join(DATA, nombre), "utf8"));

const categorias = leer("categorias.json");
const tablasMedidas = leer("medidas.json");
const tiendas = leer("tiendas.json");
const tarifas = leer("tarifasEnvio.json");
const cupones = leer("cupones.json").cupones;
const clientes = leer("clientes.json").clientes;
const prendas = leer("prendas.json").prendas.map(completarConDataset);

validar();

// --- Conexión ----------------------------------------------------------------

const credenciales = valor("--credenciales") ?? process.env.GOOGLE_APPLICATION_CREDENTIALS;
if (produccion && !credenciales) {
  salir(
    "Falta la clave de la cuenta de servicio: usa --credenciales <ruta> o " +
      "GOOGLE_APPLICATION_CREDENTIALS. Nunca la guardes dentro del repositorio.",
  );
}

initializeApp({
  projectId: destino.projectId,
  storageBucket: destino.bucket,
  ...(produccion ? { credential: cert(credenciales) } : {}),
});

const db = getFirestore();
const auth = getAuth();
const bucket = getStorage().bucket();

console.log(`Destino: ${destino.projectId}${emulador ? " (emulador)" : ""}\n`);

const sobrantes = limpiar ? await buscarSobrantes() : null;

if (!confirmado) {
  console.log(
    [
      `Se cargará en el proyecto REAL ${destino.projectId}:`,
      `  ${categorias.length} categorías, ${prendas.length} prendas y su stock, ${tiendas.length} tiendas,`,
      `  ${tarifas.length} tarifas de envío, ${cupones.length} cupones y ${clientes.length} clientes`,
      "  (cuentas en Firebase Auth + perfiles en Firestore), más las fotos en Storage.",
    ].join("\n"),
  );
  if (sobrantes) mostrarSobrantes(sobrantes);
  console.log("\nPara continuar, vuelve a ejecutar con --si.");
  process.exit(0);
}

if (sobrantes) await borrarSobrantes(sobrantes);
const fotos = await subirFotos();
await cargarCatalogo(fotos);
await cargarClientes();

console.log("\nCarga terminada.");
process.exit(0);

// --- Dataset -----------------------------------------------------------------

/**
 * Agrega a la prenda lo que viene del dataset: marca, descripción, detalles,
 * ajuste, género, fotos y medidas. Marca y textos se mantienen como en el
 * dataset; nombre, precio, tallas y color son los de ModaViva.
 */
function completarConDataset(p) {
  const carpeta = join(DATASET, p.carpeta);
  const infoPath = join(carpeta, "info.json");
  if (!existsSync(infoPath)) return { ...p, errorDataset: `no existe ${infoPath}` };

  const info = JSON.parse(readFileSync(infoPath, "utf8"));
  const secciones = info.secciones ?? {};
  const carpetaFotos = join(carpeta, CARPETA_FOTOS);
  const fotosLocales = existsSync(carpetaFotos)
    ? readdirSync(carpetaFotos).filter((f) => f.toLowerCase().endsWith(".jpg")).sort()
    : [];

  return {
    ...p,
    marca: info.marca,
    descripcion: info.descripcion,
    // El número de artículo es de la tienda de origen; no se muestra.
    detalles: (secciones["Product details"] ?? []).filter((l) => !l.startsWith("Item number")),
    ajuste: secciones["Size & fit"] ?? [],
    genero: info.categorias?.[0] === "Women" ? "mujer" : info.categorias?.[0] === "Men" ? "hombre" : null,
    fotosLocales: fotosLocales.map((f) => join(carpetaFotos, f)),
    medidas: medidasDe(p, info),
  };
}

/** Medidas en cm por talla: del dataset si las trae, si no las estándar. */
function medidasDe(p, info) {
  if (p.tallasDataset) {
    const tabla = info.guia_tallas?.Measurements?.filas;
    if (!tabla) return null;
    return Object.fromEntries(
      p.tallas.map((talla) => {
        const columna = p.tallasDataset[talla];
        const fila = {};
        for (const [nombre, valores] of Object.entries(tabla)) {
          // "A Waist circumference" -> "cintura"
          const clave = NOMBRES_MEDIDAS[nombre.replace(/^[A-Z]\s+/, "").toLowerCase()];
          if (clave && valores[columna] != null) fila[clave] = valores[columna];
        }
        return [talla, fila];
      }),
    );
  }
  const tabla = tablasMedidas[p.tablaMedidas];
  return tabla ? Object.fromEntries(p.tallas.map((t) => [t, tabla[t]])) : null;
}

// --- Validación (equivale a "no se publican fichas incompletas") -----------

function validar() {
  const errores = [];
  const subcategorias = new Map(categorias.map((c) => [c.slug, c.subcategorias.map((s) => s.slug)]));
  const codigos = new Set();

  for (const p of prendas) {
    const id = p.codigo ?? "(sin código)";
    if (p.errorDataset) {
      errores.push(`${id}: ${p.errorDataset}`);
      continue;
    }
    if (!p.codigo) errores.push(`${id}: falta el código`);
    if (codigos.has(p.codigo)) errores.push(`${id}: código repetido`);
    codigos.add(p.codigo);
    if (!p.nombre || !p.marca || !p.descripcion) errores.push(`${id}: falta nombre, marca o descripción`);
    if (!(p.precio > 0)) errores.push(`${id}: precio inválido`);
    if (p.precioPromo != null && !(p.precioPromo > 0 && p.precioPromo < p.precio)) {
      errores.push(`${id}: el precio de promoción debe ser menor al precio`);
    }
    if (!p.tallas?.length) errores.push(`${id}: sin tallas`);
    if (!p.color?.slug) errores.push(`${id}: sin color`);
    if (!p.fotosLocales.length) errores.push(`${id}: sin fotos en ${CARPETA_FOTOS}`);
    for (const talla of p.tallas ?? []) {
      if (!p.medidas?.[talla] || !Object.keys(p.medidas[talla]).length) {
        errores.push(`${id}: sin medidas para la talla ${talla}`);
      }
    }
    if (!subcategorias.get(p.categoria)?.includes(p.subcategoria)) {
      errores.push(`${id}: categoría/subcategoría inválida (${p.categoria}/${p.subcategoria})`);
    }
  }

  for (const c of categorias) {
    if (!prendas.some((p) => p.codigo === c.imagenDe)) {
      errores.push(`categoría ${c.slug}: imagenDe apunta a una prenda que no existe (${c.imagenDe})`);
    }
  }

  const uids = new Set();
  const correos = new Set();
  for (const c of clientes) {
    if (!c.uid || !c.email || !c.nombres || !c.apellidos) errores.push(`cliente ${c.uid}: faltan datos`);
    if (uids.has(c.uid)) errores.push(`cliente ${c.uid}: uid repetido`);
    if (correos.has(c.email?.toLowerCase())) errores.push(`cliente ${c.uid}: correo repetido`);
    uids.add(c.uid);
    correos.add(c.email?.toLowerCase());
  }

  if (errores.length) salir("Datos inválidos:\n  " + errores.join("\n  "));
}

// --- Limpieza ----------------------------------------------------------------

/** Documentos y fotos del catálogo que no están en los datos actuales. */
async function buscarSobrantes() {
  const codigos = new Set(prendas.map((p) => p.codigo));
  const variantes = new Set(prendas.flatMap((p) => p.tallas.map((t) => `${p.codigo}_${t}`)));
  const slugs = new Set(categorias.map((c) => c.slug));
  const fotosEsperadas = new Set(
    prendas.flatMap((p) => p.fotosLocales.map((_, i) => `prendas/${p.codigo}/${i + 1}.jpg`)),
  );

  const ids = async (coleccion) => (await db.collection(coleccion).listDocuments()).map((d) => d.id);
  const [archivosPrendas] = await bucket.getFiles({ prefix: "prendas/" });
  const [archivosCategorias] = await bucket.getFiles({ prefix: "categorias/" });

  return {
    categorias: (await ids("categorias")).filter((id) => !slugs.has(id)),
    prendas: (await ids("prendas")).filter((id) => !codigos.has(id)),
    stock: (await ids("stock")).filter((id) => !variantes.has(id)),
    // Las categorías ya no tienen foto propia: usan la de una de sus prendas.
    fotos: [...archivosPrendas, ...archivosCategorias]
      .map((f) => f.name)
      .filter((n) => !n.endsWith("/") && !fotosEsperadas.has(n)),
  };
}

function mostrarSobrantes(s) {
  const total = s.categorias.length + s.prendas.length + s.stock.length + s.fotos.length;
  console.log(`\nLimpieza: se borrarán ${total} elementos que ya no están en los datos:`);
  console.log(`  categorias (${s.categorias.length}): ${s.categorias.join(", ") || "—"}`);
  console.log(`  prendas (${s.prendas.length}): ${s.prendas.join(", ") || "—"}`);
  console.log(`  stock: ${s.stock.length} variantes`);
  console.log(`  fotos en Storage: ${s.fotos.length} archivos`);
}

async function borrarSobrantes(s) {
  mostrarSobrantes(s);
  const escritor = db.bulkWriter();
  for (const id of s.categorias) escritor.delete(db.doc(`categorias/${id}`));
  for (const id of s.prendas) escritor.delete(db.doc(`prendas/${id}`));
  for (const id of s.stock) escritor.delete(db.doc(`stock/${id}`));
  await escritor.close();
  for (const nombre of s.fotos) await bucket.file(nombre).delete({ ignoreNotFound: true });
  console.log("  Listo.\n");
}

// --- Fotos -----------------------------------------------------------------

/** Sube las fotos que cambiaron y devuelve las URL públicas por prenda. */
async function subirFotos() {
  const resultado = {};
  let subidas = 0;
  let sinCambios = 0;

  for (const p of prendas) {
    resultado[p.codigo] = [];
    for (const [i, local] of p.fotosLocales.entries()) {
      const remota = `prendas/${p.codigo}/${i + 1}.jpg`;
      const contenido = readFileSync(local);
      const md5 = createHash("md5").update(contenido).digest("base64");
      const archivo = bucket.file(remota);
      const [existe] = await archivo.exists();
      if (existe && (await archivo.getMetadata())[0].md5Hash === md5) {
        sinCambios++;
      } else {
        await archivo.save(contenido, {
          contentType: "image/jpeg",
          metadata: { cacheControl: "public, max-age=86400" },
        });
        subidas++;
      }
      // Las reglas de Storage hacen públicas estas rutas: la URL no necesita token.
      resultado[p.codigo].push(
        `${destino.hostFotos}/v0/b/${destino.bucket}/o/${encodeURIComponent(remota)}?alt=media`,
      );
    }
  }

  console.log(`Fotos: ${subidas} subidas, ${sinCambios} sin cambios`);
  return resultado;
}

// --- Catálogo --------------------------------------------------------------

async function cargarCatalogo(fotos) {
  const escritor = db.bulkWriter();
  const ahora = FieldValue.serverTimestamp();
  let variantes = 0;

  for (const c of categorias) {
    escritor.set(db.doc(`categorias/${c.slug}`), {
      nombre: c.nombre,
      imagenUrl: fotos[c.imagenDe][0],
      orden: c.orden,
      subcategorias: c.subcategorias,
    });
  }

  for (const p of prendas) {
    let stockTotal = 0;
    for (const talla of p.tallas) {
      const varianteId = `${p.codigo}_${talla}`;
      const porTienda = Object.fromEntries(
        tiendas.map((t) => [t.id, p.agotada ? 0 : stockDeEjemplo(varianteId, t.id)]),
      );
      const total = Object.values(porTienda).reduce((a, b) => a + b, 0);
      stockTotal += total;
      escritor.set(db.doc(`stock/${varianteId}`), {
        prendaId: p.codigo,
        talla,
        porTienda,
        total,
        actualizadoEn: ahora,
      });
      variantes++;
    }

    const categoria = categorias.find((c) => c.slug === p.categoria);
    const subcategoria = categoria.subcategorias.find((s) => s.slug === p.subcategoria);

    escritor.set(db.doc(`prendas/${p.codigo}`), {
      codigo: p.codigo,
      nombre: p.nombre,
      descripcion: p.descripcion,
      detalles: p.detalles,
      ajuste: p.ajuste,
      marca: p.marca,
      genero: p.genero,
      categoria: p.categoria,
      subcategoria: p.subcategoria,
      precio: p.precio,
      precioPromo: p.precioPromo ?? null,
      promoHasta: p.promoHasta ? fechaLima(p.promoHasta, true) : null,
      tallas: p.tallas,
      color: p.color,
      medidas: p.medidas,
      fotos: fotos[p.codigo],
      publicada: p.publicada,
      fotosAprobadas: p.fotosAprobadas,
      stockTotal,
      palabrasClave: palabrasClave([
        p.nombre, p.marca, categoria.nombre, subcategoria.nombre, p.color.nombre, p.codigo,
      ]),
      creadaEn: Timestamp.fromMillis(FECHA_BASE.getTime() - p.diasDesdeCreacion * 86_400_000),
      actualizadaEn: ahora,
    });
  }

  for (const t of tiendas) {
    escritor.set(db.doc(`tiendas/${t.id}`), {
      nombre: t.nombre,
      direccion: t.direccion,
      distrito: t.distrito,
      ubicacion: new GeoPoint(t.lat, t.lng),
      horario: t.horario,
      telefono: t.telefono,
    });
  }

  for (const t of tarifas) {
    escritor.set(db.doc(`tarifasEnvio/${t.id}`), {
      distrito: t.distrito,
      cobertura: t.cobertura,
      costo: t.costo,
      plazoDias: t.plazoDias,
    });
  }

  for (const c of cupones) {
    escritor.set(db.doc(`cupones/${c.codigo}`), {
      tipo: c.tipo,
      valor: c.valor,
      montoMinimo: c.montoMinimo,
      vigenteDesde: fechaLima(c.vigenteDesde, false),
      vigenteHasta: fechaLima(c.vigenteHasta, true),
      combinable: c.combinable,
      activo: c.activo,
    });
  }

  await escritor.close();
  console.log(
    `Catálogo: ${categorias.length} categorías, ${prendas.length} prendas, ${variantes} variantes de stock, ` +
      `${tiendas.length} tiendas, ${tarifas.length} tarifas, ${cupones.length} cupones`,
  );
}

/**
 * Stock de ejemplo determinista: la misma variante y tienda siempre dan el
 * mismo número, así recargar no cambia el stock. Cerca de un tercio queda en 0.
 */
function stockDeEjemplo(varianteId, tiendaId) {
  const n = createHash("sha256").update(`${varianteId}/${tiendaId}`).digest()[0] % 10;
  return n < 3 ? 0 : n - 2;
}

/** Palabras en minúsculas y sin tildes, con sus prefijos de 2 o más letras (HU-06 CA-03). */
function palabrasClave(textos) {
  const claves = new Set();
  for (const texto of textos) {
    const palabras = texto
      .normalize("NFD")
      .replace(/[̀-ͯ]/g, "")
      .toLowerCase()
      .split(/[^a-z0-9]+/)
      .filter((p) => p.length >= 2);
    for (const palabra of palabras) {
      for (let i = 2; i <= palabra.length; i++) claves.add(palabra.slice(0, i));
    }
  }
  return [...claves].sort();
}

/** "2026-12-31" a las 00:00 (inicio) o 23:59:59 (fin) en hora de Lima. */
function fechaLima(fecha, finDelDia) {
  return Timestamp.fromDate(new Date(`${fecha}T${finDelDia ? "23:59:59" : "00:00:00"}${ZONA_LIMA}`));
}

// --- Clientes --------------------------------------------------------------

async function cargarClientes() {
  let creados = 0;
  let actualizados = 0;
  const omitidos = [];

  for (const c of clientes) {
    // Un correo ya usado por otra cuenta (por ejemplo, alguien que se
    // registró en la app) no se toca: sería mezclar dos clientes.
    const duenoDelCorreo = await auth.getUserByEmail(c.email).catch(() => null);
    if (duenoDelCorreo && duenoDelCorreo.uid !== c.uid) {
      omitidos.push(`${c.email} (ya pertenece a la cuenta ${duenoDelCorreo.uid})`);
      continue;
    }

    const datosAuth = { email: c.email, displayName: `${c.nombres} ${c.apellidos}` };
    const existe = await auth.getUser(c.uid).then(() => true, () => false);
    if (existe) {
      await auth.updateUser(c.uid, datosAuth);
    } else {
      // Sin contraseña: el cliente la define con "Recuperar contraseña"
      // (HU-02 CA-05) o entra con Google si su correo es de Google.
      await auth.createUser({ uid: c.uid, ...datosAuth, emailVerified: false });
    }

    const ref = db.doc(`clientes/${c.uid}`);
    const perfil = {
      nombres: c.nombres,
      apellidos: c.apellidos,
      email: c.email,
      telefono: c.telefono ?? null,
      origen: "tienda",
      actualizadoEn: FieldValue.serverTimestamp(),
    };
    if ((await ref.get()).exists) {
      // merge: no pisa lo que el cliente ya hizo en la app (p. ej. aceptar
      // los T&C o editar sus datos).
      await ref.set(perfil, { merge: true });
      actualizados++;
    } else {
      await ref.set({ ...perfil, consentimiento: null, creadoEn: FieldValue.serverTimestamp() });
      creados++;
    }
  }

  console.log(`Clientes: ${creados} creados, ${actualizados} ya existían (actualizados)`);
  for (const o of omitidos) console.log(`  Omitido: ${o}`);
}

function salir(mensaje) {
  console.error(mensaje);
  process.exit(1);
}
