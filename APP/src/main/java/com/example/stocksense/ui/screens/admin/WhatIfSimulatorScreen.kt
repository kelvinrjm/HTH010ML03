package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.data.model.Product
import com.example.stocksense.data.model.Store
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatIfSimulatorScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    var demandChangePct by remember { mutableStateOf(0f) }
    var promoEnabled by remember { mutableStateOf(false) }

    val products by viewModel.products.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val forecasts by viewModel.forecasts.collectAsState()

    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: "") }
    var selectedStoreId by remember { mutableStateOf(stores.firstOrNull()?.id ?: "") }

    val currentProduct = products.find { it.id == selectedProductId }
    val currentStore = stores.find { it.id == selectedStoreId }

    // Baseline details
    val baseForecast = forecasts.find { it.productId == selectedProductId && it.storeId == selectedStoreId }
    val currentInv = inventory.find { it.productId == selectedProductId && it.storeId == selectedStoreId }

    val baseDemand = baseForecast?.expectedDemand ?: 100
    val availableStock = (currentInv?.currentStock ?: 50) + (currentInv?.incomingStock ?: 0)

    // Simulation multiplier logic
    val simulationFactor = (1 + demandChangePct / 100f) * (if (promoEnabled) 1.25f else 1.0f)
    val simulatedDemand = (baseDemand * simulationFactor).roundToInt()
    val simulatedGap = simulatedDemand - availableStock

    val simulatedStatus = when {
        availableStock <= 0 -> "OUT OF STOCK"
        simulatedGap > 50 -> "CRITICAL STOCKOUT RISK"
        simulatedGap > 0 -> "LOW STOCK RISK"
        availableStock > simulatedDemand * 3 -> "OVERSTOCK RISK"
        else -> "HEALTHY"
    }

    val simulatedColor = when {
        simulatedStatus.contains("CRITICAL") || simulatedStatus.contains("OUT") -> CriticalRed
        simulatedStatus.contains("LOW") -> WarningAmber
        simulatedStatus.contains("OVER") -> SoftBlue
        else -> SuccessGreen
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What-If Promotion Simulator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Silver,
                    navigationIconContentColor = Silver
                ),
                actions = {
                    IconButton(onClick = { 
                        demandChangePct = 0f
                        promoEnabled = false
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Silver)
                    }
                }
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Scope Configurations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        // Store Selection
                        var storeExpanded by remember { mutableStateOf(false) }
                        Text("Select Target Business Store:", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                        Box {
                            OutlinedButton(onClick = { storeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(currentStore?.name ?: "Select Store", color = OnSurface)
                            }
                            DropdownMenu(expanded = storeExpanded, onDismissRequest = { storeExpanded = false }) {
                                stores.forEach { s ->
                                    DropdownMenuItem(text = { Text("${s.name} (${s.city})") }, onClick = {
                                        selectedStoreId = s.id
                                        storeExpanded = false
                                    })
                                }
                            }
                        }

                        // Product Selection
                        var prodExpanded by remember { mutableStateOf(false) }
                        Text("Select Product SKU:", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                        Box {
                            OutlinedButton(onClick = { prodExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(currentProduct?.name ?: "Select Product", color = OnSurface)
                            }
                            DropdownMenu(expanded = prodExpanded, onDismissRequest = { prodExpanded = false }) {
                                products.forEach { p ->
                                    DropdownMenuItem(text = { Text(p.name) }, onClick = {
                                        selectedProductId = p.id
                                        prodExpanded = false
                                    })
                                }
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
                        Text("Simulation Multipliers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Simulated Demand Change multiplier: ${demandChangePct.toInt()}%", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = demandChangePct,
                            onValueChange = { demandChangePct = it },
                            valueRange = -50f..100f,
                            colors = SliderDefaults.colors(thumbColor = InfoBlue, activeTrackColor = InfoBlue)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = promoEnabled, onCheckedChange = { promoEnabled = it })
                            Text("Apply Global Marketing Promotion Lift (+25% Demand)")
                        }
                    }
                }
            }

            item {
                Text("Comparative Matrix (Before vs After)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Surface)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Original Baseline", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$baseDemand Units", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Surface)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Simulated Future", style = MaterialTheme.typography.labelSmall, color = InfoBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$simulatedDemand Units", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = InfoBlue)
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
                        Text("Simulated Status Impact Matrix", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(simulatedStatus, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = simulatedColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Available Local Store Stock: $availableStock units", style = MaterialTheme.typography.bodyMedium)
                        Text("Projected Net Inventory Gap: $simulatedGap units", style = MaterialTheme.typography.bodyMedium, color = if (simulatedGap > 0) CriticalRed else SuccessGreen)
                    }
                }
            }
        }
    }
}
