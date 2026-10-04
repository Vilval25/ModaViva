# HU-19 — Cálculo del pedido antes de pagar

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E5. Compra y pago |
| **Sprint** | Sprint 3 (SEM 12 – SEM 15) |
| **Semanas** | SEM 12 |
| **Encargado** | Perez |
| **Dependencias** | HU-09, HT-02 |
| **Requisitos del curso** | 6 (Corrutinas y Retrofit), 3 (Firebase), 2 (Proceso de negocio) |

> Como cliente quiero ver el detalle completo de lo que voy a pagar.

## Descripción funcional

Desde el carrito, 'Continuar' lleva al resumen del pedido. El servidor revalida stock y precios, aplica el cupón ingresado y calcula el envío según el distrito (o S/ 0 si es recojo). Se muestra el desglose completo antes de pasar a entrega y pago.

## Pantallas

- Resumen del pedido (checkout paso 1)
- Hoja para ingresar cupón

## Componentes UI

- Lista resumida de ítems
- Campo de cupón con botón 'Aplicar' y mensaje de resultado
- Desglose: subtotal, descuento, envío y total
- Aviso de cambios en ítems
- Botón 'Continuar'

## Criterios de aceptación

- [ ] **CA-01** Dado que continúo desde el carrito, cuando se abre el resumen, entonces el servidor revalida stock y precio de cada ítem antes de mostrar el total.
- [ ] **CA-02** Dado que se calcula el pedido, cuando veo el resumen, entonces aparecen subtotal, descuento, envío y total, y el total es igual a subtotal − descuento + envío.
- [ ] **CA-03** Dado que elegí despacho a un distrito, cuando se calcula el envío, entonces el costo corresponde a la tarifa de ese distrito; si elegí recojo, el envío es S/ 0.
- [ ] **CA-04** Dado que ingreso un cupón vencido, inexistente o cuyo monto mínimo no alcanzo, cuando lo aplico, entonces veo el motivo específico y no se aplica descuento.
- [ ] **CA-05** Dado que ya tengo un cupón aplicado, cuando intento aplicar otro, entonces se rechaza, salvo que ambos estén marcados como combinables por campaña.
- [ ] **CA-06** Dado que un ítem se agotó o cambió de precio durante el checkout, cuando se revalida, entonces se me informa qué cambió, el ítem se retira o actualiza y el total se recalcula.

## Tecnologías

Cloud Function (callable) que calcula el pedido con los datos de Firestore (stock, cupones, tarifas de envío), invocada desde la app con Kotlin Coroutines.

**Herramientas:** Cloud Functions, Kotlin Coroutines, Cloud Firestore
