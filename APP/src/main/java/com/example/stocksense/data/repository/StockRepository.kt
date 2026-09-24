package com.example.stocksense.data.repository

import com.example.stocksense.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlin.math.roundToInt

class StockRepository {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    private val _inventory = MutableStateFlow<List<Inventory>>(emptyList())
    val inventory: StateFlow<List<Inventory>> = _inventory.asStateFlow()

    private val _forecasts = MutableStateFlow<List<Forecast>>(emptyList())
    val forecasts: StateFlow<List<Forecast>> = _forecasts.asStateFlow()

    private val _sales = MutableStateFlow<List<SalesRecord>>(emptyList())
    val sales: StateFlow<List<SalesRecord>> = _sales.asStateFlow()

    private val _promotions = MutableStateFlow<List<Promotion>>(emptyList())
    val promotions: StateFlow<List<Promotion>> = _promotions.asStateFlow()

    private val _requests = MutableStateFlow<List<StockRequest>>(emptyList())
    val requests: StateFlow<List<StockRequest>> = _requests.asStateFlow()

    private val _transfers = MutableStateFlow<List<StockTransfer>>(emptyList())
    val transfers: StateFlow<List<StockTransfer>> = _transfers.asStateFlow()

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _allocations = MutableStateFlow<List<Allocation>>(emptyList())
    val allocations: StateFlow<List<Allocation>> = _allocations.asStateFlow()

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

    private val _actionItems = MutableStateFlow<List<ActionItem>>(emptyList())
    val actionItems: StateFlow<List<ActionItem>> = _actionItems.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _registeredUsers = MutableStateFlow<List<User>>(emptyList())

    init {
        _registeredUsers.value = listOf(
            User(id = "U1", name = "Inventory Manager", role = UserRole.ADMIN, email = "manager@stocksense.com", password = "StockSense@123"),
            User(id = "U2", name = "Store Manager", role = UserRole.STORE_MANAGER, storeId = "S1", email = "store@stocksense.com", password = "StockSense@123")
        )
        resetDemoData()
    }

    fun login(email: String, password: String): User? {
        val user = _registeredUsers.value.find { it.email.trim().lowercase() == email.trim().lowercase() && it.password == password }
        if (user != null) {
            _currentUser.value = user
        }
        return user
    }

    fun logout() {
        _currentUser.value = null
    }

    fun resetDemoData() {
        val demoStores = listOf(
            Store(id = "S1", name = "City Center Supermarket", city = "Chennai", warehouseCapacity = 5000),
            Store(id = "S2", name = "Main Street Mart", city = "Coimbatore", warehouseCapacity = 3000),
            Store(id = "S3", name = "West End Store", city = "Madurai", warehouseCapacity = 4000)
        )
        _stores.value = demoStores

        val demoProducts = listOf(
            Product(id = "P1", sku = "RIC-001", name = "Basmati Rice 5kg", category = "Grains", sellingPrice = 450.0, warehouseStock = 1200),
            Product(id = "P2", sku = "OIL-002", name = "Sunflower Oil 1L", category = "Oil", sellingPrice = 120.0, warehouseStock = 800),
            Product(id = "P3", sku = "SLT-003", name = "Tata Salt 1kg", category = "Spices", sellingPrice = 25.0, warehouseStock = 500),
            Product(id = "P4", sku = "DAL-004", name = "Toor Dal 1kg", category = "Pulses", sellingPrice = 160.0, warehouseStock = 600),
            Product(id = "P5", sku = "ATT-005", name = "Whole Wheat Atta 5kg", category = "Flour", sellingPrice = 280.0, warehouseStock = 900)
        )
        _products.value = demoProducts

        val initialInventory = mutableListOf<Inventory>()
        demoProducts.forEach { product ->
            demoStores.forEach { store ->
                // Add some initial incoming deliveries for demo
                val deliveries = if ((0..1).random() == 1) {
                    listOf(IncomingDelivery(
                        quantity = (50..100).random(),
                        expectedArrival = System.currentTimeMillis() + ((2..5).random() * 24 * 60 * 60 * 1000L)
                    ))
                } else emptyList()

                initialInventory.add(Inventory(
                    productId = product.id,
                    storeId = store.id,
                    currentStock = 50 + (10..50).random(),
                    incomingStock = deliveries.sumOf { it.quantity },
                    incomingDeliveries = deliveries
                ))
            }
        }
        _inventory.value = initialInventory

        val initialSales = mutableListOf<SalesRecord>()
        demoProducts.forEach { product ->
            demoStores.forEach { store ->
                for (i in 1..21) {
                    initialSales.add(SalesRecord(
                        storeId = store.id,
                        productId = product.id,
                        date = System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000L),
                        quantitySold = (8..12).random()
                    ))
                }
            }
        }
        _sales.value = initialSales
        _promotions.value = emptyList()
        _requests.value = emptyList()
        _transfers.value = emptyList()
        _invoices.value = emptyList()
        _actionItems.value = emptyList()
        
