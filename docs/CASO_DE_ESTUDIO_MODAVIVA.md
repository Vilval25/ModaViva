# Caso de Negocio: Tienda de Ropa en Línea con Probador Virtual (ModaViva)

## 1. Contexto Empresarial
**Moda Viva Perú S.A.C.** es una empresa retail dedicada a la comercialización de ropa casual, urbana y de vestir para damas, caballeros y niños.

* **Infraestructura física:** Cuatro tiendas físicas en Lima Metropolitana y una tienda en Arequipa (5 tiendas en total).
* **Catálogo:** ~3,500 modelos activos (marcas propias y marcas asociadas en consignación).
* **Base de clientes:** ~45,000 clientes registrados.
* **Ventas:** S/ 28 millones anuales aprox., donde el **35%** proviene del canal digital (web).
* **Meta comercial:** Crecimiento proyectado del **40%** en ventas digitales para el presente año.

---

## 2. Definición del Problema
El canal digital enfrenta un cuello de botella crítico en la rentabilidad y logística:
* **Tasa de devolución:** El **32%** de los pedidos realizados en la tienda en línea termina en cambio o devolución.
* **Causa raíz:** De las devoluciones, solo el 18% obedece a fallas de fábrica o error en la talla elegida. El **82% restante** ocurre porque el cliente, al probarse la prenda en casa, percibe que el estilo, corte, caída o color no le favorece.
* **Sesgo de catálogo:** El catálogo web exhibe prendas sobre modelos profesionales de estudio, alejados de la contextura, estatura y tono de piel del cliente promedio.
* **Impacto operativo y financiero:**
  * Sobrecostos elevados en logística inversa y reacondicionamiento de inventario.
  * Inmovilización de mercadería.
  * Deterioro de la retención de clientes.
  * Saturación del canal de soporte/postventa (~180 consultas diarias por WhatsApp/teléfono solicitando opiniones subjetivas sobre calce y combinación).
  * Fuga de conversión por incertidumbre antes del checkout.

---

## 3. Necesidad y Solución Propuesta
Desarrollo de una aplicación móvil nativa Android orientada a:
1. **Compras omnicanal fluidas:** Catálogo digital sincronizado en stock y precios con el sistema comercial central y tiendas físicas.
2. **Probador Virtual con IA Generativa:** Generación de imágenes hiperrealistas del cliente vistiendo la prenda seleccionada, adaptada a su propia contextura a partir de una fotografía capturada desde la app.
3. **Eficiencia en condiciones adversas:** Desempeño optimizado para equipos de gama media y redes móviles de baja velocidad/inestables.
4. **Métricas de impacto:** Telemetría para comparar conversión, tasa de devolución y costo de inferencia entre usuarios del probador virtual vs. flujo tradicional.

---

## 4. Áreas y Stakeholders Involucrados
* **Unidad de Comercio Electrónico:** Administración de canales y reglas de venta online.
* **Área de Catálogo y Contenido Digital:** Curaduría, fichas técnicas, medidas y aprobación fotográfica.
* **Área de Almacén y Despacho:** Preparación (picking/packing), trazabilidad de guías y despacho omnicanal (courier o recojo en tienda).
* **Área de Postventa:** Gestión de solicitudes de cambio/devolución y soporte al cliente.
* **Unidad de Marketing:** Campañas, cupones y fidelización.
* **Unidad de TI:** Integración con sistemas comerciales, APIs y servicios en la nube.
* **Gerencia Comercial:** Rentabilidad, analítica de conversión y retorno de inversión del servicio de IA.

---

## 5. Procesos Operativos Actuales
* **Publicación y Catálogo:** Solo se exhiben prendas con fotografías profesionales previamente aprobadas por Contenido Digital.
* **Venta y Reserva:** Checkout con cálculo dinámico de envío distrital o retiro en tienda; 4 medios de pago (tarjeta de crédito, débito, billetera electrónica y contra entrega). En caso de rechazo, el stock queda reservado por un tiempo límite antes de su liberación.
* **Despacho y Trazabilidad:** Estados formales (*Pendiente*, *Aprobado*, *En preparación*, *Despachado*, *Entregado*). Notificaciones al cliente en cada transición.
* **Postventa:** Ventana de 7 días calendario tras la entrega para radicar solicitudes de cambio o devolución.

---

## 6. Restricciones de Negocio y Marco Legal
* **Protección de Datos Personales (Ley N° 29733):** La fotografía del usuario es tratada como dato personal sensible. Requiere consentimiento expreso (opt-in no premarcado), posibilidad de revocación y opción de eliminación total e irreversible en cualquier momento.
* **Políticas de Producto:** Restricción estricta del uso del probador virtual en categorías específicas (ropa interior, trajes de baño).
