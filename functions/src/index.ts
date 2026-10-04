import { setGlobalOptions } from "firebase-functions/v2";

/*
 * Opciones comunes a todas las funciones. La región coincide con la de
 * Firestore (nam5). El tope de instancias evita que un error o un pico de
 * llamadas escale sin límite y genere costos: ninguna función de este
 * proyecto necesita más de unas pocas instancias a la vez.
 */
setGlobalOptions({
  region: "us-central1",
  maxInstances: 2,
  timeoutSeconds: 60,
  memory: "256MiB",
});

export { budgetGuard } from "./budgetGuard.js";
export { ping } from "./ping.js";
