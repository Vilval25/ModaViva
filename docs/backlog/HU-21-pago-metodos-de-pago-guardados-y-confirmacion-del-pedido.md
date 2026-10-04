# HU-21 — Pago, métodos de pago guardados y confirmación del pedido

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E5. Compra y pago |
| **Sprint** | Sprint 3 (SEM 12 – SEM 15) |
| **Semanas** | SEM 12 – SEM 13 |
| **Encargado** | Rojas |
| **Dependencias** | HU-19, HU-20, HU-03 (menú de Perfil) |
| **Requisitos del curso** | 6 (Corrutinas y Retrofit), 3 (Firebase), 2 (Proceso de negocio) |

> Como cliente quiero pagar con el medio que prefiera, guardar mis tarjetas para próximas compras y recibir confirmación.

## Descripción funcional

El cliente elige uno de los 4 medios de pago y paga a través de la pasarela en modo prueba (sandbox). Al aprobarse, el pedido pasa a 'Aprobado' con número correlativo, se reserva el stock, se vacía el carrito y se emite el comprobante. Esta historia también construye la opción Métodos de pago de Perfil, donde el cliente guarda y elimina tarjetas de forma segura a través de la pasarela; los demás medios de pago se eligen al momento de pagar.

## Pantallas

- Pago (checkout paso 3)
- Procesando pago
- Pedido confirmado
- Pago rechazado
- Comprobante
- Perfil > Métodos de pago
- Agregar tarjeta

## Componentes UI

- Selector de medio de pago
- Formulario o SDK de tarjeta de la pasarela
- Botón 'Pagar S/ X' que se bloquea tras el primer toque
- Pantalla de éxito con número de pedido
- Pantalla de rechazo con motivo y 'Intentar con otro medio'
- Botón 'Descargar comprobante (PDF)'
- Tarjeta guardada (marca, últimos 4 dígitos y vencimiento)
- Casilla 'Guardar tarjeta para próximas compras'
- Diálogo de confirmación para eliminar tarjeta

## Criterios de aceptación

- [ ] **CA-01** Dado que estoy en Pago, cuando veo las opciones, entonces aparecen los 4 medios de pago definidos en el caso.
- [ ] **CA-02** Dado que tengo tarjetas guardadas en Métodos de pago, cuando pago con tarjeta, entonces puedo elegir una sin volver a ingresar sus datos o usar una nueva y, si quiero, guardarla.
- [ ] **CA-03** Dado que pago con una tarjeta nueva, cuando ingreso los datos, entonces se envían directamente a la pasarela y en Firestore solo se guardan la marca y los últimos 4 dígitos.
- [ ] **CA-04** Dado que agrego una tarjeta desde Perfil > Métodos de pago con datos válidos, cuando la guardo, entonces los datos se envían a la pasarela y en la app y en Firestore solo quedan la marca, los últimos 4 dígitos, el vencimiento y el token de la pasarela.
- [ ] **CA-05** Dado que la pasarela rechaza la tarjeta, cuando intento guardarla, entonces veo el motivo y la tarjeta no se guarda.
- [ ] **CA-06** Dado que elimino una tarjeta guardada, cuando confirmo, entonces deja de aparecer en Métodos de pago y al momento de pagar.
- [ ] **CA-07** Dado que el pago es aprobado, cuando termina, entonces el pedido queda 'Aprobado' con un número correlativo único, el stock queda reservado y el carrito se vacía.
- [ ] **CA-08** Dado que el pedido fue aprobado, cuando veo la confirmación, entonces puedo descargar la boleta o factura en PDF y recibo una notificación push.
- [ ] **CA-09** Dado que el pago es rechazado, cuando termina, entonces veo el motivo devuelto por la pasarela y puedo intentar con otro medio sin perder el carrito.
- [ ] **CA-10** Dado que toco 'Pagar' varias veces o se corta la conexión durante el pago, cuando se procesa, entonces existe un solo cobro y un solo pedido.

## Tecnologías

SDK/API de la pasarela en modo prueba (sandbox), incluida la tokenización de tarjetas para guardarlas. Cloud Functions que registran las tarjetas en la pasarela y confirman el cobro y crea el pedido en una transacción de Firestore (idempotente). Retrofit + Kotlin Coroutines. Firebase Cloud Messaging para la notificación.

**Herramientas:** Pasarela de pago (sandbox), Retrofit, Cloud Functions, Cloud Firestore, FCM
