package com.autotrack.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotrack.app.data.Category
import com.autotrack.app.data.TransactionItem
import com.autotrack.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

val CategoryPalette = listOf(
    Color(0xFF10B981), // Emerald
    Color(0xFF3B82F6), // Blue
    Color(0xFFF59E0B), // Amber
    Color(0xFFEC4899), // Pink
    Color(0xFF8B5CF6), // Purple
    Color(0xFF06B6D4), // Cyan
    Color(0xFFF97316), // Orange
    Color(0xFF6366F1), // Indigo
    Color(0xFF14B8A6), // Teal
    Color(0xFFEF4444)  // Red
)

@Composable
fun OverviewScreen(
    transactions: List<TransactionItem>,
    categories: List<Category>
) {
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "income" }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // Category breakdown list
    val categoryBreakdown = remember(transactions, categories) {
        val map = mutableMapOf<String, Pair<Double, Double>>() // categoryId -> (expense, income)
        transactions.forEach { t ->
            val catKey = t.categoryId ?: "uncategorized"
            val current = map.getOrDefault(catKey, Pair(0.0, 0.0))
            if (t.type == "income") {
                map[catKey] = Pair(current.first, current.second + t.amount)
            } else {
                map[catKey] = Pair(current.first + t.amount, current.second)
            }
        }

        map.map { (catId, pair) ->
            val catObj = categories.find { it.id == catId }
            val name = catObj?.name ?: if (catId == "uncategorized") "Uncategorized" else "Category"
            val icon = catObj?.icon ?: "🏷️"
            val totalVol = pair.first + pair.second
            Triple(name, icon, pair) to totalVol
        }.sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Wallet, contentDescription = null, tint = EmeraldPrimary)
                    Text("Financial Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("Real-time cashflow analytics & category breakdown", fontSize = 11.sp, color = TextMuted)
            }
        }

        // Top 3 Metric Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Total Income Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(enabled = true),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL INCOME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("+₹${totalIncome.toInt()}", fontSize = 22.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                        Box(
                            modifier = Modifier.size(36.dp).background(EmeraldPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldPrimary)
                        }
                    }
                }

                // Total Expense Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(enabled = true),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL EXPENSE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("-₹${totalExpense.toInt()}", fontSize = 22.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = RoseExpense)
                        }
                        Box(
                            modifier = Modifier.size(36.dp).background(RoseExpense.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RoseExpense)
                        }
                    }
                }

                // Net Balance Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(enabled = true),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NET CASHFLOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${if (netBalance >= 0) "+" else "-"}₹${Math.abs(netBalance).toInt()}",
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance >= 0) EmeraldPrimary else RoseExpense
                            )
                        }
                        Box(
                            modifier = Modifier.size(36.dp).background(BlueAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wallet, contentDescription = null, tint = BlueAccent)
                        }
                    }
                }
            }
        }

        // Donut Chart & Category Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(enabled = true),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Text("Category Breakdown", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text("${categoryBreakdown.size} Categories", fontSize = 11.sp, color = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryBreakdown.isEmpty()) {
                        Text("No transactions recorded.", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(vertical = 24.dp))
                    } else {
                        // Canvas Donut Chart
                        val totalVolume = categoryBreakdown.sumOf { it.second }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(150.dp)) {
                                var startAngle = -90f
                                categoryBreakdown.forEachIndexed { idx, pair ->
                                    val sweep = if (totalVolume > 0) (pair.second / totalVolume * 360f).toFloat() else 0f
                                    val color = CategoryPalette[idx % CategoryPalette.size]
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("NET CASHFLOW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Text(
                                    text = "${if (netBalance >= 0) "+" else "-"}₹${Math.abs(netBalance).toInt()}",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netBalance >= 0) EmeraldPrimary else RoseExpense
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category List Items
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categoryBreakdown.forEachIndexed { idx, (triple, _) ->
                                val (name, icon, pair) = triple
                                val color = CategoryPalette[idx % CategoryPalette.size]

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkBg, RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(icon, fontSize = 16.sp)
                                        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
                                        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (pair.first > 0) {
                                            Text("-₹${pair.first.toInt()}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = RoseExpense)
                                        }
                                        if (pair.second > 0) {
                                            Text("+₹${pair.second.toInt()}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
