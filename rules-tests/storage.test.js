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

  test("se rechaza cualquier ruta que no esté en las reglas", async () => {
    const storage = env.authenticatedContext("cli-ana").storage();
    await assertFails(getBytes(ref(storage, "privado/x.jpg")));
    await assertFails(uploadBytes(ref(storage, "privado/y.jpg"), bytes));
  });
});
