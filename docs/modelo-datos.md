# Modelo de datos — Cloud Firestore

Modelo del backlog v2 (HT-02). Reemplaza a `diseno-firestore.md`, que describe
el backlog anterior (vinculación con la web y unicidad por DNI) y ya no se usa.

Firestore es una base documental: no hay tablas, claves foráneas ni `JOIN`. Las
colecciones se diseñan a partir de las consultas que hace cada historia, y se
duplica a propósito el dato que se lee junto (por ejemplo, `stockTotal` en la
prenda para no leer todo el stock al pintar el catálogo).

## Resumen

| Colección | ID del documento | Quién escribe | Quién lee | Historias |
| :--- | :--- | :--- | :--- | :--- |
| `clientes/{uid}` | UID de Firebase Auth | el propio cliente | el propio cliente | HU-01, HU-02, HU-03 |
| `clientes/{uid}/favoritos/{prendaId}` | código de la prenda | el propio cliente | el propio cliente | HU-08 |
| `clientes/{uid}/carrito/{varianteId}` | `{prendaId}_{talla}` | el propio cliente | el propio cliente | HU-09 |
| `categorias/{slug}` | slug de la categoría | back-office | todos (también invitados) | HU-06 |
| `prendas/{prendaId}` | código de la prenda | back-office | todos | HU-04, HU-06, HU-07 |
| `stock/{varianteId}` | `{prendaId}_{talla}` | back-office | todos | HU-05, HU-07, HU-09 |
| `tiendas/{tiendaId}` | `T01`…`T05` | back-office | todos | HU-05, HU-20 |
| `tarifasEnvio/{distrito}` | slug del distrito | back-office | todos | HU-19, HU-20 |
| `cupones/{codigo}` | código del cupón | back-office | **solo el servidor** | HU-19 |

"Back-office" es la consola de Firebase o el script de carga (`seed/`); la app
nunca escribe en esas colecciones.

**Convenciones**

- Nombres de campos en español y `camelCase`, como el código existente.
- Precios en **soles** como número con dos decimales (`89.9`). Los cálculos de
  dinero del pedido (HU-19) se hacen en el servidor y redondean a 2 decimales.
- Fechas como `timestamp` de Firestore.
- IDs legibles y estables (`BL-1002`, `T01`, `miraflores`): así el script de
  carga puede ejecutarse varias veces sin duplicar (HT-02 CA-03).
- Slugs en minúsculas, sin tildes y con guiones: `san-isidro`, `azul-marino`.

---

## `clientes/{uid}`

Perfil del cliente. El ID es el UID de Firebase Auth, así que la cuenta y el
perfil quedan enlazados por el mismo identificador (HT-02 CA-02).

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `nombres` | string | |
| `apellidos` | string | |
| `email` | string | Copia del correo de Auth; no se edita (HU-03 CA-05). |
| `telefono` | string \| null | Solo lo tienen algunos clientes existentes. |
| `origen` | `"tienda"` \| `"app"` | `tienda`: cargado por el script; `app`: registrado en la app. |
| `consentimiento` | map \| null | `{ version: string, aceptadoEn: timestamp }` (HU-01 CA-10). `null` en clientes existentes hasta que acepten en la app. |
| `creadoEn` | timestamp | |
| `actualizadoEn` | timestamp | |

No se guardan:

- **El método de acceso** (correo o Google). Se lee de Firebase Auth
  (`providerData`), que es la fuente real (HU-03 CA-01).
- **Si el correo está verificado.** Se lee de Firebase Auth
  (`isEmailVerified` en la app, `request.auth.token.email_verified` en reglas y
  funciones). Una copia en Firestore podría quedar desactualizada.

Más adelante se agregan `alturaCm` (HU-14) y la subcolección `direcciones`
(HU-20).

### `clientes/{uid}/favoritos/{prendaId}` (HU-08)

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `agregadoEn` | timestamp | Para ordenar del más reciente al más antiguo (HU-08 CA-02). |

Solo guarda la referencia: el precio, el stock y si sigue publicada se leen de
la prenda actual (HU-08 CA-03).

### `clientes/{uid}/carrito/{varianteId}` (HU-09)

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `prendaId` | string | |
| `talla` | string | |
| `cantidad` | number | |
| `agregadoEn` | timestamp | |

El ID es la variante (prenda + talla), así que agregar otra vez la misma prenda
y talla suma la cantidad en el mismo documento en lugar de crear otra línea
(HU-09 CA-02). El precio no se guarda: se usa siempre el precio actual
(HU-09 CA-06).

---

## `categorias/{slug}` (HU-06)

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `nombre` | string | "Blusas" |
| `imagenUrl` | string | Imagen de la lista de categorías: la primera foto de una de sus prendas. |
| `orden` | number | Orden en la pestaña Categorías. |
| `subcategorias` | array | `[{ slug, nombre }]` |

## `prendas/{prendaId}` (HU-04, HU-06, HU-07)

