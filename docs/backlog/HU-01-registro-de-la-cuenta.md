# HU-01 — Registro de la cuenta

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E1. Cuenta y acceso |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 5 |
| **Encargado** | Perez |
| **Dependencias** | HT-01, HT-02 (clientes existentes) |
| **Requisitos del curso** | 1 (Autenticación), 5 (MVVM/Clean Code), 4 (Material Design) |

> Como cliente nuevo quiero crear mi cuenta con mi correo o con Google para poder comprar y usar el probador virtual.

## Descripción funcional

La app no exige cuenta para explorar. El registro se inicia desde Perfil (sin sesión) o desde el aviso 'Inicia sesión para continuar' que aparece al intentar una acción que requiere cuenta (probador, favoritos, notificaciones, mis pedidos, pagar). El usuario elige 'Continuar con Google' o 'Crear cuenta con correo'. Con correo completa nombres, apellidos, correo, contraseña y confirmación, y acepta los T&C y la Política de Privacidad. Al enviar, la app verifica si el correo ya tiene una cuenta: si existe, informa al cliente que ya tiene una cuenta y le ofrece iniciar sesión o recuperar su contraseña; si no, crea la cuenta y muestra la pantalla de verificación de correo. Con Google, la cuenta se crea con el nombre y el correo de Google, sin contraseña ni verificación adicional. Al terminar, el usuario vuelve a la pantalla desde donde inició el registro.

## Pantallas

- Registro con correo
- Selector de cuenta de Google (del sistema)
- Verifica tu correo
- Modal de T&C y Política de Privacidad
- Diálogo 'Ya tienes una cuenta'

## Componentes UI

- Botón 'Continuar con Google'
- Campos de texto con validación en línea
- Indicador de requisitos de contraseña
- Casilla de aceptación (no marcada por defecto)
- Botón 'Crear cuenta' con estado de carga
- Diálogo 'Ya tienes una cuenta' con 'Iniciar sesión' y 'Recuperar contraseña'
- Botón 'Reenviar correo'
- Snackbar de error

## Criterios de aceptación

- [ ] **CA-01** Dado que falta un campo obligatorio o alguno es inválido, cuando reviso el formulario, entonces el botón 'Crear cuenta' permanece deshabilitado y el campo muestra el motivo.
- [ ] **CA-02** Dado que ingreso un correo que no tiene cuenta y datos válidos, cuando toco 'Crear cuenta', entonces se crean mi cuenta y mi perfil de cliente, y veo mis datos en Perfil.
- [ ] **CA-03** Dado que el correo ingresado ya tiene una cuenta (cliente existente de la tienda o registrado antes en la app), cuando toco 'Crear cuenta', entonces no se crea una cuenta nueva y veo 'Ya tienes una cuenta con este correo' con las opciones 'Iniciar sesión' y 'Recuperar contraseña'.
- [ ] **CA-04** Dado que la contraseña no tiene 8 o más caracteres, mayúscula, minúscula y número, cuando la escribo, entonces el indicador marca el requisito incumplido y no puedo enviar el formulario.
- [ ] **CA-05** Dado que me registré con correo, cuando se crea la cuenta, entonces recibo un correo de verificación y la app muestra 'Verifica tu correo' con opción de reenviarlo; hasta verificar no puedo confirmar pedidos.
- [ ] **CA-06** Dado que toco 'Continuar con Google' por primera vez y elijo una cuenta, cuando autorizo el acceso, entonces se crea mi cuenta con el nombre y el correo de Google, sin pedirme contraseña ni verificación de correo.
- [ ] **CA-07** Dado que inicié el registro desde una acción que requiere cuenta (probador, favoritos, notificaciones, mis pedidos o pagar), cuando termino de crear la cuenta, entonces vuelvo a esa pantalla y puedo continuar con la acción.
- [ ] **CA-08** Dado que el correo de mi cuenta de Google ya pertenece a un cliente existente, cuando continúo con Google, entonces ingreso a esa misma cuenta y no se crea un perfil duplicado.
- [ ] **CA-09** Dado que cancelo el selector de Google o falla la conexión, cuando vuelvo a la app, entonces sigo en la pantalla de registro con un mensaje y puedo intentarlo de nuevo.
- [ ] **CA-10** Dado que no acepto los T&C y la Política de Privacidad, cuando intento crear la cuenta (con correo o con Google), entonces no puedo continuar; si los acepto, se guardan en mi perfil la fecha y la versión aceptada.

## Tecnologías

Firebase Authentication con los proveedores correo/contraseña y Google. Credential Manager (Sign in with Google) para elegir la cuenta de Google en el dispositivo. Cloud Firestore para el perfil del cliente. Capa de datos con arquitectura MVVM (ViewModel + Repository) e inyección de dependencias con Hilt.

**Herramientas:** Firebase Auth SDK, Credential Manager (Sign in with Google), Cloud Firestore, Jetpack Compose + Material 3, Hilt
