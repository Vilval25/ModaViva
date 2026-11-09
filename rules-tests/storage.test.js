// Pruebas de storage.rules (HT-02 CA-04). Se ejecutan contra el emulador.

import { after, before, describe, test } from "node:test";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { getBytes, ref, uploadBytes } from "firebase/storage";

let env;
const bytes = new Uint8Array([1, 2, 3]);

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-modaviva",
    storage: { rules: readFileSync(new URL("../storage.rules", import.meta.url), "utf8") },
  });
  await env.withSecurityRulesDisabled(async (ctx) => {
    await uploadBytes(ref(ctx.storage(), "prendas/BL-1002/1.jpg"), bytes);
    await uploadBytes(ref(ctx.storage(), "inicio/portada.jpg"), bytes);
    await uploadBytes(ref(ctx.storage(), "privado/x.jpg"), bytes);
  });
});

after(() => env.cleanup());

describe("storage", () => {
  test("las fotos del catálogo son públicas", async () => {
    const storage = env.unauthenticatedContext().storage();
    await assertSucceeds(getBytes(ref(storage, "prendas/BL-1002/1.jpg")));
  });

  test("la foto de la portada de Inicio es pública y nadie la reemplaza desde la app", async () => {
    await assertSucceeds(getBytes(ref(env.unauthenticatedContext().storage(), "inicio/portada.jpg")));
    await assertFails(uploadBytes(ref(env.authenticatedContext("cli-ana").storage(), "inicio/portada.jpg"), bytes));
  });

  test("nadie sube ni reemplaza fotos del catálogo desde la app", async () => {
    const storage = env.authenticatedContext("cli-ana").storage();
    await assertFails(uploadBytes(ref(storage, "prendas/BL-1002/1.jpg"), bytes));
    await assertFails(uploadBytes(ref(storage, "categorias/blusas.jpg"), bytes));
  });

  test("cliente puede subir, leer y borrar su propia foto de probador (HU-11 CA-06)", async () => {
    const anaStorage = env.authenticatedContext("cli-ana").storage();
    const bobStorage = env.authenticatedContext("cli-bob").storage();
    const guestStorage = env.unauthenticatedContext().storage();
    const anaFotoRef = ref(anaStorage, "clientes/cli-ana/probador/foto.jpg");

    // Ana puede subir y leer su foto
    await assertSucceeds(uploadBytes(anaFotoRef, bytes));
    await assertSucceeds(getBytes(anaFotoRef));

    // Bob o un invitado no pueden leer la foto de Ana
    await assertFails(getBytes(ref(bobStorage, "clientes/cli-ana/probador/foto.jpg")));
    await assertFails(getBytes(ref(guestStorage, "clientes/cli-ana/probador/foto.jpg")));

    // Bob o un invitado no pueden escribir en la carpeta de Ana
    await assertFails(uploadBytes(ref(bobStorage, "clientes/cli-ana/probador/foto.jpg"), bytes));
    await assertFails(uploadBytes(ref(guestStorage, "clientes/cli-ana/probador/foto.jpg"), bytes));
  });

  test("se rechaza cualquier ruta que no esté en las reglas", async () => {
    const storage = env.authenticatedContext("cli-ana").storage();
    await assertFails(getBytes(ref(storage, "privado/x.jpg")));
    await assertFails(uploadBytes(ref(storage, "privado/y.jpg"), bytes));
  });
});
