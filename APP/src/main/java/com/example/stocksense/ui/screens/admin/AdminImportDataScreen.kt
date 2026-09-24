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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stocksense.data.model.*
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.theme.*

enum class ImportType(val label: String, val icon: ImageVector) {
    PRODUCTS("Products", Icons.Default.Inventory2),
    STORES("Stores", Icons.Default.Store),
    INVENTORY("Inventory", Icons.Default.Warehouse),
    SALES("Sales History", Icons.Default.PointOfSale),
    PROMOTIONS("Promotions", Icons.Default.Campaign)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminImportDataScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    var selectedType by remember { mutableStateOf<ImportType?>(null) }
    var fileSelected by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }
    var totalRows by remember { mutableStateOf(0) }
    var validRows by remember { mutableStateOf(0) }
    var invalidRows by remember { mutableStateOf(0) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    
    // Parsed Data Buffers
    var parsedProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var parsedSales by remember { mutableStateOf<List<SalesRecord>>(emptyList()) }
    var parsedStores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var parsedInventory by remember { mutableStateOf<List<Inventory>>(emptyList()) }
    var parsedPromos by remember { mutableStateOf<List<Promotion>>(emptyList()) }

    val products by viewModel.products.collectAsState()
    val stores by viewModel.stores.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Data Import", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (selectedType != null) selectedType = null 
                        else navController.popBackStack() 
                    }) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedType == null) {
                Text("Select Data Type to Import", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy)
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ImportType.entries) { type ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedType = type },
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(type.icon, contentDescription = null, tint = Navy)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(type.label, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = Silver)
                            }
                        }
                    }
                }
            } else if (!fileSelected) {
                ImportStepHeader(selectedType!!)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(64.dp), tint = Navy)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Select ${selectedType!!.label.lowercase()} file (.csv) to process into StockSense",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                fileName = "${selectedType!!.name.lowercase()}_upload.csv"
                                fileSelected = true
                                simulateParsing(selectedType!!, stores, products) { tr, vr, ir, pProd, pSales, pStores, pInv, pPromos ->
                                    totalRows = tr
                                    validRows = vr
                                    invalidRows = ir
                                    parsedProducts = pProd
                                    parsedSales = pSales
                                    parsedStores = pStores
                                    parsedInventory = pInv
                                    parsedPromos = pPromos
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CHOOSE FILE")
                        }
                    }
                }
            } else {
                // Validation Result and Preview
                ImportStepHeader(selectedType!!)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("File Information", fontWeight = FontWeight.Bold)
                        Text("File: $fileName", style = MaterialTheme.typography.bodySmall, color = SoftBlue)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Rows Detected:")
                            Text("$totalRows", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Valid Records:")
                            Text("$validRows", color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Validation Errors:")
                            Text("$invalidRows", color = CriticalRed, fontWeight = FontWeight.Bold)
                        }

                        if (invalidRows > 0) {
                            Text(
                                "Some rows contain missing IDs or malformed data and will be skipped.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CriticalRed
                            )
                        }

                        HorizontalDivider(color = Silver.copy(alpha = 0.5f))
                        
                        Text("Data Preview (Valid Records):", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                        
                        // Mock preview items
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(minOf(3, validRows)) { index ->
                                Surface(
                                    color = Background,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Record ${index + 1}: Validated for StockSense repository.", 
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { fileSelected = false }, modifier = Modifier.weight(1f)) {
                                Text("CANCEL")
                            }
                            Button(
                                onClick = {
                                    when(selectedType) {
                                        ImportType.PRODUCTS -> viewModel.importProducts(parsedProducts)
                                        ImportType.SALES -> viewModel.importSalesData(parsedSales)
                                        ImportType.STORES -> viewModel.importStores(parsedStores)
                                        ImportType.INVENTORY -> viewModel.importInventory(parsedInventory)
                                        ImportType.PROMOTIONS -> viewModel.importPromotions(parsedPromos)
                                        else -> {}
                                    }
                                    
                                    importStatusMessage = "Successfully imported $validRows records into ${selectedType!!.label}."
                                    fileSelected = false
                                    selectedType = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Text("IMPORT VALID DATA")
                            }
                        }
                    }
                }
            }

            importStatusMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(msg, color = SuccessGreen, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ImportStepHeader(type: ImportType) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(type.icon, null, tint = Navy, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Importing ${type.label}", fontWeight = FontWeight.Bold, color = Navy)
    }
}

private fun simulateParsing(
    type: ImportType,
    stores: List<Store>,
    products: List<Product>,
    onResult: (Int, Int, Int, List<Product>, List<SalesRecord>, List<Store>, List<Inventory>, List<Promotion>) -> Unit
) {
    val total = (80..200).random()
    val invalid = (0..10).random()
    val valid = total - invalid
    
    var pProd = emptyList<Product>()
    var pSales = emptyList<SalesRecord>()
    var pStores = emptyList<Store>()
    var pInv = emptyList<Inventory>()
    var pPromos = emptyList<Promotion>()

    when(type) {
        ImportType.PRODUCTS -> {
            pProd = List(valid) { i -> 
                Product(sku = "SKU-IMP-$i", name = "Bulk Item $i", category = "Imported", sellingPrice = (50..500).random().toDouble()) 
            }
        }
        ImportType.SALES -> {
            val sId = stores.firstOrNull()?.id ?: "S1"
            val pId = products.firstOrNull()?.id ?: "P1"
            pSales = List(valid) { SalesRecord(storeId = sId, productId = pId, date = System.currentTimeMillis() - (it * 86400000), quantitySold = (5..20).random()) }
        }
        ImportType.STORES -> {
            pStores = List(valid) { i -> Store(id = "S-IMP-$i", name = "Imported Mart $i", city = "New City $i") }
        }
        ImportType.INVENTORY -> {
            val sId = stores.firstOrNull()?.id ?: "S1"
            val pId = products.firstOrNull()?.id ?: "P1"
            pInv = List(valid) { Inventory(productId = pId, storeId = sId, currentStock = (20..150).random()) }
        }
        ImportType.PROMOTIONS -> {
            val sId = stores.firstOrNull()?.id ?: "S1"
            val pId = products.firstOrNull()?.id ?: "P1"
            pPromos = List(valid) { Promotion(productId = pId, storeId = sId, name = "Flash Sale", startDate = System.currentTimeMillis(), endDate = System.currentTimeMillis() + 604800000, discountPercent = 15.0, expectedLift = 1.3) }
        }
    }
    
    onResult(total, valid, invalid, pProd, pSales, pStores, pInv, pPromos)
}
