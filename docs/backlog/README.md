# Backlog de ModaViva

**Fuente de verdad del backlog durante el desarrollo.** El Excel del backlog
(entregable del curso, se guarda fuera del repositorio) se actualiza a partir de
estos archivos al cerrar cada hito; entre hitos no se edita.

## Cómo se usa

- Cada historia tiene su archivo. Sus criterios son una checklist: al implementar y
  **comprobar en un dispositivo** un criterio, se marca `[x]` en el mismo commit.
- Los criterios se citan por código, p. ej. `HU-01 CA-03`, en commits y PRs.
- Al marcar criterios, actualizar la columna **Criterios** de este índice (`hechos/total`).
- Una historia está terminada cuando cumple la [Definition of Done](SUPUESTOS.md#definition-of-done).
- Las historias del Sprint 2 y 3 son borrador hasta que se revise el Excel al cerrar el Hito 1.

## Hitos

- **Hito 1** (fin de SEM 7): cuenta + catálogo navegable con carrito.
- **Hito 2** (fin de SEM 11): probador virtual con IA.
- **Hito 3** (fin de SEM 15): compra, pago, postventa, analítica, asistencia con IA y publicación.

## Épicas

- **E0. Base del proyecto** — Diseño base e infraestructura que soportan todas las historias. Incluye la carga en Firebase de los clientes y datos que la tienda ya tiene (ver hoja SUPUESTOS).
- **E1. Cuenta y acceso** — Registro de nuevos clientes (correo o Google), inicio de sesión y recuperación de contraseña. Los clientes que ya existen se identifican y se les dirige a iniciar sesión.
- **E2. Catálogo y disponibilidad** — Mostrar el catálogo publicado y el stock vigente de la tienda.
- **E3. Navegación y carrito** — Búsqueda, detalle de producto y carrito de compras.
- **E4. Probador virtual** — Captura de fotografía, generación de imagen con la prenda puesta mediante IA y recomendación de talla.
- **E5. Compra y pago** — Cálculo del pedido, entrega y pago.
- **E6. Seguimiento y postventa** — Estado del pedido, notificaciones y cambios/devoluciones.
- **E7. Analítica y asistencia inteligente** — Funcionalidad no contemplada en el backlog de negocio original; se añade para cubrir el dashboard y las funcionalidades de IA exigidas por el curso.
- **E8. Cierre y despliegue** — Publicación de la app en tienda.

## Historias

### Sprint 1 (SEM 5 – SEM 7)

| Historia | Épica | Encargado | Semanas | Criterios |
| :--- | :--- | :--- | :--- | :---: |
| [HT-01 — Diseño base con Material Design 3](HT-01-diseno-base-con-material-design-3.md) | E0 | Vila | SEM 5 | 6/9 |
| [HT-02 — Infraestructura Firebase y carga de datos existentes](HT-02-infraestructura-firebase-y-carga-de-datos-existentes.md) | E0 | Vila | SEM 5 – SEM 6 | 7/7 |
| [HU-01 — Registro de la cuenta](HU-01-registro-de-la-cuenta.md) | E1 | Perez | SEM 5 | 0/10 |
| [HU-02 — Inicio de sesión y recuperación de contraseña](HU-02-inicio-de-sesion-y-recuperacion-de-contrasena.md) | E1 | Perez | SEM 5 – SEM 6 | 0/12 |
| [HU-03 — Perfil del cliente](HU-03-perfil-del-cliente.md) | E1 | Perez | SEM 6 | 0/7 |
| [HU-04 — Consulta del catálogo publicado](HU-04-consulta-del-catalogo-publicado.md) | E2 | Vila | SEM 6 | 8/8 |
| [HU-05 — Visualización de disponibilidad de las prendas](HU-05-visualizacion-de-disponibilidad-de-las-prendas.md) | E2 | Rojas | SEM 6 | 0/5 |
| [HU-06 — Exploración y búsqueda del catálogo](HU-06-exploracion-y-busqueda-del-catalogo.md) | E3 | Rojas | SEM 6 – SEM 7 | 0/7 |
| [HU-07 — Detalle de la prenda](HU-07-detalle-de-la-prenda.md) | E3 | Rojas | SEM 7 | 0/5 |
| [HU-08 — Favoritos](HU-08-favoritos.md) | E3 | Vila | SEM 7 | 0/8 |
| [HU-09 — Carrito de compras](HU-09-carrito-de-compras.md) | E3 | Perez | SEM 7 | 0/9 |

### Sprint 2 (SEM 8 – SEM 11) — _borrador, se revisa al cerrar el Hito 1_

| Historia | Épica | Encargado | Semanas | Criterios |
| :--- | :--- | :--- | :--- | :---: |
| [HU-10 — Consentimiento para el uso de la fotografía](HU-10-consentimiento-para-el-uso-de-la-fotografia.md) | E4 | Perez | SEM 8 | 0/7 |
| [HU-11 — Registro de la fotografía del cliente](HU-11-registro-de-la-fotografia-del-cliente.md) | E4 | Vila | SEM 8 – SEM 9 | 0/6 |
| [HU-12 — Generación de la imagen con la prenda puesta](HU-12-generacion-de-la-imagen-con-la-prenda-puesta.md) | E4 | Rojas | SEM 8 – SEM 9 | 0/7 |
| [HU-13 — Historial y límite de pruebas virtuales](HU-13-historial-y-limite-de-pruebas-virtuales.md) | E4 | Perez | SEM 10 – SEM 11 | 0/6 |
| [HU-14 — Registro de altura para la recomendación de talla](HU-14-registro-de-altura-para-la-recomendacion-de-talla.md) | E4 | Vila | SEM 10 | 0/5 |
| [HU-15 — Recomendación de talla según altura y fotografía](HU-15-recomendacion-de-talla-segun-altura-y-fotografia.md) | E4 | Rojas | SEM 10 – SEM 11 | 0/6 |
| [HU-16 — Prendas no habilitadas para el probador](HU-16-prendas-no-habilitadas-para-el-probador.md) | E4 | Perez | SEM 9 | 0/5 |
| [HU-17 — Eliminación de la fotografía](HU-17-eliminacion-de-la-fotografia.md) | E4 | Vila | SEM 11 | 0/6 |
| [HU-18 — Centro de notificaciones](HU-18-centro-de-notificaciones.md) | E6 | Rojas | SEM 11 | 0/8 |

### Sprint 3 (SEM 12 – SEM 15) — _borrador, se revisa al cerrar el Hito 1_

| Historia | Épica | Encargado | Semanas | Criterios |
| :--- | :--- | :--- | :--- | :---: |
| [HU-19 — Cálculo del pedido antes de pagar](HU-19-calculo-del-pedido-antes-de-pagar.md) | E5 | Perez | SEM 12 | 0/6 |
| [HU-20 — Selección de la modalidad de entrega y Mis direcciones](HU-20-seleccion-de-la-modalidad-de-entrega-y-mis-direcciones.md) | E5 | Vila | SEM 12 – SEM 13 | 0/10 |
| [HU-21 — Pago, métodos de pago guardados y confirmación del pedido](HU-21-pago-metodos-de-pago-guardados-y-confirmacion-del-pedido.md) | E5 | Rojas | SEM 12 – SEM 13 | 0/10 |
| [HU-22 — Vigencia del pedido pendiente de pago](HU-22-vigencia-del-pedido-pendiente-de-pago.md) | E5 | Perez | SEM 13 | 0/5 |
| [HU-23 — Seguimiento del pedido](HU-23-seguimiento-del-pedido.md) | E6 | Vila | SEM 14 | 0/8 |
| [HU-24 — Solicitud de cambio o devolución](HU-24-solicitud-de-cambio-o-devolucion.md) | E6 | Rojas | SEM 14 | 0/7 |
| [HU-25 — Dashboard personal del cliente](HU-25-dashboard-personal-del-cliente.md) | E7 | Perez | SEM 14 | 0/5 |
| [HU-26 — Recomendador de prendas](HU-26-recomendador-de-prendas.md) | E7 | Vila | SEM 15 | 0/5 |
| [HU-27 — Chatbot de soporte con IA](HU-27-chatbot-de-soporte-con-ia.md) | E7 | Rojas | SEM 15 | 0/5 |
| [HU-28 — Publicación en Google Play Store](HU-28-publicacion-en-google-play-store.md) | E8 | Perez | SEM 13 – SEM 15 | 0/6 |

## Cambios pendientes de llevar al Excel

Cambios de alcance hechos aquí después de importar el Excel. Al cerrar el hito
se pasan a la hoja PLANIFICACION y se registran en SUPUESTOS → "Cambios
respecto a la versión anterior".

- **Navegación (HT-01, HU-04, HU-09, HU-23, SUPUESTOS):** la barra inferior
  pasa de seis a cinco pestañas (Inicio, Categorías, Perfil, Probador, Mis
  pedidos), porque Material 3 recomienda de 3 a 5 destinos. Carrito se abre desde un
  ícono en la barra superior de Inicio, que queda así, de izquierda a derecha:
  Notificaciones, buscador, Favoritos y Carrito.
- **Prenda única de un solo color (HU-04, HU-05, HU-07, HU-09; modelo de datos):**
  cada producto del catálogo es una prenda única con un solo color. El color se
  muestra y sirve para buscar y filtrar (HU-06), pero no se elige: el cliente
  solo elige la talla, y el stock y el carrito van por prenda y talla. Al
  revisar el Sprint 2, ajustar HU-12 ("elige talla y color") y HU-13 (historial
  con color) en el mismo sentido.

Ver también: [Supuestos, DoR, DoD y requisitos del curso](SUPUESTOS.md).
