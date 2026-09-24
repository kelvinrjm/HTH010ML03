package com.example.stocksense.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.screens.admin.DetailedForecastCard
import com.example.stocksense.ui.screens.common.StoreBottomNavigation
import com.example.stocksense.ui.theme.Background
import com.example.stocksense.ui.theme.Navy
import com.example.stocksense.ui.theme.Silver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreForecastScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val forecasts by viewModel.forecasts.collectAsState()
    val products by viewModel.products.collectAsState()
    val inventory by viewModel.inventory.collectAsState()

    val storeId = currentUser?.storeId ?: ""
    val storeForecasts = forecasts.filter { it.storeId == storeId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Demand Forecast", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Silver
                )
            )
        },
        bottomBar = {
            StoreBottomNavigation(navController)
        }
    ) { padding ->
        if (storeForecasts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No forecast data available for this store.", color = Silver)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(storeForecasts) { forecast ->
                    val product = products.find { it.id == forecast.productId }
                    val inv = inventory.find { it.productId == forecast.productId && it.storeId == storeId }
                    val risk = viewModel.getRisk(forecast.productId, storeId)
                    
                    DetailedForecastCard(
                        product = product,
                        forecast = forecast,
                        inventory = inv,
                        risk = risk,
                        period = "7 Days"
                    )
                }
            }
        }
    }
}
