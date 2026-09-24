package com.example.stocksense.ui.screens.admin

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
import com.example.stocksense.ui.screens.common.AdminBottomNavigation
import com.example.stocksense.ui.screens.common.KpiCard
import com.example.stocksense.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val actionItems by viewModel.actionItems.collectAsState()
    val inventoryList by viewModel.inventory.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val products by viewModel.products.collectAsState()
    val transfers by viewModel.transfers.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    
    val totalStock = inventoryList.sumOf { it.currentStock }
    val lowStockCount = inventoryList.count { viewModel.getRisk(it.productId, it.storeId).status == StockStatus.LOW_STOCK }
    val criticalRiskCount = actionItems.count { it.severity == ActionSeverity.CRITICAL }
    val deliveryRiskCount = actionItems.count { it.severity == ActionSeverity.DELIVERY_RISK }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StockSense Intelligence", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Silver
                ),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AdminActionCenter.route) }) {
                        BadgedBox(badge = { if(actionItems.isNotEmpty()) Badge { Text(actionItems.size.toString()) } }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Actions", tint = Silver)
                        }
                    }
                    IconButton(onClick = { navController.navigate(Screen.AdminProfile.route) }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Silver)
                    }
                }
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
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Inventory Command",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiCard("Total Stock", totalStock.toString(), InfoBlue, Modifier.weight(1f))
                    KpiCard("Critical Risks", criticalRiskCount.toString(), CriticalRed, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiCard("Delivery Risks", deliveryRiskCount.toString(), Color(0xFFE67E22), Modifier.weight(1f))
                    KpiCard("Active Transfers", transfers.count { it.status == TransferStatus.IN_TRANSIT || it.status == TransferStatus.DELAYED }.toString(), SuccessGreen, Modifier.weight(1f))
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
                        text = "AI Action Center",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnBackground
                    )
                    TextButton(onClick = { navController.navigate(Screen.AdminActionCenter.route) }) {
                        Text("View All")
                    }
                }
            }
            
            if (actionItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Text(
                            "No immediate actions required. System is stable.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftBlue
                        )
                    }
                }
            } else {
                items(actionItems.take(3)) { action ->
                    DashboardActionCard(action) {
                        when (action.actionType) {
                            "REVIEW_ALLOCATION" -> {
                                if (action.productId != null) {
                                    viewModel.prepareAllocation(action.productId)
                                    navController.navigate(Screen.AdminAllocation.route)
                                }
                            }
                            "CREATE_TRANSFER" -> navController.navigate(Screen.AdminInventory.route)
                            "REVIEW_DELIVERY", "TRACK_DELIVERY" -> navController.navigate(Screen.AdminDeliveryTracking.route)
                            "REVIEW_REQUEST" -> navController.navigate(Screen.AdminRequests.route)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Quick Operations",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OperationButton(
                        icon = Icons.Default.ReceiptLong,
                        label = "Invoices",
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.AdminInvoices.route) }
                    )
                    OperationButton(
                        icon = Icons.Default.LocalShipping,
                        label = "Tracking",
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.AdminDeliveryTracking.route) }
                    )
                    OperationButton(
                        icon = Icons.Default.Addchart,
                        label = "Import",
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.AdminDataInputCenter.route) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { navController.navigate(Screen.AdminAllocation.route) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OPTIMIZE ALLOCATION", fontWeight = FontWeight.Bold)
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun DashboardActionCard(action: ActionItem, onClick: () -> Unit) {
    val color = when(action.severity) {
        ActionSeverity.CRITICAL -> CriticalRed
        ActionSeverity.DELIVERY_RISK -> Color(0xFFE67E22)
        ActionSeverity.WATCH -> Color(0xFFF1C40F)
        else -> InfoBlue
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(action.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(action.problem, style = MaterialTheme.typography.bodySmall, color = SoftBlue)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Silver)
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
