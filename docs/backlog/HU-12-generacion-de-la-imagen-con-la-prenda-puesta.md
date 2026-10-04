# HU-12 — Generación de la imagen con la prenda puesta

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E4. Probador virtual |
| **Sprint** | Sprint 2 (SEM 8 – SEM 11) |
| **Semanas** | SEM 8 – SEM 9 |
| **Encargado** | Rojas |
| **Dependencias** | HU-11, HU-16, HT-02 |
| **Requisitos del curso** | 6 (Corrutinas y Retrofit), 11 (Inteligencia artificial), 2 (Proceso de negocio) |

> Como cliente quiero ver una imagen mía vistiendo la prenda para decidir con seguridad si me favorece.

## Descripción funcional

Desde el detalle de una prenda habilitada, el cliente toca 'Probarme', elige talla y color, y la app solicita la generación a una Cloud Function, que llama a la API de IA generativa (la clave nunca está en la app). Se muestra el progreso y luego el resultado con las acciones disponibles.

## Pantallas

- Detalle de prenda (botón 'Probarme')
- Generando imagen (progreso)
- Resultado del probador

## Componentes UI

- Botón 'Probarme' con estado deshabilitado y motivo
- Indicador de progreso con tiempo estimado
- Imagen resultado con zoom
- Leyenda 'Simulación referencial generada por IA'
- Botones 'Agregar al carrito', 'Guardar' y 'Descartar'
- Mensaje de error con 'Reintentar'

## Criterios de aceptación

- [ ] **CA-01** Dado que no tengo sesión, foto registrada o consentimiento vigente, cuando veo una prenda, entonces 'Probarme' aparece deshabilitado con el motivo y un acceso para resolverlo.
- [ ] **CA-02** Dado que toco 'Probarme' con talla y color elegidos, cuando se genera la imagen, entonces muestra la prenda en el color elegido sobre mi foto, respetando mi pose y contextura.
- [ ] **CA-03** Dado que la generación está en curso, cuando espero, entonces veo un indicador de progreso y el resultado llega en menos de 30 s en al menos 9 de cada 10 intentos de prueba.
- [ ] **CA-04** Dado que salgo de la pantalla mientras se genera la imagen, cuando la imagen está lista, entonces recibo un aviso en Notificaciones que abre el resultado.
- [ ] **CA-05** Dado que veo el resultado, cuando lo reviso, entonces siempre aparece la leyenda 'Simulación referencial generada por IA'.
- [ ] **CA-06** Dado que veo el resultado, cuando elijo 'Agregar al carrito', 'Guardar' o 'Descartar', entonces se ejecuta esa acción; al descartar, la imagen no se guarda.
- [ ] **CA-07** Dado que el servicio de IA falla o tarda más de 60 s, cuando termina el intento, entonces veo un error con 'Reintentar' y ese intento no se descuenta de mi límite diario.

## Tecnologías

Retrofit + Kotlin Coroutines hacia una Cloud Function (HTTPS) que llama a la API externa de IA generativa (image-to-image); la API key vive solo en el servidor. WorkManager para que la generación continúe si el usuario sale de la pantalla.

**Herramientas:** Retrofit, Kotlin Coroutines, WorkManager, Cloud Functions, API de IA generativa
