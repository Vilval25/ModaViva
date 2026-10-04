# HU-16 — Prendas no habilitadas para el probador

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E4. Probador virtual |
| **Sprint** | Sprint 2 (SEM 8 – SEM 11) |
| **Semanas** | SEM 9 |
| **Encargado** | Perez |
| **Dependencias** | HU-07 |
| **Requisitos del curso** | 3 (Firebase) |

> Como cliente quiero saber cuándo una prenda no puede probarse virtualmente.

## Descripción funcional

Ciertas categorías (ropa interior, trajes de baño) no pueden usarse en el probador. La lista se administra en Remote Config. La app oculta el acceso al probador en esas prendas y el servidor rechaza cualquier solicitud para ellas, aunque la app esté desactualizada.

## Pantallas

- Detalle de prenda (sin 'Probarme' y con mensaje)
- Inicio, Categorías, búsqueda, favoritos y carrito (sin acceso al probador)

## Componentes UI

- Aviso 'Esta prenda no está disponible en el probador virtual'
- Enlace 'Ver tabla de medidas'

## Criterios de aceptación

- [ ] **CA-01** Dado que una prenda pertenece a una categoría no habilitada, cuando abro su ficha, entonces no aparece 'Probarme' y veo un mensaje breve con la política.
- [ ] **CA-02** Dado que veo ese mensaje, cuando toco 'Ver tabla de medidas', entonces se abre la tabla de medidas de la prenda.
- [ ] **CA-03** Dado que se agrega o quita una categoría en Remote Config, cuando pasan como máximo 15 min, entonces la app aplica el cambio sin publicar una nueva versión.
- [ ] **CA-04** Dado que una prenda no habilitada aparece en catálogo, búsqueda, favoritos o carrito, cuando la veo en cualquiera de esas pantallas, entonces no hay acceso al probador.
- [ ] **CA-05** Dado que una versión desactualizada de la app envía una solicitud de prueba para una prenda no habilitada, cuando llega al servidor, entonces la Cloud Function la rechaza y no se genera imagen.

## Tecnologías

Firebase Remote Config para la lista de categorías habilitadas; la misma validación se aplica en la Cloud Function del probador.

**Herramientas:** Firebase Remote Config, Cloud Functions
