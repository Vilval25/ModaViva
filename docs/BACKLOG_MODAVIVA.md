# Backlog de Producto: ModaViva Mobile App

Este documento consolida las épicas, historias de usuario, criterios de aceptación y especificaciones técnicas planificadas para el desarrollo de la aplicación móvil de **ModaViva**.

---

## E1. Cuenta y acceso
> **Descripción:** Establecer autenticación y vinculación con la cuenta web existente.
> **Sprint base:** 1

### HU-01 - Registro y vinculación de la cuenta
**Historia de Usuario:**  
_Como cliente quiero crear mi cuenta o vincularla con la que ya tengo en la web para comprar desde el celular sin registrarme dos veces._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Perez |
| **Plazo / Cronograma** | Sem 5 |
| **Tecnologías** | Firebase Authentication (email/contraseña) + Cloud Firestore para vincular al cliente existente. Capa de datos con arquitectura MVVM (ViewModel + Repository) e inyección de dependencias con Hilt. |
| **Herramientas** | Firebase Auth SDK, Cloud Firestore, Jetpack Compose + Material 3, Hilt |
| **Requerimiento Académico** | 1 (Autenticación), 5 (MVVM/Clean Code), 4 (Material Design) |

#### Criterios de Aceptación
- [ ] Formulario con datos personales y contraseña
- [ ] Vinculación automática con cuenta web existente (~45,000 clientes)
- [ ] Historial de pedidos previos visible tras vincular
- [ ] Unicidad de correo y documento
- [ ] Validación de contraseña (8+ caract., mayúscula, minúscula, número)
- [ ] Verificación de correo (24h)
- [ ] Aceptación de T&C y Política de Privacidad con registro de versión

---

### HU-02 - Inicio de sesión y recuperación de contraseña
**Historia de Usuario:**  
_Como cliente registrado quiero iniciar sesión y recuperar mi contraseña para acceder desde cualquier equipo._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Vila |
| **Plazo / Cronograma** | SEM 5 |
| **Tecnologías** | Firebase Authentication (signIn / sendPasswordResetEmail). Jetpack DataStore para persistir la sesión de forma segura. |
| **Herramientas** | Firebase Auth, Jetpack DataStore |
| **Requerimiento Académico** | 1 (Autenticación), 5 (MVVM/Clean Code) |

#### Criterios de Aceptación
- [ ] Acceso con las mismas credenciales de la web
- [ ] Bloqueo 15 min tras 5 intentos fallidos + aviso por correo
- [ ] Restablecimiento de contraseña por enlace de un solo uso (60 min)
- [ ] Cambio de contraseña efectivo en app y web
- [ ] Sesión persistente 30 días salvo cierre explícito
- [ ] Sin datos accesibles tras cerrar sesión

---

## E2. Catálogo y disponibilidad
> **Descripción:** Mostrar el catálogo real y el stock vigente de la tienda.
> **Sprint base:** 1

### HU-03 - Consulta del catálogo publicado
**Historia de Usuario:**  
_Como cliente quiero ver en la app las mismas prendas de la tienda en línea para comprar con información confiable._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Rojas |
| **Plazo / Cronograma** | SEM 6 |
| **Tecnologías** | Cloud Firestore como réplica del catálogo web + Room (SQLite) como caché local para el modo sin conexión. Sincronización con Kotlin Coroutines + Flow. Carga de imágenes con Coil. |
| **Herramientas** | Cloud Firestore, Room, Coil, Kotlin Coroutines + Flow |
| **Requerimiento Académico** | 2 (Proceso de negocio: exploración de catálogo), 3 (Firebase), 8 (SQLite), 6 (Corrutinas) |

#### Criterios de Aceptación
- [ ] Catálogo administrado desde la web (solo lectura en la app)
- [ ] Solo prendas con fotos aprobadas
- [ ] Despublicación reflejada en máx. 15 min
- [ ] Ficha con código, precio, tallas, colores, medidas
- [ ] Precio actualizado antes de confirmar pedido
- [ ] Modo offline con última versión cacheada y aviso de desactualización

