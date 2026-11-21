package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyOption
import com.example.core.MoneyUtils
import com.example.domain.BudgetTransaction
import com.example.domain.Category
import com.example.domain.TransactionType
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.EmptyStatePanel
import com.example.ui.state.BudgetUiState
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun TransactionsAndBudgetScreen(
    uiState: BudgetUiState,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onEditTransaction: (BudgetTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onSaveCategory: (existingId: String?, name: String, colorHex: Long, budgetText: String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveBudgetSettings: (
        monthlyBudgetText: String,
        preventOverflow: Boolean,
        rolloverEnabled: Boolean,
        threshold1: Int,
        threshold2: Int,
        currencyCode: String
    ) -> Unit,
    onCloseMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    if (showCategoryDialog) {
        CategoryEditorDialog(
            initial = editingCategory,
            currencyCode = uiState.preferences.currencyCode,
            onDismiss = { showCategoryDialog = false },
            onSave = { id, name, color, budget ->
                onSaveCategory(id, name, color, budget)
                showCategoryDialog = false
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            listOf("المصروفات", "الدخل", "الميزانية").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                    modifier = Modifier.testTag("tx_tab_$index")
                ) {
                    Text(label)
                }
            }
        }

        when (selectedTab) {
            0 -> ExpensesTab(
                uiState = uiState,
                onAddExpenseClick = onAddExpenseClick,
                onAddCategoryClick = {
                    editingCategory = null
                    showCategoryDialog = true
                },
                onEditTransaction = onEditTransaction,
                onDeleteTransaction = onDeleteTransaction
            )
            1 -> IncomeTab(
                uiState = uiState,
                onAddIncomeClick = onAddIncomeClick,
                onEditTransaction = onEditTransaction,
                onDeleteTransaction = onDeleteTransaction
            )
            else -> MonthlyBudgetTab(
                uiState = uiState,
                onEditCategory = { cat ->
                    editingCategory = cat
                    showCategoryDialog = true
                },
                onAddCategory = {
                    editingCategory = null
                    showCategoryDialog = true
                },
                onDeleteCategory = onDeleteCategory,
                onSaveBudgetSettings = onSaveBudgetSettings,
                onCloseMonth = onCloseMonth
            )
        }
    }
}

@Composable
private fun ExpensesTab(
    uiState: BudgetUiState,
    onAddExpenseClick: () -> Unit,
    onAddCategoryClick: () -> Unit,
    onEditTransaction: (BudgetTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val currency = uiState.preferences.currencyCode

    val expenses = remember(uiState.transactions, searchQuery, selectedCategoryId) {
        uiState.transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
                (selectedCategoryId == null || tx.categoryId == selectedCategoryId) &&
                (searchQuery.isBlank() ||
                    tx.categoryName.contains(searchQuery, ignoreCase = true) ||
                    (tx.merchant?.contains(searchQuery, ignoreCase = true) == true) ||
                    (tx.notes?.contains(searchQuery, ignoreCase = true) == true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("ابحث باسم المتجر أو التصنيف أو الملاحظات...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                    label = { Text("الكل") }
                )
                uiState.categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id },
                        label = { Text(cat.name) }
                    )
                }
                OutlinedButton(onClick = onAddCategoryClick, shape = RoundedCornerShape(50)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("تصنيف جديد")
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "لا توجد مصروفات مطابقة",
                    subtitle = "أضف مصروفاً جديداً أو عدل كلمات البحث."
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onAddExpenseClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة مصروف")
                }
            }
        } else {
            items(expenses, key = { it.id }) { tx ->
                TransactionRowCard(
                    tx = tx,
                    categories = uiState.categories,
                    currencyCode = currency,
                    onEdit = { onEditTransaction(tx) },
                    onDelete = { onDeleteTransaction(tx.id) }
                )
            }
        }
    }
}

@Composable
private fun IncomeTab(
    uiState: BudgetUiState,
    onAddIncomeClick: () -> Unit,
    onEditTransaction: (BudgetTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    val currency = uiState.preferences.currencyCode
    val activeYm = uiState.snapshot?.activeYearMonth ?: MoneyUtils.currentYearMonth()
    val incomes = remember(uiState.transactions) {
        uiState.transactions.filter { it.type == TransactionType.INCOME }
    }
    val activeMonthIncome = incomes.filter { it.yearMonth == activeYm }.sumOf { it.amountPiasters }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "إجمالي الدخل المسجل (${MoneyUtils.yearMonthLabelAr(activeYm)})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = MoneyUtils.formatCurrency(activeMonthIncome, currency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onAddIncomeClick,
                        modifier = Modifier.fillMaxWidth().testTag("add_income_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.TrendingUp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تسجيل راتب أو دخل إضافي")
                    }
                }
            }
        }

        if (incomes.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Rounded.TrendingUp,
                    title = "سجل الدخل فارغ",
                    subtitle = "أضف الراتب الشهري أو المكافآت أو مصادر الدخل الإضافية."
                )
            }
        } else {
            items(incomes, key = { it.id }) { tx ->
                TransactionRowCard(
                    tx = tx,
                    categories = uiState.categories,
                    currencyCode = currency,
                    onEdit = { onEditTransaction(tx) },
                    onDelete = { onDeleteTransaction(tx.id) }
                )
            }
        }
    }
}

