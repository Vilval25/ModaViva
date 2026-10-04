package pe.modaviva.app.ui.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.modaviva.app.R
import pe.modaviva.app.ui.components.MvEmptyState
import pe.modaviva.app.ui.components.MvPrimaryButton
import pe.modaviva.app.ui.components.MvScreen
import pe.modaviva.app.ui.components.MvSecondaryButton
import pe.modaviva.app.ui.session.SessionScreen
import pe.modaviva.app.ui.session.SessionViewModel

/**
 * Pestaña Perfil. Provisional hasta HU-02/HU-03: sin sesión ofrece iniciar
 * sesión o crear cuenta, y con sesión muestra la pantalla de sesión actual.
 */
@Composable
fun ProfileTab(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onOrdersClick: () -> Unit,
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val user by sessionViewModel.currentUser.collectAsStateWithLifecycle()

    MvScreen(title = stringResource(R.string.profile_title), modifier = modifier) { padding ->
        if (user == null) {
            MvEmptyState(
                icon = Icons.Outlined.AccountCircle,
                title = stringResource(R.string.profile_guest_title),
                message = stringResource(R.string.profile_guest_body),
                modifier = Modifier.padding(padding),
            ) {
                MvPrimaryButton(
                    text = stringResource(R.string.profile_guest_login),
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                MvSecondaryButton(
                    text = stringResource(R.string.profile_guest_register),
                    onClick = onRegisterClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            SessionScreen(
                onSignOut = {},
                onOrdersClick = onOrdersClick,
                modifier = Modifier.padding(padding),
                viewModel = sessionViewModel,
            )
        }
    }
}
