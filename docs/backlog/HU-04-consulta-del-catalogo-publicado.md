# HU-04 — Consulta del catálogo publicado

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E2. Catálogo y disponibilidad |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 6 |
| **Encargado** | Vila |
| **Dependencias** | HT-01, HT-02 |
| **Requisitos del curso** | 2 (Proceso de negocio), 3 (Firebase), 8 (SQLite (Room)), 6 (Corrutinas y Retrofit) |

> Como cliente quiero ver en la app las prendas publicadas por la tienda para comprar con información confiable.

## Descripción funcional

Al abrir la app se muestra directamente Inicio con el catálogo, sin pedir iniciar sesión ni registrarse. Inicio muestra las prendas publicadas en Firestore, administradas desde el back-office (la app solo las lee). La app guarda una copia local para consultar sin conexión y avisa cuando los datos pueden estar desactualizados.

## Pantallas

- Inicio (barra superior con Notificaciones, buscador, Favoritos y Carrito; debajo, destacados y cuadrícula de prendas)
- Banner de modo sin conexión

## Componentes UI

- Barra superior: ícono de Notificaciones con insignia, campo de búsqueda, ícono de Favoritos e ícono de Carrito con insignia
- Tarjeta de prenda (foto, nombre, precio)
- Cuadrícula con carga perezosa (lazy grid)
- Skeleton de carga
- Banner 'Sin conexión – datos del dd/mm hh:mm'
- Pull-to-refresh

## Criterios de aceptación

- [ ] **CA-01** Dado que abro la app, con o sin sesión iniciada, cuando termina de cargar, entonces veo Inicio con el catálogo, sin que se me pida iniciar sesión ni registrarme.
- [ ] **CA-02** Dado que estoy en Inicio, cuando veo la barra superior, entonces contiene el ícono de Notificaciones, el buscador y los íconos de Favoritos y Carrito, y se mantiene visible al desplazar el catálogo.
- [ ] **CA-03** Dado que una prenda está publicada y tiene fotos aprobadas en el back-office, cuando abro Inicio, entonces la veo; si no cumple alguna de las dos condiciones, no aparece.
- [ ] **CA-04** Dado que una prenda no tiene registrados código, precio, tallas, colores o medidas, cuando se sincroniza el catálogo, entonces no se muestra (no se publican fichas incompletas).
- [ ] **CA-05** Dado que una prenda se despublica en el back-office, cuando pasan como máximo 15 min o hago pull-to-refresh, entonces desaparece del catálogo.
- [ ] **CA-06** Dado que veo el catálogo, cuando reviso una tarjeta, entonces muestra foto, nombre y precio vigente.
- [ ] **CA-07** Dado que no tengo conexión, cuando abro Inicio, entonces veo la última versión guardada y un banner con la fecha y hora de la última actualización.
- [ ] **CA-08** Dado que el precio de una prenda cambió en el servidor, cuando la consulto con conexión, entonces veo el precio nuevo y no el guardado en caché.

## Tecnologías

Cloud Firestore como fuente del catálogo + Room (SQLite) como caché local para el modo sin conexión. Sincronización con Kotlin Coroutines + Flow. Carga de imágenes con Coil.

**Herramientas:** Cloud Firestore, Room, Coil, Kotlin Coroutines + Flow
