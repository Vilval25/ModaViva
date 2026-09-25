# Diseño de base de datos — Cloud Firestore (ModaViva)

Documento de diseño para la capa de datos de la app móvil. Sirve como referencia para HU-01 (registro y vinculación) y para el resto de historias que consulten datos.

---

## 1. Por qué este NO es un diagrama ER

Firestore es una base de datos **documental** (NoSQL). No existen tablas, ni claves foráneas, ni `JOIN`. Por lo tanto el diagrama clásico de Entidades-Relaciones **no aplica**.

| Concepto SQL | Equivalente en Firestore |
| :--- | :--- |
| Tabla | Colección |
| Fila | Documento (JSON) |
| Clave primaria | `id` del documento (string) |
| Clave foránea | Un campo string que guarda el `id` de otro documento |
| `JOIN` | No existe → lectura doble o datos duplicados |
| `UNIQUE` | No existe → colección espejo + transacción |
| Normalización | **Desnormalización** (se duplica lo que se lee junto) |

Regla de oro: **se diseña alrededor de las consultas, no de las entidades.** Antes de crear una colección, escribir la consulta que la usará.

---

## 2. Esquema de colecciones

```
clientes_web/                       ← simulación de la tienda en línea (datos semilla)
 └─ {clienteId}                     (autogenerado)
     ├─ nombres: string
     ├─ apellidos: string
     ├─ documento: string           ← DNI, único
     ├─ telefono: string
     ├─ email: string               ← único
     ├─ direcciones: array
     │    └─ { alias, direccion, distrito, referencia }
     ├─ origen: "web"
     ├─ vinculado: boolean          ← ya tiene cuenta en la app
     └─ authUid: string | null      ← UID de Firebase Auth, si está vinculado

clientes/                           ← perfil del cliente dentro de la app
 └─ {authUid}                       (el ID es el UID de Firebase Auth)
     ├─ nombres, apellidos, documento, telefono, email
     ├─ clienteWebId: string | null   ← vínculo con la web (HU-01)
     ├─ emailVerificado: boolean
     ├─ alturaCm: number | null        ← needed por el Probador Virtual (HU-11)
     └─ consentimiento:                ← T&C (HU-01)
          ├─ version: string
          └─ aceptadoEn: timestamp

pedidos/                            ← historial (HU-01, HU-13)
 └─ {pedidoId}
     ├─ clienteWebId: string        ← ⚠️ referencia, no JOIN
     ├─ clienteNombre: string       ← dato desnormalizado a propósito
     ├─ numero: string
     ├─ fecha: timestamp
     ├─ estado: "Pendiente" | "Aprobado" | "En preparación" | "Despachado" | "Entregado"
     ├─ items: array
     │    └─ { sku, nombre, talla, color, cantidad, precioUnitario }
     ├─ subtotal, envio, total: number
     └─ metodoPago: "Tarjeta" | "Transferencia" | "Yape" | "Plin"

documentos/                         ← índice espejo para unicidad de documento
 └─ {numeroDocumento}               (el ID ES el documento, de ahí la unicidad)
     └─ authUid: string
```

---

## 3. Las "relaciones" y cómo se resuelven

| Relación | Cómo se modela | Coste |
| :--- | :--- | :--- |
| 1 cliente web → N pedidos | `pedidos.clienteWebId` | 1 lectura con `where` |
| 1 cliente web ↔ 1 cliente app | `clientes.clienteWebId` | 1 lectura por `documentId` |
| N.app ↔ 1.web | `clientes_web.authUid` | índice para la búsqueda inversa |
| Unicidad de documento | colección `documentos` | 1 escritura extra en transacción |

### 3.1 Historial de pedidos (HU-01, criterio 3)

```kotlin
db.collection("pedidos")
    .whereEqualTo("clienteWebId", clienteWebId)
    .orderBy("fecha", Query.Direction.DESCENDING)
```

> `orderBy` sobre un campo distinto al de filtro exige un índice compuesto.
> Crear en la consola: **Firestore → Índices → Agregar índice compuesto**.

### 3.2 Búsqueda del cliente para vincular (HU-01, criterio 2)

Al registrarse, buscar si el correo o documento ya existe en la web:

