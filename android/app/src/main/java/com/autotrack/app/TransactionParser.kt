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
        "failed", "declined", "pending", "otp", "secret code",
        "verification code", "due date", "bill generated"
    )

    // Regex for amount: ₹ 500, Rs. 1,200.50, INR 45, Rs 500, Amt 500, 500.00 debited, 500 INR, 500 Rs, paid 500, sent 500
    private val amountRegex = Regex(
        """(?:₹|rs\.?|inr|amt\.?)\s*:?\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:₹|rs\.?|inr|debited|credited|paid|spent|sent)|(?:paid|sent|spent|debited|credited|transferred)\s+([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Regex for vendor / payee extraction
    private val vendorPatterns = listOf(
        // Paid/Sent/Transferred [Amount] to [Vendor]
        Regex("""(?:paid|sent|paying|transferred|transfer)\s+(?:₹|rs\.?|inr|amt\.?)?\s*[\d,]+(?:\.\d{1,2})?\s+to\s+([A-Za-z0-9\s&@\.\-_]+?)(?:\s+(?:on|using|via|ref|from|upi|a\/c|account|successful|\.|$))""", RegexOption.IGNORE_CASE),
        // Paid to / Sent to / Transfer to [Vendor]
        Regex("""(?:paid to|transfer to|transferred to|sent to|paying to)\s+([A-Za-z0-9\s&@\.\-_]+?)(?:\s+(?:on|using|via|ref|from|upi|a\/c|account|successful|\.|$))""", RegexOption.IGNORE_CASE),
        // Received [Amount] from [Vendor]
        Regex("""(?:received|credited|added)\s+(?:₹|rs\.?|inr|amt\.?)?\s*[\d,]+(?:\.\d{1,2})?\s+from\s+([A-Za-z0-9\s&@\.\-_]+?)(?:\s+(?:on|using|via|ref|to|a\/c|account|\.|$))""", RegexOption.IGNORE_CASE),
        // Received from / Credited by [Vendor]
        Regex("""(?:received from|from|credited by)\s+([A-Za-z0-9\s&@\.\-_]+?)(?:\s+(?:on|using|via|ref|to|a\/c|account|\.|$))""", RegexOption.IGNORE_CASE),
        // Towards / At / VPA [Vendor]
        Regex("""(?:towards|vpa|at)\s+([A-Za-z0-9\s&@\.\-_]+?)(?:\s+(?:on|using|via|ref|\.|$))""", RegexOption.IGNORE_CASE)
    )

    fun parse(text: String, packageName: String): ParsedTransaction? {
        if (text.isBlank()) return null
        val lower = text.lowercase()

        // Skip negative/irrelevant notifications ONLY if no transaction completion indicator is present
        val isCompleted = "successful" in lower || "success" in lower || "paid" in lower || "debited" in lower || "credited" in lower || "sent" in lower || "transferred" in lower
        if (!isCompleted && negativeKeywords.any { it in lower }) {
            return null
        }
        if ("failed" in lower || "declined" in lower || "pending" in lower || "otp" in lower) {
            return null
        }

        // Match amount across all 3 regex groups
        val match = amountRegex.find(text) ?: return null
        val rawNum = match.groupValues.getOrNull(1)?.ifEmpty { null }
            ?: match.groupValues.getOrNull(2)?.ifEmpty { null }
            ?: match.groupValues.getOrNull(3)?.ifEmpty { null }
            ?: return null
        val amount = rawNum.replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // Determine transaction direction
        val isIncome = "credited" in lower || "received" in lower || "refund" in lower || "deposited" in lower || "added" in lower
        val isExpense = "debited" in lower || "paid" in lower || "spent" in lower || "sent" in lower || "purchase" in lower || "transferred" in lower || "deducted" in lower

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
                var candidate = vMatch.groupValues.getOrNull(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    candidate = candidate.replace(Regex("""[\.\,\;]+$"""), "").trim()
                    val lowerCand = candidate.lowercase()
                    if (candidate.length in 2..40 &&
                        lowerCand != "you" &&
                        lowerCand != "successful" &&
                        lowerCand != "success" &&
                        !lowerCand.startsWith("rs") &&
                        !lowerCand.startsWith("inr") &&
                        !lowerCand.startsWith("₹")
                    ) {
                        vendor = candidate
                        break
                    }
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
            packageName.contains("navi", ignoreCase = true) -> "Navi UPI"
            packageName.contains("super.money", ignoreCase = true) || packageName.contains("supermoney", ignoreCase = true) -> "super.money"
            packageName.contains("hdfc", ignoreCase = true) -> "HDFC Bank"
            packageName.contains("icici", ignoreCase = true) -> "ICICI Bank"
            packageName.contains("sbi", ignoreCase = true) -> "SBI"
            packageName.contains("axis", ignoreCase = true) -> "Axis Bank"
            packageName.contains("kotak", ignoreCase = true) -> "Kotak Bank"
            packageName.contains("messaging", ignoreCase = true) || packageName.contains("mms", ignoreCase = true) -> "Bank SMS"
            else -> "UPI / Bank"
        }
    }
}
