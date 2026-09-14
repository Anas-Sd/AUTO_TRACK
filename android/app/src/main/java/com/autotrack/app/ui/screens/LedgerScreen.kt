package com.autotrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
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
fun LedgerScreen(
    transactions: List<TransactionItem>,
    categories: List<Category>,
    onDeleteTransaction: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Income", "Outcome", "UPI", "Cash"

    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { t ->
            val matchSearch = searchQuery.isBlank() ||
                (t.receiverVendor?.contains(searchQuery, ignoreCase = true) == true) ||
                (t.note?.contains(searchQuery, ignoreCase = true) == true) ||
                t.amount.toString().contains(searchQuery)

            val matchFilter = when (selectedFilter) {
                "Income" -> t.type == "income"
                "Outcome" -> t.type == "expense"
                "UPI" -> t.sourceApp == "UPI"
                "Cash" -> t.sourceApp == "Cash"
                else -> true
            }

            matchSearch && matchFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = EmeraldPrimary)
                    Text("Transaction Ledger", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("Search, filter, and audit your transaction log", fontSize = 11.sp, color = TextMuted)
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search vendor, note, or amount...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg,
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter Pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Income", "Outcome", "UPI", "Cash")) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = CardBg,
                            labelColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = BorderColor,
                            enabled = true,
                            selected = selectedFilter == filter
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Count Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${filteredTransactions.size} Items", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions found.", fontSize = 12.sp, color = TextMuted)
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { item ->
                val catObj = categories.find { it.id == item.categoryId }
                val catName = catObj?.name ?: "Uncategorized"
                val catIcon = catObj?.icon ?: "📦"

                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder(enabled = true),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(DarkBg, RoundedCornerShape(10.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(catIcon, fontSize = 16.sp)
                            }

                            Column {
                                Text(
                                    text = item.receiverVendor?.ifBlank { catName } ?: catName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(catName, fontSize = 10.sp, color = TextMuted)
                                    Text("•", fontSize = 10.sp, color = TextMuted)
                                    Text(item.sourceApp, fontSize = 10.sp, color = BlueAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${if (item.type == "income") "+" else "-"}₹${item.amount.toInt()}",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (item.type == "income") EmeraldPrimary else RoseExpense
                            )

                            IconButton(
                                onClick = { onDeleteTransaction(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
