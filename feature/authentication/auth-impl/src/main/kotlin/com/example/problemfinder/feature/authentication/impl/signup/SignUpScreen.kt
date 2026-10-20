package com.example.problemfinder.feature.authentication.impl.signup

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.problemfinder.core.designsystem.theme.ImmensityColors
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.feature.authentication.impl.AuthOperation
import com.example.problemfinder.feature.authentication.impl.R
import com.example.problemfinder.feature.authentication.impl.messageRes
import com.example.problemfinder.core.designsystem.R as DesignSystemR

/** Selects a layout for the supplied sign-up step; validation and step transitions belong to the ViewModel. */
@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    state: SignUpState = SignUpState(),
    onIntent: (SignUpIntent) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val window = LocalWindowInfo.current
    BackHandler(enabled = state.step == SignUpStep.PROFILE && !state.isLoading) {
        onIntent(SignUpIntent.PreviousClicked)
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
        ) {
            val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                maxWidth >= 560.dp
            when {
                landscape -> SignUpLandscape(state, onIntent)
                window.containerSize.height < 720 -> SignUpCompact(state, onIntent)
                else -> SignUpTall(state, onIntent)
            }
        }
    }
}

@Composable
private fun SignUpCompact(state: SignUpState, onIntent: (SignUpIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 440.dp)) {
            SignUpLogo(onOrange = false)
            Spacer(Modifier.height(48.dp))
            SignUpHeader(state.step, compact = true)
            Spacer(Modifier.height(28.dp))
            SignUpFields(state, onIntent)
            SignUpError(state)
            Spacer(Modifier.height(24.dp))
            SignUpAction(state, onIntent)
            SignUpSecondaryAction(state, onIntent)
        }
    }
}

@Composable
private fun SignUpTall(state: SignUpState, onIntent: (SignUpIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(ImmensityColors.Orange)) {
        Column(
            modifier = Modifier.fillMaxWidth().height(210.dp)
                .padding(horizontal = 28.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SignUpLogo(onOrange = true)
            SignUpHero(state.step)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 440.dp).weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    SignUpHeader(state.step, compact = false)
                    Spacer(Modifier.height(24.dp))
                    SignUpFields(state, onIntent)
                    SignUpError(state)
                }
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.widthIn(max = 440.dp)) {
                    SignUpAction(state, onIntent)
                    SignUpSecondaryAction(state, onIntent)
                }
            }
        }
    }
}

@Composable
private fun SignUpLandscape(state: SignUpState, onIntent: (SignUpIntent) -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(ImmensityColors.Orange)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SignUpLogo(onOrange = true)
            SignUpHero(state.step)
            SignUpSecondaryAction(state, onIntent, onOrange = true)
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SignUpFields(state, onIntent)
            SignUpError(state)
            Spacer(Modifier.height(16.dp))
            SignUpAction(state, onIntent)
        }
    }
}