---

### HU-04 - Visualización de disponibilidad de las prendas
**Historia de Usuario:**  
_Como cliente quiero ver únicamente prendas y tallas realmente disponibles para no comprar algo agotado._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Perez |
| **Plazo / Cronograma** | SEM 6 |
| **Tecnologías** | Firestore con listeners en tiempo real para el stock; WorkManager para revalidar periódicamente contra el sistema comercial vía Retrofit. |
| **Herramientas** | Firestore, WorkManager, Retrofit |
| **Requerimiento Académico** | Recurso Móvil (GPS) 3 (Firebase), 7 (WorkManager), 6 (Retrofit/Corrutinas) |

#### Criterios de Aceptación
- [ ] Stock por prenda/talla/color/almacén (máx. 15 min de desfase)
- [ ] Combinaciones sin stock deshabilitadas ('Agotado')
- [ ] Disponibilidad de recojo en las 5 tiendas
- [ ] Aviso si la prenda se agota mientras se visualiza

---

## E3. Navegación y carrito
> **Descripción:** Búsqueda, detalle de producto y carrito de compras.
> **Sprint base:** 1

### HU-05 - Exploración y búsqueda del catálogo
**Historia de Usuario:**  
_Como cliente quiero buscar y filtrar prendas para encontrar rápido lo que busco._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Vila |
| **Plazo / Cronograma** | SEM 7 |
| **Tecnologías** | Consultas paginadas sobre Firestore (Paging 3), UI en Jetpack Compose con Material 3. |
| **Herramientas** | Paging 3, Jetpack Compose |
| **Requerimiento Académico** | 2 (Proceso de negocio), 4 (Material Design) |

#### Criterios de Aceptación
- [ ] Organización por categoría/subcategoría
- [ ] Filtros combinables (marca, precio, talla, color)
- [ ] Búsqueda por texto libre
- [ ] Orden por precio o novedades
- [ ] Carga paginada de 20 en 20
- [ ] Mensaje claro sin coincidencias
- [ ] Precio promocional destacado

---

### HU-06 - Detalle de la prenda
**Historia de Usuario:**  
_Como cliente quiero ver toda la información de una prenda para decidir la compra._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Rojas |
| **Plazo / Cronograma** | SEM 8 |
| **Tecnologías** | Jetpack Compose, Coil (pager de imágenes). |
| **Herramientas** | Jetpack Compose, Coil |
| **Requerimiento Académico** | 4 (Material Design) |

#### Criterios de Aceptación
- [ ] Ficha completa con fotos a pantalla completa
- [ ] Tabla de medidas accesible
- [ ] Tallas/colores agotados deshabilitados
- [ ] Plazo de entrega y tiendas con stock
- [ ] Botón 'Agregar al carrito' condicionado a talla/color
- [ ] Marcar como favorita

---

### HU-07 - Carrito de compras
**Historia de Usuario:**  
_Como cliente quiero agregar prendas a un carrito y modificarlo antes de pagar._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Perez |
| **Plazo / Cronograma** | SEM 9 |
| **Tecnologías** | Room (SQLite) para persistencia local del carrito, sincronizado con Firestore mediante WorkManager cuando hay conexión. |
| **Herramientas** | Room, WorkManager |
| **Requerimiento Académico** | 8 (SQLite), 7 (WorkManager), 2 (Proceso de negocio) |

#### Criterios de Aceptación
- [ ] Agregar indicando talla/color/cantidad
- [ ] Editar o eliminar ítems
- [ ] Subtotal en tiempo real
- [ ] Carrito persistente y compartido con la web
- [ ] Tope según stock disponible
- [ ] Aviso si un ítem se agotó o cambió de precio

---

## E4. Probador virtual
> **Descripción:** Captura de fotografía y generación de imagen con la prenda puesta mediante IA.
> **Sprint base:** 2

