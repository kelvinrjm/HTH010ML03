package com.example.stocksense.ui.screens.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stocksense.data.model.*
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.theme.*

@Composable
fun ActiveRequestItem(request: StockRequest, productName: String, onReceive: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(productName, fontWeight = FontWeight.Bold)
                Text(
                    text = request.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when(request.status) {
                        RequestStatus.PENDING -> WarningAmber
                        RequestStatus.APPROVED -> SuccessGreen
                        RequestStatus.IN_TRANSIT -> InfoBlue
                        RequestStatus.RECEIVED -> SoftBlue
                        else -> CriticalRed
                    }
                )
            }
            Text("Qty: ${request.quantity} | Priority: ${request.priority}", style = MaterialTheme.typography.bodySmall, color = SoftBlue)
            
            if (request.status == RequestStatus.APPROVED || request.status == RequestStatus.IN_TRANSIT) {
                Button(
                    onClick = onReceive,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Mark as Received", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ForecastCard(productName: String, forecast: Forecast) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Forecast Confidence: ${forecast.confidence}", 
                        color = if(forecast.confidence == "High") SuccessGreen else WarningAmber,
                        style = MaterialTheme.typography.labelSmall)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp, 
                    contentDescription = null, 
                    tint = InfoBlue
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Expected Demand", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text("${forecast.expectedDemand} Units", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Trend", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    val trendPct = (forecast.trend * 100).toInt()
                    val trendText = if (forecast.trend >= 0) "+$trendPct%" else "$trendPct%"
                    Text(
                        text = trendText, 
                        fontWeight = FontWeight.Bold, 
                        color = if(forecast.trend >= 0) SuccessGreen else CriticalRed
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Why this forecast?", style = MaterialTheme.typography.labelSmall, color = SoftBlue, fontWeight = FontWeight.Bold)
            forecast.explanation.forEach { reason ->
                Text("• $reason", style = MaterialTheme.typography.bodySmall, color = OnSurface)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple Chart Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Background, RoundedCornerShape(4.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val path = Path()
                    val points = listOf(0.7f, 0.6f, 0.8f, 0.75f, 0.9f, 0.85f, 1.0f)
                    val width = size.width
                    val height = size.height
                    
                    path.moveTo(0f, height * (1 - points[0]))
                    points.forEachIndexed { index, point ->
                        if (index > 0) {
                            path.lineTo(width * (index / (points.size - 1f)), height * (1 - point))
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = InfoBlue,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryItemCard(
    productName: String,
    sku: String,
    storeName: String,
    stock: Int,
    status: StockStatus,
    onClick: () -> Unit
) {
    val statusColor = when(status) {
        StockStatus.HEALTHY -> SuccessGreen
        StockStatus.LOW_STOCK -> WarningAmber
        StockStatus.CRITICAL -> CriticalRed
        StockStatus.OUT_OF_STOCK -> CriticalRed
        StockStatus.OVERSTOCK -> InfoBlue
        StockStatus.DELIVERY_GAP -> Color(0xFFE67E22)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(productName, fontWeight = FontWeight.Bold, color = OnSurface)
                Text("SKU: $sku ${if(storeName.isNotEmpty()) "| $storeName" else ""}", style = MaterialTheme.typography.bodySmall, color = SoftBlue)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stock.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Text(
                    text = status.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = SoftBlue)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminBottomNavigation(navController: NavController) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route

    NavigationBar(containerColor = Surface) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("Dashboard", fontSize = 10.sp) },
            selected = currentRoute == Screen.AdminDashboard.route,
            onClick = { navController.navigate(Screen.AdminDashboard.route) {
                popUpTo(Screen.AdminDashboard.route) { inclusive = true }
            } }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
            label = { Text("Inventory", fontSize = 10.sp) },
            selected = currentRoute == Screen.AdminInventory.route,
            onClick = { navController.navigate(Screen.AdminInventory.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
            label = { Text("Forecast", fontSize = 10.sp) },
            selected = currentRoute == Screen.AdminForecast.route,
            onClick = { navController.navigate(Screen.AdminForecast.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
            label = { Text("Requests", fontSize = 10.sp) },
            selected = currentRoute == Screen.AdminRequests.route,
            onClick = { navController.navigate(Screen.AdminRequests.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
            label = { Text("More", fontSize = 10.sp) },
            selected = currentRoute == Screen.AdminMore.route,
            onClick = { navController.navigate(Screen.AdminMore.route) }
        )
    }
}

@Composable
fun StoreBottomNavigation(navController: NavController) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route

    NavigationBar(containerColor = Surface) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home", fontSize = 10.sp) },
            selected = currentRoute == Screen.StoreHome.route,
            onClick = { navController.navigate(Screen.StoreHome.route) {
                popUpTo(Screen.StoreHome.route) { inclusive = true }
            } }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
            label = { Text("Inventory", fontSize = 10.sp) },
            selected = currentRoute == Screen.StoreInventory.route,
            onClick = { navController.navigate(Screen.StoreInventory.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
            label = { Text("Forecast", fontSize = 10.sp) },
            selected = currentRoute == Screen.StoreForecast.route,
            onClick = { navController.navigate(Screen.StoreForecast.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
            label = { Text("Requests", fontSize = 10.sp) },
            selected = currentRoute == Screen.StoreRequests.route,
            onClick = { navController.navigate(Screen.StoreRequests.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
            label = { Text("More", fontSize = 10.sp) },
            selected = currentRoute == Screen.StoreMore.route,
            onClick = { navController.navigate(Screen.StoreMore.route) }
        )
    }
}
