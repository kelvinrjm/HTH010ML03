package com.example.stocksense.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stocksense.data.model.*
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.screens.common.AdminBottomNavigation
import com.example.stocksense.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminForecastScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val forecasts by viewModel.forecasts.collectAsState()
    val products by viewModel.products.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val inventory by viewModel.inventory.collectAsState()

    var selectedStoreId by remember { mutableStateOf(stores.firstOrNull()?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf("All") }
    var selectedPeriod by remember { mutableStateOf("7 Days") }
    
    val categories = listOf("All") + products.map { it.category }.distinct()

    val filteredForecasts = forecasts.filter { forecast ->
        val product = products.find { it.id == forecast.productId }
        val matchesStore = forecast.storeId == selectedStoreId
        val matchesCategory = selectedCategoryId == "All" || product?.category == selectedCategoryId
        matchesStore && matchesCategory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demand Forecasting", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            // Filters Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Store Selector
                    ScrollableTabRow(
                        selectedTabIndex = stores.indexOfFirst { it.id == selectedStoreId }.coerceAtLeast(0),
                        containerColor = Color.Transparent,
                        edgePadding = 16.dp,
                        divider = {}
                    ) {
                        stores.forEach { store ->
                            Tab(
                                selected = selectedStoreId == store.id,
                                onClick = { selectedStoreId = store.id },
                                text = { Text(store.name) }
                            )
                        }
                    }

                    // Category & Period Filters
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { /* Show all filters modal */ },
                                label = { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        
                        // Category selection chips
                        categories.forEach { cat ->
                            item {
                                FilterChip(
                                    selected = selectedCategoryId == cat,
                                    onClick = { selectedCategoryId = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("7 Days", "30 Days", "90 Days").forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (filteredForecasts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No forecasts match the selected criteria.", color = SoftBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredForecasts) { forecast ->
                        val product = products.find { it.id == forecast.productId }
                        val inv = inventory.find { it.productId == forecast.productId && it.storeId == forecast.storeId }
                        val risk = viewModel.getRisk(forecast.productId, forecast.storeId)
                        
                        DetailedForecastCard(
                            product = product,
                            forecast = forecast,
                            inventory = inv,
                            risk = risk,
                            period = selectedPeriod
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedForecastCard(
    product: Product?,
    forecast: Forecast,
    inventory: Inventory?,
    risk: InventoryRisk,
    period: String
) {
    val multiplier = when(period) {
        "30 Days" -> 4.3 // approx weeks in month
        "90 Days" -> 12.8
        else -> 1.0
    }
    val adjustedDemand = (forecast.expectedDemand * multiplier).toInt()
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product?.name ?: "Unknown Product", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("SKU: ${product?.sku ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = SoftBlue)
                    
                    if (forecast.isNewProduct) {
                        Surface(
                            color = InfoBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                "NEW PRODUCT", 
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = InfoBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                val statusColor = when(risk.status) {
                    StockStatus.HEALTHY -> SuccessGreen
                    StockStatus.LOW_STOCK -> WarningAmber
                    StockStatus.CRITICAL, StockStatus.OUT_OF_STOCK -> CriticalRed
                    StockStatus.DELIVERY_GAP -> Color(0xFFE67E22)
                    StockStatus.OVERSTOCK -> SoftBlue
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Inventory Status", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text(
                        text = risk.status.name.replace("_", " "),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Delivery Awareness Section
            if (inventory?.incomingStock ?: 0 > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = (if (risk.status == StockStatus.DELIVERY_GAP) Color(0xFFE67E22) else InfoBlue).copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, (if (risk.status == StockStatus.DELIVERY_GAP) Color(0xFFE67E22) else InfoBlue).copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (risk.status == StockStatus.DELIVERY_GAP) Icons.Default.Warning else Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = if (risk.status == StockStatus.DELIVERY_GAP) Color(0xFFE67E22) else InfoBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Delivery Awareness", 
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (risk.status == StockStatus.DELIVERY_GAP) Color(0xFFE67E22) else InfoBlue
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Incoming Stock", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                                Text("${inventory?.incomingStock} Units", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val delivery = inventory?.incomingDeliveries?.minByOrNull { it.expectedArrival }
                                Text("Exp. Delivery", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                                Text(delivery?.let { dateFormat.format(Date(it.expectedArrival)) } ?: "N/A", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        
                        if (risk.status == StockStatus.DELIVERY_GAP) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                risk.message,
                                color = Color(0xFFE67E22),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Daily Avg Demand", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text("${forecast.historicalAverage} Units/day", fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Predicted Demand ($period)", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text("$adjustedDemand Units", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Navy)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Current Stock", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text("${inventory?.currentStock ?: 0} Units", fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Stock Coverage", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                    Text("${risk.daysOfCoverage} Days", fontWeight = FontWeight.Bold, color = if(risk.daysOfCoverage < 7) CriticalRed else Navy)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("AI Insights", style = MaterialTheme.typography.labelSmall, color = SoftBlue, fontWeight = FontWeight.Bold)
            forecast.explanation.take(2).forEach { reason ->
                Text("• $reason", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Demand Trend Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Background, RoundedCornerShape(4.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val path = Path()
                    val points = if(forecast.trend >= 0) listOf(0.4f, 0.45f, 0.5f, 0.6f, 0.55f, 0.7f, 0.85f) else listOf(0.8f, 0.75f, 0.7f, 0.6f, 0.65f, 0.5f, 0.4f)
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
                        color = if(forecast.trend >= 0) SuccessGreen else CriticalRed,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Past 7 Days", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val trendPct = (forecast.trend * 100).toInt()
                    Text(
                        text = if(forecast.trend >= 0) "↑ $trendPct%" else "↓ ${-trendPct}%", 
                        color = if(forecast.trend >= 0) SuccessGreen else CriticalRed,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(" Trend", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                }
                Text("Confidence: ${forecast.confidence}", style = MaterialTheme.typography.labelSmall, color = if(forecast.confidence == "High") SuccessGreen else WarningAmber)
            }
        }
    }
}
