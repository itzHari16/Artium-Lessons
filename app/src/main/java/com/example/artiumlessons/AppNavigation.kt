package com.example.artiumlessons

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.artiumlessons.screens.*
import com.example.artiumlessons.viewmodel.LessonViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: LessonViewModel = hiltViewModel()
    val bottomNavItems = listOf(Screen.Lessons, Screen.Practice)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val shouldShowBottomBar =
                bottomNavItems.any { it.route == currentDestination?.route }

            if (shouldShowBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Lessons.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Lessons.route) {
                LessonListScreen(navController, viewModel)
            }
            composable(Screen.Practice.route) {
                PracticeHubScreen(viewModel = viewModel)
            }
            composable(
                route = "details/{title}",
                arguments = listOf(navArgument("title") { type = NavType.StringType })
            ) { backStackEntry ->
                val lessonTitle = backStackEntry.arguments?.getString("title") ?: ""
                DetailScreen(
                    lessonTitle = lessonTitle,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}


sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Lessons : Screen("lessons", "Lessons", Icons.Default.Home)
    object Practice : Screen("practice", "Practice", Icons.Default.ModelTraining)
}