package pe.modaviva.app.ui.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.modaviva.app.R
import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.ui.auth.GoogleSignInHelper
import pe.modaviva.app.ui.components.MvFormErrors
import pe.modaviva.app.ui.components.MvGoogleButton
import pe.modaviva.app.ui.components.MvPasswordField
import pe.modaviva.app.ui.components.MvPrimaryButton
import pe.modaviva.app.ui.components.MvTextButton
import pe.modaviva.app.ui.components.MvTextField

@Composable
fun RegisterScreen(
    onRegistered: (RegisterResult.Success) -> Unit,
    onLoginClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                12.dp,
                Alignment.CenterVertically,
            ),
        ) {
            // Botón superior de retroceso
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            // Botón de Google Sign-In (exige aceptación de T&C - HU-01 CA-10)
            MvGoogleButton(
                onClick = {
                    if (!state.aceptaTerminos) {
                        viewModel.onGoogleSignInError(context.getString(R.string.register_terms_error))
                        return@MvGoogleButton
                    }
                    scope.launch {
                        GoogleSignInHelper.getGoogleIdToken(context)
                            .onSuccess { idToken ->
                                viewModel.onGoogleSignIn(idToken, onRegistered)
                            }
                            .onFailure { error ->
                                if (error !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                                    viewModel.onGoogleSignInError(error.localizedMessage)
                                }
                            }
                    }
                },
                loading = state.isGoogleSubmitting,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            // Separador
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.auth_or_divider),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            MvTextField(
                value = state.nombres,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = viewModel::onNombresChange,
                label = stringResource(R.string.field_nombres),
                errorMessage = state.errorFor(AuthFormField.NOMBRES),
                onFocusLost = { viewModel.onFieldTouched(AuthFormField.NOMBRES) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )

            MvTextField(
                value = state.apellidos,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = viewModel::onApellidosChange,
                label = stringResource(R.string.field_apellidos),
                errorMessage = state.errorFor(AuthFormField.APELLIDOS),
                onFocusLost = { viewModel.onFieldTouched(AuthFormField.APELLIDOS) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )

            MvTextField(
                value = state.email,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = viewModel::onEmailChange,
                label = stringResource(R.string.field_email),
                errorMessage = state.errorFor(AuthFormField.EMAIL),
                onFocusLost = { viewModel.onFieldTouched(AuthFormField.EMAIL) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            MvPasswordField(
                value = state.password,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = viewModel::onPasswordChange,
                label = stringResource(R.string.field_password),
                isVisible = state.showPassword,
                onToggleVisibility = viewModel::onTogglePasswordVisibility,
                errorMessage = state.errorFor(AuthFormField.PASSWORD),
                onFocusLost = { viewModel.onFieldTouched(AuthFormField.PASSWORD) },
                supportingText = stringResource(R.string.support_password),
            )

            MvPasswordField(
                value = state.confirmPassword,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = viewModel::onConfirmPasswordChange,
                label = stringResource(R.string.field_confirm_password),
                isVisible = state.showPassword,
                errorMessage = state.errorFor(AuthFormField.CONFIRM_PASSWORD),
                onFocusLost = { viewModel.onFieldTouched(AuthFormField.CONFIRM_PASSWORD) },
                imeAction = ImeAction.Done,
            )

            // Checkbox de Términos y Condiciones (HU-01 CA-10)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Checkbox(
                    checked = state.aceptaTerminos,
                    onCheckedChange = viewModel::onTermsChange,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.register_terms_checkbox),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            MvPrimaryButton(
                text = stringResource(R.string.register_cta),
                onClick = { viewModel.onSubmit(onRegistered) },
                enabled = state.canSubmit,
                loading = state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )

            MvTextButton(
                text = stringResource(R.string.register_login_cta),
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
            )

            MvFormErrors(
                message = state.globalError,
                onMessageShown = viewModel::onDismissGlobalError,
            )
        }
    }
}
