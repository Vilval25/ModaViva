# Supuestos, definiciones y requisitos del curso


## Supuestos del proyecto

- **Sin web, con clientes existentes:** No existe una aplicación web y no se desarrolla. La tienda sí tiene clientes registrados previamente: el script de carga (HT-02) los crea en Firebase Authentication (con su correo) y con su perfil en Cloud Firestore, junto con prendas, stock por tienda, tiendas, cupones y tarifas de envío. Los clientes nuevos se registran desde la app con correo o con Google (HU-01). Si alguien intenta registrarse con un correo que ya tiene cuenta, se le informa y se le dirige a iniciar sesión o recuperar su contraseña.
- **Navegación y acceso como invitado:** Al abrir la app se entra directo a Inicio, que muestra el catálogo; no se pide iniciar sesión ni registrarse. La barra inferior tiene cinco pestañas: Inicio, Categorías, Perfil, Probador y Mis pedidos. Inicio tiene en su barra superior, en este orden, el ícono de Notificaciones, el buscador y los íconos de Favoritos y Carrito. Sin sesión se puede explorar el catálogo, buscar, ver el detalle de las prendas y armar el carrito (guardado en el dispositivo). Se pide iniciar sesión o crear una cuenta solo para usar el probador, ver o marcar favoritos, ver notificaciones, ver Mis pedidos, pagar y ver los datos de Perfil; al terminar, el usuario vuelve a la acción que intentaba.
- **Back-office:** Toda administración de la tienda (publicar o despublicar prendas, actualizar stock, cambiar estados de pedidos, configurar categorías del probador) se hace desde la consola de Firebase, Remote Config o el script de carga de datos (HT-02).
- **Datos de prueba:** El script de carga (HT-02) crea entre 30 y 50 clientes existentes de ejemplo (no los ~45,000 del caso) y un catálogo de ejemplo con prendas, stock en las 5 tiendas, cupones y tarifas de envío, suficiente para demostrar todos los flujos. Los clientes existentes no tienen una contraseña conocida: la definen con 'Recuperar contraseña' o ingresan con Google si su correo es de Google.
- **Lógica en el servidor:** Las claves de servicios externos (IA generativa, LLM, pasarela), la reserva y liberación de stock, los vencimientos de pedidos, el borrado garantizado de fotos y las validaciones que no deben depender de la app se ejecutan en Cloud Functions. Desplegar Cloud Functions requiere el plan Blaze de Firebase (pago por uso con capa gratuita).
- **Pagos:** La pasarela de pago se usa en modo prueba (sandbox); no se realizan cobros reales.
- **Medidas propuestas:** Algunos valores de los criterios (límites de la foto en HU-11, umbral de 30 s en 9 de 10 intentos en HU-12, sets de prueba de HU-15 y HU-27) son propuestas y pueden ajustarse, siempre que el criterio siga siendo medible.

## Formato de los criterios de aceptación

- **Dado / Cuando / Entonces:** Cada criterio describe un comportamiento observable que se puede probar y que pasa o falla: Dado (contexto inicial), Cuando (acción o evento), Entonces (resultado esperado). Lo que la historia construye se describe en 'Descripción funcional', 'Pantallas' y 'Componentes UI', no en los criterios.

## Estados de los criterios

- **Pendiente:** Aún no se ha implementado.
- **En progreso:** En desarrollo.
- **Listo:** Comprobado en un dispositivo y cumple exactamente lo que dice el criterio.
- **Estado de la HU:** Se calcula automáticamente: 'Listo' si todos sus criterios están Listo, 'Pendiente' si todos están Pendiente y 'En progreso' en cualquier otro caso.

## Definition of Ready

_Definición de Listo (una historia puede empezar a programarse cuando…)_

- Sus pantallas y componentes están identificados en las columnas 'Pantallas' y 'Componentes UI'.
- Sus criterios de aceptación son verificables (formato Dado / Cuando / Entonces).
- Sus dependencias están terminadas o se pueden simular.
- Los datos de prueba que necesita existen en Firebase (HT-02).


## Definition of Done

_Definición de Terminado (una historia está terminada cuando…)_

- Todos sus criterios están en 'Listo', comprobados en un dispositivo físico.
- La interfaz sigue los lineamientos de Material Design 3 y usa los componentes del diseño base (HT-01).
- Tiene estados de carga, vacío, error y sin conexión cuando aplica.
- Las reglas de seguridad de Firestore/Storage cubren sus datos.
- No hay claves ni secretos en el código de la app.
- Otro integrante revisó el código antes de integrarlo.


## Requisitos del curso

| N° | Requisito | Historias que lo cubren |
| :---: | :--- | :--- |
| 1 | Autenticación | HU-01, HU-02 |
| 2 | Proceso de negocio | HU-04, HU-06, HU-09, HU-12, HU-19, HU-21, HU-24 |
| 3 | Firebase | HT-02, HU-03, HU-04, HU-05, HU-08, HU-10, HU-11, HU-13, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-21, HU-22, HU-23, HU-24 |
| 4 | Material Design | HT-01, HU-01, HU-03, HU-06, HU-07, HU-08, HU-14 |
| 5 | MVVM/Clean Code | HU-01, HU-02, HU-03 |
| 6 | Corrutinas y Retrofit | HU-04, HU-12, HU-19, HU-21, HU-27 |
| 7 | WorkManager | HU-05, HU-09, HU-22 |
| 8 | SQLite (Room) | HU-04, HU-05, HU-08, HU-09, HU-13, HU-25 |
| 9 | Dashboards | HU-25 |
| 10 | Recurso móvil | HU-11, HU-18, HU-20, HU-22, HU-24 |
| 11 | Inteligencia artificial | HU-11, HU-12, HU-15, HU-26, HU-27 |
| 12 | Despliegue en Google Play Store | HU-28 |

_Los cambios respecto al backlog anterior están en la hoja SUPUESTOS del Excel._
