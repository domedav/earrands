package com.domedav.ballanceometer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.domedav.ballanceometer.ui.config.ConfigScreen
import com.domedav.ballanceometer.ui.show.ShowScreen
import com.domedav.ballanceometer.ui.theme.BallanceometerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep handling for widget deep-link; currently ignored but preserved
        @Suppress("UNUSED_VARIABLE")
        val openAddDialog = intent?.getBooleanExtra(EXTRA_SHOW_ADD_DIALOG, false) ?: false
        intent?.removeExtra(EXTRA_SHOW_ADD_DIALOG)
        enableEdgeToEdge()
        setContent {
            BallanceometerTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                label = { Text(stringResource(R.string.show_tab)) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = null
                                    )
                                }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                label = { Text(stringResource(R.string.config_tab)) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> ShowScreen()
                            1 -> ConfigScreen()
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SHOW_ADD_DIALOG = "extra_show_add_dialog"
    }
}
