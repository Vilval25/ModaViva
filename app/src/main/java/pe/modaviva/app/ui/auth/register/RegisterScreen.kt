package pe.modaviva.app.ui.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.MvPrimaryButton
import pe.modaviva.app.ui.components.MvTextButton
import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.ui.components.MvFormErrors
import pe.modaviva.app.ui.components.MvPasswordField
import pe.modaviva.app.ui.components.MvTextField

@Composable
fun RegisterScreen(
    onRegistered: (RegisterResult.Success) -> Unit,
    onLoginClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(
                12.dp,
                Alignment.CenterVertically,
            ),
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
            value = state.documento,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = viewModel::onDocumentoChange,
            label = stringResource(R.string.field_documento),
            errorMessage = state.errorFor(AuthFormField.DOCUMENTO),
            onFocusLost = { viewModel.onFieldTouched(AuthFormField.DOCUMENTO) },
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
            errorMessage = state.errorFor(AuthFormField.TELEFONO),
            onFocusLost = { viewModel.onFieldTouched(AuthFormField.TELEFONO) },
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

        MvPrimaryButton(
            text = stringResource(R.string.register_cta),
            onClick = { viewModel.onSubmit(onRegistered) },
            enabled = state.canSubmit,
            loading = state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        if (state.isCheckingDocumento) {
            Text(
                text = stringResource(R.string.register_checking),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        MvTextButton(
            text = stringResource(R.string.register_login_cta),
            onClick = onLoginClick,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        MvFormErrors(
            message = state.globalError,
            onMessageShown = viewModel::onDismissGlobalError,
        )
        }
    }
}
