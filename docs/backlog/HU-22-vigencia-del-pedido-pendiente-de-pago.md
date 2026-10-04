# HU-22 — Vigencia del pedido pendiente de pago

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E5. Compra y pago |
| **Sprint** | Sprint 3 (SEM 12 – SEM 15) |
| **Semanas** | SEM 13 |
| **Encargado** | Perez |
| **Dependencias** | HU-21 |
| **Requisitos del curso** | 10 (Recurso móvil), 7 (WorkManager), 3 (Firebase) |

> Como cliente quiero saber cuánto tiempo tengo para completar el pago para no perder la reserva de mis prendas y asegurar mi compra.

## Descripción funcional

Si el pago queda pendiente, el pedido se mantiene en estado 'Pendiente' durante 30 min con el stock reservado. La app muestra una cuenta regresiva y permite reintentar con otro medio. Al vencer, el servidor anula el pedido y libera el stock.

## Pantallas

- Pedido pendiente de pago
- Detalle de pedido

## Componentes UI

- Cuenta regresiva mm:ss
- Botón 'Pagar con otro medio'
- Notificación push de recordatorio
- Banner 'Pedido anulado'

## Criterios de aceptación

- [ ] **CA-01** Dado que mi pago queda pendiente, cuando se crea el pedido, entonces queda en estado 'Pendiente' con el stock reservado por 30 min.
- [ ] **CA-02** Dado que tengo un pedido pendiente, cuando lo abro, entonces veo el tiempo restante en cuenta regresiva y puedo reintentar con otro medio.
- [ ] **CA-03** Dado que faltan 10 min para el vencimiento, cuando llega ese momento, entonces recibo una notificación push, incluso con la app cerrada.
- [ ] **CA-04** Dado que pasan 30 min sin pago, cuando vence el plazo, entonces el servidor cambia el pedido a 'Anulado' y libera el stock, aunque mi dispositivo esté apagado.
- [ ] **CA-05** Dado que mi pedido fue anulado, cuando vuelvo a la app, entonces las prendas que aún tienen stock están de nuevo en mi carrito para reintentar.

## Tecnologías

Cloud Function programada para el vencimiento y la liberación de stock. Firebase Cloud Messaging para la notificación. WorkManager como recordatorio local de respaldo.

**Herramientas:** Cloud Functions programadas, Firebase Cloud Messaging, WorkManager
