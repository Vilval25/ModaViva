# HU-14 — Registro de altura para la recomendación de talla

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E4. Probador virtual |
| **Sprint** | Sprint 2 (SEM 8 – SEM 11) |
| **Semanas** | SEM 10 |
| **Encargado** | Vila |
| **Dependencias** | HU-01; se integra al flujo de HU-11 / HU-12 |
| **Requisitos del curso** | 4 (Material Design), 3 (Firebase) |

> Como cliente quiero registrar mi altura para que la app pueda recomendarme una talla.

## Descripción funcional

La primera vez que el cliente usa el probador (después de registrar su foto y antes de generar la imagen), un modal le pide su altura y explica para qué se usa. Puede omitirlo y editar el dato después en Perfil.

## Pantallas

- Modal de altura
- Perfil > Mis medidas

## Componentes UI

- Campo numérico en cm con validación
- Texto explicativo del propósito
- Botones 'Guardar' y 'Omitir'

## Criterios de aceptación

- [ ] **CA-01** Dado que registré mi foto y nunca ingresé mi altura, cuando toco 'Probarme' por primera vez, entonces aparece el modal de altura antes de generar la imagen.
- [ ] **CA-02** Dado que veo el modal, cuando lo leo, entonces se explica que la altura se usa solo para recomendar una talla aproximada.
- [ ] **CA-03** Dado que ingreso un valor fuera del rango 100–230 cm o no numérico, cuando intento guardar, entonces veo el rango permitido y el dato no se guarda.
- [ ] **CA-04** Dado que elijo 'Omitir', cuando continúo, entonces la generación sigue normalmente y en lugar de talla recomendada veo la tabla de tallas genérica.
- [ ] **CA-05** Dado que ya registré mi altura, cuando la edito en Perfil > Mis medidas, entonces el nuevo valor se usa en las siguientes recomendaciones.

## Tecnologías

Jetpack Compose (modal y validación). Cloud Firestore para guardar la altura en el perfil del cliente.

**Herramientas:** Jetpack Compose, Cloud Firestore
