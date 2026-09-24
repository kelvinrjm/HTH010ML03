package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.data.model.StockStatus
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.screens.common.AdminBottomNavigation
import com.example.stocksense.ui.screens.common.InventoryItemCard
import com.example.stocksense.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminInventoryScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val inventory by viewModel.inventory.collectAsState()
    val products by viewModel.products.collectAsState()
    val stores by viewModel.stores.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<StockStatus?>(null) }

    val filteredInventory = inventory.filter { inv ->
        val product = products.find { it.id == inv.productId }
        val store = stores.find { it.id == inv.storeId }
        val matchesSearch = product?.name?.contains(searchQuery, ignoreCase = true) == true ||
                           product?.sku?.contains(searchQuery, ignoreCase = true) == true ||
                           store?.name?.contains(searchQuery, ignoreCase = true) == true
        
        val risk = viewModel.getRisk(inv.productId, inv.storeId)
        val matchesStatus = selectedStatus == null || risk.status == selectedStatus
        
        matchesSearch && matchesStatus
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Management", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            // Search and Filter Bar
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search product, SKU or store...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = { selectedStatus = null },
                            label = { Text("All") }
                        )
                    }
                    StockStatus.entries.forEach { status ->
                        item {
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = { Text(status.name.replace("_", " ")) }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredInventory) { inv ->
                    val product = products.find { it.id == inv.productId }
                    val store = stores.find { it.id == inv.storeId }
                    val risk = viewModel.getRisk(inv.productId, inv.storeId)
                    
                    InventoryItemCard(
                        productName = product?.name ?: "Unknown",
                        sku = product?.sku ?: "N/A",
                        storeName = store?.name ?: "Unknown",
                        stock = inv.currentStock,
                        status = risk.status,
                        onClick = {
                            navController.navigate(Screen.AdminProductDetail.createRoute(inv.productId, inv.storeId))
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
