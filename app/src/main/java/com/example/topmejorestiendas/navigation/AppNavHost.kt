package com.example.topmejorestiendas.navigation

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.topmejorestiendas.feature.auth.ui.AuthViewModel
import com.example.topmejorestiendas.feature.auth.ui.AuthViewModelFactory
import com.example.topmejorestiendas.feature.auth.ui.LoginScreen
import com.example.topmejorestiendas.feature.auth.ui.RegisterScreen
import com.example.topmejorestiendas.feature.dashboard.ui.AddBusinessScreen
import com.example.topmejorestiendas.feature.dashboard.ui.AddBusinessViewModel
import com.example.topmejorestiendas.feature.dashboard.ui.AddBusinessViewModelFactory
import com.example.topmejorestiendas.feature.dashboard.ui.OwnerDashboardScreen
import com.example.topmejorestiendas.feature.dashboard.ui.OwnerDashboardViewModel
import com.example.topmejorestiendas.feature.dashboard.ui.OwnerDashboardViewModelFactory
import com.example.topmejorestiendas.feature.home.ui.HomeScreen
import com.example.topmejorestiendas.feature.profile.ui.EditProfileScreen
import com.example.topmejorestiendas.feature.profile.ui.ProfileScreen
import com.example.topmejorestiendas.feature.profile.ui.ProfileViewModel
import com.example.topmejorestiendas.feature.profile.ui.ProfileViewModelFactory

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "login"
) {
    val context = LocalContext.current.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(context))
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(context))
    val ownerDashboardViewModel: OwnerDashboardViewModel = viewModel(factory = OwnerDashboardViewModelFactory(context))
    val addBusinessViewModel: AddBusinessViewModel = viewModel(factory = AddBusinessViewModelFactory(context))

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate("dashboard") {
                        popUpTo("register") { inclusive = true }
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToBusiness = { businessId ->
                    navController.navigate("business_profile/$businessId")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }

        composable("dashboard") {
            OwnerDashboardScreen(
                viewModel = ownerDashboardViewModel,
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToAddBusiness = { navController.navigate("add_business") },
                onNavigateToBusinessDetail = { businessId -> 
                    navController.navigate("business_profile/$businessId") 
                }
            )
        }

        composable("add_business") {
            AddBusinessScreen(
                viewModel = addBusinessViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateToEditProfile = { navController.navigate("edit_profile") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) // Limpiar todo el stack
                    }
                }
            )
        }

        composable("edit_profile") {
            EditProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("business_profile/{businessId}") { backStackEntry ->
            val businessId = backStackEntry.arguments?.getString("businessId")
            com.example.topmejorestiendas.feature.business.ui.BusinessProfileScreen(
                businessId = businessId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
