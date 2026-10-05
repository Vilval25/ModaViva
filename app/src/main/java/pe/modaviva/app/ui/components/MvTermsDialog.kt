package pe.modaviva.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.modaviva.app.R

/**
 * Contenido formal y centralizado de los Términos del Servicio y la Política de
 * Privacidad de ModaViva (Ley N° 29733 de Protección de Datos Personales de Perú).
 */
@Composable
fun MvTermsContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TermsSection(
            icon = Icons.Outlined.Gavel,
            title = "1. Términos del Servicio ModaViva",
            body = """
                • Titular del servicio: ModaViva Perú S.A.C., identificada con RUC 20601234567, con sede en Lima, Perú.
                • Alcance: Esta aplicación permite consultar el catálogo de moda, conocer el stock y precios vigentes en soles peruanos (PEN), y adquirir prendas con recojo en tiendas físicas de Lima o entrega a domicilio.
                • Precios y Promociones: Todos los precios incluyen IGV. Las ofertas son válidas hasta la fecha indicada o hasta agotar stock disponible.
                • Uso de cuenta: El usuario es responsable de mantener la confidencialidad de su acceso y de las operaciones realizadas desde su cuenta.
            """.trimIndent(),
        )

        TermsSection(
            icon = Icons.Outlined.Shield,
            title = "2. Protección de Datos (Ley N° 29733)",
            body = """
                • Finalidad: Recopilamos nombres, apellidos, correo electrónico y número de celular únicamente para gestionar tu cuenta de cliente, tramitar tus pedidos y brindarte soporte técnico.
                • Confidencialidad: Tus datos están protegidos bajo la Ley N° 29733 y no son comercializados ni compartidos con empresas externas para fines publicitarios.
                • Derechos ARCO: Tienes derecho a solicitar el Acceso, Rectificación, Cancelación u Oposición de tus datos personales en cualquier momento desde tu perfil o escribiendo a privacidad@modaviva.pe.
            """.trimIndent(),
        )

        TermsSection(
            icon = Icons.Outlined.Lock,
            title = "3. Probador Virtual con IA y Fotografías",
            body = """
                • Uso exclusivo: La fotografía de cuerpo entero que decidas cargar se procesa de forma segura y temporal únicamente para generar la simulación de prueba de prendas y la recomendación de tallas.
                • Propiedad de tus fotos: Tu imagen nunca es publicada ni utilizada para entrenar modelos públicos de inteligencia artificial.
                • Control total: Puedes eliminar tu fotografía registrada de nuestros servidores en cualquier momento con un solo toque desde la sección de Probador Virtual.
            """.trimIndent(),
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Versión de Términos: 1.0 (Vigente 2026). Al aceptar, confirmas haber leído y estar de acuerdo con estas condiciones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun TermsSection(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.25,
        )
    }
}

/**
 * Diálogo modal para visualizar los Términos y Condiciones completos desde
 * el registro, login o perfil.
 */
@Composable
fun MvTermsDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.terms_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
            ) {
                MvTermsContent()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.profile_dialog_close))
            }
        },
    )
}
