package com.example.stocksense.ui.screens.store

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
import com.example.stocksense.ui.screens.common.StoreBottomNavigation
import com.example.stocksense.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDeliveryTrackingScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transfers by viewModel.transfers.collectAsState()
    val products by viewModel.products.collectAsState()
    val storeId = currentUser?.storeId ?: ""
    
    val storeTransfers = transfers.filter { it.toStoreId == storeId }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    var receivingTransfer by remember { mutableStateOf<StockTransfer?>(null) }
    var receivedQuantity by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incoming Deliveries", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Silver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Silver)
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
            if (storeTransfers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No deliveries for your store.", color = SoftBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(storeTransfers.reversed()) { transfer ->
                        StoreDeliveryCard(
                            transfer = transfer,
                            productName = products.find { it.id == transfer.productId }?.name ?: "Unknown",
                            dateFormat = dateFormat,
                            onReceiveClick = { 
                                receivingTransfer = transfer 
                                receivedQuantity = transfer.quantity.toString()
                            }
                        )
                    }
                }
            }
        }

        if (receivingTransfer != null) {
            AlertDialog(
                onDismissRequest = { receivingTransfer = null },
                title = { Text("Confirm Stock Receipt") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Confirm units received for ${products.find { it.id == receivingTransfer!!.productId }?.name}")
                        OutlinedTextField(
                            value = receivedQuantity,
                            onValueChange = { if(it.all { char -> char.isDigit() }) receivedQuantity = it },
                            label = { Text("Quantity Received") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (receivedQuantity.toIntOrNull() != null && receivedQuantity.toInt() < receivingTransfer!!.quantity) {
                            Text(
                                "Note: You are reporting a partial receipt. ${receivingTransfer!!.quantity - receivedQuantity.toInt()} units missing.",
                                color = CriticalRed,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val qty = receivedQuantity.toIntOrNull() ?: 0
                            viewModel.confirmReceipt(receivingTransfer!!.id, qty)
                            receivingTransfer = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("CONFIRM RECEIPT")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { receivingTransfer = null }) { Text("CANCEL") }
                }
            )
        }
    }
}

@Composable
fun StoreDeliveryCard(
    transfer: StockTransfer,
    productName: String,
    dateFormat: SimpleDateFormat,
    onReceiveClick: () -> Unit
) {
    val statusColor = when (transfer.status) {
        TransferStatus.RECEIVED -> SuccessGreen
        TransferStatus.PARTIALLY_RECEIVED -> Color(0xFFE67E22)
        TransferStatus.DELAYED -> CriticalRed
        TransferStatus.IN_TRANSIT -> InfoBlue
        else -> WarningAmber
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
                        TransferStatus.RECEIVED -> Icons.Default.Inventory
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
            Text("Expected Quantity: ${transfer.quantity} Units", style = MaterialTheme.typography.bodySmall, color = SoftBlue)
            
            if (transfer.status == TransferStatus.RECEIVED || transfer.status == TransferStatus.PARTIALLY_RECEIVED) {
                Text("Received: ${transfer.receivedQuantity ?: 0} Units", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("EXPECTED DATE", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text(
                        dateFormat.format(Date(transfer.expectedDeliveryDate)),
                        fontWeight = FontWeight.Bold,
                        color = if (transfer.status == TransferStatus.DELAYED) CriticalRed else Navy
                    )
                }
                
                if (transfer.status != TransferStatus.RECEIVED && transfer.status != TransferStatus.PARTIALLY_RECEIVED) {
                    Button(
                        onClick = onReceiveClick,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("RECEIVE STOCK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
