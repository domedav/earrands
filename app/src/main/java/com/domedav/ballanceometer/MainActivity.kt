package com.domedav.ballanceometer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.domedav.ballanceometer.ui.config.ConfigScreen
import com.domedav.ballanceometer.ui.data.DataScreen
import com.domedav.ballanceometer.ui.show.ShowScreen
import com.domedav.ballanceometer.ui.theme.BallanceometerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("UNUSED_VARIABLE")
        val openAddDialog = intent?.getBooleanExtra(EXTRA_SHOW_ADD_DIALOG, false) ?: false
        intent?.removeExtra(EXTRA_SHOW_ADD_DIALOG)
        enableEdgeToEdge()
        setContent {
            BallanceometerTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var expanded by remember { mutableStateOf(true) }

                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            // lefele görgetésre zsugorodik, tetején felfele előjön
                            if (available.y < -6) expanded = false
                            else if (available.y > 6) expanded = true
                            return Offset.Zero
                        }
                    }
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.statusBars,
                    bottomBar = {
                        // Stack-ként, nem overlay element — Scaffold bottomBar része, tiszteletben tartja navbart
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .padding(top = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PillNav(
                                selectedTab = selectedTab,
                                onSelect = { selectedTab = it },
                                expanded = expanded,
                                onToggle = { expanded = !expanded },
                                onDrag = { dy ->
                                    if (dy > 24) expanded = false
                                    else if (dy < -24) expanded = true
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .nestedScroll(nestedScrollConnection)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -28) expanded = true
                                }
                            }
                    ) {
                        when (selectedTab) {
                            0 -> ShowScreen()
                            1 -> DataScreen()
                            2 -> ConfigScreen()
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

@androidx.compose.runtime.Composable
private fun PillNav(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDrag: (Float) -> Unit
) {
    val height by animateDpAsState(if (expanded) 68.dp else 28.dp, label = "pillHeight")
    Card(
        onClick = { onToggle() },
        modifier = Modifier
            .height(height)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount -> onDrag(dragAmount) }
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!expanded) {
                // zsugorított állapot — csak pill handle, mindig felhúzható
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(2.dp)
                        )
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        PillItem(
                            selected = selectedTab == 0,
                            onClick = { onSelect(0) },
                            filled = Icons.Filled.AccountBalanceWallet,
                            outlined = Icons.Outlined.AccountBalanceWallet
                        )
                        PillItem(
                            selected = selectedTab == 1,
                            onClick = { onSelect(1) },
                            filled = Icons.Filled.BarChart,
                            outlined = Icons.Outlined.BarChart
                        )
                        PillItem(
                            selected = selectedTab == 2,
                            onClick = { onSelect(2) },
                            filled = Icons.Filled.Settings,
                            outlined = Icons.Outlined.Settings
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun PillItem(
    selected: Boolean,
    onClick: () -> Unit,
    filled: androidx.compose.ui.graphics.vector.ImageVector,
    outlined: androidx.compose.ui.graphics.vector.ImageVector
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
    val tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = tint),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(56.dp, 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) filled else outlined,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
