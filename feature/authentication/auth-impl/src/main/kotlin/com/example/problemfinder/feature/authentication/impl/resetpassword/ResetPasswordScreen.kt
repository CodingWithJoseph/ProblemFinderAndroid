package com.example.problemfinder.feature.authentication.impl.resetpassword

import android.content.res.Configuration
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.feature.authentication.impl.AuthOperation
import com.example.problemfinder.feature.authentication.impl.R
import com.example.problemfinder.feature.authentication.impl.AuthLogo
import com.example.problemfinder.feature.authentication.impl.messageRes
import com.example.problemfinder.core.designsystem.R as DesignSystemR

@Preview(
    name = "Compact",
    widthDp = 360,
    heightDp = 640
)
@Preview(
    name = "Tall",
    widthDp = 393,
    heightDp = 850
)
@Preview(
    name = "Tall Dark",
    widthDp = 393,
    heightDp = 850,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Tall Landscape",
    device = "spec:width=393dp,height=850dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape"
)
@Composable
private fun ResetPasswordScreenStandardPreview() = ImmensityTheme {
    ResetPasswordScreen()
}


@Composable
fun ResetPasswordScreen(
    modifier: Modifier = Modifier,
    state: ResetPasswordState = ResetPasswordState(),
    onIntent: (ResetPasswordIntent) -> Unit = {}
) = Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
    ) {

        val landscape = maxWidth >= 560.dp && maxWidth > maxHeight

        val contentModifier = Modifier
            .align(Alignment.Center)
            .widthIn(max = 440.dp)
            .fillMaxSize()

        when {
            landscape -> ResetPasswordScreenLandscape(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.fillMaxSize()
            )

            else -> ResetPasswordScreenStandard(
                state = state,
                onIntent = onIntent,
                scrollState = scrollState,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun ResetPasswordScreenLandscape(
    modifier: Modifier,
    state: ResetPasswordState,
    onIntent: (ResetPasswordIntent) -> Unit
) {
    Row(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            AuthLogo()
            ResetPasswordHeader()
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            ResetPasswordForm(state, onIntent)
            ResetPasswordFormFooter(state, onIntent)
        }
    }
}

@Composable
fun ResetPasswordScreenStandard(
    modifier: Modifier,
    scrollState: ScrollState,
    state: ResetPasswordState = ResetPasswordState(),
    onIntent: (ResetPasswordIntent) -> Unit = {}
) = Column(modifier = modifier.padding(top = 24.dp, bottom = 16.dp)) {

    Column(modifier = Modifier
        .weight(1f)
        .verticalScroll(scrollState)) {
        ResetPasswordMainContent(
            state = state,
            onIntent = onIntent,
            headerGap = 72.dp,
            formGap = 72.dp
        )
    }

    Spacer(Modifier.height(16.dp))

    ResetPasswordFormFooter(
        state = state,
        onIntent = onIntent
    )
}

@Composable
private fun ResetPasswordMainContent(
    state: ResetPasswordState,
    onIntent: (ResetPasswordIntent) -> Unit,
    modifier: Modifier = Modifier,
    headerGap: Dp = 72.dp,
    formGap: Dp = 32.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AuthLogo()
        Spacer(Modifier.height(headerGap))
        ResetPasswordHeader()
        Spacer(Modifier.height(formGap))
        ResetPasswordForm(state = state, onIntent = onIntent)
    }
}

@Composable
private fun ResetPasswordHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.auth_secure_your_account).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(R.string.auth_fresh_start),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.auth_choose_new_password),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResetPasswordForm(
    state: ResetPasswordState,
    onIntent: (ResetPasswordIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.auth_new_password_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = { onIntent(ResetPasswordIntent.PasswordChanged(it)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(25),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.auth_confirm_password_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.confirmationPassword,
            onValueChange = { onIntent(ResetPasswordIntent.ConfirmationPasswordChanged(it)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(25),
            modifier = Modifier.fillMaxWidth()
        )

        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes(AuthOperation.RESET_PASSWORD)),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ResetPasswordButton(
    isLoading: Boolean,
    onIntent: (ResetPasswordIntent) -> Unit
) {
    Button(
        onClick = { onIntent(ResetPasswordIntent.Submit) },
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.auth_update_password))
                Icon(
                    painter = painterResource(DesignSystemR.drawable.icon_arrow_forward),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun ResetPasswordFormFooter(
    state: ResetPasswordState,
    onIntent: (ResetPasswordIntent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {

        ResetPasswordButton(
            isLoading = state.isLoading,
            onIntent = onIntent
        )
    }
}
