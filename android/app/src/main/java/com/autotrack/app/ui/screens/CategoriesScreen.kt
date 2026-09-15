package com.autotrack.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.autotrack.app.data.Category
import com.autotrack.app.data.TransactionItem
import com.autotrack.app.ui.theme.*

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    transactions: List<TransactionItem>,
    onSelectCategory: (String) -> Unit,
    onCreateCategory: (name: String, icon: String, cap: Double?) -> Unit,
    onUpdateCategory: (id: String, name: String, icon: String, cap: Double?) -> Unit = { _, _, _, _ -> },
    onDeleteCategory: (id: String) -> Unit = {}
) {
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var editingCategoryId by remember { mutableStateOf<String?>(null) }
    var deletingCategoryId by remember { mutableStateOf<String?>(null) }

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

    val uncategorizedStats = remember(transactions) {
        var spent = 0.0
        var income = 0.0
        var count = 0
        transactions.forEach { t ->
            if (t.categoryId.isNullOrBlank() || t.categoryId.equals("uncategorized", ignoreCase = true)) {
                count++
                if (t.type == "income") income += t.amount else spent += t.amount
            }
        }
        Triple(spent, income, count)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Fixed Header Title & + Add Category Button at Top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = EmeraldPrimary)
                    Text("Categories & Balances", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("Track opening balances & totals", fontSize = 11.sp, color = TextMuted)
            }

            Button(
                onClick = {
                    editingCategoryId = null
                    deletingCategoryId = null
                    showCreateCategoryDialog = true
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // 2. Independent Scrollable Category Cards Container
        if (categories.isEmpty() && uncategorizedStats.third == 0) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text("No categories available. Tap Add to create one.", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                if (uncategorizedStats.third > 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder(enabled = true),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCategory("uncategorized") }
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(DarkBg, RoundedCornerShape(8.dp))
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("📦", fontSize = 15.sp)
                                        }
                                        Text("Uncategorized", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Text(
                                        text = "${uncategorizedStats.third} item(s)",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )
                                }
                                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("INCOME", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                        Text("+₹${uncategorizedStats.second.toInt()}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    }
                                    Column {
                                        Text("OUTCOME", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                        Text("-₹${uncategorizedStats.first.toInt()}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = RoseExpense)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("REMAINING", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                        val net = uncategorizedStats.second - uncategorizedStats.first
                                        Text(
                                            text = "${if (net >= 0) "+" else ""}₹${net.toInt()}",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (net >= 0) EmeraldPrimary else RoseExpense
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                items(categories, key = { it.id }) { cat ->
                    val stats = categoryStats.getOrDefault(cat.id, Pair(0.0, 0.0))
                    val spent = stats.first
                    val income = stats.second
                    val openingBal = cat.monthlyCap
                    val hasBalance = openingBal != null && openingBal > 0
                    val currentBal = if (hasBalance) openingBal!! + income - spent else 0.0

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder(enabled = true),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Header Row: Icon, Category Name, Opening Balance & Separate Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Icon & Name (Clickable to select category)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectCategory(cat.id) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(DarkBg, RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(cat.icon, fontSize = 15.sp)
                                    }
                                    Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // Opening Balance & Separate Edit / Delete Action Buttons
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (hasBalance) "Opening: ₹${openingBal!!.toInt()}" else "Opening: ₹0",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )

                                    // Separate Edit Button Box
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(EmeraldPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                deletingCategoryId = null
                                                showCreateCategoryDialog = false
                                                editingCategoryId = cat.id
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Category",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Separate Delete Button Box
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(RoseExpense.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .border(1.dp, RoseExpense.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                editingCategoryId = null
                                                showCreateCategoryDialog = false
                                                deletingCategoryId = cat.id
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Category",
                                            tint = RoseExpense,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                            // 3 Metrics Row: Income, Outcome/Expense, Remaining Balance (Clickable to view ledger)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCategory(cat.id) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("INCOME", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "+₹${income.toInt()}",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }

                                Column {
                                    Text("OUTCOME", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "-₹${spent.toInt()}",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = RoseExpense
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("REMAINING", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "₹${currentBal.toInt()}",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasBalance && currentBal < 0) RoseExpense else EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Category Dialog Modal
    if (showCreateCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newCatIcon by remember { mutableStateOf("📦") }
        var newCatCapText by remember { mutableStateOf("") }
        var catError by remember { mutableStateOf<String?>(null) }

        val presetIcons = listOf("🍔", "🛍️", "⚡", "🚗", "🎬", "🏠", "✈️", "💊", "📦", "💰", "🎓", "🎮")

        Dialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .statusBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable { showCreateCategoryDialog = false }
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Add New Category", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = { showCreateCategoryDialog = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                            }
                        }

                        // Category Name Field
                        Column {
                            Text("Category Name", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = {
                                    newCatName = it
                                    catError = null
                                },
                                placeholder = { Text("e.g. Groceries, Gym", fontSize = 11.sp, color = TextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkBg,
                                    unfocusedContainerColor = DarkBg,
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Icon Picker
                        Column {
                            Text("Choose Icon / Emoji", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(presetIcons) { icon ->
                                    val isSel = newCatIcon == icon
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(if (isSel) EmeraldPrimary.copy(alpha = 0.25f) else DarkBg, RoundedCornerShape(8.dp))
                                            .border(1.dp, if (isSel) EmeraldPrimary else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { newCatIcon = icon },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(icon, fontSize = 18.sp)
                                    }
                                }
                            }
                        }

                        // Opening Balance / Monthly Cap Field
                        Column {
                            Text("Opening Balance / Cap (Optional ₹)", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = newCatCapText,
                                onValueChange = { newCatCapText = it },
                                placeholder = { Text("e.g. 5000", fontSize = 11.sp, color = TextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkBg,
                                    unfocusedContainerColor = DarkBg,
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (catError != null) {
                            Text(catError!!, fontSize = 11.sp, color = RoseExpense, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCreateCategoryDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Text("Cancel", color = TextMuted, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (newCatName.isBlank()) {
                                        catError = "Enter category name"
                                        return@Button
                                    }
                                    val capVal = newCatCapText.toDoubleOrNull()
                                    onCreateCategory(newCatName.trim(), newCatIcon, capVal)
                                    showCreateCategoryDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Create", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Edit Category Dialog Modal
        if (editingCategoryId != null) {
            val catToEdit = categories.find { it.id == editingCategoryId }
            if (catToEdit != null) {
                var editName by remember(catToEdit.id) { mutableStateOf(catToEdit.name) }
                var editIcon by remember(catToEdit.id) { mutableStateOf(catToEdit.icon) }
                var editCapText by remember(catToEdit.id) {
                    mutableStateOf(catToEdit.monthlyCap?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "")
                }
                var editError by remember { mutableStateOf<String?>(null) }
                val quickIcons = listOf("🏷️", "🍔", "🛍️", "⚡", "🚗", "🎬", "💊", "💰", "📈", "🏠", "✈️", "🎮", "☕", "📱")

                Dialog(
                    onDismissRequest = { editingCategoryId = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { editingCategoryId = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .wrapContentHeight()
                                .clickable(enabled = false) {},
                            shape = RoundedCornerShape(20.dp),
                            color = CardBg,
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Edit Category", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    IconButton(onClick = { editingCategoryId = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                                    }
                                }

                                Column {
                                    Text("Category Name", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkBg,
                                            unfocusedContainerColor = DarkBg,
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = BorderColor,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Column {
                                    Text("Category Icon", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(quickIcons) { ico ->
                                            val isSelected = editIcon == ico
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else DarkBg, RoundedCornerShape(8.dp))
                                                    .border(1.dp, if (isSelected) EmeraldPrimary else BorderColor, RoundedCornerShape(8.dp))
                                                    .clickable { editIcon = ico },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(ico, fontSize = 18.sp)
                                            }
                                        }
                                    }
                                }

                                Column {
                                    Text("Opening Balance (Optional)", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = editCapText,
                                        onValueChange = { editCapText = it },
                                        placeholder = { Text("e.g. 5000", fontSize = 11.sp, color = TextMuted) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkBg,
                                            unfocusedContainerColor = DarkBg,
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = BorderColor,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (editError != null) {
                                    Text(editError!!, fontSize = 11.sp, color = RoseExpense, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { editingCategoryId = null },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Text("Cancel", color = TextMuted, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            if (editName.isBlank()) {
                                                editError = "Enter category name"
                                                return@Button
                                            }
                                            val capVal = editCapText.toDoubleOrNull()
                                            onUpdateCategory(catToEdit.id, editName.trim(), editIcon, capVal)
                                            editingCategoryId = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Category Confirmation Modal
        if (deletingCategoryId != null) {
            val catToDelete = categories.find { it.id == deletingCategoryId }
            if (catToDelete != null) {
                AlertDialog(
                    onDismissRequest = { deletingCategoryId = null },
                    title = {
                        Text("Delete Category", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            "Are you sure you want to delete category \"${catToDelete.name}\"? Existing transactions in this category will remain saved as Uncategorized.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDeleteCategory(catToDelete.id)
                                deletingCategoryId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseExpense),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Delete", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deletingCategoryId = null }) {
                            Text("Cancel", color = TextMuted, fontSize = 12.sp)
                        }
                    },
                    containerColor = CardBg
                )
            }
        }
    }
}