El ID es el código de la prenda (`BL-1002`).

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `codigo` | string | Igual al ID; se muestra en la ficha (HU-07 CA-01). |
| `nombre` | string | |
| `descripcion` | string | Texto del dataset de prendas, en su idioma original. |
| `detalles` | array\<string\> | Material, cuidado, origen ("Material: 100% cotton"). Para la ficha (HU-07). |
| `ajuste` | array\<string\> | Calce y talla que usa la modelo ("Height of model is 175cm and wears a size 26"). Útil para HU-15. |
| `marca` | string | |
| `genero` | `"mujer"` \| `"hombre"` \| null | Para filtrar (HU-06) y recomendar (HU-26). |
| `categoria` | string | Slug de `categorias`. |
| `subcategoria` | string | Slug de la subcategoría. |
| `precio` | number | Precio regular en soles. |
| `precioPromo` | number \| null | Precio de promoción; `null` si no hay (HU-06 CA-07). |
| `promoHasta` | timestamp \| null | Fin de la promoción. |
| `tallas` | array\<string\> | En orden de talla y en el sistema que se usa en Perú: `XS`–`XL` para prendas superiores y abrigos, número de cintura (`"26"`) para jeans. |
| `color` | map | `{ slug: "blanco", nombre: "Blanco", hex: "#FFFFFF" }`. Cada prenda es única y tiene **un solo color**: no se elige en la ficha, solo se muestra y sirve para buscar y filtrar (HU-06). |
| `medidas` | map | Tabla de medidas en cm por talla: `{ "S": { "pecho": 88, "largo": 62 } }` (HU-07 CA-03). Claves posibles: `largo`, `pecho`, `cintura`, `cadera`, `muslo`, `entrepierna`, `tiro`, `hombro`, `manga`. |
| `fotos` | array\<string\> | URLs de descarga de Storage, la primera es la portada. |
| `publicada` | boolean | La controla el back-office (HU-04 CA-03, CA-05). |
| `fotosAprobadas` | boolean | Ídem. |
| `stockTotal` | number | Suma de todo su stock; `0` = agotada (HU-05 CA-02). |
| `palabrasClave` | array\<string\> | Para la búsqueda (HU-06 CA-03), ver abajo. |
| `creadaEn` | timestamp | Orden "novedades" (HU-06 CA-04). |
| `actualizadaEn` | timestamp | Para la sincronización de la caché (HU-04 CA-05, CA-08). |

**El catálogo de Inicio** consulta
`publicada == true && fotosAprobadas == true`, ordenado por `creadaEn` desc. La
app además descarta las fichas incompletas (sin código, precio, tallas, color
o medidas), como pide HU-04 CA-04.

**La búsqueda** usa `palabrasClave array-contains <texto>`. El script genera la
lista a partir del nombre, la marca, la categoría, la subcategoría y el color: cada
palabra en minúsculas y sin tildes, más sus prefijos de 2 o más letras
(`"blusa"` → `bl`, `blu`, `blus`, `blusa`). Así "blu" o "BLÚ" encuentran
"Blusa". Firestore no tiene búsqueda de texto libre; para algo más completo
haría falta un servicio externo.

**`stockTotal` es un dato duplicado**: el script lo calcula al cargar. Si el
stock se edita a mano en la consola, hay que actualizarlo también. HU-05 debería
agregar una Cloud Function que lo recalcule cuando cambie `stock`.

## `stock/{varianteId}` (HU-05, HU-07, HU-09)

Un documento por combinación prenda–talla, con el stock de cada tienda.

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `prendaId` | string | Para escuchar el stock de una prenda en tiempo real. |
| `talla` | string | |
| `porTienda` | map | `{ "T01": 3, "T02": 0, "T03": 5, "T04": 1, "T05": 0 }` |
| `total` | number | Suma de `porTienda`; `0` = combinación agotada (HU-05 CA-01). |
| `actualizadoEn` | timestamp | |

La ficha escucha `stock where prendaId == X` (HU-05 CA-03, CA-05) y muestra
las tiendas con stock de la talla elegida (HU-05 CA-04).

## `tiendas/{tiendaId}` (HU-05, HU-20)

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `nombre` | string | |
| `direccion` | string | |
| `distrito` | string | Slug. |
| `ubicacion` | geopoint | Para el mapa y el orden por cercanía (HU-20 CA-09). |
| `horario` | string | |
| `telefono` | string | |

## `tarifasEnvio/{distrito}` (HU-19, HU-20)

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `distrito` | string | Nombre para mostrar. |
| `cobertura` | boolean | `false` = sin despacho (HU-20 CA-06, CA-07). |
| `costo` | number | Soles. |
| `plazoDias` | number | Días hábiles estimados (HU-20 CA-10). |

## `cupones/{codigo}` (HU-19)

Solo los lee la Cloud Function que calcula el pedido: si la app pudiera leerlos,
cualquiera podría listar todos los cupones vigentes.

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `tipo` | `"porcentaje"` \| `"monto"` | |
| `valor` | number | `15` = 15 % o S/ 15, según `tipo`. |
| `montoMinimo` | number | Subtotal mínimo para aplicarlo (HU-19 CA-04). |
| `vigenteDesde` | timestamp | |
| `vigenteHasta` | timestamp | |
| `combinable` | boolean | Si se puede usar junto con otro cupón (HU-19 CA-05). |
| `activo` | boolean | Apagarlo sin borrarlo. |

---

## Índices compuestos

| Colección | Campos | Para |
| :--- | :--- | :--- |
| `prendas` | `publicada` ↑, `fotosAprobadas` ↑, `creadaEn` ↓ | Catálogo de Inicio |

Los filtros y órdenes de HU-06 agregarán más índices; se declaran en
`firestore.indexes.json` cuando se implementen.

## Colecciones heredadas

- `documentos/{dni}`: la usa el registro actual (backlog anterior) para la
  unicidad del DNI. Las reglas la permiten de forma **temporal** hasta que
  HU-01 se rehaga sin DNI; después se elimina la regla y la colección.
- `clientes_web` y `pedidos` del documento anterior no forman parte de este
  modelo. Los pedidos se diseñan con HU-19 a HU-23.
