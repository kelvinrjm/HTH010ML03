package com.example.stocksense.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stocksense.data.model.RequestStatus
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.screens.common.ActiveRequestItem
import com.example.stocksense.ui.screens.common.StoreBottomNavigation
import com.example.stocksense.ui.theme.Background
import com.example.stocksense.ui.theme.Navy
import com.example.stocksense.ui.theme.Silver
import com.example.stocksense.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreRequestsScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val products by viewModel.products.collectAsState()

    val storeRequests = requests.filter { it.storeId == currentUser?.storeId }

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: "") }
    var quantityText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var priorityText by remember { mutableStateOf("Medium") }
    
    var expandedProduct by remember { mutableStateOf(false) }
    var expandedPriority by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Stock Requests", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Silver
                )
            )
        },
        bottomBar = {
            StoreBottomNavigation(navController)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Request")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            if (storeRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No stock requests made yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(storeRequests.reversed()) { request ->
                        val product = products.find { it.id == request.productId }
                        ActiveRequestItem(request = request, productName = product?.name ?: "Unknown") {
                            if (request.status == RequestStatus.APPROVED || request.status == RequestStatus.IN_TRANSIT) {
                                viewModel.updateRequestStatus(request.id, RequestStatus.RECEIVED, request.approvedQuantity)
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Stock Request") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Product Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedProduct,
                            onExpandedChange = { expandedProduct = !expandedProduct }
                        ) {
                            OutlinedTextField(
                                value = products.find { it.id == selectedProductId }?.name ?: "Select Product",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Product") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProduct) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedProduct,
                                onDismissRequest = { expandedProduct = false }
                            ) {
                                products.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name) },
                                        onClick = {
                                            selectedProductId = p.id
                                            expandedProduct = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            label = { Text("Reason") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Priority Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedPriority,
                            onExpandedChange = { expandedPriority = !expandedPriority }
                        ) {
                            OutlinedTextField(
                                value = priorityText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Priority") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPriority) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedPriority,
                                onDismissRequest = { expandedPriority = false }
                            ) {
                                listOf("Low", "Medium", "High", "Urgent").forEach { pr ->
                                    DropdownMenuItem(
                                        text = { Text(pr) },
                                        onClick = {
                                            priorityText = pr
                                            expandedPriority = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val qty = quantityText.toIntOrNull() ?: 0
                            if (qty > 0 && selectedProductId.isNotEmpty()) {
                                viewModel.createRequest(
                                    productId = selectedProductId,
                                    storeId = currentUser?.storeId ?: "",
                                    quantity = qty,
                                    reason = reasonText,
                                    priority = priorityText
                                )
                                showCreateDialog = false
                                quantityText = ""
                                reasonText = ""
                            }
                        }
                    ) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
