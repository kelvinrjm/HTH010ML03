package com.example.stocksense.ui.screens.admin

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
import com.example.stocksense.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminInvoiceDetailScreen(
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
                title = { Text("Invoice Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Silver)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share Logic */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Silver)
                    }
                    IconButton(onClick = { /* Download Logic */ }) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Silver)
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
                        Icon(Icons.Default.Info, contentDescription = null, tint = statusColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Current Status: ${invoice.status.name}", fontWeight = FontWeight.Bold, color = statusColor)
                            Text("Updated on ${dateFormat.format(Date(invoice.createdDate))}", fontSize = 12.sp, color = statusColor)
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
                                Text("INVOICE", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                                Text(invoice.id, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Navy)
                            }
                            Text("StockSense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Silver.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FROM:", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                                Text(invoice.fromName, fontWeight = FontWeight.Bold)
                                Text(invoice.fromAddress, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("TO:", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                                Text(invoice.toName, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text(invoice.toAddress, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                            }
                        }
                    }
                }

                // Items Table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ITEMS", fontWeight = FontWeight.Bold, color = Navy)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth().background(Background).padding(8.dp)) {
                            Text("Description", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Qty", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("Rate", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                            Text("Total", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                        }
                        
                        invoice.items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(item.productName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("SKU: ${item.sku}", fontSize = 10.sp, color = SoftBlue)
                                }
                                Text("${item.quantity}", modifier = Modifier.weight(0.5f), fontSize = 14.sp, textAlign = TextAlign.Center)
                                Text("₹${String.format("%.2f", item.rate)}", modifier = Modifier.weight(1f), fontSize = 14.sp, textAlign = TextAlign.End)
                                Text("₹${String.format("%.2f", item.quantity * item.rate)}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End)
                            }
                            HorizontalDivider(color = Silver.copy(alpha = 0.3f))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                            Row(modifier = Modifier.width(150.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", color = SoftBlue)
                                Text("₹${String.format("%.2f", invoice.subtotal)}")
                            }
                            Row(modifier = Modifier.width(150.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tax:", color = SoftBlue)
                                Text("₹${String.format("%.2f", invoice.tax)}")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.width(150.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL:", fontWeight = FontWeight.Bold, color = Navy)
                                Text("₹${String.format("%.2f", invoice.total)}", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                            }
                        }
                    }
                }

                // Transaction Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TRANSACTION SUMMARY", fontWeight = FontWeight.Bold, color = Navy)
                        
                        DetailRow("Expected Delivery", deliveryFormat.format(Date(invoice.expectedDelivery)))
                        DetailRow("Approved By", invoice.approvedBy)
                        DetailRow("Reference ID", invoice.requestId ?: invoice.allocationId ?: invoice.transferId ?: "N/A")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { navController.navigate(Screen.AdminDeliveryTracking.route) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy)
                        ) {
                            Icon(Icons.Default.LocalShipping, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TRACK DELIVERY")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SoftBlue, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
    }
}
