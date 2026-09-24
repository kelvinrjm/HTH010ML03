package com.example.stocksense.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stocksense.data.model.*
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.screens.admin.DashboardActionCard
import com.example.stocksense.ui.screens.common.KpiCard
import com.example.stocksense.ui.screens.common.StoreBottomNavigation
import com.example.stocksense.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreHomeScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val actionItems by viewModel.actionItems.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val transfers by viewModel.transfers.collectAsState()
    
    val storeId = currentUser?.storeId ?: ""
    val store = stores.find { it.id == storeId }
    
    val storeActionItems = actionItems.filter { it.storeId == storeId }
    val activeTransfers = transfers.filter { it.toStoreId == storeId && it.status != TransferStatus.RECEIVED }
    
    val storeInventory = inventory.filter { it.storeId == storeId }
    val healthyCount = storeInventory.count { viewModel.getRisk(it.productId, it.storeId).status == StockStatus.HEALTHY }
    val stockoutRiskCount = storeInventory.count { 
        val risk = viewModel.getRisk(it.productId, it.storeId)
        risk.status == StockStatus.CRITICAL || risk.status == StockStatus.OUT_OF_STOCK || risk.status == StockStatus.DELIVERY_GAP
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(store?.name ?: "Store Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Location: ${store?.city ?: "Unknown"}", style = MaterialTheme.typography.labelSmall, color = Silver.copy(alpha = 0.7f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Silver
                ),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.StoreProfile.route) }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Silver)
                    }
                }
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
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Store Inventory Status",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiCard("Healthy Stock", "$healthyCount / ${storeInventory.size}", SuccessGreen, Modifier.weight(1f))
                    KpiCard("Stockout Risks", stockoutRiskCount.toString(), CriticalRed, Modifier.weight(1f))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Store Actions",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnBackground
                    )
                    if (storeActionItems.isNotEmpty()) {
                        Surface(
                            color = CriticalRed,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${storeActionItems.size} Alerts",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            if (storeActionItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Text(
                            "No immediate actions required for your store.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftBlue
                        )
                    }
                }
            } else {
                items(storeActionItems) { action ->
                    DashboardActionCard(action) {
                        when (action.actionType) {
                            "REVIEW_REQUEST" -> navController.navigate(Screen.StoreRequests.route)
                            "TRACK_DELIVERY" -> navController.navigate(Screen.StoreDeliveryTracking.route)
                            "VIEW_FORECAST" -> navController.navigate(Screen.StoreForecast.route)
                            else -> navController.navigate(Screen.StoreInventory.route)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Incoming Deliveries",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnBackground
                    )
                    TextButton(onClick = { navController.navigate(Screen.StoreDeliveryTracking.route) }) {
                        Text("Track All")
                    }
                }
            }

            if (activeTransfers.isEmpty()) {
                item {
                    Text("No incoming shipments currently scheduled.", color = SoftBlue, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(activeTransfers.take(2)) { transfer ->
                    val product = viewModel.products.value.find { it.id == transfer.productId }
                    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.StoreDeliveryTracking.route) },
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Silver)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalShipping, 
                                contentDescription = null, 
                                tint = if(transfer.status == TransferStatus.DELAYED) CriticalRed else InfoBlue
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product?.name ?: "Unknown Product", fontWeight = FontWeight.Bold)
                                Text("${transfer.quantity} Units from Warehouse", style = MaterialTheme.typography.bodySmall, color = SoftBlue)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(dateFormat.format(Date(transfer.expectedDeliveryDate)), fontWeight = FontWeight.Bold, color = Navy)
                                Text("Expected", fontSize = 10.sp, color = SoftBlue)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OperationButton(
                        icon = Icons.Default.AddShoppingCart,
                        label = "New Request",
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.StoreRequests.route) }
                    )
                    OperationButton(
                        icon = Icons.Default.ReceiptLong,
                        label = "Invoices",
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.StoreInvoices.route) }
                    )
                    OperationButton(
                        icon = Icons.Default.History,
                        label = "History",
                        modifier = Modifier.weight(1f),
                        onClick = { /* History route */ }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun OperationButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Silver)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = Navy)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Navy)
        }
    }
}
