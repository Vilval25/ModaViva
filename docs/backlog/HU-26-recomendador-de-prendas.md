# HU-26 — Recomendador de prendas

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E7. Analítica y asistencia inteligente |
| **Sprint** | Sprint 3 (SEM 12 – SEM 15) |
| **Semanas** | SEM 15 |
| **Encargado** | Vila |
| **Dependencias** | HU-13, HU-21 |
| **Requisitos del curso** | 11 (Inteligencia artificial) |

> Como cliente quiero recibir recomendaciones de prendas según mis compras y pruebas para descubrir prendas que me gusten.

## Descripción funcional

En Inicio aparece la sección 'Recomendado para ti', calculada según las categorías, colores y marcas de lo que el cliente compró, probó o marcó como favorito.

## Pantallas

- Inicio (sección 'Recomendado para ti')

## Componentes UI

- Carrusel horizontal de tarjetas de prenda
- Etiqueta de motivo ('Porque probaste…')

## Criterios de aceptación

- [ ] **CA-01** Dado que tengo historial (compras, pruebas o favoritos), cuando abro Inicio, entonces veo 'Recomendado para ti' con al menos 6 prendas relacionadas con ese historial.
- [ ] **CA-02** Dado que veo una recomendación, cuando la reviso, entonces indica el motivo (por ejemplo, 'Porque probaste Casaca X').
- [ ] **CA-03** Dado que una prenda ya la compré o está agotada, cuando se generan las recomendaciones, entonces no aparece.
- [ ] **CA-04** Dado que no tengo sesión o no tengo historial, cuando abro Inicio, entonces la sección muestra novedades o más vendidos en lugar de quedar vacía.
- [ ] **CA-05** Dado que pruebo o compro una prenda nueva, cuando vuelvo a Inicio, entonces las recomendaciones se actualizan.

## Tecnologías

Modelo de recomendación basado en contenido (similitud por categoría, color y marca con el historial del cliente), calculado en Cloud Functions o en el dispositivo.

**Herramientas:** Cloud Functions, Cloud Firestore
