# HU-10 — Consentimiento para el uso de la fotografía

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E4. Probador virtual |
| **Sprint** | Sprint 2 (SEM 8 – SEM 11) |
| **Semanas** | SEM 8 |
| **Encargado** | Perez |
| **Dependencias** | HU-01 |
| **Requisitos del curso** | 3 (Firebase) |

> Como cliente quiero que se me pida consentimiento antes de usar mi foto.

## Descripción funcional

La pestaña Probador requiere sesión: sin ella explica qué es el probador y ofrece iniciar sesión o crear una cuenta. La primera vez que el cliente con sesión entra al Probador ve el aviso de privacidad (Ley N° 29733) y decide si da su consentimiento. Sin consentimiento puede navegar y comprar normalmente, pero el probador queda deshabilitado. Puede revocar el consentimiento desde Perfil.

## Pantallas

- Probador (sin sesión)
- Aviso de privacidad del probador
- Perfil > Privacidad

## Componentes UI

- Texto del aviso con scroll
- Casilla de consentimiento (no marcada)
- Botones 'Aceptar y continuar' y 'Ahora no'
- Interruptor de consentimiento en Perfil
- Diálogo de confirmación de revocación

## Criterios de aceptación

- [ ] **CA-01** Dado que no tengo sesión, cuando toco la pestaña Probador, entonces veo una explicación breve del probador y las opciones 'Iniciar sesión' y 'Crear cuenta'.
- [ ] **CA-02** Dado que tengo sesión y no he dado consentimiento, cuando entro al Probador, entonces veo el aviso de privacidad antes de cualquier opción para subir una foto.
- [ ] **CA-03** Dado que estoy en el aviso, cuando se abre, entonces la casilla aparece desmarcada y 'Aceptar y continuar' está deshabilitado hasta que la marque.
- [ ] **CA-04** Dado que acepto, cuando se guarda el consentimiento, entonces queda registrado mi identificador, la fecha y hora y la versión del aviso aceptada.
- [ ] **CA-05** Dado que elijo 'Ahora no', cuando sigo usando la app, entonces puedo navegar y comprar normalmente y el botón 'Probarme' indica que requiere consentimiento.
- [ ] **CA-06** Dado que revoco el consentimiento en Perfil, cuando confirmo, entonces el probador se deshabilita y se inicia la eliminación de mi foto e imágenes generadas (ver HU-17).
- [ ] **CA-07** Dado que se publica una nueva versión del aviso, cuando vuelvo a entrar al Probador, entonces se me pide aceptarla de nuevo.

## Tecnologías

Cloud Firestore para registrar el consentimiento con identificador, fecha y versión.

**Herramientas:** Cloud Firestore
