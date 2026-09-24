package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.data.model.Product
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val stores by viewModel.stores.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Units") }
    var currentStock by remember { mutableStateOf("") }
    var minStock by remember { mutableStateOf("10") }
    var maxStock by remember { mutableStateOf("500") }
    var warehouseStock by remember { mutableStateOf("1000") }
    var leadTime by remember { mutableStateOf("3") }
    var seasonality by remember { mutableStateOf("1.0") }
    var isPromoted by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    
    var selectedStoreId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var expandedStore by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Product", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Silver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Silver)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Product Core Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy)
            
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category *") }, modifier = Modifier.fillMaxWidth())
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = price, 
                    onValueChange = { price = it }, 
                    label = { Text("Selling Price *") }, 
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (e.g. Kg, Box)") }, modifier = Modifier.weight(1f))
            }

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            HorizontalDivider()
            Text("Inventory & Supply Chain", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy)

            ExposedDropdownMenuBox(
                expanded = expandedStore,
                onExpandedChange = { expandedStore = !expandedStore },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = stores.find { it.id == selectedStoreId }?.name ?: "Assign to Store *",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Store Location") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStore) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedStore,
                    onDismissRequest = { expandedStore = false }
                ) {
                    stores.forEach { store ->
                        DropdownMenuItem(
                            text = { Text("${store.name} (${store.city})") },
                            onClick = {
                                selectedStoreId = store.id
                                expandedStore = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentStock, 
                    onValueChange = { currentStock = it }, 
                    label = { Text("Initial Stock *") }, 
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = leadTime, 
                    onValueChange = { leadTime = it }, 
                    label = { Text("Lead Time (Days)") }, 
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minStock, 
                    onValueChange = { minStock = it }, 
                    label = { Text("Min Stock") }, 
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = maxStock, 
                    onValueChange = { maxStock = it }, 
                    label = { Text("Max Stock") }, 
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = warehouseStock, 
                onValueChange = { warehouseStock = it }, 
                label = { Text("Total Warehouse Stock Available") }, 
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            HorizontalDivider()
            Text("Forecasting Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy)

            OutlinedTextField(
                value = seasonality, 
                onValueChange = { seasonality = it }, 
                label = { Text("Seasonality Factor (e.g. 1.2 for 20% lift)") }, 
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = isPromoted, onCheckedChange = { isPromoted = it })
                Text("Currently under Promotion")
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val pValue = price.toDoubleOrNull() ?: 0.0
                    val sValue = currentStock.toIntOrNull() ?: 0
                    val minS = minStock.toIntOrNull() ?: 0
                    val maxS = maxStock.toIntOrNull() ?: 0
                    val wS = warehouseStock.toIntOrNull() ?: 0
                    val lT = leadTime.toIntOrNull() ?: 3
                    val sF = seasonality.toDoubleOrNull() ?: 1.0

                    if (name.isBlank() || sku.isBlank() || category.isBlank() || price.isBlank() || currentStock.isBlank() || selectedStoreId.isBlank()) {
                        errorMessage = "Please fill all required fields (*)"
                    } else if (maxS < minS) {
                        errorMessage = "Max stock must be >= Min stock"
                    } else {
                        val product = Product(
                            name = name,
                            sku = sku,
                            category = category,
                            sellingPrice = pValue,
                            unit = unit,
                            warehouseStock = wS,
                            leadTime = lT,
                            seasonalityFactor = sF,
                            isPromoted = isPromoted,
                            description = description
                        )
                        viewModel.addProduct(product, sValue, selectedStoreId)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy)
            ) {
                Text("SAVE PRODUCT", fontWeight = FontWeight.Bold)
            }
        }
    }
}
