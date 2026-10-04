# Carga y administración de datos (HT-02)

La app **no lleva datos del negocio**: al abrirse descarga el catálogo, el stock y
el perfil del cliente desde Firebase. Este repositorio tampoco los guarda: solo
tiene el código que los carga.

| Dónde | Qué hay |
| :--- | :--- |
| **Este repositorio** | Código de la app, script de carga (`seed/`), reglas de seguridad y Cloud Functions. Ningún dato del negocio. |
| **Carpeta de datos** (fuera del repo, se comparte por un canal privado del equipo) | Clientes, tiendas, tarifas, cupones, catálogo y dataset de prendas con sus fotos. |
| **Firebase** (`modaviva-dsm`) | Lo que la app consulta: Firestore, Auth y Storage. |

Modelo de las colecciones: [`modelo-datos.md`](modelo-datos.md).

---

## Carpeta de datos

```
<fuente>/
├── datos/
│   ├── clientes.json       clientes que la tienda ya tenía
│   ├── tiendas.json        las 5 tiendas
│   ├── tarifasEnvio.json   costo y plazo de despacho por distrito
│   ├── cupones.json
│   ├── categorias.json     categorías y subcategorías del catálogo
│   ├── prendas.json        catálogo: precio en soles, tallas, color, casos de prueba
│   └── medidas.json        medidas estándar por tipo de prenda
└── repositorio/            dataset de prendas, una carpeta por producto:
    └── <producto>/
        ├── info.json                 marca, descripción, detalles, ajuste, medidas
        └── principal_1094x1238/      fotos (la que no tiene sufijo es la portada)
```

### Formato de los archivos

**`clientes.json`** — `{ "clientes": [ { uid, nombres, apellidos, email, telefono } ] }`.
El `uid` es fijo (`cli-tienda-001`): será el UID en Firebase Auth y el ID del
perfil en `clientes/{uid}`. Para probar la recuperación de contraseña con un
correo real, agrega un cliente con ese correo y vuelve a cargar.

**`tiendas.json`** — `[ { id, nombre, direccion, distrito, lat, lng, horario, telefono } ]`.

**`tarifasEnvio.json`** — `[ { id, distrito, cobertura, costo, plazoDias } ]`; el `id` es
el slug del distrito.

**`cupones.json`** — `{ "cupones": [ { codigo, tipo, valor, montoMinimo, vigenteDesde,
vigenteHasta, combinable, activo } ] }`; fechas `AAAA-MM-DD` en hora de Lima.

**`categorias.json`** — `[ { slug, nombre, orden, imagenDe, subcategorias: [ { slug, nombre } ] } ]`.
`imagenDe` es el código de la prenda cuya portada se usa como imagen de la categoría.

**`prendas.json`** — `{ "prendas": [ ... ] }`, cada una con:

| Campo | Ejemplo | Notas |
| :--- | :--- | :--- |
| `codigo` | `"JN-1001"` | ID del documento en Firestore. |
| `carpeta` | `"<producto>"` | Carpeta del producto en `repositorio/`. |
| `nombre` | `"Jean skinny"` | Nombre en español que muestra la app. |
| `categoria`, `subcategoria` | `"jeans"`, `"skinny"` | Slugs de `categorias.json`. |
| `precio`, `precioPromo`, `promoHasta` | `179.9`, `null`, `null` | Soles. |
| `color` | `{ "slug": "rojo", "nombre": "Rojo", "hex": "#B3121F" }` | Uno por prenda. |
| `tallas` | `["25", "26", "27"]` | XS–XL o número de cintura. |
| `tallasDataset` | `{ "25": "25" }` o `null` | Talla → columna de la tabla de medidas de `info.json`. |
| `tablaMedidas` | `"superior"` | Si `tallasDataset` es `null`: tabla de `medidas.json`. |
| `publicada`, `fotosAprobadas` | `true`, `true` | Visibilidad en Inicio (HU-04 CA-03). |
| `diasDesdeCreacion` | `30` | Orden "novedades". |
| `agotada` | `true` (opcional) | Carga todo su stock en 0. |

Marca, descripción, detalles, ajuste, género, fotos y medidas se leen de
`info.json`. Antes de escribir nada, el script valida que cada prenda esté
completa (código, precio, tallas, color, fotos y medidas de todas sus tallas);
si algo falta, no carga.

El stock de ejemplo por tienda se genera solo y siempre da los mismos números.

