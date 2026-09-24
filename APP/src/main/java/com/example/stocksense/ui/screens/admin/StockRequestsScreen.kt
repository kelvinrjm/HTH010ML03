package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.data.model.RequestStatus
import com.example.stocksense.data.model.StockRequest
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.screens.common.AdminBottomNavigation
import com.example.stocksense.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockRequestsScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val requests by viewModel.requests.collectAsState()
    val products by viewModel.products.collectAsState()
    val stores by viewModel.stores.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Requests", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Silver
                )
            )
        },
        bottomBar = {
            AdminBottomNavigation(navController)
        }
    ) { padding ->
        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No pending requests", color = SoftBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requests.sortedByDescending { it.timestamp }) { request ->
                    val product = products.find { it.id == request.productId }
                    val store = stores.find { it.id == request.storeId }
                    RequestCard(
                        request = request,
                        productName = product?.name ?: "Unknown",
                        storeName = store?.name ?: "Unknown",
                        onApprove = { viewModel.updateRequestStatus(request.id, RequestStatus.APPROVED, request.quantity) },
                        onReject = { viewModel.updateRequestStatus(request.id, RequestStatus.REJECTED) }
                    )
                }
            }
        }
    }
}

@Composable
fun RequestCard(
    request: StockRequest,
    productName: String,
    storeName: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val statusColor = when(request.status) {
        RequestStatus.PENDING -> WarningAmber
        RequestStatus.APPROVED -> SuccessGreen
        RequestStatus.RECEIVED -> InfoBlue
        RequestStatus.REJECTED -> CriticalRed
        else -> SoftBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Silver)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(productName, fontWeight = FontWeight.Bold)
                    Text("Store: $storeName", style = MaterialTheme.typography.bodySmall, color = SoftBlue)
                }
                Text(
                    text = request.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Quantity: ${request.quantity}", modifier = Modifier.weight(1f))
                Text("Priority: ${request.priority}", color = if(request.priority == "High") CriticalRed else OnSurface)
            }
            
            Text("Reason: ${request.reason}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Text("Date: ${dateFormat.format(Date(request.timestamp))}", style = MaterialTheme.typography.labelSmall, color = SoftBlue, modifier = Modifier.padding(top = 4.dp))
            
            if (request.status == RequestStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CriticalRed)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve")
                    }
                }
            }
        }
    }
}
