package pe.modaviva.app.ui.auth.login

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.R
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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

        Button(
            onClick = { viewModel.onSubmit(onLoggedIn) },
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
                Text(text = stringResource(R.string.login_cta))
            }
        }

        OutlinedButton(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.login_register_cta))
        }

        MvFormErrors(
            message = state.globalError,
            onMessageShown = viewModel::onDismissError,
        )
    }
}
