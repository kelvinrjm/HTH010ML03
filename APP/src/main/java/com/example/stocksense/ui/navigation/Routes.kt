package com.example.stocksense.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    
    // Admin (Inventory Manager) Routes
    object AdminDashboard : Screen("admin_dashboard")
    object AdminActionCenter : Screen("admin_action_center")
    object AdminInventory : Screen("admin_inventory")
    object AdminForecast : Screen("admin_forecast")
    object AdminAllocation : Screen("admin_allocation")
    object AdminRequests : Screen("admin_requests")
    object AdminMore : Screen("admin_more")
    object AdminReports : Screen("admin_reports")
    object AdminSettings : Screen("admin_settings")
    object AdminProfile : Screen("admin_profile")
    object AdminDataInputCenter : Screen("admin_data_input_center")
    object AdminImportData : Screen("admin_import_data")
    object AdminStoreSetup : Screen("admin_store_setup")
    object AdminAddProduct : Screen("admin_add_product")
    object AdminWhatIf : Screen("admin_what_if")
    object AdminInvoices : Screen("admin_invoices")
    object AdminDeliveryTracking : Screen("admin_delivery_tracking")
    
    object AdminProductDetail : Screen("admin_product_detail/{productId}/{storeId}") {
        fun createRoute(productId: String, storeId: String) = "admin_product_detail/$productId/$storeId"
    }

    object AdminInvoiceDetail : Screen("admin_invoice_detail/{invoiceId}") {
        fun createRoute(invoiceId: String) = "admin_invoice_detail/$invoiceId"
    }
    
    // Store Manager Routes
    object StoreHome : Screen("store_home")
    object StoreInventory : Screen("store_inventory")
    object StoreForecast : Screen("store_forecast")
    object StoreRequests : Screen("store_requests")
    object StoreMore : Screen("store_more")
    object StoreSettings : Screen("store_settings")
    object StoreProfile : Screen("store_profile")
    object StoreInvoices : Screen("store_invoices")
    object StoreDeliveryTracking : Screen("store_delivery_tracking")

    object StoreProductDetail : Screen("store_product_detail/{productId}") {
        fun createRoute(productId: String) = "store_product_detail/$productId"
    }

    object StoreInvoiceDetail : Screen("store_invoice_detail/{invoiceId}") {
        fun createRoute(invoiceId: String) = "store_invoice_detail/$invoiceId"
    }
}
