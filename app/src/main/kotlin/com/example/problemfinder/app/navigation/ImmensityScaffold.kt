package com.example.problemfinder.app.navigation

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
    onSignOutClicked: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }


    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHost)
        },
        content = content,
        topBar = {
            if(showAppChrome){
                TopAppBar(
                    title = { Text(text = stringResource(R.string.app_name)) },
                    navigationIcon = {},
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
                                        painter = painterResource(R.drawable.icon_menu),
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
                                            painter = painterResource(R.drawable.icon_settings),
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
                                            painter = painterResource(R.drawable.icon_sign_out),
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
                    content = {
                        MainTab.entries.forEach {
                            NavigationBarItem(
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
