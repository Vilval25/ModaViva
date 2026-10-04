# HU-02 — Inicio de sesión y recuperación de contraseña

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E1. Cuenta y acceso |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 5 – SEM 6 |
| **Encargado** | Perez |
| **Dependencias** | HU-01 |
| **Requisitos del curso** | 1 (Autenticación), 5 (MVVM/Clean Code) |

> Como cliente registrado quiero iniciar sesión y recuperar mi contraseña para acceder desde cualquier dispositivo.

## Descripción funcional

El usuario ingresa con correo y contraseña o con 'Continuar con Google'. Los clientes existentes de la tienda que aún no tienen contraseña la definen con 'Recuperar contraseña'. Sin sesión, Perfil muestra las opciones 'Iniciar sesión' y 'Crear cuenta', y las acciones que requieren cuenta muestran el aviso 'Inicia sesión para continuar'. Si olvidó su contraseña, solicita un enlace de restablecimiento por correo. Desde Perfil puede cambiar su contraseña y cerrar sesión. La sesión se mantiene abierta hasta que el usuario la cierre o pasen 30 días sin uso.

## Pantallas

- Perfil (sin sesión)
- Hoja 'Inicia sesión para continuar'
- Iniciar sesión
- Recuperar contraseña
- Confirmación de envío del enlace
- Perfil > Cambiar contraseña
- Diálogo 'Cerrar sesión'

## Componentes UI

- Campo de correo y campo de contraseña con mostrar/ocultar
- Botón 'Continuar con Google'
- Enlace '¿Olvidaste tu contraseña?'
- Botón 'Ingresar' con estado de carga
- Mensaje de bloqueo temporal
- Diálogo de confirmación de cierre de sesión

## Criterios de aceptación

- [ ] **CA-01** Dado que no tengo sesión, cuando abro la pestaña Perfil, entonces veo las opciones 'Iniciar sesión' y 'Crear cuenta' en lugar de mis datos.
- [ ] **CA-02** Dado que no tengo sesión, cuando intento usar el probador, abrir o marcar favoritos, abrir notificaciones, abrir Mis pedidos o pagar, entonces aparece el aviso 'Inicia sesión para continuar' con 'Iniciar sesión' y 'Crear cuenta'; si lo cierro, sigo navegando como invitado.
- [ ] **CA-03** Dado que ingreso credenciales válidas, cuando inicio sesión, entonces vuelvo a la pantalla desde donde inicié el acceso, en cualquier dispositivo.
- [ ] **CA-04** Dado que mi cuenta usa Google, cuando toco 'Continuar con Google' en Iniciar sesión, entonces accedo a mi cuenta sin ingresar contraseña.
- [ ] **CA-05** Dado que soy un cliente existente de la tienda que nunca usó la app, cuando solicito recuperar mi contraseña, entonces recibo el enlace, defino una contraseña y puedo iniciar sesión con mis datos.
- [ ] **CA-06** Dado que ingreso credenciales incorrectas, cuando inicio sesión, entonces veo 'Correo o contraseña incorrectos' sin indicar cuál de los dos falló.
- [ ] **CA-07** Dado que acumulo 5 intentos fallidos seguidos, cuando intento ingresar de nuevo, entonces veo que el acceso está bloqueado temporalmente (15 min) y recibo un correo de aviso.
- [ ] **CA-08** Dado que solicito recuperar mi contraseña, cuando ingreso un correo, entonces veo siempre el mismo mensaje de confirmación (exista o no la cuenta) y, si existe, recibo un enlace de un solo uso.
- [ ] **CA-09** Dado que abro un enlace de restablecimiento ya usado o vencido, cuando intento usarlo, entonces se me indica que no es válido y puedo solicitar otro.
- [ ] **CA-10** Dado que cambio mi contraseña, cuando uso la app en otro dispositivo con sesión abierta, entonces esa sesión se cierra y se me pide la nueva contraseña.
- [ ] **CA-11** Dado que no cierro sesión, cuando reabro la app dentro de los 30 días, entonces entro sin volver a ingresar credenciales; pasados 30 días sin uso, la sesión se cierra y entro como invitado.
- [ ] **CA-12** Dado que cierro sesión, cuando reabro la app, entonces entro a Inicio como invitado y en el dispositivo no queda caché personal, carrito ni fotos del cliente anterior.

## Tecnologías

Firebase Authentication (correo/contraseña y Google; signIn / sendPasswordResetEmail / updatePassword). Credential Manager para Sign in with Google. Jetpack DataStore para persistir la sesión y la fecha de último uso. Control de intentos fallidos en el servidor (Cloud Function); revisar su alcance frente a la protección nativa de Firebase.

**Herramientas:** Firebase Auth, Credential Manager, Jetpack DataStore, Cloud Functions
