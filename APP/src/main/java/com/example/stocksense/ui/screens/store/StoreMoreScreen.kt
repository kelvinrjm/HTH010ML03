package com.example.stocksense.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.screens.admin.MoreMenuItem
import com.example.stocksense.ui.screens.common.StoreBottomNavigation
import com.example.stocksense.ui.theme.Background
import com.example.stocksense.ui.theme.Navy
import com.example.stocksense.ui.theme.Silver
import com.example.stocksense.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreMoreScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More Options", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Silver
                )
            )
        },
        bottomBar = {
            StoreBottomNavigation(navController)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Store Documents & Shipments", style = MaterialTheme.typography.labelMedium, color = Navy, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                MoreMenuItem(
                    icon = Icons.Default.ReceiptLong,
                    title = "My Invoices",
                    subtitle = "View incoming or completed shipment invoices",
                    onClick = { navController.navigate(Screen.StoreInvoices.route) }
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.LocalShipping,
                    title = "Incoming Deliveries",
                    subtitle = "Track active transfers and confirm receipt",
                    onClick = { navController.navigate(Screen.StoreDeliveryTracking.route) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Account & System", style = MaterialTheme.typography.labelMedium, color = Navy, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                MoreMenuItem(
                    icon = Icons.Default.Person,
                    title = "Profile",
                    subtitle = "View your user profile and store assignment details",
                    onClick = { navController.navigate(Screen.StoreProfile.route) }
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "Manage preferences and clear prototype demo data",
                    onClick = { navController.navigate(Screen.StoreSettings.route) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.Logout,
                    title = "Log Out",
                    subtitle = "End your active store session",
                    onClick = {
                        viewModel.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    contentColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