@Composable
private fun SignUpLogo(onOrange: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(
                if (onOrange) DesignSystemR.drawable.logo_dark else DesignSystemR.drawable.logo_orange
            ),
            contentDescription = stringResource(DesignSystemR.string.logo_immensity),
            modifier = Modifier.size(23.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = stringResource(DesignSystemR.string.wordmark_immensity),
            color = if (onOrange) ImmensityColors.Ink else MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SignUpHero(step: SignUpStep) {
    Column {
        Text(
            text = stringResource(
                if (step == SignUpStep.CREDENTIALS) R.string.auth_signup_eyebrow
                else R.string.auth_signup_profile_eyebrow
            ).uppercase(),
            color = ImmensityColors.Ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                if (step == SignUpStep.CREDENTIALS) R.string.auth_signup_headline
                else R.string.auth_signup_profile_headline
            ),
            color = ImmensityColors.Ink,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SignUpHeader(step: SignUpStep, compact: Boolean) {
    Column {
        if (compact) {
            Text(
                text = stringResource(
                    if (step == SignUpStep.CREDENTIALS) R.string.auth_signup_eyebrow
                    else R.string.auth_signup_profile_eyebrow
                ).uppercase(),
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(18.dp))
        }
        Text(
            text = stringResource(
                if (step == SignUpStep.CREDENTIALS) R.string.auth_signup_title
                else R.string.auth_signup_profile_title
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                if (step == SignUpStep.CREDENTIALS) R.string.auth_signup_description
                else R.string.auth_signup_profile_description
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SignUpFields(state: SignUpState, onIntent: (SignUpIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (state.step) {
            SignUpStep.CREDENTIALS -> {
                SignUpField(
                    labelRes = R.string.auth_email_label,
                    hintRes = R.string.auth_email_hint,
                    value = state.email,
                    onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
                SignUpField(
                    labelRes = R.string.auth_password_label,
                    hintRes = R.string.auth_signup_password_hint,
                    value = state.password,
                    onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            SignUpStep.PROFILE -> {
                SignUpField(
                    labelRes = R.string.auth_first_name_label,
                    hintRes = R.string.auth_first_name_hint,
                    value = state.firstname,
                    onValueChange = { onIntent(SignUpIntent.FirstnameChanged(it)) },
                    imeAction = ImeAction.Next
                )
                SignUpField(
                    labelRes = R.string.auth_last_name_label,
                    hintRes = R.string.auth_last_name_hint,
                    value = state.lastname,
                    onValueChange = { onIntent(SignUpIntent.LastnameChanged(it)) }
                )
            }
        }
    }
}

@Composable
private fun SignUpField(
    @StringRes labelRes: Int,
    @StringRes hintRes: Int,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(stringResource(hintRes)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SignUpError(state: SignUpState) {
    state.error?.let { error ->
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(error.messageRes(AuthOperation.SIGN_UP)),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SignUpAction(state: SignUpState, onIntent: (SignUpIntent) -> Unit) {
    Button(
        onClick = {
            onIntent(
                if (state.step == SignUpStep.CREDENTIALS) SignUpIntent.ContinueClicked
                else SignUpIntent.Submit
            )
        },
        enabled = !state.isLoading,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(
                        if (state.step == SignUpStep.CREDENTIALS) R.string.auth_signup_continue
                        else R.string.auth_create_account
                    )
                )
                Icon(
                    painter = painterResource(DesignSystemR.drawable.icon_arrow_forward),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun SignUpSecondaryAction(
    state: SignUpState,
    onIntent: (SignUpIntent) -> Unit,
    onOrange: Boolean = false
) {
    TextButton(
        onClick = {
            onIntent(
                if (state.step == SignUpStep.CREDENTIALS) SignUpIntent.SignInClicked
                else SignUpIntent.PreviousClicked
            )
        },
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(
                if (state.step == SignUpStep.CREDENTIALS) R.string.auth_existing_account_sign_in
                else R.string.auth_signup_back_to_details
            ),
            color = if (onOrange) ImmensityColors.Ink else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(name = "Compact", widthDp = 360, heightDp = 640)
@Preview(name = "Compact Dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignUpCompactPreview() = ImmensityTheme { SignUpScreen() }

@Preview(name = "Tall", widthDp = 393, heightDp = 850)
@Preview(name = "Tall Dark", widthDp = 393, heightDp = 850, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignUpTallPreview() = ImmensityTheme { SignUpScreen() }

@Preview(name = "Profile", widthDp = 393, heightDp = 850)
@Preview(name = "Profile Compact", widthDp = 360, heightDp = 640)
@Composable
private fun SignUpProfilePreview() = ImmensityTheme {
    SignUpScreen(state = SignUpState(step = SignUpStep.PROFILE))
}

@Preview(
    name = "Landscape",
    device = "spec:width=393dp,height=850dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape"
)
@Composable
private fun SignUpLandscapePreview() = ImmensityTheme { SignUpScreen() }
