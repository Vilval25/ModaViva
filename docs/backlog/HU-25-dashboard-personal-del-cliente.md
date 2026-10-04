# HU-25 — Dashboard personal del cliente

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E7. Analítica y asistencia inteligente |
| **Sprint** | Sprint 3 (SEM 12 – SEM 15) |
| **Semanas** | SEM 14 |
| **Encargado** | Perez |
| **Dependencias** | HU-21, HU-13 |
| **Requisitos del curso** | 9 (Dashboards), 8 (SQLite (Room)) |

> Como cliente quiero ver un panel con mi actividad para entender mis hábitos de compra y de pruebas virtuales.

## Descripción funcional

La sección 'Mi actividad' en Perfil muestra gráficos de compras por mes, prendas probadas frente a compradas y el total ahorrado en promociones, filtrables por rango de fechas.

## Pantallas

- Perfil > Mi actividad

## Componentes UI

- Selector de rango de fechas
- Gráfico de barras de compras por mes
- Gráfico comparativo probadas vs. compradas
- Tarjeta 'Total ahorrado'
- Estado vacío

## Criterios de aceptación

- [ ] **CA-01** Dado que tengo pedidos aprobados, cuando abro 'Mi actividad', entonces veo un gráfico de barras con el monto comprado por mes; los pedidos anulados no se cuentan.
- [ ] **CA-02** Dado que usé el probador, cuando veo el panel, entonces se muestra cuántas prendas probé y cuántas de ellas compré.
- [ ] **CA-03** Dado que compré con descuentos o promociones, cuando veo el panel, entonces el total ahorrado es la suma de (precio original − precio pagado) de mis pedidos aprobados en el rango.
- [ ] **CA-04** Dado que cambio el rango de fechas, cuando lo aplico, entonces todos los gráficos y totales se recalculan para ese rango.
- [ ] **CA-05** Dado que no tengo actividad en el rango elegido, cuando abro el panel, entonces veo un mensaje en lugar de gráficos vacíos.

## Tecnologías

Librería de gráficos para Android (Vico o MPAndroidChart) alimentada con datos locales (Room) y de Firestore.

**Herramientas:** Vico o MPAndroidChart, Room, Cloud Firestore
