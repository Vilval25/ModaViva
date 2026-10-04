# HU-03 — Perfil del cliente

| Campo | Detalle |
| :--- | :--- |
| **Tipo** | Historia de usuario |
| **Épica** | E1. Cuenta y acceso |
| **Sprint** | Sprint 1 (SEM 5 – SEM 7) |
| **Semanas** | SEM 6 |
| **Encargado** | Perez |
| **Dependencias** | HU-01, HU-02 |
| **Requisitos del curso** | 4 (Material Design), 3 (Firebase), 5 (MVVM/Clean Code) |

> Como cliente quiero ver y editar mis datos y acceder a las opciones de mi cuenta desde Perfil para administrar mi información.

## Descripción funcional

Con sesión iniciada, la pestaña Perfil muestra los datos del cliente (nombre, correo y método de acceso) y un menú con las opciones de la cuenta: Editar datos, Mis direcciones, Métodos de pago, Cambiar contraseña, Mis medidas, Privacidad, Mi actividad, Acerca de y Cerrar sesión. Cada opción del menú se agrega cuando se completa la historia que la construye (Cambiar contraseña y Cerrar sesión en HU-02, Privacidad en HU-10, Mis medidas en HU-14, Mis direcciones en HU-20, Métodos de pago en HU-21, Mi actividad en HU-25). Sin sesión, la pestaña se comporta como indica HU-02.

## Pantallas

- Perfil (con sesión)
- Editar datos personales
- Acerca de (versión y enlaces a T&C y Política de Privacidad)

## Componentes UI

- Encabezado con avatar (inicial o foto de Google), nombre y correo
- Lista de opciones con íconos (ListItem de Material 3)
- Formulario de edición con validación en línea
- Snackbar 'Cambios guardados'
- Diálogo de confirmación de cierre de sesión

## Criterios de aceptación

- [ ] **CA-01** Dado que tengo sesión, cuando abro la pestaña Perfil, entonces veo mi nombre, mi correo y si ingreso con correo o con Google.
- [ ] **CA-02** Dado que estoy en Perfil, cuando reviso el menú, entonces cada opción disponible abre su pantalla y Cerrar sesión pide confirmación.
- [ ] **CA-03** Dado que edito mi nombre o apellidos con valores válidos, cuando guardo, entonces el cambio se ve en Perfil y se mantiene al reiniciar la app o al entrar desde otro dispositivo.
- [ ] **CA-04** Dado que dejo un campo obligatorio vacío o inválido, cuando intento guardar, entonces veo el motivo y no se guarda.
- [ ] **CA-05** Dado que abro Editar datos, cuando veo el formulario, entonces el correo se muestra pero no se puede modificar, porque identifica la cuenta.
- [ ] **CA-06** Dado que ingresé con Google, cuando veo el menú, entonces la opción Cambiar contraseña no aparece.
- [ ] **CA-07** Dado que abro Acerca de, cuando se muestra, entonces veo la versión de la app y los enlaces a los T&C y a la Política de Privacidad vigentes.

## Tecnologías

Jetpack Compose + Material 3 (TopAppBar, ListItem, OutlinedTextField). Cloud Firestore para el perfil del cliente y Firebase Authentication para conocer el método de acceso. MVVM (ViewModel + Repository).

**Herramientas:** Jetpack Compose + Material 3, Cloud Firestore, Firebase Auth
