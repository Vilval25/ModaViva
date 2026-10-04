# HT-01 — Diseño base con Material Design 3

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia técnica |
| **Épica** | E0. Base del proyecto |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 5 |
| **Encargado** | Vila |
| **Dependencias** | — |
| **Requisitos del curso** | 4 (Material Design) |

> Como equipo queremos un tema y componentes base de Material Design 3 para que todas las pantallas sean consistentes.

## Descripción funcional

Definir el tema de la app siguiendo los lineamientos de Material Design 3: esquema de color claro y oscuro (generado con Material Theme Builder), tipografía y formas. Implementar los componentes base reutilizables y la navegación principal con barra inferior de cinco pestañas: Inicio (muestra el catálogo directamente y tiene en la barra superior, en este orden, el ícono de Notificaciones, el buscador y los íconos de Favoritos y Carrito), Categorías, Perfil, Probador y Mis pedidos. Al abrir la app se entra directo a Inicio, sin pedir iniciar sesión. Las pantallas de cada historia se construyen con estos componentes.

## Pantallas

- Barra inferior: Inicio, Categorías, Perfil, Probador, Mis pedidos
- Componentes comunes de estado: carga, vacío, error y sin conexión

## Componentes UI

- Tema Material 3 (claro/oscuro)
- Barra de navegación inferior
- Barra superior de Inicio: Notificaciones (con insignia), buscador, Favoritos y Carrito (con insignia)
- Tarjeta de prenda
- Botones primario/secundario/destructivo
- Chips de filtro y selección
- Campo de texto con mensaje de error
- Snackbar y diálogo de confirmación

## Criterios de aceptación

- [ ] **CA-01** Dado el tema de la app, cuando se revisa, entonces usa el esquema de color, la tipografía y las formas de Material Design 3 generados con Material Theme Builder.
- [ ] **CA-02** Dado que el sistema cambia entre modo claro y oscuro, cuando se abre la app, entonces aplica la paleta correspondiente sin textos ilegibles.
- [ ] **CA-03** Dado que abro la app, cuando termina de cargar, entonces se muestra Inicio con el catálogo, haya o no sesión iniciada.
- [ ] **CA-04** Dado la barra inferior, cuando la veo en cualquier pantalla principal, entonces muestra en este orden Inicio, Categorías, Perfil, Probador y Mis pedidos, con la pestaña actual resaltada.
- [ ] **CA-05** Dado que estoy en Inicio, cuando veo la barra superior, entonces muestra, de izquierda a derecha, el ícono de Notificaciones, el buscador y los íconos de Favoritos y Carrito.
- [ ] **CA-06** Dado la navegación principal, cuando se recorre la app, entonces todas las pantallas listadas en este backlog son alcanzables en máximo 3 toques desde Inicio.
- [ ] **CA-07** Dado un componente base (botón, tarjeta de prenda, campo de texto, diálogo), cuando se usa en una pantalla, entonces se toma del módulo común y no se redefine.
- [ ] **CA-08** Dado cualquier pantalla que consulta datos, cuando se implementa, entonces usa los componentes comunes de carga, vacío, error y sin conexión.
- [ ] **CA-09** Los textos cumplen contraste mínimo WCAG AA y todos los elementos táctiles miden al menos 48 dp.

## Tecnologías

Material Design 3 como guía de diseño. Material Theme Builder para generar el esquema de color. Tema y componentes base implementados en Jetpack Compose + Material 3 en un módulo común reutilizable.

**Herramientas:** Material Theme Builder, Jetpack Compose + Material 3
