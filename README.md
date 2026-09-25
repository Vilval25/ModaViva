# ModaViva — App Móvil Android

## Resumen y propósito

**Moda Viva Perú S.A.C.** es una empresa retail dedicada a la comercialización de ropa casual, urbana y de vestir, con presencia en 5 tiendas físicas y una base de ~45,000 clientes registrados. Su canal digital enfrenta un problema crítico: el **32% de los pedidos** terminan en cambio o devolución, y solo el **18%** de esas devoluciones se debe a fallas de fábrica o a una talla equivocada. El **82% restante** ocurre porque el cliente, al probarse la prenda en casa, percibe que el estilo, corte, caída o color no le favorece.

Esa incertidumbre se agrava porque el catálogo web exhibe las prendas sobre modelos profesionales de estudio, alejados de la contextura, estatura y tono de piel del cliente promedio. El resultado es un sobrecosto en logística inversa, inmovilización de mercadería, deterioro de la retención y ~180 consultas diarias de soporte pidiendo opiniones subjetivas sobre calce y combinación.

El objetivo de este proyecto es reducir esa tasa de devolución y elevar la conversión digital, manteniendo un diseño eficiente en equipos de gama media y redes móviles de baja velocidad.

## Solución propuesta

Aplicación móvil **Android nativa**, construida en Jetpack Compose, con cinco módulos clave:

| Módulo | Descripción |
| :--- | :--- |
| **Probador virtual con IA generativa** | Genera imágenes hiperrealistas del cliente vistiendo la prenda seleccionada, adaptadas a su contextura a partir de una fotografía capturada en la app. Incluye estimación de medidas corporales con **MediaPipe** y recomendación de talla, con soporte de modelos de difusión (**Diffusers**) para la generación. |
| **Catálogo y disponibilidad** | Réplica del catálogo comercial con stock, precio y talla en tiempo real. |
| **Sincronización offline/online** | Cache local con **Room** y sincronización con **Firebase Firestore** para operar sin conexión y revalidar datos al recuperar la red. |
| **Carrito, checkout y pasarela de pagos** | Cálculo de pedido, selección de envío (domicilio o recojo en tienda) y pago con los 4 medios vigentes, sin almacenar datos de tarjeta. |
| **Dashboard personal** | Telemetría y analítica de la actividad del cliente: compras, pruebas virtuales y ahorro en promociones. |

El tratamiento de la fotografía del cliente se rige por la **Ley N° 29733** de Protección de Datos Personales: consentimiento expreso, posibilidad de revocación y eliminación total e irreversible.

## Stack tecnológico y arquitectura

- **Arquitectura:** MVVM + Clean Architecture
- **UI:** Jetpack Compose + Material 3
- **Inyección de dependencias:** Hilt
- **Persistencia local:** Room (SQLite)
- **Networking:** Retrofit + Kotlin Coroutines
- **Backend:** Firebase (Authentication, Cloud Firestore, Storage, Cloud Messaging, Remote Config, Cloud Functions)
- **Otros:** WorkManager, Paging 3, Coil, CameraX, MediaPipe / ML Kit

**Configuración del proyecto:** AGP 9.3.0 · Gradle 9.6.0 · Kotlin 2.2.10 · minSdk 26 · targetSdk 37 · JDK 17+

## Documentación

- **[Caso de estudio](docs/CASO_DE_ESTUDIO_MODAVIVA.md)** — Contexto empresarial, definición del problema, stakeholders, procesos actuales y restricciones legales.
- **[Backlog de producto](docs/BACKLOG_MODAVIVA.md)** — 8 épicas y 22 historias de usuario con encargado asignado, cronograma, stack por historia y criterios de aceptación.

## Estructura del proyecto

```
ModaViva/
├── README.md
├── docs/                       # Documentación del proyecto
│   ├── CASO_DE_ESTUDIO_MODAVIVA.md
│   └── BACKLOG_MODAVIVA.md
├── app/                        # Módulo de aplicación Android
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml   # Catálogo de versiones
```

## Compilación

```bash
./gradlew :app:assembleDebug
```

El APK generado queda en `app/build/outputs/apk/debug/`.

## Equipo

| Encargado | Historias de usuario |
| :--- | :--- |
| **Perez** | HU-01, HU-04, HU-07, HU-10, HU-11, HU-14, HU-17, HU-20 |
| **Vila** | HU-02, HU-05, HU-08, HU-12, HU-15, HU-18, HU-21 |
| **Rojas** | HU-03, HU-06, HU-09, HU-13, HU-16, HU-19, HU-22 |
