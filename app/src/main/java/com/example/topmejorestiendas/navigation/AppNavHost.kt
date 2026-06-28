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
import com.example.topmejorestiendas.feature.dashboard.ui.ManageBusinessScreen
import com.example.topmejorestiendas.feature.dashboard.ui.ManageBusinessViewModel
import com.example.topmejorestiendas.feature.dashboard.ui.ManageBusinessViewModelFactory
import com.example.topmejorestiendas.feature.dashboard.ui.EditBusinessScreen
import com.example.topmejorestiendas.feature.dashboard.ui.EditBusinessViewModel
import com.example.topmejorestiendas.feature.dashboard.ui.EditBusinessViewModelFactory
import com.example.topmejorestiendas.feature.dashboard.ui.BusinessReviewsScreen
import com.example.topmejorestiendas.feature.dashboard.ui.BusinessReviewsViewModel
import com.example.topmejorestiendas.feature.dashboard.ui.BusinessReviewsViewModelFactory
import com.example.topmejorestiendas.feature.home.ui.HomeScreen
import com.example.topmejorestiendas.feature.home.ui.StoreMapScreen
import com.example.topmejorestiendas.feature.home.ui.StoreMapViewModel
import com.example.topmejorestiendas.feature.home.ui.StoreMapViewModelFactory
import com.example.topmejorestiendas.feature.profile.ui.EditProfileScreen
import com.example.topmejorestiendas.feature.profile.ui.ProfileScreen
import com.example.topmejorestiendas.feature.profile.ui.ProfileViewModel
import com.example.topmejorestiendas.feature.profile.ui.ProfileViewModelFactory
import com.example.topmejorestiendas.feature.profile.ui.ReviewHistoryScreen
import com.example.topmejorestiendas.feature.profile.ui.ReviewHistoryViewModel
import com.example.topmejorestiendas.feature.profile.ui.ReviewHistoryViewModelFactory

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
    
    val manageBusinessViewModel: ManageBusinessViewModel = viewModel(factory = ManageBusinessViewModelFactory(context))
    val editBusinessViewModel: EditBusinessViewModel = viewModel(factory = EditBusinessViewModelFactory(context))
    val businessReviewsViewModel: BusinessReviewsViewModel = viewModel(factory = BusinessReviewsViewModelFactory(context))

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
            val homeViewModel: com.example.topmejorestiendas.feature.home.ui.HomeViewModel = viewModel(factory = com.example.topmejorestiendas.feature.home.ui.HomeViewModelFactory(context))
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToBusiness = { businessId ->
                    navController.navigate("business_profile/$businessId")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToMap = {
                    navController.navigate("store_map")
                }
            )
        }

        composable("store_map") {
            val storeMapViewModel: StoreMapViewModel = viewModel(factory = StoreMapViewModelFactory(context))
            StoreMapScreen(
                viewModel = storeMapViewModel,
                onNavigateToBusiness = { businessId ->
                    navController.navigate("business_profile/$businessId")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("dashboard") {
            OwnerDashboardScreen(
                viewModel = ownerDashboardViewModel,
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToAddBusiness = { navController.navigate("add_business") },
                onNavigateToBusinessDetail = { businessId -> 
                    navController.navigate("manage_business/$businessId") 
                },
                onNavigateToInbox = { navController.navigate("reservations_inbox") }
            )
        }

        composable("reservations_inbox") {
            val inboxViewModel: com.example.topmejorestiendas.feature.dashboard.ui.ReservationsInboxViewModel = viewModel(factory = com.example.topmejorestiendas.feature.dashboard.ui.ReservationsInboxViewModelFactory(context))
            com.example.topmejorestiendas.feature.dashboard.ui.ReservationsInboxScreen(
                viewModel = inboxViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("manage_business/{businessId}") { backStackEntry ->
            val businessId = backStackEntry.arguments?.getString("businessId") ?: ""
            ManageBusinessScreen(
                businessId = businessId,
                viewModel = manageBusinessViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditBusiness = { id -> navController.navigate("edit_business/$id") },
                onNavigateToReviews = { id -> navController.navigate("business_reviews/$id") }
            )
        }

        composable("edit_business/{businessId}") { backStackEntry ->
            val businessId = backStackEntry.arguments?.getString("businessId") ?: ""
            EditBusinessScreen(
                businessId = businessId,
                viewModel = editBusinessViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("business_reviews/{businessId}") { backStackEntry ->
            val businessId = backStackEntry.arguments?.getString("businessId") ?: ""
            BusinessReviewsScreen(
                businessId = businessId,
                viewModel = businessReviewsViewModel,
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToReviewHistory = { navController.navigate("review_history") },
                onNavigateToReservations = { navController.navigate("client_reservations") },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }

        composable("client_reservations") {
            val clientReservationsViewModel: com.example.topmejorestiendas.feature.profile.ui.ClientReservationsViewModel = viewModel(factory = com.example.topmejorestiendas.feature.profile.ui.ClientReservationsViewModelFactory(context))
            com.example.topmejorestiendas.feature.profile.ui.ClientReservationsScreen(
                viewModel = clientReservationsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("review_history") {
            val reviewHistoryViewModel: ReviewHistoryViewModel = viewModel(factory = ReviewHistoryViewModelFactory(context))
            ReviewHistoryScreen(
                viewModel = reviewHistoryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("edit_profile") {
            EditProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("business_profile/{businessId}") { backStackEntry ->
            val businessId = backStackEntry.arguments?.getString("businessId") ?: ""
            val businessProfileViewModel: com.example.topmejorestiendas.feature.business.ui.BusinessProfileViewModel = viewModel(factory = com.example.topmejorestiendas.feature.business.ui.BusinessProfileViewModelFactory(context, businessId))
            com.example.topmejorestiendas.feature.business.ui.BusinessProfileScreen(
                viewModel = businessProfileViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
