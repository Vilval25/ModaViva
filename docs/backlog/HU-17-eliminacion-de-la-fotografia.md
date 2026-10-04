# HU-17 — Eliminación de la fotografía

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E4. Probador virtual |
| **Sprint** | Sprint 2 (SEM 8 – SEM 11) |
| **Semanas** | SEM 11 |
| **Encargado** | Vila |
| **Dependencias** | HU-11, HU-13 |
| **Requisitos del curso** | 3 (Firebase) |

> Como cliente quiero eliminar mi fotografía en cualquier momento.

## Descripción funcional

Desde Probador > Mi foto el cliente elimina su fotografía. Se borran la foto y todas las imágenes generadas, se emite una constancia en pantalla y por correo, y el probador queda deshabilitado hasta registrar una nueva foto. La cuenta, pedidos y carrito se conservan.

## Pantallas

- Probador > Mi foto
- Diálogo de confirmación
- Constancia de eliminación

## Componentes UI

- Botón 'Eliminar mi foto' (estilo destructivo)
- Diálogo que explica las consecuencias
- Pantalla de constancia con fecha y hora

## Criterios de aceptación

- [ ] **CA-01** Dado que tengo una foto registrada, cuando entro a Probador > Mi foto, entonces veo la opción 'Eliminar mi foto'.
- [ ] **CA-02** Dado que toco eliminar, cuando aparece el diálogo, entonces se explica que se borrarán la foto y las imágenes generadas; si cancelo, no se elimina nada.
- [ ] **CA-03** Dado que confirmo, cuando termina la solicitud, entonces la foto deja de mostrarse de inmediato, veo una constancia con fecha y hora y la recibo también por correo y en Notificaciones.
- [ ] **CA-04** Dado que confirmé la eliminación, cuando pasan como máximo 24 h, entonces la foto y todas las imágenes generadas ya no existen en Storage ni en el historial, aunque no haya vuelto a abrir la app.
- [ ] **CA-05** Dado que eliminé mi foto, cuando intento usar el probador, entonces está deshabilitado hasta que registre una nueva.
- [ ] **CA-06** Dado que eliminé mi foto, cuando reviso mi cuenta, entonces mis datos, pedidos y carrito se mantienen.

## Tecnologías

Firebase Storage (borrado) + Cloud Firestore para el registro de la eliminación. Cloud Function que garantiza el borrado completo en el servidor y envía la constancia por correo.

**Herramientas:** Firebase Storage, Cloud Firestore, Cloud Functions
