package com.wassaver.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wassaver.app.data.model.StatusFile
import com.wassaver.app.ui.screens.DirectChatScreen
import com.wassaver.app.ui.screens.HomeScreen
import com.wassaver.app.ui.screens.SavedScreen
import com.wassaver.app.ui.screens.StatusViewerScreen
import com.wassaver.app.ui.screens.ViewOnceScreen
import com.wassaver.app.ui.theme.WASSaverTheme
import com.wassaver.app.viewmodel.StatusViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WASSaverTheme {
                MainApp()
            }
        }
    }
}

enum class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    VIEW_ONCE("Media", Icons.Filled.PermMedia, Icons.Outlined.PermMedia),
    SAVED("Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    DIRECT_CHAT("Direct", Icons.AutoMirrored.Filled.Send, Icons.AutoMirrored.Outlined.Send)
}

@Composable
fun MainApp() {
    val viewModel: StatusViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Start View Once auto-save watcher when app is active
    DisposableEffect(Unit) {
        viewModel.viewOnceWatcher.startWatching()
        onDispose {
            viewModel.viewOnceWatcher.stopWatching()
        }
    }

    // Status viewer state
    var viewerStatuses by remember { mutableStateOf<List<StatusFile>?>(null) }
    var viewerInitialIndex by remember { mutableIntStateOf(0) }

    if (viewerStatuses != null) {
        // Full-screen status viewer
        StatusViewerScreen(
            statusFiles = viewerStatuses!!,
            initialIndex = viewerInitialIndex,
            viewModel = viewModel,
            onBack = { viewerStatuses = null }
        )
    } else {
        // Main app with bottom nav
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    BottomNavItem.entries.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onStatusClick = { _, index, statuses ->
                            viewerStatuses = statuses
                            viewerInitialIndex = index
                        }
                    )
                    1 -> ViewOnceScreen(
                        viewModel = viewModel,
                        onStatusClick = { _, index, statuses ->
                            viewerStatuses = statuses
                            viewerInitialIndex = index
                        }
                    )
                    2 -> SavedScreen(
                        viewModel = viewModel,
                        onStatusClick = { _, index, statuses ->
                            viewerStatuses = statuses
                            viewerInitialIndex = index
                        }
                    )
                    3 -> DirectChatScreen()
                }
            }
        }
    }
}
