package com.example.stocksense.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.data.model.UserRole
import com.example.stocksense.data.model.Product
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    storeId: String,
    viewModel: StockViewModel,
    navController: NavController
) {
    val products by viewModel.products.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val forecasts by viewModel.forecasts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val product = products.find { it.id == productId }
    val store = stores.find { it.id == storeId }
    val inv = inventory.find { it.productId == productId && it.storeId == storeId }
    val forecast = forecasts.find { it.productId == productId && it.storeId == storeId }
    val risk = viewModel.getRisk(productId, storeId)

    var showRequestDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "Product Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Silver,
                    navigationIconContentColor = Silver
                )
            )
        }
    ) { padding ->
        if (product == null || store == null || inv == null || forecast == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Product data not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Inventory Summary - ${store.name}", style = MaterialTheme.typography.labelMedium, color = SoftBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                DetailItem("Current Stock", inv.currentStock.toString())
                                DetailItem("Incoming", inv.incomingStock.toString())
                                DetailItem("Coverage", "${risk.daysOfCoverage} Days")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Status: ${risk.status.name.replace("_", " ")}",
                                color = when(risk.status) {
                                    com.example.stocksense.data.model.StockStatus.HEALTHY -> SuccessGreen
                                    com.example.stocksense.data.model.StockStatus.CRITICAL -> CriticalRed
                                    else -> WarningAmber
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Navy),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Silver)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Demand Forecast", color = Silver, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Expected Demand", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                                    Text("${forecast.expectedDemand} Units", style = MaterialTheme.typography.headlineSmall, color = Silver, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Confidence", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                                    Text(forecast.confidence, color = if(forecast.confidence == "High") SuccessGreen else WarningAmber, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = InfoBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Why this forecast?", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            forecast.explanation.forEach { reason ->
                                Text("• $reason", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (forecast.promotionFactor > 1.0) {
                                Text("• Active promotion detected (+${((forecast.promotionFactor - 1) * 100).roundToInt()}% impact)", color = SuccessGreen)
                            }
                            if (forecast.seasonalityFactor > 1.0) {
                                Text("• Seasonal demand peak period", color = InfoBlue)
                            }
                        }
                    }
                }

                if (currentUser?.role == UserRole.STORE_MANAGER) {
                    item {
                        Button(
                            onClick = { showRequestDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Request Stock")
                        }
                    }
                } else {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { navController.navigate(Screen.AdminAllocation.route) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Allocate")
                            }
                            OutlinedButton(
                                onClick = { /* Suggest Transfer */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Transfer")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRequestDialog && product != null) {
        StockRequestDialog(
            productName = product.name,
            onDismiss = { showRequestDialog = false },
            onSubmit = { qty, reason, priority ->
                viewModel.createRequest(productId, storeId, qty, reason, priority)
                showRequestDialog = false
            }
        )
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SoftBlue)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StockRequestDialog(
    productName: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, String, String) -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request $productName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(quantity.toIntOrNull() ?: 0, reason, priority) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
