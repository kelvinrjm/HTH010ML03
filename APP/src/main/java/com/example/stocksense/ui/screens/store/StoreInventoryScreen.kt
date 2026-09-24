package com.example.stocksense.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.data.model.StockStatus
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.screens.common.InventoryItemCard
import com.example.stocksense.ui.screens.common.StoreBottomNavigation
import com.example.stocksense.ui.theme.Background
import com.example.stocksense.ui.theme.Navy
import com.example.stocksense.ui.theme.Silver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreInventoryScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val products by viewModel.products.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }

    val storeInventory = inventory.filter { inv ->
        inv.storeId == currentUser?.storeId &&
        products.find { it.id == inv.productId }?.name?.contains(searchQuery, ignoreCase = true) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Inventory", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search inventory...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(storeInventory) { inv ->
                    val product = products.find { it.id == inv.productId }
                    val risk = viewModel.getRisk(inv.productId, inv.storeId)
                    
                    InventoryItemCard(
                        productName = product?.name ?: "Unknown",
                        sku = product?.sku ?: "N/A",
                        storeName = "", // Don't need store name here as it's the current store
                        stock = inv.currentStock,
                        status = risk.status,
                        onClick = {
                            navController.navigate(Screen.AdminProductDetail.createRoute(inv.productId, inv.storeId))
                        }
                    )
                }
            }
        }
    }
}
