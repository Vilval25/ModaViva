# HU-15 — Recomendación de talla según altura y fotografía

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E4. Probador virtual |
| **Sprint** | Sprint 2 (SEM 8 – SEM 11) |
| **Semanas** | SEM 10 – SEM 11 |
| **Encargado** | Rojas |
| **Dependencias** | HU-11, HU-14 |
| **Requisitos del curso** | 11 (Inteligencia artificial), 3 (Firebase) |

> Como cliente quiero saber qué talla me queda según mi altura y mi fotografía para reducir el riesgo de comprar una talla equivocada.

## Descripción funcional

Con la foto de HU-11 y la altura de HU-14, la app estima medidas corporales aproximadas mediante pose estimation y las compara con la tabla de tallas de cada prenda. La talla recomendada se muestra en la ficha de la prenda y en el resultado del probador.

## Pantallas

- Detalle de prenda (talla recomendada)
- Resultado del probador (talla recomendada)
- Hoja '¿Cómo calculamos tu talla?'

## Componentes UI

- Chip 'Tu talla: M' resaltado en el selector
- Texto de aviso referencial
- Enlace explicativo
- Mensaje de error con opción de cambiar la foto

## Criterios de aceptación

- [ ] **CA-01** Dado que tengo foto y altura registradas, cuando abro una prenda con tabla de tallas, entonces veo una talla recomendada resaltada en el selector.
- [ ] **CA-02** Dado que se genera una prueba virtual, cuando veo el resultado, entonces también se muestra la talla recomendada para esa prenda.
- [ ] **CA-03** Dado que mis medidas estimadas quedan entre dos tallas, cuando se calcula la recomendación, entonces se muestran ambas (por ejemplo, 'Entre M y L').
- [ ] **CA-04** Dado que veo una talla recomendada, cuando la leo, entonces aparece el aviso 'Recomendación aproximada, no garantiza el ajuste exacto'.
- [ ] **CA-05** Dado que la foto no permite detectar los puntos corporales necesarios, cuando se intenta calcular, entonces veo un mensaje con la opción de registrar otra foto y se muestra la tabla de tallas genérica.
- [ ] **CA-06** Dado el set de prueba del equipo (10 personas con talla real conocida), cuando se ejecuta la estimación, entonces la talla recomendada coincide con la real en al menos 7 casos.

## Tecnologías

ML Kit Pose Detection (o MediaPipe Pose) en el dispositivo para estimar medidas a partir de la foto y la altura. Cloud Firestore para la tabla de tallas por prenda (medidas en cm). Comparación en el dispositivo o en Cloud Functions.

**Herramientas:** ML Kit Pose Detection / MediaPipe Pose, Cloud Firestore
