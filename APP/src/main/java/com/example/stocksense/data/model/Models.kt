package com.example.stocksense.data.model

import java.util.UUID

enum class StockStatus {
    HEALTHY, LOW_STOCK, CRITICAL, OUT_OF_STOCK, OVERSTOCK, DELIVERY_GAP
}

enum class RiskLevel {
    NONE, LOW, WATCH, MEDIUM, HIGH, CRITICAL, DELIVERY_RISK
}

enum class RequestStatus {
    PENDING, APPROVED, PARTIALLY_APPROVED, REJECTED, GENERATED, IN_TRANSIT, DELAYED, RECEIVED, PARTIALLY_RECEIVED, CANCELLED
}

enum class TransferStatus {
    SUGGESTED, PENDING_APPROVAL, APPROVED, GENERATED, DISPATCHED, IN_TRANSIT, DELAYED, RECEIVED, PARTIALLY_RECEIVED, CANCELLED
}

enum class InvoiceStatus {
    DRAFT, PENDING_APPROVAL, APPROVED, GENERATED, IN_TRANSIT, RECEIVED, PARTIALLY_RECEIVED, CANCELLED
}

enum class ActionSeverity {
    INFO, WARNING, WATCH, DELIVERY_RISK, CRITICAL
}

enum class UserRole {
    ADMIN, STORE_MANAGER
}

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: UserRole,
    val email: String,
    val password: String = "password123",
    val storeId: String? = null
)

data class Store(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val businessName: String = "",
    val type: String = "Retail",
    val address: String = "123 Business Rd, Industrial Area",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = "India",
    val contactNumber: String = "+91 98765 43210",
    val email: String = "",
    val warehouseCapacity: Int = 5000,
    val currentWarehouseStock: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val sku: String,
    val name: String,
    val category: String,
    val unit: String = "Units",
    val sellingPrice: Double,
    val warehouseStock: Int = 0,
    val leadTime: Int = 3, // days
    val isPromoted: Boolean = false,
    val seasonalityFactor: Double = 1.0,
    val description: String = ""
)

data class IncomingDelivery(
    val id: String = UUID.randomUUID().toString(),
    val quantity: Int,
    val expectedArrival: Long,
    val status: String = "In Transit",
    val originalExpectedArrival: Long = expectedArrival
)

data class Inventory(
    val productId: String,
    val storeId: String,
    val currentStock: Int,
    val incomingStock: Int = 0, // This should eventually be derived from incomingDeliveries
    val incomingDeliveries: List<IncomingDelivery> = emptyList(),
    val minStock: Int = 10,
    val maxStock: Int = 500
)

data class SalesRecord(
    val id: String = UUID.randomUUID().toString(),
    val storeId: String,
    val productId: String,
    val date: Long,
    val quantitySold: Int,
    val promotion: Boolean = false,
    val holiday: Boolean = false
)

data class Forecast(
    val productId: String,
    val storeId: String,
    val expectedDemand: Int,
    val dailyDemand: Double,
    val trend: Double, // percentage change
    val confidence: String, // High, Medium, Low
    val explanation: List<String>,
    val historicalAverage: Int,
    val seasonalityFactor: Double = 1.0,
    val promotionFactor: Double = 1.0,
    val isNewProduct: Boolean = false
)

data class InventoryRisk(
    val productId: String,
    val storeId: String,
    val projectedStock: Int,
    val daysOfCoverage: Int,
    val daysUntilDelivery: Int?,
    val status: StockStatus,
    val riskLevel: RiskLevel,
    val message: String = ""
)

data class AllocationItem(
    val storeId: String,
    val forecastDemand: Int,
    val currentStock: Int,
    val recommendedQuantity: Int,
    val manualAdjustment: Int = 0,
    val status: String = "Pending"
)

data class Allocation(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val warehouseStock: Int,
    val items: List<AllocationItem>,
    val status: String = "Pending", // Pending, Approved
    val timestamp: Long = System.currentTimeMillis(),
    val approvedBy: String? = null,
    val approvalDate: Long? = null,
    val invoiceId: String? = null
)

data class StockRequest(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val storeId: String,
    val quantity: Int,
    val approvedQuantity: Int? = null,
    val reason: String,
    val priority: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val invoiceId: String? = null
)

data class StockTransfer(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val fromStoreId: String,
    val toStoreId: String,
    val quantity: Int,
    val receivedQuantity: Int? = null,
    val reason: String,
    val status: TransferStatus = TransferStatus.PENDING_APPROVAL,
    val timestamp: Long = System.currentTimeMillis(),
    val invoiceId: String? = null,
    val expectedDeliveryDate: Long = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000L),
    val dispatchDate: Long? = null
)

data class InvoiceItem(
    val productId: String,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val rate: Double,
    val unit: String = "Units"
)

data class Invoice(
    val id: String = "INV-${System.currentTimeMillis() / 1000}",
    val requestId: String? = null,
    val allocationId: String? = null,
    val transferId: String? = null,
    val fromName: String,
    val fromAddress: String = "Main Warehouse, Sector 5, Chennai",
    val toName: String,
    val toAddress: String,
    val items: List<InvoiceItem>,
    val subtotal: Double,
    val tax: Double = 0.0,
    val total: Double,
    val createdDate: Long = System.currentTimeMillis(),
    val approvedBy: String,
    val status: InvoiceStatus = InvoiceStatus.GENERATED,
    val expectedDelivery: Long
)

data class ActionItem(
    val id: String = UUID.randomUUID().toString(),
    val severity: ActionSeverity,
    val title: String,
    val storeId: String? = null,
    val productId: String? = null,
    val problem: String,
    val reason: String,
    val recommendation: String,
    val impact: String,
    val actionType: String, // VIEW_FORECAST, REVIEW_ALLOCATION, etc.
    val relatedId: String? = null,
    val status: String = "OPEN", // OPEN, IN_REVIEW, COMPLETED, DISMISSED
    val timestamp: Long = System.currentTimeMillis()
)

data class Alert(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: String, // Stockout, Overstock, Demand Spike, etc.
    val productId: String? = null,
    val storeId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class Promotion(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val storeId: String,
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val discountPercent: Double,
    val expectedLift: Double // multiplier e.g. 1.2
)
