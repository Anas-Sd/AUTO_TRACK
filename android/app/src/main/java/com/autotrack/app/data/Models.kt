package com.autotrack.app.data

data class Category(
    val id: String,
    val name: String,
    val icon: String = "🏷️",
    val color: String = "#10B981",
    val monthlyCap: Double? = null
)

data class TransactionItem(
    val id: String = "",
    val vaultCode: String = "",
    val amount: Double = 0.0,
    val type: String = "expense", // "expense" or "income"
    val receiverVendor: String? = null,
    val categoryId: String? = null,
    val sourceApp: String = "UPI", // "UPI" or "Cash"
    val note: String? = null,
    val occurredAt: String = ""
)