### HU-08 - Consentimiento para el uso de la fotografía
**Historia de Usuario:**  
_Como cliente quiero que se me pida consentimiento antes de usar mi foto._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Vila |
| **Plazo / Cronograma** | SEM 5 - SEM 8 |
| **Tecnologías** | Cloud Firestore para registrar el consentimiento con fecha y versión. |
| **Herramientas** | Cloud Firestore |
| **Requerimiento Académico** | 3 (Firebase) |

#### Criterios de Aceptación
- [ ] Aviso de privacidad (Ley N° 29733) antes de habilitar carga de foto
- [ ] Consentimiento expreso, casilla no marcada por defecto
- [ ] Registro de identificador, fecha y versión aceptada
- [ ] Compra posible sin consentimiento (probador deshabilitado)
- [ ] Revocación desde el perfil

---

### HU-09 - Registro de la fotografía del cliente
**Historia de Usuario:**  
_Como cliente quiero registrar una fotografía mía para verme con las prendas antes de comprar._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Rojas |
| **Plazo / Cronograma** | SEM 5 - SEM 8 |
| **Tecnologías** | CameraX para la captura nativa y MediaStore/Intent para la galería; compresión local; subida a Firebase Storage. |
| **Herramientas** | CameraX, Firebase Storage, Coil |
| **Requerimiento Académico** | 10 (Recurso móvil: cámara), 3 (Firebase) |

#### Criterios de Aceptación
- [ ] Foto desde cámara o galería
- [ ] Guía de cómo tomarla (cuerpo completo, buena luz)
- [ ] Validación de formato/tamaño/resolución
- [ ] Detección de persona de cuerpo completo
- [ ] Compresión local antes de enviar
- [ ] Reemplazo con eliminación de la anterior
- [ ] Visible solo para el cliente

---

### HU-10 - Generación de la imagen con la prenda puesta
**Historia de Usuario:**  
_Como cliente quiero ver una imagen mía vistiendo la prenda para decidir con seguridad si me favorece._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Perez |
| **Plazo / Cronograma** | SEM 5 - SEM 8 |
| **Tecnologías** | Retrofit + Kotlin Coroutines para consumir la API externa de IA generativa (proveedor de image-to-image); WorkManager para procesar en segundo plano; Room para el historial local de pruebas. |
| **Herramientas** | Retrofit, Kotlin Coroutines, WorkManager, Room, API de IA generativa (ej. servicio de generación de imágenes vía Cloud Functions) |
| **Requerimiento Académico** | 6 (Corrutinas y Retrofit), 11 (IA: procesamiento de imágenes / deep learning generativo), 8 (SQLite), 2 (Proceso de negocio: prueba virtual) |

#### Criterios de Aceptación
- [ ] Botón habilitado solo con foto y consentimiento vigentes
- [ ] Imagen ajustada a la contextura, color y talla
- [ ] Generación en <30s con indicador de progreso
- [ ] Aviso de simulación referencial por IA
- [ ] Agregar al carrito / guardar / descartar el resultado
- [ ] Historial de las últimas 20 pruebas comparables
- [ ] Reintento sin consumir cuota si falla el servicio
- [ ] Aviso de límite diario de pruebas
- [ ] Evento de uso emitido para reportes (sin incluir la foto)

---

### HU-11 - Recomendación de talla según altura y fotografía
**Historia de Usuario:**  
_Como cliente quiero saber qué talla me queda según mi altura y mi fotografía, para reducir el riesgo dec comprar una talla equivocada._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Perez |
| **Plazo / Cronograma** | SEM 5 - SEM 8 |
| **Tecnologías** | MediaPipe Pose (o ML Kit Pose Detection) para estimar medidas corporales a partir de la foto de HU-09,
        Cloud Firestore para la tabla de tallas por prenda (medidas en cm); cálculo de comparación vía Cloud Functions. |
| **Herramientas** | MediaPipe Pose / ML Kit Pose Detection, Cloud Firestore, Cloud Functions |
| **Requerimiento Académico** | 11 (IA: procesamiento de imágenes), 3 (Firebase) |