```kotlin
db.collection("clientes_web").whereEqualTo("email", email).get()
db.collection("clientes_web").whereEqualTo("documento", documento).get()
```

Si hay coincidencia → **vincular**: escribir `clientes/{authUid}` con el `clienteWebId` y marcar `clientes_web/{id}.vinculado = true`. Esta es la "vinculación automática con la cuenta web existente".

Si no hay coincidencia → **crear** en ambas colecciones (registro nuevo).

### 3.3 Unicidad de documento (HU-01, criterio 4)

⚠️ **Este es el punto donde casi todos se equivocan.** Firestore **no tiene `UNIQUE`**. Si dos personas se registran con el mismo DNI al mismo tiempo, un `get()` seguido de un `set()` deja pasar al segundo.

La solución es usar la colección espejo `documentos`, donde **el ID del documento es el número de documento** — eso sí garantiza unicidad. Y hacerlo dentro de una **transacción**:

```kotlin
db.runTransaction { transaction ->
    val ref = db.collection("documentos").document(documento)
    val existing = transaction.get(ref)
    if (existing.exists()) {
        throw IllegalStateException("Documento ya registrado")
    }
    transaction.set(ref, mapOf("authUid" to authUid))
    transaction.set(db.collection("clientes").document(authUid), perfil)
    null
}
```

> **La unicidad del correo la garantiza Firebase Auth**, no hace falta hacer nada extra. Auth ya rechaza correos duplicados con `ERROR_EMAIL_ALREADY_IN_USE`.

### 3.4 Verificación de correo (HU-01, criterio 6)

La maneja Firebase Auth (`sendEmailVerification`) con un listener de `onEmailVerified`. En Firestore solo se refleja el resultado en `clientes/{authUid}.emailVerificado`. Reglas: permitir lectura pero **bloquear escritura** de `pedidos` mientras `emailVerificado == false`.

---

## 4. Datos semilla

El sistema web histórico de ModaViva es ficticio, así que se simula con documentos sembrados en `clientes_web` y `pedidos`.

- **Volumen:** ~300 documentos en `clientes_web` (suficiente para representar los ~45,000 clientes del caso de estudio sin pagar ni slowing).
- **No se siembran usuarios de Firebase Auth.** Crear 300 cuentas reales de Auth es inviable por costo y por los límites de email. Las cuentas de prueba se crean a mano (2 o 3) para las pruebas de HU-02 (login).
- Los documentos semilla deben llevar un campo marcador, p. ej. `origen: "web"`, para distinguirlos de los creados desde la app.

---

## 5. Reglas de seguridad (borrador)

Las reglas importan **más** que el archivo `google-services.json`, que no contiene secretos.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Solo el propio cliente puede leer y escribir su perfil
    match /clientes/{authUid} {
      allow read, write: if request.auth != null && request.auth.uid == authUid;
    }

    // La web escribe; la app solo lee (catálogo y clientes existentes)
    match /clientes_web/{clienteId} {
      allow read: if request.auth != null;
      allow write: if false;   // solo desde backend/web
    }

    // Historial: lectura del dueño, escritura solo desde el backend de pedidos
    match /pedidos/{pedidoId} {
      allow read: if request.auth != null;
      allow write: if false;
    }

    // El índice espejo solo se escribe, nunca se lee desde el cliente
    match /documentos/{documento} {
      allow read, write: if false;
    }
  }
}
```

> Con `allow write: if false` en `pedidos`, la app no puede crear pedidos: esa lógica
> vive en el backend/web, que es lo correcto para un flujo de e-commerce.

---

## 6. Qué falta decidir (pendiente para fases siguientes)

- [ ] Confirmar región de Firestore (recomendado: `southamerica-east1` por cercanía a Perú).
- [ ] Definir si el catálogo (HU-03) va en su propia colección `prendas` o embebido.
- [ ] Definir la estrategia de índice compuesto para `pedidos` (orden por fecha).
- [ ] Definir cómo se sincroniza catálogo → Room (HU-03, modo offline).
- [ ] Reglas para el Probador Virtual y las fotos del cliente (HU-11): consentimiento explícito y retención de datos personales.
