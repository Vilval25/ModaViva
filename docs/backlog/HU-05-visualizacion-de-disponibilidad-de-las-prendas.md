# HU-05 — Visualización de disponibilidad de las prendas

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E2. Catálogo y disponibilidad |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 6 |
| **Encargado** | Rojas |
| **Dependencias** | HU-04 |
| **Requisitos del curso** | 3 (Firebase), 7 (WorkManager), 8 (SQLite (Room)) |

> Como cliente quiero ver únicamente prendas y tallas realmente disponibles para no comprar algo agotado.

## Descripción funcional

Cada prenda es única y tiene un solo color; cada combinación prenda–talla tiene stock por tienda en Firestore. La app escucha los cambios en tiempo real mientras el cliente ve una prenda y refresca periódicamente la caché local. Muestra qué tallas están agotadas y en qué tiendas hay stock para recojo.

## Pantallas

- Inicio y Categorías (etiqueta 'Agotado')
- Detalle de prenda (selector de talla y disponibilidad por tienda)

## Componentes UI

- Chips de talla con estado deshabilitado
- Etiqueta 'Agotado'
- Lista de tiendas con indicador de stock
- Snackbar de aviso de cambio de stock

## Criterios de aceptación

- [ ] **CA-01** Dado que una talla tiene stock 0 en todas las tiendas, cuando la veo, entonces aparece deshabilitada con la etiqueta 'Agotado'.
- [ ] **CA-02** Dado que todas las tallas de una prenda están agotadas, cuando navego el catálogo, entonces su tarjeta muestra 'Agotado' y no permite agregarla al carrito.
- [ ] **CA-03** Dado que el stock cambia en el back-office, cuando pasan como máximo 15 min, entonces la app refleja el nuevo stock; si estoy viendo esa prenda, el cambio se ve de inmediato.
- [ ] **CA-04** Dado que elijo una talla, cuando consulto la disponibilidad, entonces veo cuáles de las 5 tiendas tienen stock para recojo.
- [ ] **CA-05** Dado que estoy viendo una prenda, cuando la talla seleccionada se agota, entonces la opción se deshabilita y aparece un aviso sin cerrar la pantalla.

## Tecnologías

Firestore con listeners en tiempo real para el stock. WorkManager para refrescar periódicamente la caché local (Room) de stock.

**Herramientas:** Firestore, WorkManager, Room
