package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.stocksense.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActionCenterScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val actionItems by viewModel.actionItems.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val products by viewModel.products.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Action Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Silver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Silver
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            if (actionItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = SuccessGreen.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No actions required.",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftBlue
                        )
                        Text(
                            "Everything is operating within normal parameters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftBlue
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Recommended Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(actionItems) { action ->
                        ActionCard(
                            action = action,
                            storeName = stores.find { it.id == action.storeId }?.name,
                            productName = products.find { it.id == action.productId }?.name,
                            onActionClick = {
                                when (action.actionType) {
                                    "REVIEW_ALLOCATION" -> {
                                        if (action.productId != null) {
                                            viewModel.prepareAllocation(action.productId)
                                            navController.navigate(Screen.AdminAllocation.route)
                                        }
                                    }
                                    "CREATE_TRANSFER" -> navController.navigate(Screen.AdminInventory.route)
                                    "REVIEW_DELIVERY", "TRACK_DELIVERY" -> navController.navigate(Screen.AdminDeliveryTracking.route)
                                    "VIEW_FORECAST" -> navController.navigate(Screen.AdminForecast.route)
                                    "REVIEW_REQUEST" -> navController.navigate(Screen.AdminRequests.route)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    action: ActionItem,
    storeName: String?,
    productName: String?,
    onActionClick: () -> Unit
) {
    val severityColor = when (action.severity) {
        ActionSeverity.CRITICAL -> CriticalRed
        ActionSeverity.DELIVERY_RISK -> Color(0xFFE67E22) // Orange
        ActionSeverity.WATCH -> Color(0xFFF1C40F) // Yellow
        ActionSeverity.WARNING -> Color(0xFF3498DB) // Blue
        ActionSeverity.INFO -> Color(0xFF95A5A6) // Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = severityColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = action.severity.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = severityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "ID: ${action.id.takeLast(6)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            
            if (storeName != null || productName != null) {
                Text(
                    text = "${storeName ?: ""} ${if (storeName != null && productName != null) "—" else ""} ${productName ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = action.problem,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reason", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text(action.reason, style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Impact", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text(action.impact, style = MaterialTheme.typography.bodySmall, color = if (action.severity == ActionSeverity.CRITICAL) CriticalRed else Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = Silver.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = severityColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = action.recommendation,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Navy,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when (action.actionType) {
                        "REVIEW_ALLOCATION" -> "REVIEW ALLOCATION"
                        "CREATE_TRANSFER" -> "MANAGE TRANSFERS"
                        "REVIEW_DELIVERY" -> "REVIEW DELIVERY"
                        "TRACK_DELIVERY" -> "TRACK DELIVERY"
                        "VIEW_FORECAST" -> "VIEW FORECAST"
                        "REVIEW_REQUEST" -> "REVIEW REQUEST"
                        else -> "TAKE ACTION"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
