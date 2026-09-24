package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.screens.common.AdminBottomNavigation
import com.example.stocksense.ui.theme.Background
import com.example.stocksense.ui.theme.Navy
import com.example.stocksense.ui.theme.Silver
import com.example.stocksense.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMoreScreen(
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
            AdminBottomNavigation(navController)
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
                Text("Business Operations", style = MaterialTheme.typography.labelMedium, color = Navy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                MoreMenuItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI Action Center",
                    subtitle = "Automated business decisions and priority alerts",
                    onClick = { navController.navigate(Screen.AdminActionCenter.route) }
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.ReceiptLong,
                    title = "Invoices & Documents",
                    subtitle = "Manage approved allocations and transfer documents",
                    onClick = { navController.navigate(Screen.AdminInvoices.route) }
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.LocalShipping,
                    title = "Delivery Tracking",
                    subtitle = "Track active transfers and simulate delays",
                    onClick = { navController.navigate(Screen.AdminDeliveryTracking.route) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Management Tools", style = MaterialTheme.typography.labelMedium, color = Navy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                MoreMenuItem(
                    icon = Icons.Default.Addchart,
                    title = "Data Input Center",
                    subtitle = "Manually add or bulk import products, stores and sales",
                    onClick = { navController.navigate(Screen.AdminDataInputCenter.route) }
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.Science,
                    title = "What-If Simulator",
                    subtitle = "Simulate promotion impacts and delivery delays",
                    onClick = { navController.navigate(Screen.AdminWhatIf.route) }
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.Assessment,
                    title = "Reports & Analytics",
                    subtitle = "View inventory summaries and stock performance",
                    onClick = { navController.navigate(Screen.AdminReports.route) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("System", style = MaterialTheme.typography.labelMedium, color = Navy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                MoreMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "Manage notifications, theme, and app data",
                    onClick = { navController.navigate(Screen.AdminSettings.route) }
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.Person,
                    title = "Profile",
                    subtitle = "Manage your account and view role details",
                    onClick = { navController.navigate(Screen.AdminProfile.route) }
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
                    subtitle = "End your current session safely",
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

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = contentColor)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Silver)
        }
    }
}