        recalculateEverything()
    }

    fun recalculateEverything() {
        updateForecasts()
        updateActionCenter()
        updateAlerts()
    }

    private fun updateForecasts() {
        val newForecasts = mutableListOf<Forecast>()
        val productsList = _products.value
        val storesList = _stores.value
        val salesList = _sales.value
        val promosList = _promotions.value

        productsList.forEach { product ->
            storesList.forEach { store ->
                val productSales = salesList.filter { it.productId == product.id && it.storeId == store.id }
                
                var isNewProduct = false
                val historicalAvg = if (productSales.size >= 7) {
                    productSales.map { it.quantitySold }.average().toInt()
                } else {
                    isNewProduct = true
                    val categorySales = salesList.filter { 
                        it.storeId == store.id && 
                        productsList.find { p -> p.id == it.productId }?.category == product.category 
                    }
                    if (categorySales.isNotEmpty()) {
                        categorySales.map { it.quantitySold }.average().toInt()
                    } else {
                        10
                    }
                }
                
                val trendFactor = 1.05
                val seasonalFactor = product.seasonalityFactor
                val activePromo = promosList.find { 
                    it.productId == product.id && 
                    it.storeId == store.id && 
                    System.currentTimeMillis() in it.startDate..it.endDate 
                }
                val promotionFactor = activePromo?.expectedLift ?: (if (product.isPromoted) 1.2 else 1.0)
                
                val dailyDemand = (historicalAvg * trendFactor * seasonalFactor * promotionFactor)
                val expectedDemand = (dailyDemand * 7).roundToInt()
                
                val confidence = when {
                    productSales.size >= 21 -> "High"
                    productSales.size >= 7 -> "Medium"
                    else -> "Low"
                }

                val explanation = mutableListOf<String>()
                if (isNewProduct) {
                    explanation.add("Forecast is based on category baseline because this product has insufficient historical sales.")
                } else {
                    explanation.add("Historical daily average is $historicalAvg units.")
                    explanation.add("Recent sales trend shows 5% growth.")
                }
                if (promotionFactor > 1.0) explanation.add("Active promotion detected for this location.")
                if (seasonalFactor > 1.0) explanation.add("Seasonal demand lift accounted for.")

                newForecasts.add(Forecast(
                    productId = product.id,
                    storeId = store.id,
                    expectedDemand = expectedDemand,
                    dailyDemand = dailyDemand,
                    trend = 0.05,
                    confidence = confidence,
                    explanation = explanation,
                    historicalAverage = historicalAvg,
                    seasonalityFactor = seasonalFactor,
                    promotionFactor = promotionFactor,
                    isNewProduct = isNewProduct
                ))
            }
        }
        _forecasts.value = newForecasts
    }

    private fun updateAlerts() {
        val newAlerts = mutableListOf<Alert>()
        _inventory.value.forEach { inv ->
            val risk = calculateRisk(inv.productId, inv.storeId)
            val product = _products.value.find { it.id == inv.productId }
            val store = _stores.value.find { it.id == inv.storeId }
            
            if (product != null && store != null) {
                if (risk.riskLevel == RiskLevel.CRITICAL || risk.riskLevel == RiskLevel.HIGH) {
                    newAlerts.add(Alert(
                        title = "Critical Risk: ${product.name}",
                        message = risk.message,
                        type = "Stockout",
                        productId = product.id,
                        storeId = store.id
                    ))
                } else if (risk.status == StockStatus.OVERSTOCK) {
                    newAlerts.add(Alert(
                        title = "Overstock Alert: ${product.name}",
                        message = "${store.name} has excessive stock relative to forecast.",
                        type = "Overstock",
                        productId = product.id,
                        storeId = store.id
                    ))
                }
            }
        }
        _alerts.value = newAlerts
    }

    fun calculateRisk(productId: String, storeId: String): InventoryRisk {
        val inv = _inventory.value.find { it.productId == productId && it.storeId == storeId }
            ?: return InventoryRisk(productId, storeId, 0, 0, null, StockStatus.OUT_OF_STOCK, RiskLevel.CRITICAL, "No inventory record found.")
        
        val forecast = _forecasts.value.find { it.productId == productId && it.storeId == storeId }
            ?: return InventoryRisk(productId, storeId, inv.currentStock, 30, null, StockStatus.HEALTHY, RiskLevel.NONE, "No forecast available.")
        
        val dailyDemand = forecast.dailyDemand.coerceAtLeast(0.1)
        val daysOfCoverage = (inv.currentStock / dailyDemand).toInt()
        
        val nextDelivery = inv.incomingDeliveries.filter { it.expectedArrival > System.currentTimeMillis() }
            .minByOrNull { it.expectedArrival }
        
        val daysUntilDelivery = nextDelivery?.let { 
            ((it.expectedArrival - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt() 
        }

        val totalAvailable = inv.currentStock + inv.incomingStock
        val projectedStock = (totalAvailable - forecast.expectedDemand).coerceAtLeast(0)

        var status = when {
            inv.currentStock <= 0 -> StockStatus.OUT_OF_STOCK
            daysOfCoverage < 3 -> StockStatus.CRITICAL
            daysOfCoverage < 7 -> StockStatus.LOW_STOCK
            daysOfCoverage > 30 -> StockStatus.OVERSTOCK
            else -> StockStatus.HEALTHY
        }

        var riskLevel = when(status) {
            StockStatus.OUT_OF_STOCK -> RiskLevel.CRITICAL
            StockStatus.CRITICAL -> RiskLevel.HIGH
            StockStatus.LOW_STOCK -> RiskLevel.MEDIUM
            StockStatus.OVERSTOCK -> RiskLevel.LOW
            StockStatus.HEALTHY -> RiskLevel.NONE
            else -> RiskLevel.NONE
        }

        var message = "Inventory is currently ${status.name.lowercase().replace('_', ' ')}."

        if (daysUntilDelivery != null) {
            if (daysUntilDelivery > daysOfCoverage) {
                status = StockStatus.DELIVERY_GAP
                riskLevel = RiskLevel.DELIVERY_RISK
                message = "CRITICAL: Stock may run out approximately ${daysUntilDelivery - daysOfCoverage} days before incoming stock arrives."
            } else if (daysUntilDelivery > daysOfCoverage - 2) {
                riskLevel = RiskLevel.WATCH
                message = "WATCH: Delivery is close to projected stockout."
            }
        }

        return InventoryRisk(productId, storeId, projectedStock, daysOfCoverage, daysUntilDelivery, status, riskLevel, message)
    }

    private fun updateActionCenter() {
        val newActions = mutableListOf<ActionItem>()
        val productsList = _products.value
        val storesList = _stores.value
        val inventoryList = _inventory.value
        val forecastsList = _forecasts.value

        // 1. Critical Stockouts & Delivery Risks
        inventoryList.forEach { inv ->
            val risk = calculateRisk(inv.productId, inv.storeId)
            val product = productsList.find { it.id == inv.productId }
            val store = storesList.find { it.id == inv.storeId }

            if (product != null && store != null) {
                if (risk.riskLevel == RiskLevel.DELIVERY_RISK || risk.riskLevel == RiskLevel.CRITICAL) {
                    newActions.add(ActionItem(
                        severity = ActionSeverity.CRITICAL,
                        title = "Critical Stockout: ${product.name}",
                        storeId = store.id,
                        productId = product.id,
                        problem = "Stockout expected in ${risk.daysOfCoverage} days.",
                        reason = if (risk.riskLevel == RiskLevel.DELIVERY_RISK) "Incoming shipment delayed beyond stockout date." else "Current inventory is below forecast demand.",
                        recommendation = "Allocate units from warehouse or transfer from another store.",
                        impact = "Loss of sales for approximately ${risk.daysUntilDelivery?.minus(risk.daysOfCoverage) ?: 0} days.",
                        actionType = "REVIEW_ALLOCATION"
                    ))
                } else if (risk.riskLevel == RiskLevel.WATCH) {
                    newActions.add(ActionItem(
                        severity = ActionSeverity.WATCH,
                        title = "Delivery Watch: ${product.name}",
                        storeId = store.id,
                        productId = product.id,
                        problem = "Stock coverage (${risk.daysOfCoverage}d) is tight for delivery (${risk.daysUntilDelivery}d).",
                        reason = "Delivery schedule is close to depletion point.",
                        recommendation = "Monitor delivery or expedite if possible.",
                        impact = "Possible low stock during peak hours.",
                        actionType = "REVIEW_DELIVERY"
                    ))
                }
            }
        }

        // 2. Overstock Actions
        inventoryList.forEach { inv ->
            val risk = calculateRisk(inv.productId, inv.storeId)
            if (risk.status == StockStatus.OVERSTOCK) {
                val product = productsList.find { it.id == inv.productId }
                val store = storesList.find { it.id == inv.storeId }
                if (product != null && store != null) {
                    newActions.add(ActionItem(
                        severity = ActionSeverity.WARNING,
                        title = "Overstock: ${product.name}",
                        storeId = store.id,
                        productId = product.id,
                        problem = "Inventory significantly above projected demand.",
                        reason = "Stock coverage exceeds 30 days.",
                        recommendation = "Consider transferring excess stock to high-demand locations.",
                        impact = "Tied up capital and warehouse space.",
                        actionType = "CREATE_TRANSFER"
                    ))
                }
            }
        }

        // 3. Pending Approvals
        _allocations.value.filter { it.status == "Pending" }.forEach { alloc ->
            val product = productsList.find { it.id == alloc.productId }
            newActions.add(ActionItem(
                severity = ActionSeverity.CRITICAL,
                title = "Pending Allocation: ${product?.name ?: "Unknown"}",
                productId = alloc.productId,
                problem = "New AI Allocation recommended for this product.",
                reason = "Demand pattern changes detected.",
                recommendation = "Review and approve the suggested quantities.",
                impact = "Delayed fulfillment for multiple stores.",
                actionType = "REVIEW_ALLOCATION",
                relatedId = alloc.id
            ))
        }

        // 4. Pending Requests
        _requests.value.filter { it.status == RequestStatus.PENDING }.forEach { req ->
            val product = productsList.find { it.id == req.productId }
            val store = storesList.find { it.id == req.storeId }
            newActions.add(ActionItem(
                severity = ActionSeverity.WARNING,
                title = "Pending Request: ${product?.name}",
                storeId = req.storeId,
                productId = req.productId,
                problem = "Store request for ${req.quantity} units.",
                reason = req.reason,
                recommendation = "Review and approve or modify the request.",
                impact = "Store stock levels remaining low.",
                actionType = "REVIEW_REQUEST",
                relatedId = req.id
            ))
        }

        // 5. Active Transfers / Deliveries
        _transfers.value.filter { it.status == TransferStatus.IN_TRANSIT || it.status == TransferStatus.DELAYED }.forEach { tr ->
             val product = productsList.find { it.id == tr.productId }
             if (tr.status == TransferStatus.DELAYED) {
                 newActions.add(ActionItem(
                     severity = ActionSeverity.DELIVERY_RISK,
                     title = "Delayed Transfer: ${product?.name}",
                     productId = tr.productId,
                     problem = "Transfer TRF-${tr.id.takeLast(4)} is delayed.",
                     reason = "Logistic delay in transit.",
                     recommendation = "Check with logistics or initiate emergency stock move.",
                     impact = "Potential stockout at destination store.",
                     actionType = "TRACK_DELIVERY",
                     relatedId = tr.id
                 ))
             }
        }

        _actionItems.value = newActions.sortedBy { 
            when(it.severity) {
                ActionSeverity.CRITICAL -> 0
                ActionSeverity.DELIVERY_RISK -> 1
                ActionSeverity.WATCH -> 2
                ActionSeverity.WARNING -> 3
                ActionSeverity.INFO -> 4
            }
        }
    }

    fun addStore(store: Store) {
        if (_stores.value.any { it.id == store.id }) return
        _stores.update { it + store }
        recalculateEverything()
    }

    fun addProduct(product: Product, initialStock: Int, storeId: String) {
        if (_products.value.none { it.id == product.id || it.sku == product.sku }) {
            _products.update { it + product }
        }
        
        if (_inventory.value.none { it.productId == product.id && it.storeId == storeId }) {
            _inventory.update { it + Inventory(
                productId = product.id, 
                storeId = storeId, 
                currentStock = initialStock, 
                incomingStock = 0
            ) }
        }
        recalculateEverything()
    }

    fun importStores(storesList: List<Store>) {
        _stores.update { current ->
            val updated = current.toMutableList()
            storesList.forEach { s ->
                val idx = updated.indexOfFirst { it.id == s.id }
                if (idx != -1) updated[idx] = s else updated.add(s)
            }
            updated
        }
        recalculateEverything()
    }

    fun importProducts(productsList: List<Product>) {
        _products.update { current ->
            val updated = current.toMutableList()
            productsList.forEach { p ->
                val idx = updated.indexOfFirst { it.sku == p.sku }
                if (idx != -1) updated[idx] = p else updated.add(p)
            }
            updated
        }
        recalculateEverything()
    }

    fun importInventory(inventoryList: List<Inventory>) {
        _inventory.update { current ->
            val updated = current.toMutableList()
            inventoryList.forEach { inv ->
                val idx = updated.indexOfFirst { it.productId == inv.productId && it.storeId == inv.storeId }
                if (idx != -1) updated[idx] = inv else updated.add(inv)
            }
            updated
        }
        recalculateEverything()
    }

    fun importSalesData(records: List<SalesRecord>) {
        _sales.update { it + records }
        recalculateEverything()
    }

    fun importPromotions(promos: List<Promotion>) {
        _promotions.update { it + promos }
        recalculateEverything()
    }

    fun generateAllocation(productId: String): Allocation {
        val productInv = _inventory.value.filter { it.productId == productId }
        val product = _products.value.find { it.id == productId }
        val warehouseStock = product?.warehouseStock ?: 0
        val productForecasts = _forecasts.value.filter { it.productId == productId }
        
        val items = _stores.value.map { store ->
            val inv = productInv.find { it.storeId == store.id }?.currentStock ?: 0
            val demand = productForecasts.find { it.storeId == store.id }?.expectedDemand ?: 0
            AllocationItem(store.id, demand, inv, 0)
        }
        
        var remainingWarehouse = warehouseStock
        val sortedItems = items.map { it to (it.forecastDemand - it.currentStock).coerceAtLeast(0) }
            .sortedByDescending { it.second }
            
        val finalItems = sortedItems.map { (item, gap) ->
            val recommended = if (remainingWarehouse >= gap) gap else remainingWarehouse
            remainingWarehouse -= recommended
            item.copy(recommendedQuantity = recommended)
        }
        
        return Allocation(productId = productId, warehouseStock = warehouseStock, items = finalItems)
    }

    fun approveAllocation(allocation: Allocation) {
        val user = _currentUser.value
        val updatedItems = allocation.items.filter { (it.recommendedQuantity + it.manualAdjustment) > 0 }
        
        if (updatedItems.isEmpty()) return

        val product = _products.value.find { it.id == allocation.productId } ?: return
        
        val invoiceItems = updatedItems.map { item ->
            InvoiceItem(
                productId = product.id,
                productName = product.name,
                sku = product.sku,
                quantity = item.recommendedQuantity + item.manualAdjustment,
                rate = product.sellingPrice
            )
        }

        // In a real system, we might group by store, but here we'll create a single internal invoice for the allocation process
        // or a list of invoices. Let's create one invoice per store destination for clarity.
        
        updatedItems.forEach { item ->
            val store = _stores.value.find { it.id == item.storeId }
            val qty = item.recommendedQuantity + item.manualAdjustment
            
            val invoice = Invoice(
                allocationId = allocation.id,
                fromName = "Main Warehouse",
                toName = store?.name ?: "Unknown Store",
                toAddress = store?.address ?: "Store Address",
                items = listOf(InvoiceItem(product.id, product.name, product.sku, qty, product.sellingPrice)),
                subtotal = qty * product.sellingPrice,
                total = qty * product.sellingPrice,
                approvedBy = user?.name ?: "Manager",
                expectedDelivery = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L)
            )
            
            _invoices.update { it + invoice }

            val transfer = StockTransfer(
                productId = allocation.productId,
                fromStoreId = "WAREHOUSE",
                toStoreId = item.storeId,
                quantity = qty,
                reason = "AI Allocation",
                status = TransferStatus.GENERATED,
                invoiceId = invoice.id,
                expectedDeliveryDate = invoice.expectedDelivery
            )
            _transfers.update { it + transfer }

            // Create incoming delivery for the destination store
            updateInventoryIncoming(item.storeId, allocation.productId, qty, invoice.expectedDelivery)
        }

        _allocations.update { it + allocation.copy(
            status = "Approved", 
            approvedBy = user?.name, 
            approvalDate = System.currentTimeMillis()
        ) }
        
        recalculateEverything()
    }

    fun updateRequestStatus(requestId: String, status: RequestStatus, approvedQty: Int? = null) {
        val user = _currentUser.value
        _requests.update { list ->
            list.map { req ->
                if (req.id == requestId) {
                    var invoiceId = req.invoiceId
                    if (status == RequestStatus.APPROVED && req.invoiceId == null) {
                        val product = _products.value.find { it.id == req.productId }
                        val store = _stores.value.find { it.id == req.storeId }
                        val qty = approvedQty ?: req.quantity
                        
                        if (product != null) {
                            val invoice = Invoice(
                                requestId = req.id,
                                fromName = "Main Warehouse",
                                toName = store?.name ?: "Unknown Store",
                                toAddress = store?.address ?: "Store Address",
                                items = listOf(InvoiceItem(product.id, product.name, product.sku, qty, product.sellingPrice)),
                                subtotal = qty * product.sellingPrice,
                                total = qty * product.sellingPrice,
                                approvedBy = user?.name ?: "Manager",
                                expectedDelivery = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L)
                            )
                            _invoices.update { it + invoice }
                            invoiceId = invoice.id

                            val transfer = StockTransfer(
                                productId = req.productId,
                                fromStoreId = "WAREHOUSE",
                                toStoreId = req.storeId,
                                quantity = qty,
                                reason = "Stock Request Approval",
                                status = TransferStatus.GENERATED,
                                invoiceId = invoice.id,
                                expectedDeliveryDate = invoice.expectedDelivery
                            )
                            _transfers.update { it + transfer }
                            
                            updateInventoryIncoming(req.storeId, req.productId, qty, invoice.expectedDelivery)
                        }
                    }

                    val updated = req.copy(
                        status = status, 
                        approvedQuantity = approvedQty ?: req.approvedQuantity,
                        invoiceId = invoiceId
                    )
                    
                    if (status == RequestStatus.RECEIVED || status == RequestStatus.PARTIALLY_RECEIVED) {
                        applyInventoryChange(req.productId, req.storeId, approvedQty ?: req.quantity)
                        // Also update associated transfer and invoice
                        completeAssociatedTransfer(req.id, status, approvedQty ?: req.quantity)
                    }
                    updated
                } else req
            }
        }
        recalculateEverything()
    }

    private fun updateInventoryIncoming(storeId: String, productId: String, quantity: Int, arrival: Long) {
        _inventory.update { list ->
            list.map { inv ->
                if (inv.productId == productId && inv.storeId == storeId) {
                    val newDeliveries = inv.incomingDeliveries + IncomingDelivery(quantity = quantity, expectedArrival = arrival)
                    inv.copy(
                        incomingStock = newDeliveries.sumOf { it.quantity },
                        incomingDeliveries = newDeliveries
                    )
                } else inv
            }
        }
    }

    private fun completeAssociatedTransfer(requestId: String, status: RequestStatus, receivedQty: Int) {
        val transferStatus = if (status == RequestStatus.RECEIVED) TransferStatus.RECEIVED else TransferStatus.PARTIALLY_RECEIVED
        _transfers.update { list ->
            list.map { tr ->
                val associatedInvoice = _invoices.value.find { it.requestId == requestId }
                if (tr.invoiceId == associatedInvoice?.id) {
                    tr.copy(status = transferStatus, receivedQuantity = receivedQty)
                } else tr
            }
        }
        
        _invoices.update { list ->
            list.map { inv ->
                if (inv.requestId == requestId) {
                    inv.copy(status = if (status == RequestStatus.RECEIVED) InvoiceStatus.RECEIVED else InvoiceStatus.PARTIALLY_RECEIVED)
                } else inv
            }
        }
    }

    fun confirmReceipt(transferId: String, receivedQty: Int) {
        _transfers.update { list ->
            list.map { tr ->
                if (tr.id == transferId) {
                    val status = if (receivedQty >= tr.quantity) TransferStatus.RECEIVED else TransferStatus.PARTIALLY_RECEIVED
                    
                    // Update Inventory
                    applyInventoryChange(tr.productId, tr.toStoreId, receivedQty)
                    
                    // Remove from incoming
                    removeIncomingDelivery(tr.toStoreId, tr.productId, tr.quantity)

                    // Update Invoice Status
                    _invoices.update { invList ->
                        invList.map { inv ->
                            if (inv.id == tr.invoiceId) {
                                inv.copy(status = if (status == TransferStatus.RECEIVED) InvoiceStatus.RECEIVED else InvoiceStatus.PARTIALLY_RECEIVED)
                            } else inv
                        }
                    }

                    tr.copy(status = status, receivedQuantity = receivedQty)
                } else tr
            }
        }
        recalculateEverything()
    }

    private fun removeIncomingDelivery(storeId: String, productId: String, quantity: Int) {
        _inventory.update { list ->
            list.map { inv ->
                if (inv.productId == productId && inv.storeId == storeId) {
                    // Simple logic: remove the first one that matches quantity, or just reduce total
                    val updatedDeliveries = inv.incomingDeliveries.toMutableList()
                    val idx = updatedDeliveries.indexOfFirst { it.quantity == quantity }
                    if (idx != -1) updatedDeliveries.removeAt(idx) else if (updatedDeliveries.isNotEmpty()) updatedDeliveries.removeAt(0)
                    
                    inv.copy(
                        incomingStock = (inv.incomingStock - quantity).coerceAtLeast(0),
                        incomingDeliveries = updatedDeliveries
                    )
                } else inv
            }
        }
    }

    fun createRequest(request: StockRequest) {
        _requests.update { it + request }
        recalculateEverything()
    }

    private fun applyInventoryChange(productId: String, storeId: String, quantity: Int) {
        _inventory.update { list ->
            list.map { inv ->
                if (inv.productId == productId && inv.storeId == storeId) {
                    inv.copy(currentStock = inv.currentStock + quantity)
                } else inv
            }
        }
    }
    
    fun simulateDeliveryDelay(transferId: String, days: Int) {
        _transfers.update { list ->
            list.map { tr ->
                if (tr.id == transferId) {
                    val newDate = tr.expectedDeliveryDate + (days * 24 * 60 * 60 * 1000L)
                    
                    // Update inventory incoming delivery date as well
                    _inventory.update { invList ->
                        invList.map { inv ->
                            if (inv.productId == tr.productId && inv.storeId == tr.toStoreId) {
                                val updatedDeliveries = inv.incomingDeliveries.map { del ->
                                    if (del.expectedArrival == tr.expectedDeliveryDate) del.copy(expectedArrival = newDate) else del
                                }
                                inv.copy(incomingDeliveries = updatedDeliveries)
                            } else inv
                        }
                    }
                    
                    tr.copy(status = TransferStatus.DELAYED, expectedDeliveryDate = newDate)
                } else tr
            }
        }
        recalculateEverything()
    }
}
