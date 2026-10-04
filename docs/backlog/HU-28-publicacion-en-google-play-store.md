# HU-28 — Publicación en Google Play Store

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia técnica |
| **Épica** | E8. Cierre y despliegue |
| **Sprint** | Sprint 3 (SEM 12 – SEM 15) |
| **Semanas** | SEM 13 – SEM 15 |
| **Encargado** | Perez |
| **Dependencias** | Versión estable del Hito 2 para iniciar la prueba cerrada |
| **Requisitos del curso** | 12 (Despliegue en Google Play Store) |

> Como equipo del proyecto queremos publicar la app para que los clientes puedan descargarla.

## Descripción funcional

Preparar el App Bundle firmado, la ficha de la tienda y la política de privacidad; publicar primero en prueba cerrada y después en producción. Se inicia en la SEM 13 porque Google Play puede exigir un periodo de prueba cerrada antes de habilitar producción.

## Pantallas

- Ficha de Google Play (ícono, capturas, descripción)
- Perfil > Acerca de (versión y enlace a la política de privacidad)

## Componentes UI

- Ícono adaptable
- Capturas de las pantallas principales
- Gráfico de funciones (1024 × 500)

## Criterios de aceptación

- [ ] **CA-01** Dado que la cuenta de desarrollador está creada, cuando se sube el .aab firmado con Play App Signing, entonces Play Console lo acepta sin errores.
- [ ] **CA-02** Dado que la ficha está completa (descripción, capturas, ícono, clasificación de contenido y sección de seguridad de datos), cuando se envía a revisión, entonces no quedan campos obligatorios pendientes.
- [ ] **CA-03** Dado que la app usa fotos del cliente, cuando se revisa la ficha, entonces existe una URL pública de política de privacidad conforme a la Ley N° 29733 y la sección de seguridad de datos declara el uso de fotos.
- [ ] **CA-04** Dado que la app está en prueba cerrada, cuando los testers la instalan, entonces pueden completar el flujo registro → catálogo → probador → compra sin errores bloqueantes.
- [ ] **CA-05** Dado que la cuenta de desarrollador es personal y nueva, cuando se solicita acceso a producción, entonces ya se cumplió el requisito de prueba cerrada vigente de Google Play.
- [ ] **CA-06** Dado que la revisión es aprobada, cuando se publica en producción, entonces la app se puede descargar desde Google Play.

## Tecnologías

Android App Bundle (.aab), Google Play Console, Play App Signing.

**Herramientas:** Google Play Console, Play App Signing
