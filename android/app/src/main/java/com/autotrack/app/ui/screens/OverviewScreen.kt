package com.autotrack.app.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotrack.app.data.Category
import com.autotrack.app.data.TransactionItem
import com.autotrack.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
    categories: List<Category>,
    userName: String = "User"
) {
    val context = LocalContext.current
    var selectedCatIndex by remember { mutableStateOf<Int?>(null) }
    var timeframeFilter by remember { mutableStateOf("Today") } // "Today", "Monthly", "Yearly", "Custom"

    var customStartDate by remember { mutableStateOf<String?>(null) }
    var customEndDate by remember { mutableStateOf<String?>(null) }

    // Date Range Picker launcher
    fun launchCustomDateRangePicker() {
        val cal = Calendar.getInstance()
        val startPicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val start = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                // Pick end date
                val endPicker = DatePickerDialog(
                    context,
                    { _, endYear, endMonth, endDay ->
                        val end = String.format(Locale.US, "%04d-%02d-%02d", endYear, endMonth + 1, endDay)
                        customStartDate = start
                        customEndDate = end
                        timeframeFilter = "Custom"
                        selectedCatIndex = null
                        Toast.makeText(context, "Filter applied: $start to $end", Toast.LENGTH_SHORT).show()
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                endPicker.setTitle("Select End Date")
                endPicker.show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        startPicker.setTitle("Select Start Date")
        startPicker.show()
    }

    // Bottom 3 Metrics Cards use ALL transactions
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "income" }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    // Filter transactions specifically for Donut & Category Breakdown
    val filteredDonutTransactions = remember(transactions, timeframeFilter, customStartDate, customEndDate) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val thisMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val thisYearStr = SimpleDateFormat("yyyy", Locale.US).format(Date())

        transactions.filter { t ->
            when (timeframeFilter) {
                "Today" -> t.occurredAt.startsWith(todayStr)
                "Monthly" -> t.occurredAt.startsWith(thisMonthStr)
                "Yearly" -> t.occurredAt.startsWith(thisYearStr)
                "Custom" -> {
                    if (customStartDate != null && customEndDate != null) {
                        val d = t.occurredAt.take(10)
                        d >= customStartDate!! && d <= customEndDate!!
                    } else true
                }
                else -> true
            }
        }
    }

    // Category breakdown list based ONLY on filtered transactions
    val categoryBreakdown = remember(filteredDonutTransactions, categories) {
        val map = mutableMapOf<String, Pair<Double, Double>>() // categoryId -> (expense, income)
        filteredDonutTransactions.forEach { t ->
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
            val openingCap = catObj?.monthlyCap
            val totalVol = pair.first + pair.second
            Triple(catId, name to icon, pair to openingCap) to totalVol
        }.sortedByDescending { it.second }
    }

    // Filtered net balance for center of Donut when nothing selected
    val filteredDonutNet = remember(filteredDonutTransactions) {
        val inc = filteredDonutTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val exp = filteredDonutTransactions.filter { it.type == "expense" }.sumOf { it.amount }
        inc - exp
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // 1. Header Title & Greeting
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Hi, $userName 👋",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(EmeraldPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Wallet, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                    }
                    Text("Financial Overview", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                }
            }
        }

        // 2. Donut Chart & Category Breakdown Card (With Timeframe Filter)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Timeframe Filter Bar (Pic 1 & Pic 5 Match Fix)
                    Surface(
                        color = DarkBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Today", "Monthly", "Yearly", "Custom").forEach { option ->
                                val isSelected = timeframeFilter == option
                                val label = if (option == "Custom" && customStartDate != null && customEndDate != null) {
                                    "${customStartDate!!.takeLast(5)} - ${customEndDate!!.takeLast(5)}"
                                } else option

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) EmeraldPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedCatIndex = null
                                            if (option == "Custom") {
                                                launchCustomDateRangePicker()
                                            } else {
                                                timeframeFilter = option
                                                Toast.makeText(context, "Filter applied: $option", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryBreakdown.isEmpty()) {
                        Text("No transactions in selected timeframe.", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(vertical = 24.dp))
                    } else {
                        val totalVolume = categoryBreakdown.sumOf { it.second }

                        // Donut Chart Box with interactive center
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .size(190.dp)
                                    .pointerInput(categoryBreakdown) {
                                        detectTapGestures { offset ->
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            val dx = offset.x - cx
                                            val dy = offset.y - cy
                                            val dist = Math.hypot(dx.toDouble(), dy.toDouble())

                                            // Radius check for donut ring
                                            if (dist >= 45.dp.toPx() && dist <= 95.dp.toPx()) {
                                                val angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                val normAngle = (angle - (-90f) + 360f) % 360f

                                                var currentAngle = 0f
                                                var tappedIdx: Int? = null
                                                categoryBreakdown.forEachIndexed { idx, pair ->
                                                    val sweep = if (totalVolume > 0) (pair.second / totalVolume * 360f).toFloat() else 0f
                                                    if (normAngle >= currentAngle && normAngle < currentAngle + sweep) {
                                                        tappedIdx = idx
                                                    }
                                                    currentAngle += sweep
                                                }
                                                selectedCatIndex = if (selectedCatIndex == tappedIdx) null else tappedIdx
                                            } else {
                                                selectedCatIndex = null
                                            }
                                        }
                                    }
                            ) {
                                var startAngle = -90f
                                val gapAngle = if (categoryBreakdown.size > 1) 4f else 0f
                                categoryBreakdown.forEachIndexed { idx, pair ->
                                    val sweep = if (totalVolume > 0) (pair.second / totalVolume * 360f).toFloat() else 0f
                                    val adjustedSweep = Math.max(1f, sweep - gapAngle)
                                    val adjustedStart = startAngle + (gapAngle / 2f)
                                    val isSelected = selectedCatIndex == idx
                                    val color = CategoryPalette[idx % CategoryPalette.size]

                                    drawArc(
                                        color = if (selectedCatIndex == null || isSelected) color else color.copy(alpha = 0.30f),
                                        startAngle = adjustedStart,
                                        sweepAngle = adjustedSweep,
                                        useCenter = false,
                                        style = Stroke(
                                            width = if (isSelected) 30.dp.toPx() else 24.dp.toPx(),
                                            cap = StrokeCap.Butt
                                        )
                                    )
                                    startAngle += sweep
                                }
                            }

                            // Dynamic Donut Center Display
                            if (selectedCatIndex != null && selectedCatIndex!! < categoryBreakdown.size) {
                                val item = categoryBreakdown[selectedCatIndex!!].first
                                val (name, icon) = item.second
                                val (pair, openingCap) = item.third
                                val (expense, income) = pair
                                val bal = income - expense
                                val remBal = if (openingCap != null) openingCap + income - expense else null

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(icon, fontSize = 14.sp)
                                        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("↗", fontSize = 12.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                        Text("+₹${income.toInt()}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("↘", fontSize = 12.sp, color = RoseExpense, fontWeight = FontWeight.Bold)
                                        Text("-₹${expense.toInt()}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = RoseExpense)
                                    }

                                    HorizontalDivider(color = BorderColor, modifier = Modifier.width(90.dp).padding(vertical = 2.dp))

                                    Text(
                                        text = "Bal: ${if (bal >= 0) "+" else "-"}₹${Math.abs(bal).toInt()}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (bal >= 0) EmeraldPrimary else RoseExpense
                                    )
                                    if (remBal != null) {
                                        Text(
                                            text = "Rem Bal: ₹${remBal.toInt()}",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            color = BlueAccent
                                        )
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("NET CASHFLOW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${if (filteredDonutNet >= 0) "+" else "-"}₹${Math.abs(filteredDonutNet).toInt()}",
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (filteredDonutNet >= 0) EmeraldPrimary else RoseExpense
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable Category List below Donut (Max 5 items visible, scrollable)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryBreakdown.forEachIndexed { idx, (itemData, _) ->
                                    val (name, icon) = itemData.second
                                    val (pair, _) = itemData.third
                                    val color = CategoryPalette[idx % CategoryPalette.size]
                                    val isSelected = selectedCatIndex == idx

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) color.copy(alpha = 0.20f) else DarkBg,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) color else BorderColor,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedCatIndex = if (selectedCatIndex == idx) null else idx
                                            }
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

        // 3. THIRD: Total Metrics Cards (Always shows overall total metrics)
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

                // Net Cashflow Card
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
    }
}