#### Criterios de Aceptación
- [ ] Solicitud de altura la primera vez que usa el probador (modal posterior a HU-09, antes de HU-10)
- [ ] Mensaje explicando el propósito del dato: recomendar talla aproximada
- [ ] Validación de rango razonable de altura (100–230 cm)
- [ ] Dato editable después desde el perfil
- [ ] Estimación de medidas corporales por pose estimation sobre la foto ya registrada en HU-09
- [ ] Comparación de medidas estimadas contra tabla de tallas por prenda (medidas en cm por talla)
- [ ] Talla recomendada mostrada junto al resultado del probador y en la ficha de la prenda
- [ ] Sin altura registrada: se muestra la tabla de tallas genérica, sin bloquear el flujo
- [ ] Aviso de que es una aproximación referencial, no garantía exacta de talla
- [ ] Reintento o mensaje de error si la foto no permite detectar puntos corporales claros

---

### HU-12 - Prendas no habilitadas para el probador
**Historia de Usuario:**  
_Como cliente quiero saber cuándo una prenda no puede probarse virtualmente._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Vila |
| **Plazo / Cronograma** | SEM 5 - SEM 8 |
| **Tecnologías** | Firebase Remote Config para la lista de categorías habilitadas, validado también en el backend. |
| **Herramientas** | Firebase Remote Config |
| **Requerimiento Académico** | 3 (Firebase) |

#### Criterios de Aceptación
- [ ] Opción oculta en categorías no habilitadas (ropa interior, trajes de baño)
- [ ] Mensaje breve explicando la política
- [ ] Alternativa: acceso directo a tabla de medidas
- [ ] Configuración de categorías administrada desde el portal web
- [ ] Cambio reflejado en máx. 15 min sin nueva publicación
- [ ] Restricción aplicada en catálogo, búsqueda, favoritos y carrito
- [ ] Rechazo en servidor aunque la app esté desactualizada

---

### HU-13 - Eliminación de la fotografía
**Historia de Usuario:**  
_Como cliente quiero eliminar mi fotografía en cualquier momento._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Rojas |
| **Plazo / Cronograma** | SEM 5 - SEM 8 |
| **Tecnologías** | Firebase Storage (borrado) + Cloud Firestore para el registro de la eliminación. |
| **Herramientas** | Firebase Storage, Cloud Firestore |
| **Requerimiento Académico** | 3 (Firebase) |

#### Criterios de Aceptación
- [ ] Opción disponible desde el perfil
- [ ] Confirmación explícita antes de eliminar
- [ ] Eliminación definitiva en máx. 24h (foto + imágenes generadas)
- [ ] Constancia en pantalla y por correo
- [ ] Probador deshabilitado hasta nueva foto
- [ ] Cuenta, pedidos y carrito se conservan

---

## E5. Compra y pago
> **Descripción:** Cálculo del pedido, entrega y pago.
> **Sprint base:** 3

### HU-14 - Cálculo del pedido antes de pagar
**Historia de Usuario:**  
_Como cliente quiero ver el detalle completo de lo que voy a pagar._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Perez |
| **Plazo / Cronograma** | SEM 9 - SEM 10 |
| **Tecnologías** | Retrofit + Coroutines para consumir los servicios del sistema comercial (stock, cupones, costos de envío). |
| **Herramientas** | Retrofit, Kotlin Coroutines |
| **Requerimiento Académico** | 6 (Corrutinas y Retrofit), 2 (Proceso de negocio: compra) |

#### Criterios de Aceptación
- [ ] Revalidación de stock al confirmar
- [ ] Desglose: subtotal, descuento, envío, total
- [ ] Costo de envío según distrito
- [ ] Validación de cupón (vigencia, monto mínimo)
- [ ] Máx. 1 cupón salvo excepción de campaña
- [ ] Recalculo si una prenda quedó sin stock

---

