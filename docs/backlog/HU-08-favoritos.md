# HU-08 — Favoritos

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E3. Navegación y carrito |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 7 |
| **Encargado** | Vila |
| **Dependencias** | HU-02, HU-07 |
| **Requisitos del curso** | 8 (SQLite (Room)), 3 (Firebase), 4 (Material Design) |

> Como cliente quiero guardar prendas como favoritas y verlas en una lista para encontrarlas rápido más adelante.

## Descripción funcional

El cliente marca o desmarca prendas con el ícono de corazón en la ficha de detalle o en la tarjeta del catálogo. La pantalla Favoritos se abre desde el ícono de la barra superior de Inicio y lista las prendas guardadas con su precio y disponibilidad actuales. Requiere sesión.

## Pantallas

- Favoritos (desde el ícono de la barra superior de Inicio)
- Favoritos vacío

## Componentes UI

- Ícono de corazón (relleno / vacío) en la ficha y en la tarjeta de prenda
- Cuadrícula de prendas favoritas con precio y etiqueta 'Agotado'
- Botón quitar con opción 'Deshacer'
- Estado vacío con botón 'Ir a Inicio'

## Criterios de aceptación

- [ ] **CA-01** Dado que tengo sesión, cuando toco el corazón en la ficha o en la tarjeta de una prenda, entonces queda marcada como favorita y el ícono se rellena; si lo vuelvo a tocar, se desmarca.
- [ ] **CA-02** Dado que tengo favoritos, cuando toco el ícono de Favoritos en la barra superior de Inicio, entonces veo mis prendas favoritas, de la más reciente a la más antigua, con su precio vigente.
- [ ] **CA-03** Dado que una prenda favorita se agotó, cambió de precio o se despublicó, cuando abro Favoritos, entonces se muestra con la etiqueta 'Agotado', con el precio actualizado o con el aviso 'Ya no disponible', según corresponda.
- [ ] **CA-04** Dado que toco una prenda en Favoritos, cuando se abre, entonces veo su ficha de detalle.
- [ ] **CA-05** Dado que quito una prenda de Favoritos, cuando lo hago, entonces desaparece de la lista y puedo deshacerlo durante unos segundos.
- [ ] **CA-06** Dado que marqué favoritos, cuando reinicio la app o inicio sesión en otro dispositivo, entonces veo los mismos favoritos.
- [ ] **CA-07** Dado que no tengo favoritos, cuando abro la pantalla, entonces veo un mensaje y un botón para ir a Inicio.
- [ ] **CA-08** Dado que no tengo sesión, cuando toco un corazón o el ícono de Favoritos, entonces aparece el aviso 'Inicia sesión para continuar'; si inicio sesión desde una prenda, esta queda marcada como favorita.

## Tecnologías

Room como caché local de favoritos y Cloud Firestore (subcolección de favoritos por cliente) para sincronizar entre dispositivos. UI en Jetpack Compose + Material 3.

**Herramientas:** Room, Cloud Firestore, Jetpack Compose
