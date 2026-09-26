package pe.modaviva.app.ui.screens.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.MvTextField

@Composable
fun RegisterScreen(
    onRegistered: (RegisterResult.Success) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.globalError) {
        val message = state.globalError
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onDismissGlobalError()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.register_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        MvTextField(
            value = state.nombres,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = viewModel::onNombresChange,
            label = stringResource(R.string.field_nombres),
            errorMessage = state.errorFor(RegisterField.NOMBRES),
            onFocusLost = { viewModel.onFieldTouched(RegisterField.NOMBRES) },
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
            errorMessage = state.errorFor(RegisterField.APELLIDOS),
            onFocusLost = { viewModel.onFieldTouched(RegisterField.APELLIDOS) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )

        MvTextField(
            value = state.documento,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = viewModel::onDocumentoChange,
            label = stringResource(R.string.field_documento),
            errorMessage = state.errorFor(RegisterField.DOCUMENTO),
            onFocusLost = { viewModel.onFieldTouched(RegisterField.DOCUMENTO) },
            supportingText = stringResource(R.string.support_documento),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        )

        MvTextField(
            value = state.telefono,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = viewModel::onTelefonoChange,
            label = stringResource(R.string.field_telefono),
            errorMessage = state.errorFor(RegisterField.TELEFONO),
            onFocusLost = { viewModel.onFieldTouched(RegisterField.TELEFONO) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
        )

        MvTextField(
            value = state.email,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.field_email),
            errorMessage = state.errorFor(RegisterField.EMAIL),
            onFocusLost = { viewModel.onFieldTouched(RegisterField.EMAIL) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )

        MvTextField(
            value = state.password,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.field_password),
            errorMessage = state.errorFor(RegisterField.PASSWORD),
            onFocusLost = { viewModel.onFieldTouched(RegisterField.PASSWORD) },
            supportingText = stringResource(R.string.support_password),
            visualTransformation = if (state.showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            trailingIcon = {
                IconButton(onClick = viewModel::onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (state.showPassword) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = stringResource(R.string.cd_toggle_password),
                    )
                }
            },
        )

        MvTextField(
            value = state.confirmPassword,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = viewModel::onConfirmPasswordChange,
            label = stringResource(R.string.field_confirm_password),
            errorMessage = state.errorFor(RegisterField.CONFIRM_PASSWORD),
            onFocusLost = { viewModel.onFieldTouched(RegisterField.CONFIRM_PASSWORD) },
            visualTransformation = if (state.showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        )

        Button(
            onClick = { viewModel.onSubmit(onRegistered) },
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(text = stringResource(R.string.register_cta))
            }
        }

        if (state.isCheckingEmail || state.isCheckingDocumento) {
            Text(
                text = stringResource(R.string.register_checking),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
