package com.example.stocksense.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.data.model.InvoiceStatus
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.screens.admin.InvoiceListItem
import com.example.stocksense.ui.theme.Background
import com.example.stocksense.ui.theme.Navy
import com.example.stocksense.ui.theme.Silver
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreInvoicesScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    
    val storeId = currentUser?.storeId ?: ""
    val storeName = stores.find { it.id == storeId }?.name ?: "Store"
    
    // Filter invoices that are addressed to this store
    val storeInvoices = invoices.filter { it.toName == storeName }
    
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Invoices", fontWeight = FontWeight.Bold) },
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
            if (storeInvoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(64.dp), tint = Silver.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No invoices received yet.", color = Silver)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(storeInvoices.reversed()) { invoice ->
                        InvoiceListItem(invoice, dateFormat) {
                            navController.navigate(Screen.StoreInvoiceDetail.createRoute(invoice.id))
                        }
                    }
                }
            }
        }
    }
}
