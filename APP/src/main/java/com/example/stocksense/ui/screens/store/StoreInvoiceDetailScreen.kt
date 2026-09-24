package com.example.stocksense.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stocksense.data.model.*
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.screens.admin.DetailRow
import com.example.stocksense.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreInvoiceDetailScreen(
    invoiceId: String,
    viewModel: StockViewModel,
    navController: NavController
) {
    val invoices by viewModel.invoices.collectAsState()
    val invoice = invoices.find { it.id == invoiceId }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val deliveryFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Invoice", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Silver)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Download summary share link */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Silver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Silver)
            )
        }
    ) { padding ->
        if (invoice == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Invoice not found.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Banner
                val statusColor = when (invoice.status) {
                    InvoiceStatus.APPROVED, InvoiceStatus.GENERATED -> SuccessGreen
                    InvoiceStatus.IN_TRANSIT -> Color(0xFF3498DB)
                    InvoiceStatus.RECEIVED -> Navy
                    InvoiceStatus.PARTIALLY_RECEIVED -> Color(0xFFE67E22)
                    else -> SoftBlue
                }
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = statusColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Status: ${invoice.status.name}", fontWeight = FontWeight.Bold, color = statusColor)
                            Text("Expected Arrival: ${deliveryFormat.format(Date(invoice.expectedDelivery))}", fontSize = 12.sp, color = Navy)
                        }
                    }
                }

                // Invoice Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("CLIENT COPY", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                                Text(invoice.id, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Navy)
                            }
                            Text("StockSense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Silver.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("STORE CLIENT:", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                        Text(invoice.toName, fontWeight = FontWeight.Bold)
                        Text(invoice.toAddress, style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("FROM SUPPLIER:", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                        Text(invoice.fromName, fontWeight = FontWeight.Bold)
                    }
                }

                // Items Table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DELIVERED ITEMS", fontWeight = FontWeight.Bold, color = Navy)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        invoice.items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(item.productName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("SKU: ${item.sku}", fontSize = 10.sp, color = SoftBlue)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${item.quantity} Units", fontWeight = FontWeight.Bold, color = Navy)
                                    Text("Rate: ₹${item.rate}", fontSize = 11.sp, color = SoftBlue)
                                }
                            }
                            HorizontalDivider(color = Silver.copy(alpha = 0.3f))
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Amount Due:", fontWeight = FontWeight.Bold, color = Navy)
                            Text("₹${String.format("%.2f", invoice.total)}", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
                        }
                    }
                }

                // Actions
                if (invoice.status != InvoiceStatus.RECEIVED && invoice.status != InvoiceStatus.PARTIALLY_RECEIVED) {
                    Button(
                        onClick = { navController.navigate(Screen.StoreDeliveryTracking.route) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy)
                    ) {
                        Text("GO TO RECEIVING PANEL")
                    }
                }
            }
        }
    }
}