---

## Cargar

Requisitos: Node 22+, y para el emulador la CLI de Firebase y Java 21+ (sirve
el JDK de Android Studio: `C:\Program Files\Android\Android Studio\jbr`).

```bash
cd seed
npm install
```

### En el emulador (sin costo, para desarrollar)

```bash
# desde la raíz del repo
firebase emulators:start --project demo-modaviva --only auth,firestore,storage
# en otra terminal
node seed/cargar.mjs --emulador --fuente "<ruta de la carpeta de datos>"
node seed/verificar.mjs --emulador --fuente "<ruta de la carpeta de datos>"
```

Las URLs de las fotos apuntan a `10.0.2.2:9199`, que es tu PC vista desde el
emulador de Android.

### En producción (`modaviva-dsm`)

Hace falta la clave de una cuenta de servicio: consola de Firebase →
Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada.
**Guárdala fuera del repositorio** y no la compartas; da acceso total al
proyecto. Al terminar, se puede revocar en Google Cloud → IAM → Cuentas de
servicio → `firebase-adminsdk-…` → Claves.

```bash
# 1. Vista previa: no escribe nada, muestra qué cargará (y qué borrará con --limpiar)
node seed/cargar.mjs --produccion --fuente "<datos>" --credenciales "<clave.json>" [--limpiar]
# 2. Carga
node seed/cargar.mjs --produccion --fuente "<datos>" --credenciales "<clave.json>" [--limpiar] --si
# 3. Verificación
node seed/verificar.mjs --produccion --fuente "<datos>" --credenciales "<clave.json>"
```

- Se puede ejecutar **cuantas veces haga falta**: los IDs son fijos y los clientes
  se actualizan, nunca se duplican. Las fotos solo se suben si cambiaron.
- Sin `--limpiar` **no borra nada**. Con `--limpiar` borra las categorías,
  prendas, stock y fotos que ya no están en los datos (útil al quitar prendas).
- Si el correo de un cliente ya pertenece a otra cuenta (alguien que se
  registró en la app), lo omite y lo informa.
- Los perfiles se actualizan con `merge`: no se pierde lo que el cliente hizo en
  la app (aceptar los T&C, editar sus datos).

---

## Administrar (back-office)

No hay panel propio: se administra desde la [consola de Firebase](https://console.firebase.google.com/project/modaviva-dsm/firestore).

| Tarea | Cómo |
| :--- | :--- |
| Publicar o despublicar una prenda | `prendas/{codigo}` → `publicada` (o `fotosAprobadas`). Actualiza también `actualizadaEn`. |
| Cambiar un precio o una promoción | `prendas/{codigo}` → `precio`, `precioPromo`, `promoHasta`; y `actualizadaEn`. |
| Cambiar stock | `stock/{codigo}_{talla}` → `porTienda.Txx` y `total`. **Actualiza también `prendas/{codigo}.stockTotal`** (hasta que HU-05 lo recalcule con una Cloud Function). |
| Activar o desactivar un cupón | `cupones/{codigo}` → `activo`. |
| Agregar una prenda | Agrégala a `prendas.json` y su carpeta al dataset, y vuelve a cargar. |

Para cambios grandes conviene editar los archivos de la carpeta de datos y
volver a cargar, así los datos y Firebase no se separan.

---

## Reglas de seguridad y Cloud Functions

- Reglas: `firestore.rules` y `storage.rules`, con pruebas en `rules-tests/`:
  ```bash
  firebase emulators:exec --project demo-modaviva --only firestore,storage "npm --prefix rules-tests test"
  ```
  Publicar: `firebase deploy --only firestore,storage`.
- Cloud Functions en `functions/` (región `us-central1`, máximo 2 instancias).
  **Solo las despliega la persona a cargo de la facturación**, después de
  revisar el PR: `firebase deploy --only functions`.

### Control de gastos

El proyecto está en plan Blaze con un **presupuesto de US$1 al mes** (alertas al
50 %, 90 % y 100 %). La función `budgetGuard` recibe el gasto del presupuesto por
el tema Pub/Sub `billing-budget` y, si llega al 100 %, **desactiva la
facturación del proyecto**: no se cobra nada más, no se pierden datos y las
funciones dejan de responder hasta volver a vincular la cuenta de facturación.
Para probarla sin cortar nada, publica en el tema un mensaje con el atributo
`dryRun=true`.
