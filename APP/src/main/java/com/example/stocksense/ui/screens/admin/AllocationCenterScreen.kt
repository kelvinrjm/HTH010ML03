package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.data.model.AllocationItem
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllocationCenterScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val products by viewModel.products.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val currentAllocation by viewModel.currentAllocation.collectAsState()
    
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: "") }
    
    LaunchedEffect(selectedProductId) {
        if (selectedProductId.isNotEmpty()) {
            viewModel.prepareAllocation(selectedProductId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Allocation Center", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            // Product Selector
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Product to Allocate", style = MaterialTheme.typography.labelMedium, color = SoftBlue)
                    var expanded by remember { mutableStateOf(false) }
                    val selectedProduct = products.find { it.id == selectedProductId }
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedProduct?.name ?: "Select Product", color = OnSurface)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            products.forEach { product ->
                                DropdownMenuItem(
                                    text = { Text(product.name) },
                                    onClick = {
                                        selectedProductId = product.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            currentAllocation?.let { allocation ->
                val totalAllocated = allocation.items.sumOf { it.recommendedQuantity + it.manualAdjustment }
                val remainingWarehouse = allocation.warehouseStock - totalAllocated

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        KpiSmall("Warehouse Stock", allocation.warehouseStock.toString())
                        KpiSmall("Total Allocated", totalAllocated.toString())
                        KpiSmall("Remaining", remainingWarehouse.toString(), if (remainingWarehouse < 0) CriticalRed else SuccessGreen)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Store Allocations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allocation.items) { item ->
                            val store = stores.find { it.id == item.storeId }
                            AllocationItemCard(store?.name ?: "Unknown", item) { adj ->
                                viewModel.updateAllocationAdjustment(item.storeId, adj)
                            }
                        }
                    }
                    
                    Button(
                        onClick = { 
                            viewModel.approveAllocation()
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        enabled = remainingWarehouse >= 0,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Approve and Deploy Allocation")
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun KpiSmall(label: String, value: String, valueColor: Color = OnSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SoftBlue)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun AllocationItemCard(storeName: String, item: AllocationItem, onAdjustmentChange: (Int) -> Unit) {
    var textValue by remember(item.manualAdjustment) { 
        mutableStateOf(if (item.manualAdjustment == 0) "" else item.manualAdjustment.toString()) 
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Silver)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(storeName, fontWeight = FontWeight.Bold)
                    Text("Demand: ${item.forecastDemand} | Current: ${item.currentStock}", 
                        style = MaterialTheme.typography.bodySmall, color = SoftBlue)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("AI Recommended", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                    Text(item.recommendedQuantity.toString(), fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Silver.copy(alpha = 0.5f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text("Manual Adjustment:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        textValue = newValue
                        val adj = newValue.toIntOrNull() ?: 0
                        onAdjustmentChange(adj)
                    },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true
                )
            }
        }
    }
}
