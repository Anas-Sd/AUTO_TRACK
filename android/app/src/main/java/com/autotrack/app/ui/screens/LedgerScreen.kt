package com.autotrack.app.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotrack.app.data.Category
import com.autotrack.app.data.TransactionItem
import com.autotrack.app.ui.components.EditTransactionDialog
import com.autotrack.app.ui.theme.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    transactions: List<TransactionItem>,
    categories: List<Category>,
    initialCategoryFilter: String? = null,
    onDeleteTransaction: (String) -> Unit,
    onEditTransaction: (
        id: String,
        amount: Double,
        type: String,
        vendor: String?,
        categoryId: String?,
        sourceApp: String,
        note: String?,
        occurredAt: String?
    ) -> Unit
) {
    val context = LocalContext.current

    // State for Search, Filters, Sort, and Dialog
    var searchQuery by remember { mutableStateOf("") }
    var isFilterExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    var selectedTimeframe by remember { mutableStateOf("All Time") } // "All Time", "Today", "This Month", "By Month", "Custom"
    var selectedMonth by remember { mutableStateOf("") } // e.g. "2026-09"
    var selectedCategoryFilter by remember { mutableStateOf(initialCategoryFilter ?: "All") } // "All" or categoryId
    var selectedTypeFilter by remember { mutableStateOf("All") } // "All", "Income", "Outcome"
    var selectedSourceFilter by remember { mutableStateOf("All") } // "All", "UPI", "Cash"
    var selectedSort by remember { mutableStateOf("Newest First") } // "Newest First", "Oldest First", "Amount High-Low", "Amount Low-High"

    var customStartDate by remember { mutableStateOf<String?>(null) }
    var customEndDate by remember { mutableStateOf<String?>(null) }

    // When category selected from Categories screen, set filter but KEEP menu closed
    LaunchedEffect(initialCategoryFilter) {
        if (!initialCategoryFilter.isNullOrBlank()) {
            selectedCategoryFilter = initialCategoryFilter
            isFilterExpanded = false
        }
    }

    // Reset filters
    fun resetAllFilters() {
        searchQuery = ""
        selectedTimeframe = "All Time"
        selectedMonth = ""
        selectedCategoryFilter = "All"
        selectedTypeFilter = "All"
        selectedSourceFilter = "All"
        selectedSort = "Newest First"
        customStartDate = null
        customEndDate = null
        isFilterExpanded = false
        Toast.makeText(context, "Filters reset to default", Toast.LENGTH_SHORT).show()
    }

    val isFilterActive = searchQuery.isNotBlank() || selectedTimeframe != "All Time" || selectedCategoryFilter != "All" || selectedTypeFilter != "All" || selectedSourceFilter != "All"

    // Date Range Picker launcher
    fun launchCustomDateRangePicker() {
        val cal = Calendar.getInstance()
        val startPicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val start = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                val endPicker = DatePickerDialog(
                    context,
                    { _, endYear, endMonth, endDay ->
                        val end = String.format(Locale.US, "%04d-%02d-%02d", endYear, endMonth + 1, endDay)
                        customStartDate = start
                        customEndDate = end
                        selectedTimeframe = "Custom"
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

    // Edit Transaction Dialog State
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }

    // Dropdown expanded states
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    // Available months in dataset
    val availableMonths = remember(transactions) {
        transactions.mapNotNull {
            if (it.occurredAt.length >= 7) it.occurredAt.take(7) else null
        }.distinct().sortedDescending()
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val sortedCategoriesForFilter = remember(categories, transactions) {
        com.autotrack.app.DataSyncManager.getSortedCategoriesByRecency(categories, transactions)
    }

    LaunchedEffect(
        searchQuery,
        selectedTimeframe,
        selectedMonth,
        selectedCategoryFilter,
        selectedTypeFilter,
        selectedSourceFilter,
        selectedSort,
        customStartDate,
        customEndDate
    ) {
        try {
            listState.scrollToItem(0)
        } catch (_: Exception) {}
    }

    // Filter & Sort Logic
    val filteredTransactions = remember(
        transactions,
        searchQuery,
        selectedTimeframe,
        selectedMonth,
        selectedCategoryFilter,
        selectedTypeFilter,
        selectedSourceFilter,
        selectedSort,
        customStartDate,
        customEndDate
    ) {
        val istZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istZone }.format(Date())
        val thisMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = istZone }.format(Date())

        transactions.filter { t ->
            // Search Query
            val matchSearch = searchQuery.isBlank() ||
                (t.receiverVendor?.contains(searchQuery, ignoreCase = true) == true) ||
                (t.note?.contains(searchQuery, ignoreCase = true) == true) ||
                t.amount.toString().contains(searchQuery)

            // Timeframe Filter
            val matchTimeframe = when (selectedTimeframe) {
                "Today" -> t.occurredAt.startsWith(todayStr)
                "This Month" -> t.occurredAt.startsWith(thisMonthStr)
                "By Month" -> if (selectedMonth.isNotBlank()) t.occurredAt.startsWith(selectedMonth) else true
                "Custom" -> {
                    if (customStartDate != null && customEndDate != null) {
                        val d = t.occurredAt.take(10)
                        d >= customStartDate!! && d <= customEndDate!!
                    } else true
                }
                else -> true
            }

            // Category Filter
            val matchCategory = when (selectedCategoryFilter) {
                "All" -> true
                "uncategorized", "Uncategorized" -> t.categoryId.isNullOrBlank() || t.categoryId.equals("uncategorized", ignoreCase = true)
                else -> t.categoryId == selectedCategoryFilter
            }

            // Type Filter
            val matchType = when (selectedTypeFilter) {
                "Income" -> t.type == "income"
                "Outcome" -> t.type == "expense"
                else -> true
            }

            // Source Filter
            val matchSource = when (selectedSourceFilter) {
                "UPI" -> t.sourceApp.equals("UPI", ignoreCase = true)
                "Cash" -> t.sourceApp.equals("Cash", ignoreCase = true)
                else -> true
            }

            matchSearch && matchTimeframe && matchCategory && matchType && matchSource
        }.let { list ->
            when (selectedSort) {
                "Oldest First" -> list.sortedBy { it.occurredAt }
                "Amount High-Low" -> list.sortedByDescending { it.amount }
                "Amount Low-High" -> list.sortedBy { it.amount }
                else -> list.sortedByDescending { it.occurredAt }
            }
        }
    }

    // Export CSV Function
    fun exportLedgerToCSV() {
        try {
            val fileName = "AutoTrack_Ledger_${System.currentTimeMillis()}.csv"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val csvFile = File(downloadsDir, fileName)

            FileWriter(csvFile).use { writer ->
                writer.append("ID,Date,Time,Type,Category,Amount(INR),PaymentMethod,Vendor,Note\n")
                filteredTransactions.forEach { item ->
                    val catObj = categories.find { c -> c.id == item.categoryId }
                    val catName = catObj?.name ?: "Uncategorized"
                    val datePart = item.occurredAt.take(10)
                    val timePart = if (item.occurredAt.length >= 19) item.occurredAt.substring(11, 19) else ""
                    writer.append("\"${item.id}\",\"$datePart\",\"$timePart\",\"${item.type}\",\"$catName\",${item.amount},\"${item.sourceApp}\",\"${item.receiverVendor ?: ""}\",\"${item.note ?: ""}\"\n")
                }
            }

            Toast.makeText(context, "Exported ${filteredTransactions.size} items to Downloads/$fileName", Toast.LENGTH_LONG).show()

            // Open Share Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "AutoTrack Ledger Export")
                putExtra(Intent.EXTRA_TEXT, "Exported ${filteredTransactions.size} transactions from AutoTrack.")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Ledger CSV"))

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Title (Fixed Top)
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = EmeraldPrimary)
                Text("Transaction Ledger", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text("Search, filter, and audit your transaction log", fontSize = 11.sp, color = TextMuted)
        }

        // 2. Search Bar (Fixed Top)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search vendor, note, or amount...", fontSize = 12.sp, color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
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

        // 3. Filter, Sort & Download Control Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filters Toggle Button
            OutlinedButton(
                onClick = { isFilterExpanded = !isFilterExpanded },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isFilterExpanded || isFilterActive) EmeraldPrimary.copy(alpha = 0.15f) else CardBg,
                    contentColor = if (isFilterExpanded || isFilterActive) EmeraldPrimary else Color.White
                ),
                border = BorderStroke(1.dp, if (isFilterExpanded || isFilterActive) EmeraldPrimary else BorderColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Sort Dropdown Button
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { isSortMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CardBg,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Sort: $selectedSort", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (isSortMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { isSortMenuExpanded = false },
                    modifier = Modifier.heightIn(max = 210.dp).background(CardBg).border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                ) {
                    listOf("Newest First", "Oldest First", "Amount High-Low", "Amount Low-High").forEach { option ->
                        val isSelected = selectedSort == option
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Sort: $option",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) EmeraldPrimary else Color.White
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            onClick = {
                                selectedSort = option
                                isSortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Reset Filters Button
            if (isFilterActive) {
                IconButton(
                    onClick = { resetAllFilters() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(RoseExpense.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, RoseExpense, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset Filters", tint = RoseExpense, modifier = Modifier.size(20.dp))
                }
            }

            // Download CSV Icon Button
            IconButton(
                onClick = { exportLedgerToCSV() },
                modifier = Modifier
                    .size(40.dp)
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download CSV", tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
            }
        }

        // 4. Expanded Filter Criteria Card
        if (isFilterExpanded) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder(enabled = true),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Text("Filter Criteria", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Text(
                            text = "Reset All",
                            fontSize = 11.sp,
                            color = RoseExpense,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { resetAllFilters() }
                        )
                    }

                    // Section 1: Timeframe
                    Column {
                        Text("Timeframe", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("All Time", "Today", "This Month", "By Month", "Custom")) { tf ->
                                val isSel = selectedTimeframe == tf
                                val tfLabel = if (tf == "Custom" && customStartDate != null && customEndDate != null) {
                                    "${customStartDate!!.takeLast(5)} - ${customEndDate!!.takeLast(5)}"
                                } else tf

                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        if (tf == "Custom") {
                                            launchCustomDateRangePicker()
                                        } else {
                                            selectedTimeframe = tf
                                        }
                                    },
                                    label = { Text(tfLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkBg,
                                        labelColor = TextMuted
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(borderColor = BorderColor, enabled = true, selected = isSel),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Month picker if "By Month" is selected
                        if (selectedTimeframe == "By Month" && availableMonths.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box {
                                OutlinedButton(
                                    onClick = { monthDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderColor),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(if (selectedMonth.isBlank()) "Select Month" else selectedMonth, fontSize = 11.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = monthDropdownExpanded,
                                    onDismissRequest = { monthDropdownExpanded = false },
                                    modifier = Modifier.background(CardBg)
                                ) {
                                    availableMonths.forEach { month ->
                                        DropdownMenuItem(
                                            text = { Text(month, fontSize = 11.sp, color = Color.White) },
                                            onClick = {
                                                selectedMonth = month
                                                monthDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Dropdowns Row (Category, Type, Source)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text("Category", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                OutlinedButton(
                                    onClick = { categoryDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg, contentColor = Color.White),
                                    border = BorderStroke(1.dp, BorderColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    val catLabel = when (selectedCategoryFilter) {
                                        "All" -> "All Categories"
                                        "uncategorized", "Uncategorized" -> "📦 Uncategorized"
                                        else -> categories.find { it.id == selectedCategoryFilter }?.name ?: "Category"
                                    }
                                    Text(catLabel, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 210.dp).background(CardBg)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Categories", fontSize = 11.sp, color = Color.White) },
                                    onClick = {
                                        selectedCategoryFilter = "All"
                                        categoryDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📦 Uncategorized", fontSize = 11.sp, color = Color.White) },
                                    onClick = {
                                        selectedCategoryFilter = "uncategorized"
                                        categoryDropdownExpanded = false
                                    }
                                )
                                sortedCategoriesForFilter.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${cat.icon} ${cat.name}", fontSize = 11.sp, color = Color.White) },
                                        onClick = {
                                            selectedCategoryFilter = cat.id
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Type Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text("Type", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                OutlinedButton(
                                    onClick = { typeDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg, contentColor = Color.White),
                                    border = BorderStroke(1.dp, BorderColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(if (selectedTypeFilter == "All") "All Types" else selectedTypeFilter, fontSize = 10.sp, maxLines = 1)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = typeDropdownExpanded,
                                onDismissRequest = { typeDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 210.dp).background(CardBg)
                            ) {
                                listOf("All", "Income", "Outcome").forEach { tOption ->
                                    DropdownMenuItem(
                                        text = { Text(if (tOption == "All") "All Types" else tOption, fontSize = 11.sp, color = Color.White) },
                                        onClick = {
                                            selectedTypeFilter = tOption
                                            typeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Source Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text("Source", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                OutlinedButton(
                                    onClick = { sourceDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg, contentColor = Color.White),
                                    border = BorderStroke(1.dp, BorderColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(if (selectedSourceFilter == "All") "All Sources" else selectedSourceFilter, fontSize = 10.sp, maxLines = 1)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = sourceDropdownExpanded,
                                onDismissRequest = { sourceDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 210.dp).background(CardBg)
                            ) {
                                listOf("All", "UPI", "Cash").forEach { sOption ->
                                    DropdownMenuItem(
                                        text = { Text(if (sOption == "All") "All Sources" else sOption, fontSize = 11.sp, color = Color.White) },
                                        onClick = {
                                            selectedSourceFilter = sOption
                                            sourceDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Items Count Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${filteredTransactions.size} Items", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
        }

        // 6. Independent Scrollable Transaction Cards List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions found.", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { item ->
                    val catObj = categories.find { it.id == item.categoryId }
                    val catName = catObj?.name ?: "Uncategorized"
                    val catIcon = catObj?.icon ?: "📦"

                    // Format Date & Time separately
                    val (formattedDate, formattedTime) = remember(item.occurredAt) {
                        try {
                            val istZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = istZone }
                            val date = input.parse(item.occurredAt.take(19))
                            if (date != null) {
                                val dateFmt = SimpleDateFormat("dd MMM", Locale.US).apply { timeZone = istZone }
                                val timeFmt = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = istZone }
                                Pair(dateFmt.format(date), timeFmt.format(date))
                            } else {
                                Pair(item.occurredAt.take(10), "")
                            }
                        } catch (e: Exception) {
                            Pair(item.occurredAt.take(10), "")
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder(enabled = true),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // LEFT: Note (big) & Category (small below it)
                            Column(
                                modifier = Modifier.weight(1.3f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                val mainTitle = when {
                                    !item.note.isNullOrBlank() -> item.note
                                    !item.receiverVendor.isNullOrBlank() -> item.receiverVendor
                                    else -> catName
                                }
                                Text(
                                    text = mainTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(catIcon, fontSize = 11.sp)
                                    Text(
                                        text = catName,
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // MIDDLE: Date followed by Time
                            Column(
                                modifier = Modifier.weight(0.9f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = formattedDate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = formattedTime,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }

                            // RIGHT: Amount & Cash/UPI badge
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "${if (item.type == "income") "+" else "-"}₹${item.amount.toInt()}",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.type == "income") EmeraldPrimary else RoseExpense
                                )

                                val isCash = item.sourceApp.equals("Cash", ignoreCase = true)
                                val badgeBg = if (isCash) Color(0xFFF59E0B).copy(alpha = 0.15f) else BlueAccent.copy(alpha = 0.15f)
                                val badgeBorder = if (isCash) Color(0xFFF59E0B).copy(alpha = 0.4f) else BlueAccent.copy(alpha = 0.4f)
                                val badgeText = if (isCash) Color(0xFFFBBF24) else BlueAccent

                                Surface(
                                    color = badgeBg,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, badgeBorder)
                                ) {
                                    Text(
                                        text = item.sourceApp,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // ACTIONS: Edit & Delete Buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                IconButton(
                                    onClick = { editingTransaction = item },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = { onDeleteTransaction(item.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Transaction Dialog Modal
    if (editingTransaction != null) {
        EditTransactionDialog(
            transaction = editingTransaction!!,
            categories = categories,
            onDismissRequest = { editingTransaction = null },
            onSaveTransaction = { id, amount, type, vendor, categoryId, sourceApp, note, occurredAt ->
                onEditTransaction(id, amount, type, vendor, categoryId, sourceApp, note, occurredAt)
            }
        )
    }
}
