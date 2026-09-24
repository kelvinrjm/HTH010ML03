package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stocksense.data.model.*
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDeliveryTrackingScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val transfers by viewModel.transfers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val products by viewModel.products.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    var showDelayDialog by remember { mutableStateOf<StockTransfer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery Tracking", fontWeight = FontWeight.Bold) },
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
        ) {
            if (transfers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active deliveries found.", color = SoftBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(transfers.reversed()) { transfer ->
                        DeliveryCard(
                            transfer = transfer,
                            productName = products.find { it.id == transfer.productId }?.name ?: "Unknown",
                            fromStore = if (transfer.fromStoreId == "WAREHOUSE") "Main Warehouse" else stores.find { it.id == transfer.fromStoreId }?.name ?: "Unknown",
                            toStore = stores.find { it.id == transfer.toStoreId }?.name ?: "Unknown",
                            dateFormat = dateFormat,
                            onDelayClick = { showDelayDialog = transfer }
                        )
                    }
                }
            }
        }

        if (showDelayDialog != null) {
            AlertDialog(
                onDismissRequest = { showDelayDialog = null },
                title = { Text("Simulate Delivery Delay") },
                text = { Text("How many days of delay would you like to simulate for this shipment?") },
                confirmButton = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { 
                                viewModel.simulateDeliveryDelay(showDelayDialog!!.id, 1)
                                showDelayDialog = null 
                            }) { Text("+1 Day") }
                            Button(onClick = { 
                                viewModel.simulateDeliveryDelay(showDelayDialog!!.id, 3)
                                showDelayDialog = null 
                            }) { Text("+3 Days") }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDelayDialog = null }) { Text("CANCEL") }
                }
            )
        }
    }
}

@Composable
fun DeliveryCard(
    transfer: StockTransfer,
    productName: String,
    fromStore: String,
    toStore: String,
    dateFormat: SimpleDateFormat,
    onDelayClick: () -> Unit
) {
    val statusColor = when (transfer.status) {
        TransferStatus.RECEIVED -> SuccessGreen
        TransferStatus.DELAYED -> CriticalRed
        TransferStatus.IN_TRANSIT -> InfoBlue
        TransferStatus.GENERATED, TransferStatus.APPROVED -> WarningAmber
        else -> SoftBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (transfer.status) {
                        TransferStatus.RECEIVED -> Icons.Default.CheckCircle
                        TransferStatus.DELAYED -> Icons.Default.Warning
                        else -> Icons.Default.LocalShipping
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "TRF-${transfer.id.takeLast(6)}",
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        transfer.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${transfer.quantity} Units", style = MaterialTheme.typography.bodySmall, color = SoftBlue)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("FROM", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text(fromStore, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Icon(Icons.Default.ArrowForward, null, tint = Silver, modifier = Modifier.padding(horizontal = 8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("TO", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text(toStore, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = Silver.copy(alpha = 0.3f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("EXPECTED DELIVERY", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text(
                        dateFormat.format(Date(transfer.expectedDeliveryDate)),
                        fontWeight = FontWeight.Bold,
                        color = if (transfer.status == TransferStatus.DELAYED) CriticalRed else Navy
                    )
                }
                
                if (transfer.status != TransferStatus.RECEIVED) {
                    OutlinedButton(
                        onClick = onDelayClick,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("SIMULATE DELAY", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
