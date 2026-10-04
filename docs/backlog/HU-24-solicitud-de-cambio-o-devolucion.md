# HU-24 — Solicitud de cambio o devolución

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E6. Seguimiento y postventa |
| **Sprint** | Sprint 3 (SEM 12 – SEM 15) |
| **Semanas** | SEM 14 |
| **Encargado** | Rojas |
| **Dependencias** | HU-23, HU-13 |
| **Requisitos del curso** | 10 (Recurso móvil), 3 (Firebase), 2 (Proceso de negocio) |

> Como cliente quiero solicitar un cambio o devolución desde la app.

## Descripción funcional

Desde un pedido entregado (hasta 7 días después), el cliente registra la solicitud indicando prenda, tipo, motivo y fotos. La app solo registra la solicitud y muestra su estado; la evaluación y resolución se hacen en el back-office.

## Pantallas

- Solicitud de cambio o devolución
- Confirmación de solicitud
- Mis solicitudes / detalle de solicitud

## Componentes UI

- Selector de prenda del pedido
- Selector Cambio / Devolución
- Lista de motivos predefinidos
- Campo de texto para 'Otro'
- Botón para adjuntar fotos (cámara o galería) con miniaturas
- Chip de estado de la solicitud

## Criterios de aceptación

- [ ] **CA-01** Dado que mi pedido pasó a 'Entregado' hace 7 días o menos, cuando lo abro, entonces puedo iniciar una solicitud; pasados los 7 días la opción no aparece.
- [ ] **CA-02** Dado que inicio la solicitud, cuando la completo, entonces debo elegir prenda, tipo (cambio o devolución) y un motivo de la lista para poder enviarla.
- [ ] **CA-03** Dado que elijo el motivo 'Otro', cuando intento enviar sin escribir el detalle, entonces no puedo continuar.
- [ ] **CA-04** Dado que ya adjunté 3 fotos, cuando intento agregar otra, entonces se me indica que el máximo es 3.
- [ ] **CA-05** Dado que envío la solicitud, cuando se registra, entonces recibo un número de solicitud y su estado es 'En evaluación'.
- [ ] **CA-06** Dado que existe una prueba virtual previa de esa prenda, cuando se registra la solicitud, entonces queda marcado en el registro que hubo prueba virtual.
- [ ] **CA-07** Dado que la solicitud fue resuelta en el back-office, cuando la consulto, entonces veo su estado y la resolución; la app no permite aprobar ni rechazar solicitudes.

## Tecnologías

CameraX / Photo Picker para adjuntar fotos de la prenda. Firebase Storage para las fotos y Cloud Firestore para el registro de la solicitud.

**Herramientas:** CameraX, Firebase Storage, Cloud Firestore
