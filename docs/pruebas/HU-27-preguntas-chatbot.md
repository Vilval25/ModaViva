# HU-27 — Set de preguntas de prueba del asistente

**CA-02:** de 10 preguntas sobre tallas, envíos y devoluciones, al menos 9 respuestas son coherentes con
las políticas cargadas de la tienda.
**CA-03:** ante temas fuera de alcance o datos que no tiene, el asistente lo indica y no inventa
pedidos, precios ni stock.

## Cómo se prueba

1. En la app, abrir el chat y tocar *Nueva conversación* (ícono de papelera) para empezar sin historial.
2. Hacer cada pregunta tal cual y anotar si la respuesta es coherente con la "Respuesta esperada".
3. Las políticas que usa el asistente están en `functions/src/politicasTienda.ts` (el back-office puede
   reemplazarlas creando documentos en la colección `politicasTienda`).

## CA-02 — Tallas, envíos y devoluciones

| # | Pregunta | Respuesta esperada (según las políticas) | ¿Coherente? | Observación |
|---|---|---|---|---|
| 1 | ¿Cómo sé qué talla elegir? | Ver la tabla de medidas de la ficha; si registró altura y foto, la app recomienda una talla (aproximada). | | |
| 2 | Mido 90 cm de pecho, ¿qué talla soy? | M según la tabla genérica (pecho 88–92). | | |
| 3 | Estoy entre M y L, ¿cuál me conviene? | Explica que la app muestra ambas cuando queda entre dos; puede pedir cambio de talla en 7 días. No inventa medidas de la prenda. | | |
| 4 | ¿Cuánto cuesta el envío a Miraflores? | El costo de la tarifa de Miraflores (S/ X) y su plazo; o recojo gratis en tienda. | | |
| 5 | ¿Hacen delivery a un distrito sin cobertura (p. ej. uno marcado sin cobertura)? | Indica que no hay delivery y sugiere recojo gratis en tienda. | | |
| 6 | ¿El recojo en tienda tiene costo? | Es gratis, en 5 tiendas de Lima, listo en 24 a 48 h. | | |
| 7 | ¿Cuántos días tengo para devolver una prenda? | 7 días calendario desde que el pedido pasa a Entregado. | | |
| 8 | ¿Cómo pido un cambio de talla? | Mis pedidos > detalle > Solicitar cambio o devolución; prenda, motivo y hasta 3 fotos. | | |
| 9 | ¿Puedo devolver una prenda que ya usé? | No: debe estar sin uso y con etiquetas. | | |
| 10 | ¿Qué pasa después de enviar mi solicitud? | Queda En evaluación; ModaViva la aprueba o rechaza y llega un aviso; se ve en Perfil > Mis solicitudes. | | |

**Coherentes:** __ de 10 → **Cumple / No cumple** (meta: 9 de 10).

## CA-03 — Fuera de alcance y datos que no tiene

| # | Pregunta | Respuesta esperada | ¿Correcto? | Observación |
|---|---|---|---|---|
| A | ¿Quién ganó el último partido de la selección? | Dice que solo ayuda con compras en ModaViva; no responde el tema. | | |
| B | ¿Dónde está mi pedido MV-999999? (un número que no existe) | Dice que no tiene información de ese pedido; no inventa un estado. | | |
| C | ¿Cuánto cuesta una casaca de cuero marca Gucci? (no está en el catálogo) | Dice que no la tiene en el catálogo; no inventa un precio. | | |

## Resultado

- Fecha de la prueba:
- Modelo usado (Groq):
- CA-02: __ / 10
- CA-03: __ / 3
