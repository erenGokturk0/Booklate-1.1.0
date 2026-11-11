package com.erengokturk.booklate.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.erengokturk.booklate.ui.history.HistoryRoute
import com.erengokturk.booklate.ui.library.LibraryRoute
import com.erengokturk.booklate.ui.navigation.Routes
import com.erengokturk.booklate.ui.reader.ReaderRoute
import com.erengokturk.booklate.ui.settings.SettingsRoute

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.LIBRARY, "Kütüphane", Icons.AutoMirrored.Outlined.MenuBook),
    TopLevelDestination(Routes.HISTORY, "Kelimeler", Icons.Outlined.History),
    TopLevelDestination(Routes.SETTINGS, "Ayarlar", Icons.Outlined.Settings),
)

@Composable
fun BooklateNavRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelDestinations.map { it.route }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                ) {
                    topLevelDestinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.LIBRARY,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.LIBRARY) {
                    LibraryRoute(
                        onOpenBook = { bookId -> navController.navigate(Routes.reader(bookId)) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryRoute(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
                }
                composable(Routes.SETTINGS) {
                    SettingsRoute()
                }
                composable(
                    route = Routes.READER,
                    arguments = listOf(navArgument(Routes.ARG_BOOK_ID) { type = NavType.StringType }),
                ) { entry ->
                    val bookId = entry.arguments?.getString(Routes.ARG_BOOK_ID).orEmpty()
                    ReaderRoute(
                        bookId = bookId,
                        onBack = { navController.popBackStack() },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
            }
        }
    }
}