### HU-15 - Selección de la modalidad de entrega
**Historia de Usuario:**  
_Como cliente quiero elegir despacho a domicilio o recojo en tienda._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Vila |
| **Plazo / Cronograma** | SEM 9 - SEM 10 |
| **Tecnologías** | FusedLocationProviderClient para geolocalizar al cliente y Maps SDK for Android para mostrar tiendas cercanas. |
| **Herramientas** | FusedLocationProviderClient, Maps SDK for Android |
| **Requerimiento Académico** | 10 (Recurso móvil: geolocalización, GPS) |

#### Criterios de Aceptación
- [ ] Elección entre despacho o recojo
- [ ] Direcciones guardadas y reutilizables
- [ ] Listado de las 5 tiendas con stock disponible
- [ ] Recojo en tienda sin costo de envío
- [ ] Aviso si el distrito no está cubierto
- [ ] Plazo estimado mostrado

---

### HU-16 - Pago y confirmación del pedido
**Historia de Usuario:**  
_Como cliente quiero pagar con el medio que prefiera y recibir confirmación._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Rojas |
| **Plazo / Cronograma** | SEM 9 - SEM 10 |
| **Tecnologías** | Retrofit + Coroutines para la pasarela de pagos; Cloud Firestore para registrar el pedido. |
| **Herramientas** | Retrofit, Cloud Firestore |
| **Requerimiento Académico** | 6 (Corrutinas y Retrofit), 3 (Firebase), 2 (Proceso de negocio: compra) |

#### Criterios de Aceptación
- [ ] 4 medios de pago vigentes
- [ ] Pago con tarjeta vía pasarela ya contratada (sin almacenar el número completo)
- [ ] Pedido 'Aprobado' con número correlativo y stock reservado
- [ ] Boleta o factura descargable
- [ ] Confirmación en pantalla y notificación
- [ ] Motivo de rechazo informado
- [ ] Sin duplicidad de cobro o pedido

---

### HU-17 - Vigencia del pedido pendiente de pago
**Historia de Usuario:**  
_Como cliente quiero saber cuánto tiempo tengo para completar el pago para evitar perder la reserva de mis prednas y asegurar mi compra.._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Perez |
| **Plazo / Cronograma** | SEM 9 - SEM 10 |
| **Tecnologías** | WorkManager para el temporizador diferido y disparar la notificación de vencimiento. |
| **Herramientas** | WorkManager, Firebase Cloud Messaging |
| **Requerimiento Académico** | 10 (Recurso móvil: Notificaciones) 7 (WorkManager) |

#### Criterios de Aceptación
- [ ] Estado 'Pendiente' máx. 30 min
- [ ] Tiempo restante visible y reintento con otro medio
- [ ] Notificación push en 10 min antes del vencimiento
- [ ] Paso a 'Anulado' y liberación de stock inmediato a los 30 min
- [ ] Prendas disponibles en el carrito para reintentar

---

## E6. Seguimiento y postventa
> **Descripción:** Estado del pedido, notificaciones y cambios/devoluciones.
> **Sprint base:** 3

### HU-18 - Seguimiento del pedido
**Historia de Usuario:**  
_Como cliente quiero conocer el estado de mi pedido y ser avisado de cada cambio._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Vila |
| **Plazo / Cronograma** | SEM 11 - SEM 12 |
| **Tecnologías** | Firebase Cloud Messaging para push + Firestore listeners para el estado en tiempo real. |
| **Herramientas** | Firebase Cloud Messaging, Firestore |
| **Requerimiento Académico** | 3 (Firebase) |

#### Criterios de Aceptación
- [ ] Estados: Pendiente, Aprobado, En preparación, Despachado, Entregado, Anulado
- [ ] Historial con fecha y hora de cada cambio
- [ ] Notificación push por cada cambio
- [ ] Empresa de transporte y N° de seguimiento en 'Despachado'
- [ ] Aviso de recojo listo en tienda
- [ ] Habilitación de cambio/devolución al llegar a 'Entregado'
- [ ] Reflejo del cambio en máx. 10 min

---

