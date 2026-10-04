package com.example.problemfinder.feature.authentication.impl

import android.content.res.Configuration
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.problemfinder.core.designsystem.theme.ImmensityColors
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
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
private fun AuthMessageScreenStandardPreview() = ImmensityTheme {
    AuthMessageScreen(messageRes = R.string.auth_signup_email_sent_message, onBackToSignIn = {})
}


@Composable
fun AuthMessageScreen(
    @StringRes messageRes: Int,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) = Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
    ) {

        val landscape = maxWidth >= 560.dp && maxWidth > maxHeight
        val compact = maxHeight < 720.dp
        val contentModifier = Modifier
            .align(Alignment.Center)
            .widthIn(max = 440.dp)
            .fillMaxSize()

        when {
            landscape -> AuthMessageScreenLandscape(
                messageRes = messageRes,
                onBackToSignIn = onBackToSignIn,
                modifier = Modifier.fillMaxSize()
            )

            compact -> AuthMessageScreen(
                messageRes = messageRes,
                onBackToSignIn = onBackToSignIn,
                headerGap = 128.dp,
                formGap = 54.dp,
                modifier = contentModifier
            )

            else -> AuthMessageScreen(
                messageRes = messageRes,
                onBackToSignIn = onBackToSignIn,
                headerGap = 128.dp,
                formGap = 54.dp,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun AuthMessageScreenLandscape(
    @StringRes messageRes: Int,
    modifier: Modifier,
    onBackToSignIn: () -> Unit
) {
    Row(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            AuthLogo()
            AuthMessageHeader(messageRes = messageRes)
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom
        ) {
            AuthMessageText()
            AuthMessageFormFooter(onBackToSignIn)
        }
    }
}

@Composable
private fun AuthMessageScreen(
    @StringRes messageRes: Int,
    modifier: Modifier,
    headerGap: Dp = 128.dp,
    formGap: Dp = 64.dp,
    onBackToSignIn: () -> Unit
) = Column(modifier = modifier.padding(top = 24.dp, bottom = 16.dp)) {

    Column(
        modifier = Modifier
            .weight(1f)
    ) {
        AuthMessageMainContent(
            messageRes = messageRes,
            headerGap = headerGap,
            formGap = formGap
        )
    }

    Spacer(Modifier.height(16.dp))

    AuthMessageFormFooter(
        onBackToSignIn = onBackToSignIn
    )
}

@Composable
private fun AuthMessageMainContent(
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
    headerGap: Dp = 154.dp,
    formGap: Dp = 64.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AuthLogo()
        Spacer(Modifier.height(headerGap))
        AuthMessageHeader(messageRes = messageRes)
        Spacer(Modifier.height(formGap))
        AuthMessageText()
    }
}

@Composable
private fun AuthMessageHeader(@StringRes messageRes: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AuthAirplaneIcon()

        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(R.string.auth_one_more_step).uppercase(),
            style = TextStyle(letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(R.string.auth_check_your_inbox),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AuthMessageText() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = ImmensityColors.Muted.copy(alpha = .10f),
                shape = RoundedCornerShape(25)
            )
            .padding(vertical = 15.dp, horizontal = 15.dp)
    ) {
        Text(
            stringResource(R.string.auth_link_arrival_time),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AuthMessageButton(onBackToSignIn: () -> Unit) {
    Button(
        onClick = { onBackToSignIn() },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.auth_back_to_sign_in))
            Icon(
                painter = painterResource(DesignSystemR.drawable.icon_arrow_forward),
                contentDescription = stringResource(DesignSystemR.string.icon_forward_arrow)
            )
        }
    }
}

@Composable
private fun AuthMessageFormFooter(onBackToSignIn: () -> Unit) {
    Column {
        AuthMessageButton(onBackToSignIn = onBackToSignIn)
        Spacer(modifier = Modifier.size(12.dp))

    }
}
