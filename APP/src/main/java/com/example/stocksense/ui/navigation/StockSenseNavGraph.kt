package com.example.stocksense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.stocksense.data.model.UserRole
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.screens.LoginScreen
import com.example.stocksense.ui.screens.RegisterScreen
import com.example.stocksense.ui.screens.admin.*
import com.example.stocksense.ui.screens.store.*
import com.example.stocksense.ui.screens.common.*

@Composable
fun StockSenseNavGraph(
    viewModel: StockViewModel,
    navController: NavHostController = rememberNavController()
) {
    val currentUser by viewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Welcome.route) { WelcomeScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(viewModel, navController) }
        composable(Screen.Register.route) { RegisterScreen(viewModel, navController) }

        // Admin Routes
        composable(Screen.AdminDashboard.route) { AdminDashboardScreen(viewModel, navController) }
        composable(Screen.AdminActionCenter.route) { AdminActionCenterScreen(viewModel, navController) }
        composable(Screen.AdminInventory.route) { AdminInventoryScreen(viewModel, navController) }
        composable(Screen.AdminForecast.route) { AdminForecastScreen(viewModel, navController) }
        composable(Screen.AdminAllocation.route) { AllocationCenterScreen(viewModel, navController) }
        composable(Screen.AdminRequests.route) { StockRequestsScreen(viewModel, navController) }
        composable(Screen.AdminMore.route) { AdminMoreScreen(viewModel, navController) }
        composable(Screen.AdminReports.route) { ReportsScreen(viewModel, navController) }
        composable(Screen.AdminSettings.route) { SettingsScreen(viewModel, navController) }
        composable(Screen.AdminProfile.route) { ProfileScreen(viewModel, navController) }
        composable(Screen.AdminDataInputCenter.route) { DataInputCenterScreen(navController) }
        composable(Screen.AdminImportData.route) { AdminImportDataScreen(viewModel, navController) }
        composable(Screen.AdminStoreSetup.route) { AdminStoreSetupScreen(viewModel, navController) }
        composable(Screen.AdminAddProduct.route) { AddProductScreen(viewModel, navController) }
        composable(Screen.AdminWhatIf.route) { WhatIfSimulatorScreen(viewModel, navController) }
        composable(Screen.AdminInvoices.route) { AdminInvoicesScreen(viewModel, navController) }
        composable(Screen.AdminDeliveryTracking.route) { AdminDeliveryTrackingScreen(viewModel, navController) }
        
        composable(
            route = Screen.AdminProductDetail.route,
            arguments = listOf(
                navArgument("productId") { type = androidx.navigation.NavType.StringType },
                navArgument("storeId") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            ProductDetailScreen(productId, storeId, viewModel, navController)
        }

        composable(
            route = Screen.AdminInvoiceDetail.route,
            arguments = listOf(navArgument("invoiceId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            AdminInvoiceDetailScreen(invoiceId, viewModel, navController)
        }

        // Store Manager Routes
        composable(Screen.StoreHome.route) { StoreHomeScreen(viewModel, navController) }
        composable(Screen.StoreInventory.route) { StoreInventoryScreen(viewModel, navController) }
        composable(Screen.StoreForecast.route) { StoreForecastScreen(viewModel, navController) }
        composable(Screen.StoreRequests.route) { StoreRequestsScreen(viewModel, navController) }
        composable(Screen.StoreMore.route) { StoreMoreScreen(viewModel, navController) }
        composable(Screen.StoreSettings.route) { SettingsScreen(viewModel, navController) }
        composable(Screen.StoreProfile.route) { ProfileScreen(viewModel, navController) }
        composable(Screen.StoreInvoices.route) { StoreInvoicesScreen(viewModel, navController) }
        composable(Screen.StoreDeliveryTracking.route) { StoreDeliveryTrackingScreen(viewModel, navController) }
        
        composable(
            route = Screen.StoreProductDetail.route,
            arguments = listOf(
                navArgument("productId") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val storeId = currentUser?.storeId ?: ""
            ProductDetailScreen(productId, storeId, viewModel, navController)
        }

        composable(
            route = Screen.StoreInvoiceDetail.route,
            arguments = listOf(navArgument("invoiceId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            StoreInvoiceDetailScreen(invoiceId, viewModel, navController)
        }
    }
}
