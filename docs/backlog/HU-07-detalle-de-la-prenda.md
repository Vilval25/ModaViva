# HU-07 — Detalle de la prenda

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E3. Navegación y carrito |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 7 |
| **Encargado** | Rojas |
| **Dependencias** | HU-04, HU-05 |
| **Requisitos del curso** | 4 (Material Design) |

> Como cliente quiero ver toda la información de una prenda para decidir la compra.

## Descripción funcional

Al tocar una prenda se abre su ficha: galería de fotos, nombre, código, precio, descripción, color de la prenda, selector de talla, tabla de medidas, plazo de entrega, tiendas con stock, favorito y botón para agregar al carrito. El botón 'Probarme' se habilita en HU-12.

## Pantallas

- Detalle de prenda
- Galería de fotos a pantalla completa
- Hoja de tabla de medidas

## Componentes UI

- Carrusel (pager) de fotos con zoom
- Chips de talla
- Muestra del color de la prenda (no seleccionable)
- Bottom sheet de tabla de medidas
- Ícono de favorito
- Botón 'Agregar al carrito'
- Texto de plazo estimado de entrega

## Criterios de aceptación

- [ ] **CA-01** Dado que abro una prenda, cuando carga la ficha, entonces veo fotos, nombre, código, precio, descripción, color y tallas.
- [ ] **CA-02** Dado que toco una foto, cuando se abre la galería a pantalla completa, entonces puedo deslizar entre fotos y hacer zoom.
- [ ] **CA-03** Dado que toco 'Tabla de medidas', cuando se abre, entonces veo las medidas en cm por talla sin salir de la ficha.
- [ ] **CA-04** Dado que no he elegido talla, cuando veo el botón de compra, entonces está deshabilitado con el texto 'Elige tu talla'.
- [ ] **CA-05** Dado que elijo una talla disponible, cuando la selecciono, entonces veo el plazo estimado de entrega y las tiendas con stock (ver HU-05).

## Tecnologías

Jetpack Compose y Coil (pager de imágenes con zoom).

**Herramientas:** Jetpack Compose, Coil, Room
