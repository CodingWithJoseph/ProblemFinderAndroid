package com.example.problemfinder.app.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.problemfinder.app.ApplicationState
import com.example.problemfinder.app.R
import com.example.problemfinder.core.designsystem.R as DesignSystemR

/**
 * Shared bars and snackbar host. Navigation supplies route-derived visibility and callbacks;
 * the only local state is menu expansion, while sign-out progress comes from ApplicationState.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmensityScaffold(
    content: @Composable (PaddingValues) -> Unit,
    state: ApplicationState,
    snackbarHost: SnackbarHostState,
    showAppChrome: Boolean,
    selectedTab: MainTab?,
    onTabSelected: (MainTab) -> Unit,
    onSettingsClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.background,
        contentColor = colorScheme.onBackground,
        snackbarHost = {
            SnackbarHost(snackbarHost)
        },
        content = content,
        topBar = {
            if(showAppChrome){
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.background,
                        scrolledContainerColor = colorScheme.background,
                        titleContentColor = colorScheme.onBackground,
                        navigationIconContentColor = colorScheme.onBackground,
                        actionIconContentColor = colorScheme.onBackground,
                    ),
                    title = {
                        Image(
                            painter = painterResource(DesignSystemR.drawable.logo_orange),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    navigationIcon = {
                        if (onNavigateBack != null) IconButton(onClick = onNavigateBack) {
                            Icon(painterResource(DesignSystemR.drawable.icon_arrow_back), stringResource(R.string.action_navigate_back))
                        }
                    },
                    actions = {
                        if(state.isSigningOut) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }else {
                            IconButton(
                                onClick = { isExpanded = true },
                                content = {
                                    Icon(
                                        painter = painterResource(DesignSystemR.drawable.icon_menu),
                                        contentDescription = stringResource(R.string.top_app_bar_nav_icon)
                                    )
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false },
                            content = {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_item_settings)) },
                                    enabled = !state.isSigningOut,
                                    onClick = {
                                        isExpanded = false
                                        onSettingsClicked()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(DesignSystemR.drawable.icon_settings),
                                            contentDescription = null
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_item_sign_out)) },
                                    enabled = !state.isSigningOut,
                                    onClick = {
                                        isExpanded = false
                                        onSignOutClicked()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(DesignSystemR.drawable.icon_sign_out),
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }
        },
        bottomBar = {
            if(showAppChrome && selectedTab != null){
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = colorScheme.background,
                    contentColor = colorScheme.onBackground,
                    tonalElevation = 0.dp,
                    content = {
                        MainTab.entries.forEach {
                            NavigationBarItem(
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = colorScheme.secondaryFixed,
                                    selectedIconColor = colorScheme.onSecondaryFixed,
                                    unselectedIconColor = colorScheme.onSurfaceVariant,
                                ),
                                selected = it == selectedTab,
                                onClick = { onTabSelected(it) },
                                enabled = !state.isSigningOut,
                                icon = {
                                    Icon(
                                        painter = painterResource(it.icon),
                                        contentDescription = stringResource(it.label)
                                    )
                                }
                            )
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
