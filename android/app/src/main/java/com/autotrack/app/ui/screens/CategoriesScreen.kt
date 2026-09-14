package com.autotrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotrack.app.data.Category
import com.autotrack.app.data.TransactionItem
import com.autotrack.app.ui.theme.*

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    transactions: List<TransactionItem>
) {
    val categoryStats = remember(transactions, categories) {
        val map = mutableMapOf<String, Pair<Double, Double>>() // categoryId -> (expense, income)
        transactions.forEach { t ->
            val catId = t.categoryId ?: return@forEach
            val current = map.getOrDefault(catId, Pair(0.0, 0.0))
            if (t.type == "income") {
                map[catId] = Pair(current.first, current.second + t.amount)
            } else {
                map[catId] = Pair(current.first + t.amount, current.second)
            }
        }
        map
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = EmeraldPrimary)
                    Text("Categories & Opening Balances", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("Track opening balances & transaction totals", fontSize = 11.sp, color = TextMuted)
            }
        }

        if (categories.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("No categories available.", fontSize = 12.sp, color = TextMuted)
                }
            }
        } else {
            items(categories, key = { it.id }) { cat ->
                val stats = categoryStats.getOrDefault(cat.id, Pair(0.0, 0.0))
                val spent = stats.first
                val income = stats.second
                val openingBal = cat.monthlyCap
                val hasBalance = openingBal != null && openingBal > 0
                val currentBal = if (hasBalance) openingBal!! + income - spent else 0.0
                val netSpent = spent - income
                val percent = if (hasBalance) Math.max(0.0, Math.min(((netSpent / openingBal!!) * 100), 100.0)).toInt() else 0
                val isOverBalance = hasBalance && currentBal < 0

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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(DarkBg, RoundedCornerShape(10.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat.icon, fontSize = 16.sp)
                                }
                                Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (spent > 0) {
                                    Text("-₹${spent.toInt()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = RoseExpense)
                                }
                                if (income > 0) {
                                    Text("+₹${income.toInt()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                                if (spent == 0.0 && income == 0.0) {
                                    Text("₹0", fontSize = 11.sp, color = TextMuted)
                                }
                            }

                            if (hasBalance) {
                                Text("Opening: ₹${openingBal!!.toInt()}", fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        if (hasBalance) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { percent / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = if (isOverBalance) RoseExpense else if (percent > 80) AmberWarning else EmeraldPrimary,
                                trackColor = DarkBg
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isOverBalance) "⚠️ Over opening balance" else "$percent% used",
                                    fontSize = 10.sp,
                                    color = if (isOverBalance) RoseExpense else TextMuted,
                                    fontWeight = if (isOverBalance) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "Rem: ₹${currentBal.toInt()}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No opening balance set", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}
