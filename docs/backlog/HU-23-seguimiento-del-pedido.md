# HU-23 — Seguimiento del pedido

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E6. Seguimiento y postventa |
| **Sprint** | Sprint 3 (SEM 12 – SEM 15) |
| **Semanas** | SEM 14 |
| **Encargado** | Vila |
| **Dependencias** | HU-21, HU-18 |
| **Requisitos del curso** | 3 (Firebase) |

> Como cliente quiero conocer el estado de mi pedido y ser avisado de cada cambio.

## Descripción funcional

La pestaña Mis pedidos de la barra inferior (la última, después de Probador) lista todos los pedidos del cliente; el detalle muestra una línea de tiempo de estados que se actualiza en tiempo real. Cada cambio de estado (hecho desde el back-office) genera una notificación push. Cada aviso queda también en Notificaciones.

## Pantallas

- Mis pedidos (pestaña de la barra inferior)
- Mis pedidos (sin sesión / sin pedidos)
- Detalle de pedido con línea de tiempo

## Componentes UI

- Lista de pedidos con chip de estado
- Línea de tiempo vertical con fecha y hora
- Tarjeta de transporte con número de seguimiento copiable
- Banner 'Listo para recoger'
- Botón 'Solicitar cambio o devolución'

## Criterios de aceptación

- [ ] **CA-01** Dado que no tengo sesión, cuando toco la pestaña Mis pedidos, entonces veo las opciones 'Iniciar sesión' y 'Crear cuenta' en lugar de la lista.
- [ ] **CA-02** Dado que tengo sesión y aún no he hecho pedidos, cuando abro Mis pedidos, entonces veo un mensaje y un botón para ir a Inicio.
- [ ] **CA-03** Dado que tengo pedidos, cuando abro la pestaña Mis pedidos, entonces veo cada uno con número, fecha, total y estado (Pendiente, Aprobado, En preparación, Despachado, Entregado o Anulado).
- [ ] **CA-04** Dado que abro un pedido, cuando veo el detalle, entonces cada cambio de estado aparece en la línea de tiempo con fecha y hora.
- [ ] **CA-05** Dado que el estado cambia en el back-office, cuando pasan como máximo 10 min, entonces la app lo refleja y recibo una notificación push, que también queda en Notificaciones.
- [ ] **CA-06** Dado que el pedido pasa a 'Despachado', cuando veo el detalle, entonces se muestran la empresa de transporte y el número de seguimiento.
- [ ] **CA-07** Dado que elegí recojo en tienda, cuando el pedido está listo, entonces recibo un aviso con la tienda y su horario.
- [ ] **CA-08** Dado que el pedido está 'Entregado', cuando veo el detalle, entonces aparece 'Solicitar cambio o devolución'; en otros estados no aparece.

## Tecnologías

Firebase Cloud Messaging para las notificaciones push + listeners de Firestore para el estado en tiempo real.

**Herramientas:** Firebase Cloud Messaging, Cloud Firestore
