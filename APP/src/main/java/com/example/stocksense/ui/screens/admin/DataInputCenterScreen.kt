package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataInputCenterScreen(navController: NavController) {
    var selectedMethod by remember { mutableStateOf<String?>(null) } // "MANUAL" or "IMPORT"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Input Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (selectedMethod != null) selectedMethod = null 
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedMethod == null) {
                Text(
                    "Add or import the data StockSense needs to understand your inventory and demand.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftBlue,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                LargeOptionButton(
                    icon = Icons.Default.EditNote,
                    title = "ADD MANUALLY",
                    subtitle = "Enter individual records one by one",
                    onClick = { selectedMethod = "MANUAL" }
                )

                Spacer(modifier = Modifier.height(16.dp))

                LargeOptionButton(
                    icon = Icons.Default.UploadFile,
                    title = "IMPORT FILE",
                    subtitle = "Bulk upload data via CSV or Excel",
                    onClick = { selectedMethod = "IMPORT" }
                )
            } else {
                Text(
                    text = if (selectedMethod == "MANUAL") "Select data type to add manually" else "Select data type to import",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier.padding(bottom = 24.dp).align(Alignment.Start)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        DataTypeCard(
                            icon = Icons.Default.Store,
                            title = "Stores",
                            onClick = {
                                if (selectedMethod == "MANUAL") navController.navigate(Screen.AdminStoreSetup.route)
                                else navController.navigate(Screen.AdminImportData.route)
                            }
                        )
                    }
                    item {
                        DataTypeCard(
                            icon = Icons.Default.Inventory2,
                            title = "Products",
                            onClick = {
                                if (selectedMethod == "MANUAL") navController.navigate(Screen.AdminAddProduct.route)
                                else navController.navigate(Screen.AdminImportData.route)
                            }
                        )
                    }
                    item {
                        DataTypeCard(
                            icon = Icons.Default.Warehouse,
                            title = "Inventory",
                            onClick = {
                                navController.navigate(Screen.AdminImportData.route)
                            }
                        )
                    }
                    item {
                        DataTypeCard(
                            icon = Icons.Default.PointOfSale,
                            title = "Sales History",
                            onClick = {
                                navController.navigate(Screen.AdminImportData.route)
                            }
                        )
                    }
                    item {
                        DataTypeCard(
                            icon = Icons.Default.Campaign,
                            title = "Promotions",
                            onClick = {
                                navController.navigate(Screen.AdminImportData.route)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LargeOptionButton(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = Navy)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SoftBlue)
            }
        }
    }
}

@Composable
fun DataTypeCard(icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Silver)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Silver)
        }
    }
}