@Composable
private fun MonthlyBudgetTab(
    uiState: BudgetUiState,
    onEditCategory: (Category) -> Unit,
    onAddCategory: () -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveBudgetSettings: (String, Boolean, Boolean, Int, Int, String) -> Unit,
    onCloseMonth: () -> Unit
) {
    val hh = uiState.snapshot?.household
    val currency = uiState.preferences.currencyCode
    var budgetText by remember(hh?.monthlyBudgetPiasters) {
        mutableStateOf(hh?.let { MoneyUtils.piastersToInputString(it.monthlyBudgetPiasters) } ?: "")
    }
    var preventOverflow by remember(hh?.preventBudgetOverflow) { mutableStateOf(hh?.preventBudgetOverflow ?: false) }
    var rolloverEnabled by remember(hh?.rolloverEnabled) { mutableStateOf(hh?.rolloverEnabled ?: true) }
    var showCloseMonthDialog by remember { mutableStateOf(false) }

    if (showCloseMonthDialog && uiState.snapshot != null) {
        val snap = uiState.snapshot
        AlertDialog(
            onDismissRequest = { showCloseMonthDialog = false },
            title = { Text("إغلاق شهر ${MoneyUtils.yearMonthLabelAr(snap.activeYearMonth)}", fontWeight = FontWeight.Bold) },
            text = {
                val rollover = if (rolloverEnabled) snap.remainingBudgetPiasters.coerceAtLeast(0L) else 0L
                Text(
                    "سيتم أرشفة مصروفات الشهر (${MoneyUtils.formatCurrency(snap.totalExpensePiasters, currency)}) والانتقال إلى الشهر التالي (${MoneyUtils.yearMonthLabelAr(MoneyUtils.nextYearMonth(snap.activeYearMonth))}).\n" +
                        if (rolloverEnabled) "سيتم ترحيل الرصيد المتبقي: ${MoneyUtils.formatCurrency(rollover, currency)}"
                        else "الترحيل معطل — ستبدأ الميزانية من الصفر."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showCloseMonthDialog = false
                    onCloseMonth()
                }) { Text("تأكيد الإغلاق") }
            },
            dismissButton = {
                TextButton(onClick = { showCloseMonthDialog = false }) { Text("إلغاء") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "إعدادات الميزانية الشهرية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text("الميزانية الشهرية الإجمالية (${CurrencyOption.fromCode(currency).symbolAr})") },
                        placeholder = { Text("اتركه 0 لاستخدام مجموع ميزانيات التصنيفات") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("monthly_budget_input")
                    )
                    if ((hh?.carriedOverPiasters ?: 0L) > 0L) {
                        Text(
                            text = "رصيد مرحّل من الشهر السابق: +${MoneyUtils.formatCurrency(hh?.carriedOverPiasters ?: 0L, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("منع تجاوز الميزانية اختيارياً", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("رفض إضافة مصروف يتجاوز الميزانية المتاحة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = preventOverflow, onCheckedChange = { preventOverflow = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ترحيل الرصيد المتبقي للشهر التالي", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("إضافة الفائض عند إغلاق الشهر تلقائياً", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = rolloverEnabled, onCheckedChange = { rolloverEnabled = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onSaveBudgetSettings(
                                    budgetText,
                                    preventOverflow,
                                    rolloverEnabled,
                                    hh?.alertThreshold1 ?: 70,
                                    hh?.alertThreshold2 ?: 90,
                                    currency
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("save_budget_settings_button")
                        ) { Text("حفظ الميزانية") }
                        OutlinedButton(
                            onClick = { showCloseMonthDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.CloseFullscreen, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("إغلاق الشهر")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ميزانية كل تصنيف",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onAddCategory) {
                    Icon(Icons.Rounded.Category, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة تصنيف")
                }
            }
        }

        val summaries = uiState.snapshot?.categorySummaries ?: emptyList()
        items(summaries, key = { it.category.id }) { summary ->
            val cat = summary.category
            val ratio = if (summary.budgetPiasters > 0L) {
                (summary.spentPiasters.toFloat() / summary.budgetPiasters.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val barColor = when {
                summary.budgetPiasters > 0L && summary.spentPiasters >= summary.budgetPiasters -> DangerRed
                ratio >= 0.7f -> WarningAmber
                else -> Color(cat.colorHex.toInt())
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditCategory(cat) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(iconKey = cat.iconKey, colorHex = cat.colorHex, sizeDp = 40)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            val budgetLabel = if (cat.budgetPiasters > 0L) {
                                "الميزانية: ${MoneyUtils.formatCurrency(cat.budgetPiasters, currency)}"
                            } else "اضغط لتحديد ميزانية"
                            Text(budgetLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = MoneyUtils.formatCurrency(summary.spentPiasters, currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }
                    if (cat.budgetPiasters > 0L) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                            color = barColor
                        )
                    }
                    if (cat.customCategory) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onDeleteCategory(cat.id) }) {
                                Text("حذف التصنيف", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
