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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
fun AdminInvoicesScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val invoices by viewModel.invoices.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoices & Documents", fontWeight = FontWeight.Bold) },
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
            if (invoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(64.dp), tint = SoftBlue.copy(alpha = 0.5f))
                        Text("No invoices generated yet.", color = SoftBlue)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(invoices.reversed()) { invoice ->
                        InvoiceListItem(invoice, dateFormat) {
                            navController.navigate(Screen.AdminInvoiceDetail.createRoute(invoice.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceListItem(
    invoice: Invoice,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val statusColor = when (invoice.status) {
        InvoiceStatus.APPROVED, InvoiceStatus.GENERATED -> SuccessGreen
        InvoiceStatus.IN_TRANSIT -> Color(0xFF3498DB)
        InvoiceStatus.RECEIVED -> Navy
        InvoiceStatus.PARTIALLY_RECEIVED -> Color(0xFFE67E22)
        else -> SoftBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(invoice.id, fontWeight = FontWeight.Bold, color = Navy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            invoice.status.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text("To: ${invoice.toName}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Date: ${dateFormat.format(Date(invoice.createdDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftBlue
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${String.format("%.2f", invoice.total)}",
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                Text(
                    "${invoice.items.sumOf { it.quantity }} units",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftBlue
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null, tint = Silver)
        }
    }
}
