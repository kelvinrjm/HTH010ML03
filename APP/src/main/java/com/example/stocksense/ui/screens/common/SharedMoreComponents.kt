package com.example.stocksense.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stocksense.data.model.StockStatus
import com.example.stocksense.data.model.UserRole
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: StockViewModel, navController: NavController) {
    val inventory by viewModel.inventory.collectAsState()
    val products by viewModel.products.collectAsState()
    val requests by viewModel.requests.collectAsState()

    val totalStock = inventory.sumOf { it.currentStock }
    val lowStockCount = inventory.count { viewModel.getRisk(it.productId, it.storeId).status == StockStatus.LOW_STOCK }
    val stockoutRiskCount = inventory.count { viewModel.getRisk(it.productId, it.storeId).status == StockStatus.CRITICAL }
    val overstockCount = inventory.count { viewModel.getRisk(it.productId, it.storeId).status == StockStatus.OVERSTOCK }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Silver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Silver)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Inventory Analytics Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Overall Stock Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Units in Network:")
                            Text("$totalStock", fontWeight = FontWeight.Bold, color = InfoBlue)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Low Stock Items:")
                            Text("$lowStockCount", fontWeight = FontWeight.Bold, color = WarningAmber)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Critical Stockout Risks:")
                            Text("$stockoutRiskCount", fontWeight = FontWeight.Bold, color = CriticalRed)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Overstocked Items:")
                            Text("$overstockCount", fontWeight = FontWeight.Bold, color = SoftBlue)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Fulfillment & Requests", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Stock Requests:")
                            Text("${requests.size}", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pending Approval:")
                            Text("${requests.count { it.status == com.example.stocksense.data.model.RequestStatus.PENDING }}", fontWeight = FontWeight.Bold, color = WarningAmber)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: StockViewModel, navController: NavController) {
    var notificationEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("English") }
    var isDarkTheme by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Silver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Silver)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Alerts & Notifications", fontWeight = FontWeight.Medium)
                        Switch(checked = notificationEnabled, onCheckedChange = { notificationEnabled = it })
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Theme Mode (Dark Theme)", fontWeight = FontWeight.Medium)
                        Switch(checked = isDarkTheme, onCheckedChange = { isDarkTheme = it })
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.resetDemoData()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RESET DEMO DATA", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "StockSense Prototype v1.0.0\nStatus: Fully Functional UI Prototype Mode",
                style = MaterialTheme.typography.bodySmall,
                color = SoftBlue,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: StockViewModel, navController: NavController) {
    val currentUser by viewModel.currentUser.collectAsState()
    val stores by viewModel.stores.collectAsState()

    val assignedStore = stores.find { it.id == currentUser?.storeId }?.name ?: "All Stores (Global Network)"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Silver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Silver)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = Navy,
                modifier = Modifier.size(100.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(currentUser?.name ?: "Unknown User", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(currentUser?.email ?: "N/A", style = MaterialTheme.typography.bodyMedium, color = SoftBlue)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Assigned Role:", fontWeight = FontWeight.Bold)
                        Text(if (currentUser?.role == UserRole.ADMIN) "Inventory Manager (Admin)" else "Store Manager")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Scope / Location:", fontWeight = FontWeight.Bold)
                        Text(assignedStore)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    viewModel.logout()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOG OUT FROM SESSION", fontWeight = FontWeight.Bold)
            }
        }
    }
}
