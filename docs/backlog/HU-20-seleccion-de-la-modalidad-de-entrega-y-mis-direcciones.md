# HU-20 — Selección de la modalidad de entrega y Mis direcciones

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E5. Compra y pago |
| **Sprint** | Sprint 3 (SEM 12 – SEM 15) |
| **Semanas** | SEM 12 – SEM 13 |
| **Encargado** | Vila |
| **Dependencias** | HU-19, HU-03 (menú de Perfil) |
| **Requisitos del curso** | 10 (Recurso móvil) |

> Como cliente quiero elegir despacho a domicilio o recojo en tienda, y tener mis direcciones guardadas para no escribirlas en cada compra.

## Descripción funcional

El cliente elige despacho a domicilio (dirección guardada o nueva) o recojo en tienda. Para el recojo ve en un mapa las tiendas con stock, ordenadas por cercanía si concede el permiso de ubicación. Se muestra el plazo estimado. Esta historia también construye la opción Mis direcciones de Perfil, donde el cliente registra, edita y elimina sus direcciones de entrega y elige una predeterminada.

## Pantallas

- Entrega (checkout paso 2)
- Perfil > Mis direcciones
- Nueva / editar dirección
- Mapa de tiendas

## Componentes UI

- Selector segmentado Despacho / Recojo
- Lista de direcciones guardadas con etiqueta 'Predeterminada'
- Formulario de dirección (nombre, calle y número, referencia, distrito desplegable, teléfono de contacto)
- Diálogo de confirmación para eliminar dirección
- Mapa con marcadores de tiendas
- Tarjeta de tienda con stock y distancia
- Texto de plazo estimado
- Solicitud de permiso de ubicación

## Criterios de aceptación

- [ ] **CA-01** Dado que estoy en el checkout, cuando llego a Entrega, entonces puedo elegir entre despacho a domicilio o recojo en tienda.
- [ ] **CA-02** Dado que elijo despacho, cuando selecciono la dirección, entonces puedo usar una de Mis direcciones (la predeterminada aparece seleccionada) o agregar una nueva, que también queda guardada en Mis direcciones.
- [ ] **CA-03** Dado que agrego una dirección desde Perfil > Mis direcciones con los campos obligatorios completos, cuando la guardo, entonces aparece en la lista y, si es la primera, queda como predeterminada.
- [ ] **CA-04** Dado que tengo varias direcciones, cuando marco una como predeterminada, entonces esa dirección aparece seleccionada al elegir despacho a domicilio.
- [ ] **CA-05** Dado que edito o elimino una dirección, cuando confirmo, entonces el cambio se refleja en Mis direcciones y en la selección de entrega.
- [ ] **CA-06** Dado que guardo una dirección de un distrito sin cobertura de despacho, cuando se guarda, entonces aparece con el aviso 'Sin cobertura de despacho'.
- [ ] **CA-07** Dado que mi distrito no tiene cobertura, cuando lo selecciono, entonces veo 'No llegamos a este distrito' y se me sugiere el recojo en tienda.
- [ ] **CA-08** Dado que elijo recojo, cuando se abre el mapa, entonces veo solo las tiendas (de las 5) con stock de todos mis ítems y el envío pasa a S/ 0.
- [ ] **CA-09** Dado que concedo el permiso de ubicación, cuando veo las tiendas, entonces se ordenan de la más cercana a la más lejana con su distancia; si lo niego, se listan sin distancia y el flujo continúa.
- [ ] **CA-10** Dado que elegí modalidad y destino, cuando reviso el resumen, entonces veo el plazo estimado de entrega o de recojo.

## Tecnologías

FusedLocationProviderClient para geolocalizar al cliente y Maps SDK for Android (Maps Compose) para mostrar las tiendas. Direcciones del cliente y cobertura por distrito en Cloud Firestore.

**Herramientas:** FusedLocationProviderClient, Maps SDK for Android, Cloud Firestore
