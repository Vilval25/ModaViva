package pe.modaviva.app.ui.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
fun LoginScreen(
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit = {},
    onLoggedIn: (LoginResult.Success) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
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
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            // Botón de Google Sign-In
            MvGoogleButton(
                onClick = {
                    scope.launch {
                        GoogleSignInHelper.getGoogleIdToken(context)
                            .onSuccess { idToken ->
                                viewModel.onGoogleSignIn(idToken, onLoggedIn)
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
                imeAction = ImeAction.Done,
            )

            MvPrimaryButton(
                text = stringResource(R.string.login_cta),
                onClick = { viewModel.onSubmit(onLoggedIn) },
                enabled = state.canSubmit,
                loading = state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )

            MvTextButton(
                text = stringResource(R.string.login_register_cta),
                onClick = onRegisterClick,
                modifier = Modifier.fillMaxWidth(),
            )

            MvFormErrors(
                message = state.globalError,
                onMessageShown = viewModel::onDismissError,
            )
        }
    }
}
