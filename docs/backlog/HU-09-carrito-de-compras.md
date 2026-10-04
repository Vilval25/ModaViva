# HU-09 — Carrito de compras

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E3. Navegación y carrito |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 7 |
| **Encargado** | Perez |
| **Dependencias** | HU-05, HU-07 |
| **Requisitos del curso** | 8 (SQLite (Room)), 7 (WorkManager), 2 (Proceso de negocio) |

> Como cliente quiero agregar prendas a un carrito y modificarlo antes de pagar.

## Descripción funcional

El cliente agrega prendas indicando talla y cantidad; puede editar cantidades o eliminar ítems y ve el subtotal al instante. El carrito funciona sin cuenta: se guarda en el dispositivo y, al iniciar sesión, sus ítems se suman al carrito de la cuenta, que se recupera en cualquier dispositivo. Para pagar se requiere sesión.

## Pantallas

- Carrito (desde el ícono de la barra superior de Inicio)
- Carrito vacío

## Componentes UI

- Ítem de carrito (foto, talla, color de la prenda, precio)
- Selector de cantidad (+/–)
- Eliminar con opción 'Deshacer'
- Resumen de subtotal
- Insignia con cantidad en el ícono de Carrito de la barra superior de Inicio
- Aviso de cambio de precio o stock por ítem

## Criterios de aceptación

- [ ] **CA-01** Dado que elegí talla y cantidad, cuando toco 'Agregar al carrito', entonces el ítem aparece en el carrito y la insignia se actualiza.
- [ ] **CA-02** Dado que la misma prenda con la misma talla ya está en el carrito, cuando la agrego otra vez, entonces se suma la cantidad en lugar de crear otra línea.
- [ ] **CA-03** Dado que cambio la cantidad o elimino un ítem, cuando confirmo, entonces el subtotal se recalcula al instante y puedo deshacer la eliminación durante unos segundos.
- [ ] **CA-04** Dado que intento una cantidad mayor al stock disponible, cuando toco '+', entonces el selector se detiene en el máximo y veo 'Solo quedan N unidades'.
- [ ] **CA-05** Dado que cierro la app o pierdo la conexión, cuando vuelvo, entonces el carrito se mantiene; y al iniciar sesión en otro dispositivo veo el mismo carrito.
- [ ] **CA-06** Dado que un ítem se agotó o cambió de precio desde que lo agregué, cuando abro el carrito, entonces el ítem muestra un aviso, el subtotal usa el precio actual y los ítems agotados no se suman.
- [ ] **CA-07** Dado que no tengo sesión, cuando agrego prendas, entonces se guardan en el carrito del dispositivo; al iniciar sesión, se suman al carrito de mi cuenta sin duplicar líneas.
- [ ] **CA-08** Dado que no tengo sesión, cuando toco 'Continuar' para pagar, entonces se me pide iniciar sesión o crear una cuenta y después continúo con el pedido sin perder el carrito.
- [ ] **CA-09** Dado que el carrito está vacío, cuando lo abro, entonces veo un mensaje y un botón para ir a Inicio.

## Tecnologías

Room (SQLite) para la persistencia local del carrito, sincronizado con Firestore mediante WorkManager cuando hay conexión.

**Herramientas:** Room, WorkManager, Cloud Firestore
