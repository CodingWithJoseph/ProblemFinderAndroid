package com.example.problemfinder.feature.authentication.impl.forgotpassword

import android.content.res.Configuration
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.feature.authentication.impl.AuthMailIcon
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
private fun ForgotPasswordScreenStandardPreview() = ImmensityTheme {
    ForgotPasswordScreen()
}


/** Renders the email request form; submission and navigation are expressed as intents to the caller. */
@Composable
fun ForgotPasswordScreen(
    modifier: Modifier = Modifier,
    state: ForgotPasswordState = ForgotPasswordState(),
    onIntent: (ForgotPasswordIntent) -> Unit = {}
) = Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

    val configuration = LocalConfiguration.current
    val window = LocalWindowInfo.current
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {

        val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            maxWidth >= 560.dp
        val compact = window.containerSize.height < 720
        val contentModifier = Modifier
            .align(Alignment.Center)
            .widthIn(max = 440.dp)
            .fillMaxSize()

        when {
            landscape -> ForgotPasswordScreenLandscape(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.fillMaxSize()
            )

            compact -> ForgotPasswordScreen(
                state = state,
                onIntent = onIntent,
                headerGap = 64.dp,
                scrollState = scrollState,
                modifier = contentModifier
            )

            else -> ForgotPasswordScreen(
                state = state,
                onIntent = onIntent,
                scrollState = scrollState,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun ForgotPasswordScreenLandscape(
    modifier: Modifier,
    state: ForgotPasswordState,
    onIntent: (ForgotPasswordIntent) -> Unit
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
            ForgotPasswordHeader()
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            ForgotPasswordForm(state, onIntent)
            ForgotPasswordFormFooter(state, onIntent)
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    modifier: Modifier,
    scrollState: ScrollState,
    headerGap: Dp = 128.dp,
    state: ForgotPasswordState = ForgotPasswordState(),
    onIntent: (ForgotPasswordIntent) -> Unit = {}
) = Column(modifier = modifier.padding(top = 24.dp, bottom = 16.dp)) {

    Column(modifier = Modifier
        .weight(1f)
        .verticalScroll(scrollState)) {
        ForgotPasswordMainContent(
            state = state,
            onIntent = onIntent,
            headerGap = headerGap,
            formGap = 32.dp
        )
    }

    Spacer(Modifier.height(16.dp))

    ForgotPasswordFormFooter(
        state = state,
        onIntent = onIntent
    )
}

@Composable
private fun ForgotPasswordMainContent(
    state: ForgotPasswordState,
    onIntent: (ForgotPasswordIntent) -> Unit,
    modifier: Modifier = Modifier,
    headerGap: Dp = 128.dp,
    formGap: Dp = 32.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AuthLogo()
        Spacer(Modifier.height(headerGap))
        ForgotPasswordHeader()
        Spacer(Modifier.height(formGap))
        ForgotPasswordForm(state = state, onIntent = onIntent)
    }
}

@Composable
private fun ForgotPasswordHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        AuthMailIcon()

        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(R.string.auth_account_recovery).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(R.string.auth_help_get_back_in),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.auth_enter_your_email_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ForgotPasswordForm(
    state: ForgotPasswordState,
    onIntent: (ForgotPasswordIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.auth_email_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = { onIntent(ForgotPasswordIntent.EmailChanged(it)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(25),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes(AuthOperation.FORGOT_PASSWORD)),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ForgotPasswordButton(
    isLoading: Boolean,
    onIntent: (ForgotPasswordIntent) -> Unit
) {
    Button(
        onClick = { onIntent(ForgotPasswordIntent.Submit) },
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
                Text(stringResource(R.string.auth_send_reset_email))
                Icon(
                    painter = painterResource(DesignSystemR.drawable.icon_arrow_forward),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun ForgotPasswordFormFooter(
    state: ForgotPasswordState,
    onIntent: (ForgotPasswordIntent) -> Unit
) {
    Column {

        ForgotPasswordButton(
            isLoading = state.isLoading,
            onIntent = onIntent
        )

        Spacer(modifier = Modifier.size(12.dp))
        BackToSignInLink(onIntent = onIntent)

    }
}

@Composable
private fun BackToSignInLink(onIntent: (ForgotPasswordIntent) -> Unit) {
    TextButton(onClick = { onIntent(ForgotPasswordIntent.SignInClicked) }, contentPadding = PaddingValues(0.dp)) {
        Text(stringResource(R.string.auth_remember_sign_in))
    }
}

