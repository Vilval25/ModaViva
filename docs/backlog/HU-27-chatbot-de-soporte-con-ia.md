# HU-27 — Chatbot de soporte con IA

> ⚠️ **Borrador.** Las historias del Sprint 2 y 3 son una base y se revisarán al cerrar el Hito 1. No implementar sin confirmar que el texto es definitivo.

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E7. Analítica y asistencia inteligente |
| **Sprint** | Sprint 3 (SEM 12 – SEM 15) |
| **Semanas** | SEM 15 |
| **Encargado** | Rojas |
| **Dependencias** | HT-02 |
| **Requisitos del curso** | 11 (Inteligencia artificial), 6 (Corrutinas y Retrofit) |

> Como cliente quiero resolver dudas por chat para decidir mejor mi compra.

## Descripción funcional

Un botón flotante abre el chat de soporte. El asistente (un LLM llamado a través de una Cloud Function) responde dudas sobre tallas, envíos y devoluciones usando como contexto las políticas de la tienda.

## Pantallas

- Chat de soporte

## Componentes UI

- Botón flotante de chat
- Burbujas de mensajes
- Indicador 'escribiendo…'
- Sugerencias rápidas ('¿Cuánto cuesta el envío?')
- Aviso 'Asistente automático'

## Criterios de aceptación

- [ ] **CA-01** Dado que estoy en cualquier pantalla principal, cuando toco el botón de chat, entonces se abre el chat y al cerrarlo vuelvo a la pantalla donde estaba.
- [ ] **CA-02** Dado el set de 10 preguntas de prueba sobre tallas, envíos y devoluciones, cuando se las hago al asistente, entonces al menos 9 respuestas son coherentes con las políticas cargadas de la tienda.
- [ ] **CA-03** Dado que pregunto algo fuera de esos temas o que el asistente no sabe, cuando responde, entonces lo indica y no inventa datos de pedidos, precios o stock.
- [ ] **CA-04** Dado que estoy en una conversación, cuando salgo y vuelvo al chat en la misma sesión, entonces veo el historial de la conversación.
- [ ] **CA-05** Dado que el servicio del LLM no responde, cuando envío un mensaje, entonces veo un error y puedo reintentar.

## Tecnologías

Retrofit + Kotlin Coroutines hacia una Cloud Function que llama a la API del LLM (la clave vive solo en el servidor). Cloud Firestore para el historial de conversación.

**Herramientas:** Retrofit, Cloud Functions, API de LLM (ej. Gemini/OpenAI), Cloud Firestore
