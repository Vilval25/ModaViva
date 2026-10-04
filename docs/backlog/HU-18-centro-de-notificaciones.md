# HU-18 — Centro de notificaciones

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E6. Seguimiento y postventa |
| **Sprint** | Sprint 2 (SEM 8 – SEM 11) |
| **Semanas** | SEM 11 |
| **Encargado** | Rojas |
| **Dependencias** | HU-02; recibe avisos de HU-12 y HU-17 (y luego de HU-21 a HU-24) |
| **Requisitos del curso** | 10 (Recurso móvil), 3 (Firebase) |

> Como cliente quiero ver en un solo lugar todos los avisos de la app para no perderme nada importante sobre mis pruebas, pedidos y cuenta.

## Descripción funcional

La pantalla Notificaciones se abre desde el ícono de la barra superior de Inicio y reúne todos los avisos que la app envía al cliente: imagen del probador lista (HU-12), constancia de eliminación de foto (HU-17) y, en el sprint 3, pago, vencimiento, cambios de estado de pedidos y solicitudes (HU-21 a HU-24). Cada aviso se guarda en Firestore y, si corresponde, también llega como notificación push. El ícono muestra cuántos avisos no se han leído.

## Pantallas

- Notificaciones (desde el ícono de la barra superior de Inicio)
- Notificaciones vacío
- Explicación previa al permiso de notificaciones del sistema

## Componentes UI

- Ícono de Notificaciones con insignia de no leídos
- Lista de avisos con ícono por tipo, título, texto, fecha y hora
- Indicador de aviso no leído
- Acción 'Marcar todo como leído'
- Estado vacío
- Diálogo previo al permiso del sistema (Android 13+)

## Criterios de aceptación

- [ ] **CA-01** Dado que tengo avisos, cuando toco el ícono de Notificaciones en la barra superior de Inicio, entonces veo la lista del más reciente al más antiguo, con título, texto, fecha y hora.
- [ ] **CA-02** Dado que tengo avisos sin leer, cuando veo Inicio, entonces el ícono muestra una insignia con la cantidad; al abrir un aviso se marca como leído y la insignia se actualiza.
- [ ] **CA-03** Dado que tengo avisos sin leer, cuando toco 'Marcar todo como leído', entonces todos quedan como leídos y la insignia desaparece.
- [ ] **CA-04** Dado que toco un aviso, cuando se abre, entonces voy a la pantalla relacionada (resultado del probador, constancia de eliminación, pedido o solicitud).
- [ ] **CA-05** Dado que se genera un aviso con la app cerrada, cuando llega, entonces recibo una notificación push y el aviso también queda en la bandeja.
- [ ] **CA-06** Dado que uso Android 13 o superior y no he dado permiso, cuando la app lo solicita, entonces primero explica para qué se usan las notificaciones; si lo rechazo, los avisos igual quedan en la bandeja.
- [ ] **CA-07** Dado que no tengo avisos, cuando abro la pantalla, entonces veo un mensaje indicándolo.
- [ ] **CA-08** Dado que no tengo sesión, cuando toco el ícono de Notificaciones, entonces aparece el aviso 'Inicia sesión para continuar'.

## Tecnologías

Cloud Firestore (colección de avisos por cliente con estado leído / no leído y listener para la insignia). Cloud Functions que crean el aviso y envían el push con Firebase Cloud Messaging. Permiso POST_NOTIFICATIONS en Android 13+.

**Herramientas:** Cloud Firestore, Firebase Cloud Messaging, Cloud Functions
