# HU-06 — Exploración y búsqueda del catálogo

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E3. Navegación y carrito |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 6 – SEM 7 |
| **Encargado** | Rojas |
| **Dependencias** | HU-04 |
| **Requisitos del curso** | 2 (Proceso de negocio), 4 (Material Design) |

> Como cliente quiero buscar y filtrar prendas para encontrar rápido lo que busco.

## Descripción funcional

Desde la pestaña Categorías el cliente navega por categoría y subcategoría. Con el buscador de la barra superior de Inicio busca por texto libre, combina filtros (marca, precio, talla, color) y ordena los resultados por precio o novedades. Los resultados se cargan de 20 en 20.

## Pantallas

- Categorías (pestaña de la barra inferior)
- Listado de prendas de una subcategoría
- Búsqueda
- Hoja inferior de filtros
- Estado 'sin resultados'

## Componentes UI

- Buscador de la barra superior de Inicio con sugerencias de búsquedas recientes
- Lista de categorías con imagen y subcategorías desplegables
- Bottom sheet de filtros (marca, rango de precio, talla, color)
- Selector de orden
- Chips de filtros activos con 'Limpiar'
- Insignia de precio promocional
- Indicador de carga al final de la lista

## Criterios de aceptación

- [ ] **CA-01** Dado que elijo una categoría y subcategoría, cuando se cargan los resultados, entonces solo veo prendas de esa subcategoría.
- [ ] **CA-02** Dado que aplico varios filtros a la vez, cuando los confirmo, entonces todos los resultados cumplen todos los filtros y los filtros activos se muestran como chips removibles.
- [ ] **CA-03** Dado que toco el buscador de la barra superior de Inicio y escribo al menos 2 caracteres, cuando busco, entonces veo prendas cuyo nombre, marca o categoría contienen el texto, sin distinguir mayúsculas ni tildes.
- [ ] **CA-04** Dado que elijo ordenar por precio (ascendente o descendente) o por novedades, cuando se aplica, entonces el listado respeta ese orden.
- [ ] **CA-05** Dado que llego al final de la lista y hay más resultados, cuando hago scroll, entonces se cargan los siguientes 20 con un indicador de carga.
- [ ] **CA-06** Dado que no hay coincidencias, cuando termina la búsqueda, entonces veo 'No encontramos prendas para …' con la opción de limpiar filtros.
- [ ] **CA-07** Dado que una prenda tiene promoción vigente, cuando aparece en el listado, entonces muestra el precio promocional destacado y el precio original tachado.

## Tecnologías

Consultas paginadas sobre Firestore con Paging 3; campo de palabras clave normalizado para la búsqueda. UI en Jetpack Compose con Material 3.

**Herramientas:** Paging 3, Jetpack Compose, Cloud Firestore
