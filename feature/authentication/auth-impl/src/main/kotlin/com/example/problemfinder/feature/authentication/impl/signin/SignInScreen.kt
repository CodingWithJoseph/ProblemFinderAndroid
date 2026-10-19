package com.example.problemfinder.feature.authentication.impl.signin

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
    name = "Compact Dark",
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Compact Landscape",
    device = "spec:width=360dp,height=640dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape"
)
@Composable
private fun SignInScreenCompactPreview() {
    ImmensityTheme {
        SignInScreen(state = SignInState(), onIntent = {})
    }
}

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
private fun SignInScreenStandardPreview() = ImmensityTheme {
    SignInScreen()
}


/** Renders the supplied form state and emits intents; responsive layouts share the same fields and actions. */
@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    state: SignInState = SignInState(),
    onIntent: (SignInIntent) -> Unit = {}
) = Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

    val configuration = LocalConfiguration.current
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
        val compact = configuration.screenHeightDp < 720
        val contentModifier = Modifier
            .align(Alignment.Center)
            .widthIn(max = 440.dp)
            .fillMaxSize()

        when {
            landscape -> SignInScreenLandscape(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.fillMaxSize()
            )

            compact -> SignInScreenCompact(
                state = state,
                onIntent = onIntent,
                scrollState = scrollState,
                modifier = contentModifier
            )

            else -> SignInScreenStandard(
                state = state,
                onIntent = onIntent,
                scrollState = scrollState,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun SignInScreenLandscape(
    modifier: Modifier,
    state: SignInState,
    onIntent: (SignInIntent) -> Unit
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
            SignInHeader()
            SignUpLink(onIntent)
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            SignInForm(state, onIntent)
            SignInFormFooter(state, onIntent)
            SignInButton(state.isLoading, onIntent)
        }
    }
}

@Composable
fun SignInScreenCompact(
    modifier: Modifier,
    scrollState: ScrollState,
    state: SignInState = SignInState(),
    onIntent: (SignInIntent) -> Unit = {}
) = Column(modifier = modifier
    .verticalScroll(scrollState)
    .padding(top = 24.dp, bottom = 8.dp)) {

    SignInMainContent(
        state = state,
        onIntent = onIntent,
        headerGap = 48.dp
    )

    Spacer(Modifier.height(16.dp))

    SignInActions(
        isLoading = state.isLoading,
        onIntent = onIntent
    )
}

@Composable
fun SignInScreenStandard(
    modifier: Modifier,
    scrollState: ScrollState,
    state: SignInState = SignInState(),
    onIntent: (SignInIntent) -> Unit = {}
) = Column(modifier = modifier.padding(top = 24.dp, bottom = 16.dp)) {

    Column(modifier = Modifier
        .weight(1f)
        .verticalScroll(scrollState)) {
        SignInMainContent(
            state = state,
            onIntent = onIntent,
            headerGap = 72.dp,
            formGap = 72.dp
        )
    }

    Spacer(Modifier.height(16.dp))

    SignInActions(
        isLoading = state.isLoading,
        onIntent = onIntent
    )
}

@Composable
private fun SignInMainContent(
    state: SignInState,
    onIntent: (SignInIntent) -> Unit,
    modifier: Modifier = Modifier,
    headerGap: Dp = 72.dp,
    formGap: Dp = 32.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AuthLogo()
        Spacer(Modifier.height(headerGap))
        SignInHeader()
        Spacer(Modifier.height(formGap))
        SignInForm(state = state, onIntent = onIntent)
        SignInFormFooter(state = state, onIntent = onIntent)
    }
}

@Composable
private fun SignInHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.auth_sign_in_greeting).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(R.string.auth_sign_in_headline),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.auth_sign_in_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SignInForm(
    state: SignInState,
    onIntent: (SignInIntent) -> Unit,
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
            onValueChange = { onIntent(SignInIntent.EmailChanged(it)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(25),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.auth_password_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = { onIntent(SignInIntent.PasswordChanged(it)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(25),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SignInActions(
    isLoading: Boolean,
    onIntent: (SignInIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SignInButton(isLoading = isLoading, onIntent = onIntent)
        SignUpLink(onIntent = onIntent)
    }
}

@Composable
private fun SignInButton(
    isLoading: Boolean,
    onIntent: (SignInIntent) -> Unit
) {
    Button(
        onClick = { onIntent(SignInIntent.Submit) },
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
                Text(stringResource(R.string.auth_sign_in))
                Icon(
                    painter = painterResource(DesignSystemR.drawable.icon_arrow_forward),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun SignUpLink(onIntent: (SignInIntent) -> Unit) {
    TextButton(onClick = { onIntent(SignInIntent.SignUpClicked) }, contentPadding = PaddingValues(0.dp)) {
        Text(stringResource(R.string.auth_new_user_create_account))
    }
}

@Composable
private fun SignInFormFooter(
    state: SignInState,
    onIntent: (SignInIntent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = { onIntent(SignInIntent.ForgotPasswordClicked) },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("${stringResource(R.string.auth_forgot_password)}?")
        }
    }

    state.error?.let { error ->
        Text(
            text = stringResource(error.messageRes(AuthOperation.SIGN_IN)),
            color = MaterialTheme.colorScheme.error
        )
    }
}
