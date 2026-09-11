package com.autotrack.app

data class ParsedTransaction(
    val amount: Double,
    val type: String, // "income" or "expense"
    val vendor: String?,
    val rawText: String,
    val sourceApp: String
)

object TransactionParser {

    private val negativeKeywords = listOf(
        "failed", "declined", "pending", "requested", "reminder", "otp", "secret code",
        "verification code", "due date", "bill generated", "will be debited"
    )

    // Regex for amount: ₹ 500, Rs. 1,200.50, INR 45
    private val amountRegex = Regex(
        """(?:₹|rs\.?|inr)\s?([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Regex for vendor / payee extraction
    private val vendorPatterns = listOf(
        Regex("""(?:paid to|transfer to|sent to|to)\s+([A-Za-z0-9\s&@\.\-_]+?)(?:\s+(?:on|using|via|ref|from|upi|a\/c|\.|$))""", RegexOption.IGNORE_CASE),
        Regex("""(?:at|vpa)\s+([A-Za-z0-9\s&@\.\-_]+?)(?:\s+(?:on|using|via|ref|\.|$))""", RegexOption.IGNORE_CASE),
        Regex("""(?:received from|from)\s+([A-Za-z0-9\s&@\.\-_]+?)(?:\s+(?:on|using|via|ref|to|a\/c|\.|$))""", RegexOption.IGNORE_CASE)
    )

    fun parse(text: String, packageName: String): ParsedTransaction? {
        if (text.isBlank()) return null
        val lower = text.lowercase()

        // Skip negative/irrelevant notifications
        if (negativeKeywords.any { it in lower }) {
            return null
        }

        // Match amount
        val match = amountRegex.find(text) ?: return null
        val rawNum = match.groupValues.getOrNull(1) ?: return null
        val amount = rawNum.replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // Determine transaction direction
        val isIncome = "credited" in lower || "received" in lower || "refund" in lower || "deposited" in lower
        val isExpense = "debited" in lower || "paid" in lower || "spent" in lower || "sent" in lower || "purchase" in lower

        val type = when {
            isIncome && !isExpense -> "income"
            isExpense -> "expense"
            else -> "expense" // Default fallback for payment alerts
        }

        // Extract vendor if available
        var vendor: String? = null
        for (pattern in vendorPatterns) {
            val vMatch = pattern.find(text)
            if (vMatch != null) {
                val candidate = vMatch.groupValues.getOrNull(1)?.trim()
                if (!candidate.isNullOrBlank() && candidate.length in 2..40 && !candidate.equals("you", ignoreCase = true)) {
                    vendor = candidate
                    break
                }
            }
        }

        val friendlySource = getFriendlyAppName(packageName)

        return ParsedTransaction(
            amount = amount,
            type = type,
            vendor = vendor,
            rawText = text,
            sourceApp = friendlySource
        )
    }

    fun getFriendlyAppName(packageName: String): String {
        return when {
            packageName.contains("nbu.paisa", ignoreCase = true) -> "Google Pay"
            packageName.contains("phonepe", ignoreCase = true) -> "PhonePe"
            packageName.contains("paytm", ignoreCase = true) -> "Paytm"
            packageName.contains("npci", ignoreCase = true) -> "BHIM"
            packageName.contains("hdfc", ignoreCase = true) -> "HDFC Bank"
            packageName.contains("icici", ignoreCase = true) -> "ICICI Bank"
            packageName.contains("sbi", ignoreCase = true) -> "SBI"
            packageName.contains("axis", ignoreCase = true) -> "Axis Bank"
            packageName.contains("kotak", ignoreCase = true) -> "Kotak Bank"
            else -> "UPI / Bank"
        }
    }
}
