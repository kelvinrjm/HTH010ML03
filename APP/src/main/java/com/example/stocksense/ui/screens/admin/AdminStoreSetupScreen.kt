package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.data.model.Store
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.theme.Background
import com.example.stocksense.ui.theme.Navy
import com.example.stocksense.ui.theme.Silver
import com.example.stocksense.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStoreSetupScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val stores by viewModel.stores.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("4000") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Setup & Locations", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Silver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Silver)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Store")
            }
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
                Text(
                    "Manage your business locations. All data in StockSense is dynamic and depends on these store definitions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(stores) { store ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Silver)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(store.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Location/City: ${store.city}", style = MaterialTheme.typography.bodyMedium)
                        Text("Warehouse Storage Capacity: ${store.warehouseCapacity} units", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add New Dynamic Store") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Store Name *") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City / Location *") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Warehouse Capacity") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (name.isNotBlank() && city.isNotBlank()) {
                            val cap = capacity.toIntOrNull() ?: 5000
                            viewModel.addStore(
                                Store(
                                    id = "S${stores.size + 1}",
                                    name = name,
                                    city = city,
                                    warehouseCapacity = cap
                                )
                            )
                            showAddDialog = false
                            name = ""
                            city = ""
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
