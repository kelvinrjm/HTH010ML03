package com.example.stocksense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.stocksense.data.repository.StockRepository
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.StockSenseNavGraph
import com.example.stocksense.ui.theme.StockSenseTheme

class MainActivity : ComponentActivity() {
    companion object {
        // Shared repository for prototype state consistency
        val repository = StockRepository()
    }

    private val viewModel = StockViewModel(repository)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StockSenseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StockSenseNavGraph(viewModel)
                }
            }
        }
    }
}
