# HT-02 — Infraestructura Firebase y carga de datos existentes

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia técnica |
| **Épica** | E0. Base del proyecto |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 5 – SEM 6 |
| **Encargado** | Vila |
| **Dependencias** | — |
| **Requisitos del curso** | 3 (Firebase) |

> Como equipo queremos un backend en Firebase con los clientes y datos que la tienda ya tiene para desarrollar y demostrar la app.

## Descripción funcional

Crear el proyecto Firebase y cargar, con un script reproducible, lo que la tienda ya tiene: sus clientes registrados (en Firebase Authentication y con su perfil en Firestore), prendas, stock por tienda, tiendas, cupones y tarifas de envío. Los clientes nuevos se registran desde la app (HU-01). Se configuran las reglas de seguridad y las Cloud Functions base. La administración (publicar prendas, cambiar stock o estados de pedido) se hace desde la consola de Firebase o desde el script (back-office).

## Pantallas

— (sin interfaz en la app)

## Componentes UI

—

## Criterios de aceptación

- [ ] **CA-01** Dado un proyecto Firebase vacío, cuando se ejecuta el script de carga, entonces se crean las colecciones clientes, prendas, stock, tiendas (5), cupones y tarifas de envío con datos consistentes entre sí.
- [ ] **CA-02** Dado el archivo de clientes existentes, cuando se ejecuta el script, entonces cada cliente queda creado en Firebase Authentication con su correo y con su perfil en Firestore, enlazados por el mismo identificador.
- [ ] **CA-03** Dado que el script ya se ejecutó, cuando se ejecuta de nuevo, entonces no se duplican clientes ni registros.
- [ ] **CA-04** Dado un usuario sin sesión o un cliente distinto al dueño, cuando intenta leer o escribir datos de otro cliente, entonces las Security Rules rechazan la operación (verificado en el emulador).
- [ ] **CA-05** Dado que la app llama a una Cloud Function de prueba, cuando se ejecuta, entonces responde correctamente (prueba de humo del backend).
- [ ] **CA-06** Dado el APK generado, cuando se inspecciona, entonces no contiene ninguna clave de servicios externos (IA, LLM, pasarela de pago); estas viven solo en el servidor.
- [ ] **CA-07** La forma de cargar y administrar los clientes y datos de la tienda está documentada en el repositorio.

## Tecnologías

Firebase (Authentication, Cloud Firestore, Storage, Cloud Functions, Remote Config). Script de carga con Firebase Admin SDK (crea usuarios en Authentication y documentos en Firestore). Firebase Emulator Suite para probar reglas y funciones en local. Nota: desplegar Cloud Functions requiere el plan Blaze.

**Herramientas:** Firebase CLI, Firebase Emulator Suite, Firebase Admin SDK, Cloud Functions
