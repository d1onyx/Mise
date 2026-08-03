package com.d1onix.dishlab.feature.home.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.MiseScreenHeader
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.home.resources.Res
import com.d1onix.dishlab.feature.home.resources.auth_continue
import com.d1onix.dishlab.feature.home.resources.auth_email
import com.d1onix.dishlab.feature.home.resources.auth_email_error
import com.d1onix.dishlab.feature.home.resources.auth_failed
import com.d1onix.dishlab.feature.home.resources.auth_name
import com.d1onix.dishlab.feature.home.resources.auth_name_error
import com.d1onix.dishlab.feature.home.resources.auth_password
import com.d1onix.dishlab.feature.home.resources.auth_password_error
import com.d1onix.dishlab.feature.home.resources.auth_register
import com.d1onix.dishlab.feature.home.resources.auth_sign_in
import com.d1onix.dishlab.feature.home.resources.auth_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AuthContent(state, viewModel::onAction)
}

@Composable
internal fun AuthContent(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 20.dp).screenIn(),
    ) {
        MiseScreenHeader(
            title = stringResource(Res.string.auth_title),
            onBackClick = { onAction(AuthAction.BackClicked) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Spacer(Modifier.height(28.dp))
        AuthModeControl(
            selected = state.mode,
            onSelected = { onAction(AuthAction.ModeChanged(it)) },
        )
        Spacer(Modifier.height(28.dp))
        if (state.mode == AuthMode.Register) {
            AuthField(
                value = state.displayName,
                label = stringResource(Res.string.auth_name),
                onValueChange = { onAction(AuthAction.DisplayNameChanged(it)) },
                error = state.submitted && state.displayName.trim().length < 2,
                errorText = stringResource(Res.string.auth_name_error),
            )
            Spacer(Modifier.height(14.dp))
        }
        AuthField(
            value = state.email,
            label = stringResource(Res.string.auth_email),
            onValueChange = { onAction(AuthAction.EmailChanged(it)) },
            error = state.submitted && !state.emailValid,
            errorText = stringResource(Res.string.auth_email_error),
        )
        Spacer(Modifier.height(14.dp))
        AuthField(
            value = state.password,
            label = stringResource(Res.string.auth_password),
            onValueChange = { onAction(AuthAction.PasswordChanged(it)) },
            error = state.submitted && state.password.length < 6,
            errorText = stringResource(Res.string.auth_password_error),
            secure = true,
        )
        if (state.authenticationFailed) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(Res.string.auth_failed),
                style = MiseTheme.typography.bodySmall,
                color = MiseTheme.colors.red,
            )
        }
        Spacer(Modifier.weight(1f))
        MisePrimaryButton(
            text = stringResource(Res.string.auth_continue),
            onClick = { onAction(AuthAction.ContinueClicked) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        )
    }
}

@Composable
private fun AuthModeControl(selected: AuthMode, onSelected: (AuthMode) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().background(MiseTheme.colors.panel, shape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AuthMode.entries.forEach { mode ->
            val active = selected == mode
            Text(
                text = stringResource(
                    if (mode == AuthMode.SignIn) Res.string.auth_sign_in else Res.string.auth_register,
                ),
                style = MiseTheme.typography.body,
                textAlign = TextAlign.Center,
                color = if (active) MiseTheme.colors.onLime else MiseTheme.colors.textMuted,
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (active) MiseTheme.colors.lime else Color.Transparent, shape)
                    .clickable { onSelected(mode) }
                    .padding(vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    error: Boolean,
    errorText: String,
    secure: Boolean = false,
) {
    val colors = MiseTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MiseTheme.typography.monoSmall, color = colors.textMuted)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.merge(MiseTheme.typography.body).copy(color = colors.text),
            cursorBrush = SolidColor(colors.lime),
            visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.panel, RoundedCornerShape(8.dp))
                .border(1.dp, if (error) colors.red else colors.border, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
        )
        if (error) Text(errorText, style = MiseTheme.typography.bodySmall, color = colors.red)
    }
}