### HU-19 - Solicitud de cambio o devolución
**Historia de Usuario:**  
_Como cliente quiero solicitar un cambio o devolución desde la app._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Rojas |
| **Plazo / Cronograma** | SEM 11 - SEM 12 |
| **Tecnologías** | CameraX/galería para adjuntar fotos de la prenda a devolver; Cloud Firestore para el registro de la solicitud. |
| **Herramientas** | CameraX, Firebase Storage, Firestore |
| **Requerimiento Académico** | 10 (Recurso móvil: cámara), 3 (Firebase), 2 (Proceso de negocio: postventa) |

#### Criterios de Aceptación
- [ ] Habilitado solo 7 días tras 'Entregado'
- [ ] Selección de prenda, tipo y motivo predefinido
- [ ] Motivo 'Otro' con texto libre
- [ ] Hasta 3 fotografías adjuntas
- [ ] Número de solicitud y estado 'En evaluación'
- [ ] Registro de si hubo prueba virtual previa
- [ ] Consulta de estado y resolución
- [ ] La app no resuelve, solo registra la solicitud

---

## E7. Analítica y asistencia inteligente
> **Descripción:** Funcionalidad no contemplada en el backlog de negocio original; se añade para cubrir el dashboard y las funcionalidades de IA exigidas por el curso.
> **Sprint base:** 3

### HU-20 - Dashboard personal del cliente
**Historia de Usuario:**  
_Como cliente quiero ver un panel con mi actividad para entender mis hábitos de compra y de pruebas virtuales._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Perez |
| **Plazo / Cronograma** | SEM 11 - SEM 12 (en paralelo) |
| **Tecnologías** | Librería de gráficos nativa para Android (MPAndroidChart o Vico) alimentada con datos locales (Room) y de Firestore. |
| **Herramientas** | MPAndroidChart o Vico, Room |
| **Requerimiento Académico** | 9 (Dashboards) |

#### Criterios de Aceptación
- [ ] Gráfico de compras por mes
- [ ] Gráfico de prendas probadas virtualmente vs. compradas
- [ ] Total ahorrado en promociones
- [ ] Filtro por rango de fechas

---

### HU-21 - Recomendador de prendas y chatbot de soporte con IA
**Historia de Usuario:**  
_Como cliente quiero recibir recomendaciones de prendas y resolver dudas por chat para decidir mejor mi compra._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Vila |
| **Plazo / Cronograma** | SEM 11 - SEM 12 (en paralelo) |
| **Tecnologías** | Modelo de recomendación simple (basado en contenido/historial) + chatbot consumiendo una API de LLM vía Retrofit y corrutinas. |
| **Herramientas** | Retrofit, API de LLM (ej. Gemini/OpenAI), Cloud Firestore (historial de chat) |
| **Requerimiento Académico** | 11 (IA: recomendaciones + chat con LLM), 6 (Corrutinas y Retrofit) |

#### Criterios de Aceptación
- [ ] Sección 'Recomendado para ti' según historial y prendas probadas
- [ ] Chat de soporte disponible desde cualquier pantalla
- [ ] El chat responde dudas de tallas, envíos y devoluciones
- [ ] Historial de conversación guardado por sesión

---

## E8. Cierre y despliegue
> **Descripción:** Publicación de la app en tienda.
> **Sprint base:** 3

### HU-22 (tarea técnica) - Publicación en Google Play Store
**Historia de Usuario:**  
_Como equipo del proyecto queremos publicar la app para que los clientes puedan descargarla._

| Campo | Detalle |
| :--- | :--- |
| **Encargado** | Rojas |
| **Plazo / Cronograma** | SEM 12 |
| **Tecnologías** | Android App Bundle (.aab), Google Play Console, Play App Signing. |
| **Herramientas** | Google Play Console, Play App Signing |
| **Requerimiento Académico** | 12 (Despliegue en Google Play Store) |

#### Criterios de Aceptación
- [ ] Cuenta de desarrollador en Google Play Console
- [ ] Firma de la app (App Bundle) y ficha de la tienda completa
- [ ] Cumplimiento de políticas de privacidad (Ley N° 29733) en la ficha
- [ ] Publicación en pista interna/cerrada antes de producción

---
