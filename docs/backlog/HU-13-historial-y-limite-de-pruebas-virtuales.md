# HU-13 — Historial y límite de pruebas virtuales

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E4. Probador virtual |
| **Sprint** | Sprint 2 (SEM 8 – SEM 11) |
| **Semanas** | SEM 10 – SEM 11 |
| **Encargado** | Perez |
| **Dependencias** | HU-12 |
| **Requisitos del curso** | 8 (SQLite (Room)), 3 (Firebase) |

> Como cliente quiero revisar mis pruebas anteriores y saber cuántas me quedan para comparar prendas antes de decidir.

## Descripción funcional

Las pruebas guardadas forman un historial de las últimas 20, que permite comparar dos resultados lado a lado. Existe un límite diario de generaciones visible para el cliente. Cada generación registra un evento de uso para reportes, sin incluir la foto.

## Pantallas

- Probador (pestaña principal: mi foto e historial de pruebas)
- Comparar (2 imágenes lado a lado)
- Diálogo de límite alcanzado

## Componentes UI

- Cuadrícula de pruebas con prenda, talla, color y fecha
- Selección múltiple (máx. 2) y botón 'Comparar'
- Contador 'Te quedan N pruebas hoy'
- Diálogo de límite diario

## Criterios de aceptación

- [ ] **CA-01** Dado que guardé pruebas, cuando abro el historial, entonces veo las últimas 20, de la más reciente a la más antigua, con prenda, talla, color y fecha.
- [ ] **CA-02** Dado que ya tengo 20 pruebas guardadas, cuando guardo una nueva, entonces se elimina automáticamente la más antigua.
- [ ] **CA-03** Dado que selecciono 2 pruebas, cuando toco 'Comparar', entonces las veo lado a lado.
- [ ] **CA-04** Dado que tengo pruebas disponibles hoy, cuando veo 'Probarme', entonces se muestra cuántas me quedan; al llegar al límite diario configurado, el botón se deshabilita e indica cuándo se renueva.
- [ ] **CA-05** Dado que termina una generación, cuando se registra el uso, entonces se guarda un evento con cliente, prenda, fecha y resultado (éxito o error), sin la foto ni la imagen generada.
- [ ] **CA-06** Dado que estoy sin conexión, cuando abro el historial, entonces veo las pruebas guardadas en el dispositivo.

## Tecnologías

Room para el historial local de pruebas. Cloud Firestore para el contador diario y los eventos de uso. Remote Config para el valor del límite diario.

**Herramientas:** Room, Cloud Firestore, Firebase Remote Config
