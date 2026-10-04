import { onMessagePublished } from "firebase-functions/v2/pubsub";
import { logger } from "firebase-functions/v2";
import { GoogleAuth } from "google-auth-library";

/**
 * Corte automático de gastos.
 *
 * El presupuesto de Cloud Billing publica en el tema `billing-budget` varias
 * veces al día el gasto acumulado del mes. Cuando el gasto alcanza el monto
 * del presupuesto, esta función desvincula la cuenta de facturación del
 * proyecto: a partir de ahí no se cobra nada más, el proyecto queda con los
 * límites del plan gratuito y las funciones dejan de responder. Los datos no
 * se borran; para volver basta con vincular otra vez la cuenta de facturación.
 *
 * Google informa el gasto con unas horas de retraso, así que el corte puede
 * llegar después de superar un poco el presupuesto.
 *
 * Un mensaje con el atributo `dryRun=true` no corta nada: solo comprueba que
 * la función tiene permiso para hacerlo y lo registra en el log. Sirve para
 * probar la instalación sin desactivar la facturación.
 */

const TOPIC = "billing-budget";
const BILLING_SCOPE = "https://www.googleapis.com/auth/cloud-billing";
const PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
const UNLINK_PERMISSION = "resourcemanager.projects.deleteBillingAssignment";

/** Campos del mensaje que envía Cloud Billing que usa esta función. */
interface BudgetNotification {
  budgetDisplayName: string;
  costAmount: number;
  budgetAmount: number;
  currencyCode: string;
}

const auth = new GoogleAuth({ scopes: [BILLING_SCOPE, PLATFORM_SCOPE] });

export const budgetGuard = onMessagePublished(
  { topic: TOPIC, maxInstances: 1, retry: false },
  async (event) => {
    const projectId = process.env.GCLOUD_PROJECT;
    if (!projectId) {
      logger.error("No se pudo determinar el proyecto (GCLOUD_PROJECT vacío).");
      return;
    }

    const message = event.data.message;
    const dryRun = message.attributes?.dryRun === "true";
    const notification = message.json as BudgetNotification;
    const { costAmount, budgetAmount, currencyCode, budgetDisplayName } = notification;

    logger.info("Notificación de presupuesto", {
      budgetDisplayName,
      costAmount,
      budgetAmount,
      currencyCode,
      dryRun,
    });

    if (dryRun) {
      const allowed = await canUnlinkBilling(projectId);
      if (allowed) {
        logger.info("Prueba OK: la función tiene permiso para cortar la facturación.");
      } else {
        logger.error(
          `Prueba FALLIDA: falta el permiso ${UNLINK_PERMISSION}. ` +
            "Asigna el rol 'Administrador de facturación del proyecto' a la cuenta de servicio.",
        );
      }
      return;
    }

    if (typeof costAmount !== "number" || typeof budgetAmount !== "number") {
      logger.error("Mensaje sin costAmount/budgetAmount; se ignora.", { notification });
      return;
    }

    if (costAmount < budgetAmount) {
      logger.info(`Gasto ${costAmount} ${currencyCode} bajo el presupuesto; sin acción.`);
      return;
    }

    if (!(await isBillingEnabled(projectId))) {
      logger.info("La facturación ya está desactivada; sin acción.");
      return;
    }

    logger.warn(
      `Gasto ${costAmount} ${currencyCode} alcanzó el presupuesto de ` +
        `${budgetAmount} ${currencyCode}. Desactivando la facturación de ${projectId}.`,
    );
    await disableBilling(projectId);
    logger.warn("Facturación desactivada.");
  },
);

async function isBillingEnabled(projectId: string): Promise<boolean> {
  const client = await auth.getClient();
  const res = await client.request<{ billingEnabled?: boolean }>({
    url: `https://cloudbilling.googleapis.com/v1/projects/${projectId}/billingInfo`,
  });
  return res.data.billingEnabled === true;
}

async function disableBilling(projectId: string): Promise<void> {
  const client = await auth.getClient();
  await client.request({
    url: `https://cloudbilling.googleapis.com/v1/projects/${projectId}/billingInfo`,
    method: "PUT",
    data: { billingAccountName: "" },
  });
}

async function canUnlinkBilling(projectId: string): Promise<boolean> {
  const client = await auth.getClient();
  const res = await client.request<{ permissions?: string[] }>({
    url: `https://cloudresourcemanager.googleapis.com/v3/projects/${projectId}:testIamPermissions`,
    method: "POST",
    data: { permissions: [UNLINK_PERMISSION] },
  });
  return res.data.permissions?.includes(UNLINK_PERMISSION) === true;
}
