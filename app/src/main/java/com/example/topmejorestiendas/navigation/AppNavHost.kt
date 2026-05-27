package com.example.topmejorestiendas.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.topmejorestiendas.feature.home.ui.HomeScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "home"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToBusiness = { businessId ->
                    navController.navigate("business_profile/$businessId")
                }
            )
        }
        
        composable("business_profile/{businessId}") { backStackEntry ->
            val businessId = backStackEntry.arguments?.getString("businessId")
            // Placeholder: BusinessProfileScreen(businessId)
        }
    }
}
