package com.example.stocksense.ui

import androidx.lifecycle.ViewModel
import com.example.stocksense.data.model.*
import com.example.stocksense.data.repository.StockRepository
import kotlinx.coroutines.flow.*

class StockViewModel(private val repository: StockRepository) : ViewModel() {

    val currentUser = repository.currentUser
    val products = repository.products
    val stores = repository.stores
    val inventory = repository.inventory
    val forecasts = repository.forecasts
    val requests = repository.requests
    val transfers = repository.transfers
    val alerts = repository.alerts
    val allocations = repository.allocations
    val sales = repository.sales
    val promotions = repository.promotions
    val invoices = repository.invoices
    val actionItems = repository.actionItems

    private val _currentAllocation = MutableStateFlow<Allocation?>(null)
    val currentAllocation = _currentAllocation.asStateFlow()

    fun login(email: String, password: String): Boolean {
        return repository.login(email, password) != null
    }

    fun logout() {
        repository.logout()
    }

    fun getRisk(productId: String, storeId: String): InventoryRisk {
        return repository.calculateRisk(productId, storeId)
    }

    fun createRequest(productId: String, storeId: String, quantity: Int, reason: String, priority: String) {
        val request = StockRequest(
            productId = productId,
            storeId = storeId,
            quantity = quantity,
            reason = reason,
            priority = priority
        )
        repository.createRequest(request)
    }

    fun updateRequestStatus(requestId: String, status: RequestStatus, approvedQty: Int? = null) {
        repository.updateRequestStatus(requestId, status, approvedQty)
    }

    fun importSalesData(records: List<SalesRecord>) {
        repository.importSalesData(records)
    }

    fun importProducts(products: List<Product>) {
        repository.importProducts(products)
    }

    fun importStores(storesList: List<Store>) {
        repository.importStores(storesList)
    }

    fun importInventory(inventoryList: List<Inventory>) {
        repository.importInventory(inventoryList)
    }

    fun importPromotions(promos: List<Promotion>) {
        repository.importPromotions(promos)
    }

    fun addStore(store: Store) {
        repository.addStore(store)
    }

    fun addProduct(product: Product, initialStock: Int, storeId: String) {
        repository.addProduct(product, initialStock, storeId)
    }

    fun prepareAllocation(productId: String) {
        _currentAllocation.value = repository.generateAllocation(productId)
    }

    fun updateAllocationAdjustment(storeId: String, adjustment: Int) {
        _currentAllocation.update { alloc ->
            alloc?.copy(items = alloc.items.map { 
                if (it.storeId == storeId) it.copy(manualAdjustment = adjustment) else it 
            })
        }
    }

    fun approveAllocation() {
        _currentAllocation.value?.let {
            repository.approveAllocation(it)
            _currentAllocation.value = null
        }
    }

    fun resetDemoData() {
        repository.resetDemoData()
    }
    
    fun confirmReceipt(transferId: String, receivedQty: Int) {
        repository.confirmReceipt(transferId, receivedQty)
    }
    
    fun simulateDeliveryDelay(transferId: String, days: Int) {
        repository.simulateDeliveryDelay(transferId, days)
    }
}
