# HU-12 CA-03 — Tiempos de generación del probador virtual

**Criterio:** el resultado llega en menos de 30 s en al menos 9 de cada 10 intentos.

## Cómo se mide

- En la app, el resultado de cada prueba nueva muestra **"Generada en X s"**: es el tiempo desde que se
  toca *Probarme* hasta que aparece la imagen (incluye la llamada a la IA y el guardado en Storage).
- En el servidor, cada generación queda registrada con su duración (`duracionMs`) en los logs de la
  función `probarPrenda` (mensaje *"Prueba virtual generada"*):

  ```
  firebase functions:log --only probarPrenda
  ```

- Condiciones: usar prendas **no probadas antes** (las del historial se recuperan al instante y no
  cuentan), quedarse en la pantalla hasta ver el resultado y anotar el tiempo mostrado.

## Resultados

| # | Fecha y hora | Prenda (código) | Red (Wi-Fi / datos) | Tiempo (s) | ¿< 30 s? |
|---|---|---|---|---|---|
| 1 | | | | | |
| 2 | | | | | |
| 3 | | | | | |
| 4 | | | | | |
| 5 | | | | | |
| 6 | | | | | |
| 7 | | | | | |
| 8 | | | | | |
| 9 | | | | | |
| 10 | | | | | |

**Intentos bajo 30 s:** __ de 10 → **Cumple / No cumple** (meta: 9 de 10).

## Observaciones

- Si un intento supera los 60 s se corta, aparece *Reintentar* y no se descuenta del límite diario (CA-07).
