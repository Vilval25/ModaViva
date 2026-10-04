# HU-11 — Registro de la fotografía del cliente

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E4. Probador virtual |
| **Sprint** | Sprint 2 (SEM 8 – SEM 11) |
| **Semanas** | SEM 8 – SEM 9 |
| **Encargado** | Vila |
| **Dependencias** | HU-10 |
| **Requisitos del curso** | 10 (Recurso móvil), 3 (Firebase), 11 (Inteligencia artificial) |

> Como cliente quiero registrar una fotografía mía para verme con las prendas antes de comprar.

## Descripción funcional

Tras dar su consentimiento, el cliente toma o elige una foto de cuerpo completo siguiendo una guía. La app valida la imagen, detecta que haya una persona de cuerpo completo, la comprime y la sube a un espacio privado. Puede ver y reemplazar su foto desde Perfil.

## Pantallas

- Guía para tomar la foto
- Cámara
- Selector de galería
- Vista previa y confirmación
- Probador > Mi foto

## Componentes UI

- Ejemplos de foto correcta e incorrecta
- Vista de cámara con silueta guía
- Botón de captura y cambio de cámara
- Vista previa con 'Usar foto' / 'Repetir'
- Barra de progreso de subida
- Mensajes de validación

## Criterios de aceptación

- [ ] **CA-01** Dado que di consentimiento, cuando entro a registrar mi foto, entonces puedo elegir cámara o galería y veo la guía (cuerpo completo, buena luz, fondo simple).
- [ ] **CA-02** Dado que la imagen no es JPG o PNG, pesa más de 10 MB o mide menos de 720 × 1280 px, cuando la elijo, entonces se rechaza indicando el motivo específico.
- [ ] **CA-03** Dado que en la foto no se detecta una persona de cuerpo completo, cuando la confirmo, entonces se rechaza con el mensaje 'Necesitamos ver tu cuerpo completo, de pies a cabeza'.
- [ ] **CA-04** Dado que la foto es válida, cuando la confirmo, entonces se comprime en el dispositivo antes de subirse y veo el progreso de la subida.
- [ ] **CA-05** Dado que ya tenía una foto, cuando registro una nueva, entonces la anterior se elimina del almacenamiento y solo queda la nueva.
- [ ] **CA-06** Dado que otro usuario o alguien sin sesión solicita mi foto, cuando se hace la petición, entonces las reglas de Storage la rechazan.

## Tecnologías

CameraX para la captura nativa y Photo Picker/MediaStore para la galería. ML Kit Pose Detection en el dispositivo para validar cuerpo completo. Compresión local y subida a Firebase Storage con reglas por usuario.

**Herramientas:** CameraX, ML Kit Pose Detection, Firebase Storage, Coil
