package pe.modaviva.app.ui.auth.login

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.MvPrimaryButton
import pe.modaviva.app.ui.components.MvSecondaryButton
import pe.modaviva.app.domain.model.AuthFormField
import pe.modaviva.app.ui.components.MvFormErrors
import pe.modaviva.app.ui.components.MvPasswordField
import pe.modaviva.app.ui.components.MvTextField

@Composable
fun LoginScreen(
    onRegisterClick: () -> Unit,
    onLoggedIn: (LoginResult.Success) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
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
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
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
            imeAction = ImeAction.Done,
        )

        MvPrimaryButton(
            text = stringResource(R.string.login_cta),
            onClick = { viewModel.onSubmit(onLoggedIn) },
            enabled = state.canSubmit,
            loading = state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        MvSecondaryButton(
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
