package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyOption
import com.example.core.MoneyUtils
import com.example.domain.FundEntry
import com.example.domain.FundEntryType
import com.example.domain.InventoryItem
import com.example.domain.ShoppingFrequency
import com.example.domain.ShoppingItem
import com.example.domain.SinkingFund
import com.example.domain.TransactionType
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.EmptyStatePanel
import com.example.ui.state.BudgetUiState
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LocalMasroufExtraColors
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PlanningHubScreen(
    uiState: BudgetUiState,
    initialSubTab: Int = 0,
    onAddFund: (name: String, targetText: String, targetDateMillis: Long) -> Unit,
    onFundMovement: (fund: SinkingFund, type: FundEntryType, amountText: String, note: String?) -> Unit,
    onSaveShoppingItem: (
        existingId: String?,
        name: String,
        freq: ShoppingFrequency,
        qtyLabel: String,
        expectedPriceText: String,
        categoryId: String
    ) -> Unit,
    onToggleShoppingPurchased: (ShoppingItem) -> Unit,
    onConvertShoppingToExpense: (ShoppingItem, String) -> Unit,
    onDeleteShoppingItem: (String) -> Unit,
    onSaveInventoryItem: (
        existingId: String?,
        name: String,
        qtyText: String,
        minText: String,
        unit: String,
        expiryMillis: Long
    ) -> Unit,
    onAdjustInventory: (InventoryItem, Double) -> Unit,
    onAddInventoryToShopping: (InventoryItem) -> Unit,
    onDeleteInventoryItem: (String) -> Unit,
    onCompleteWeeklyReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    val tabs = listOf("المخصصات السنوية", "المشتريات الدورية", "مخزون المنزل", "المراجعة الأسبوعية")

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("planning_tab_$index")
                )
            }
        }

        when (selectedTab) {
            0 -> AnnualFundsSection(
                funds = uiState.funds,
                entries = uiState.fundEntries,
                currencyCode = uiState.preferences.currencyCode,
                onAddFund = onAddFund,
                onFundMovement = onFundMovement
            )
            1 -> PeriodicShoppingSection(
                items = uiState.shoppingItems,
                categories = uiState.categories,
                currencyCode = uiState.preferences.currencyCode,
                onSaveItem = onSaveShoppingItem,
                onTogglePurchased = onToggleShoppingPurchased,
                onConvertToExpense = onConvertShoppingToExpense,
                onDelete = onDeleteShoppingItem
            )
            2 -> HomeInventorySection(
                items = uiState.inventoryItems,
                onSaveItem = onSaveInventoryItem,
                onAdjust = onAdjustInventory,
                onAddToShopping = onAddInventoryToShopping,
                onDelete = onDeleteInventoryItem
            )
            else -> WeeklyReviewSection(
                uiState = uiState,
                onCompleteWeeklyReview = onCompleteWeeklyReview
            )
        }
    }
}

@Composable
private fun AnnualFundsSection(
    funds: List<SinkingFund>,
    entries: List<FundEntry>,
    currencyCode: String,
    onAddFund: (String, String, Long) -> Unit,
    onFundMovement: (SinkingFund, FundEntryType, String, String?) -> Unit
) {
    val extra = LocalMasroufExtraColors.current
    var showAddFund by remember { mutableStateOf(false) }
    var activeFundAction by remember { mutableStateOf<Pair<SinkingFund, FundEntryType>?>(null) }

    if (showAddFund) {
        var name by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("") }
        var monthsAhead by remember { mutableIntStateOf(12) }
        AlertDialog(
            onDismissRequest = { showAddFund = false },
            title = { Text("إنشاء مخصص ادخار سنوي", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المخصص (مثال: مصاريف المدارس)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("المبلغ المستهدف (${CurrencyOption.fromCode(currencyCode).symbolAr})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 6, 12).forEach { m ->
                            FilterChip(
                                selected = monthsAhead == m,
                                onClick = { monthsAhead = m },
                                label = { Text("بعد $m شهور") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val targetDate = System.currentTimeMillis() + monthsAhead * 30L * 24L * 3600L * 1000L
                    onAddFund(name, target, targetDate)
                    showAddFund = false
                }) { Text("إنشاء") }
            },
            dismissButton = { TextButton(onClick = { showAddFund = false }) { Text("إلغاء") } }
        )
    }

    activeFundAction?.let { (fund, type) ->
        var amount by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { activeFundAction = null },
            title = {
                Text(
                    text = "${type.labelAr}: ${fund.name}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "التحويل إلى المخصصات لا يُحتسب مصروفاً منزلياً مكرراً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("المبلغ (${CurrencyOption.fromCode(currencyCode).symbolAr})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("fund_movement_amount")
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("ملاحظة (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onFundMovement(fund, type, amount, note)
                    activeFundAction = null
                }) { Text("تأكيد ${type.labelAr}") }
            },
            dismissButton = { TextButton(onClick = { activeFundAction = null }) { Text("إلغاء") } }
        )
    }

    val totalSaved = funds.sumOf { it.savedPiasters }
    val totalTarget = funds.sumOf { it.targetPiasters }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("إجمالي المدخر في المخصصات", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = MoneyUtils.formatCurrency(totalSaved, currencyCode),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "من إجمالي المستهدف: ${MoneyUtils.formatCurrency(totalTarget, currencyCode)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(onClick = { showAddFund = true }, shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("صندوق")
                        }
                    }
                }
            }
        }

        items(funds, key = { it.id }) { fund ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(iconKey = fund.iconKey, colorHex = 0xFFD4AF37)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fund.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "موعد الهدف: ${MoneyUtils.formatDateAr(fund.targetDateMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = extra.accentGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${(fund.progressPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { fund.progressPercent },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = extra.accentGold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "المدخر: ${MoneyUtils.formatCurrency(fund.savedPiasters, currencyCode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "المتبقي: ${MoneyUtils.formatCurrency(fund.remainingPiasters, currencyCode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { activeFundAction = fund to FundEntryType.DEPOSIT },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("إيداع") }
                        OutlinedButton(
                            onClick = { activeFundAction = fund to FundEntryType.WITHDRAWAL },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("سحب") }
                    }
                }
            }
        }

        if (entries.isNotEmpty()) {
            item {
                Text(
                    text = "سجل الإيداعات والسحوبات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(entries.take(12), key = { it.id }) { entry ->
                val fundName = funds.firstOrNull { it.id == entry.fundId }?.name ?: "صندوق"
                val isDep = entry.entryType == FundEntryType.DEPOSIT
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${entry.entryType.labelAr} • $fundName", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${MoneyUtils.formatDateAr(entry.dateEpochMillis)} • ${entry.addedByName}" +
                                    (entry.note?.let { " • $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${if (isDep) "+" else "-"}${MoneyUtils.formatCurrency(entry.amountPiasters, currencyCode)}",
                            fontWeight = FontWeight.Bold,
                            color = if (isDep) SuccessGreen else DangerRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodicShoppingSection(
    items: List<ShoppingItem>,
    categories: List<com.example.domain.Category>,
    currencyCode: String,
    onSaveItem: (String?, String, ShoppingFrequency, String, String, String) -> Unit,
    onTogglePurchased: (ShoppingItem) -> Unit,
    onConvertToExpense: (ShoppingItem, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var selectedFreq by remember { mutableStateOf<ShoppingFrequency?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var convertingItem by remember { mutableStateOf<ShoppingItem?>(null) }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("1") }
        var price by remember { mutableStateOf("") }
        var freq by remember { mutableStateOf(selectedFreq ?: ShoppingFrequency.WEEKLY) }
        var catId by remember {
            mutableStateOf(categories.firstOrNull { it.id.contains("grocer") }?.id ?: categories.firstOrNull()?.id ?: "cat_groceries")
        }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة منتج لقائمة المشتريات", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المنتج") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("الكمية (مثال: 2 كجم)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("السعر المتوقع (${CurrencyOption.fromCode(currencyCode).symbolAr})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShoppingFrequency.entries.forEach { f ->
                            FilterChip(selected = freq == f, onClick = { freq = f }, label = { Text(f.labelAr) })
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { c ->
                            FilterChip(selected = catId == c.id, onClick = { catId = c.id }, label = { Text(c.name) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveItem(null, name, freq, qty, price, catId)
                    showAddDialog = false
                }) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
        )
    }

    convertingItem?.let { item ->
        var actualPrice by remember(item) { mutableStateOf(MoneyUtils.piastersToInputString(item.expectedPricePiasters)) }
        AlertDialog(
            onDismissRequest = { convertingItem = null },
            title = { Text("تحويل إلى مصروف فعلي", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("سيتم تسجيل شراء \"${item.name}\" في سجل المصروفات وربطه بهذا البند لمنع تكرار الاحتساب.")
                    OutlinedTextField(
                        value = actualPrice,
                        onValueChange = { actualPrice = it },
                        label = { Text("السعر الفعلي المدفوع (${CurrencyOption.fromCode(currencyCode).symbolAr})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onConvertToExpense(item, actualPrice)
                    convertingItem = null
                }) { Text("تسجيل كمصروف") }
            },
            dismissButton = { TextButton(onClick = { convertingItem = null }) { Text("إلغاء") } }
        )
    }

    val filtered = remember(items, selectedFreq) {
        if (selectedFreq == null) items else items.filter { it.frequency == selectedFreq }
    }
    val expectedTotal = filtered.sumOf { it.expectedPricePiasters }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي القائمة المتوقع", style = MaterialTheme.typography.labelLarge)
                        Text(
                            MoneyUtils.formatCurrency(expectedTotal, currencyCode),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة منتج")
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedFreq == null, onClick = { selectedFreq = null }, label = { Text("الكل") })
                ShoppingFrequency.entries.forEach { f ->
                    FilterChip(selected = selectedFreq == f, onClick = { selectedFreq = f }, label = { Text(f.labelAr) })
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Rounded.ShoppingCart,
                    title = "قائمة المشتريات فارغة",
                    subtitle = "أضف مشتريات أول الشهر أو الأسبوعية لتقدير التكلفة وتحويلها لمصروفات بضغطة واحدة."
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = item.purchased,
                                onCheckedChange = { onTogglePurchased(item) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Text(
                                    text = "${item.quantityLabel} • ${item.frequency.labelAr} • متوقع: ${MoneyUtils.formatCurrency(item.expectedPricePiasters, currencyCode)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDelete(item.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (item.convertedExpenseId == null) {
                                OutlinedButton(
                                    onClick = { convertingItem = item },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("تحويل إلى مصروف")
                                }
                            } else {
                                Text(
                                    text = "✓ تم التسجيل كمصروف فعلي",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeInventorySection(
    items: List<InventoryItem>,
    onSaveItem: (String?, String, String, String, String, Long) -> Unit,
    onAdjust: (InventoryItem, Double) -> Unit,
    onAddToShopping: (InventoryItem) -> Unit,
    onDelete: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("2") }
        var min by remember { mutableStateOf("1") }
        var unit by remember { mutableStateOf("عبوة") }
        var expiryDays by remember { mutableIntStateOf(30) }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة صنف لمخزون المنزل", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الصنف (مثال: زيت طعام)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = { Text("الكمية الحالية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = min,
                            onValueChange = { min = it },
                            label = { Text("حد التنبيه") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("عبوة", "كجم", "لتر", "قطعة", "كرتونة").forEach { u ->
                            FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                        }
                    }
                    Text("صلاحية المنتج", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "بدون صلاحية", 7 to "7 أيام", 30 to "شهر", 180 to "6 شهور").forEach { (days, lbl) ->
                            FilterChip(selected = expiryDays == days, onClick = { expiryDays = days }, label = { Text(lbl) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val expiryMillis = if (expiryDays > 0) System.currentTimeMillis() + expiryDays * 24L * 3600L * 1000L else 0L
                    onSaveItem(null, name, qty, min, unit, expiryMillis)
                    showAddDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("جرد ومخزون المنزل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة صنف")
                }
            }
        }

        if (items.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Rounded.Inventory2,
                    title = "مخزون المنزل فارغ",
                    subtitle = "سجل المواد الغذائية والمنظفات المتوفرة لمتابعة النقص وتواريخ الصلاحية."
                )
            }
        } else {
            items(items, key = { it.id }) { item ->
                val isLow = item.quantity <= item.minThreshold
                val daysLeft = MoneyUtils.daysUntil(item.expiryEpochMillis)
                val isExpiring = item.expiryEpochMillis > 0L && daysLeft <= 7L

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "المتاح: ${String.format(Locale.US, "%.1f", item.quantity)} ${item.unit} (حد التنبيه: ${item.minThreshold})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.expiryEpochMillis > 0L) {
                                    Text(
                                        text = "تنتهي الصلاحية: ${MoneyUtils.formatDateAr(item.expiryEpochMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isExpiring) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                item.lastActionNote?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onAdjust(item, -1.0) }) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "استهلاك")
                                }
                                IconButton(onClick = { onAdjust(item, 1.0) }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "إعادة شراء")
                                }
                                IconButton(onClick = { onDelete(item.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        if (isLow || isExpiring) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isExpiring) DangerRed.copy(alpha = 0.14f) else WarningAmber.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = if (isExpiring) "قريب انتهاء الصلاحية" else "مخزون منخفض",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpiring) DangerRed else WarningAmber,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                TextButton(onClick = { onAddToShopping(item) }) {
                                    Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("أضف للمشتريات")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyReviewSection(
    uiState: BudgetUiState,
    onCompleteWeeklyReview: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(15 * 60) }
    var running by remember { mutableStateOf(false) }
    val checklist = remember {
        mutableStateListOf(false, false, false, false, false)
    }

    LaunchedEffect(running) {
        while (running && secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
        if (secondsLeft == 0) running = false
    }

    val currency = uiState.preferences.currencyCode
    val weekStart = MoneyUtils.startOfWeekMillis()
    val weekExpenses = uiState.transactions.filter {
        it.type == TransactionType.EXPENSE && it.dateEpochMillis >= weekStart
    }.sumOf { it.amountPiasters }
    val remaining = uiState.snapshot?.remainingBudgetPiasters ?: 0L
    val lowInv = uiState.inventoryItems.count { it.quantity <= it.minThreshold }
    val pendingShopping = uiState.shoppingItems.count { !it.purchased }
    val overBudgetCats = uiState.snapshot?.categorySummaries?.count { it.budgetPiasters > 0L && it.spentPiasters > it.budgetPiasters } ?: 0

    val steps = listOf(
        "مراجعة المصروفات الفعلية للأسبوع" to "أنفقت الأسرة خلال آخر 7 أيام: ${MoneyUtils.formatCurrency(weekExpenses, currency)}",
        "التحقق من المتبقي من الميزانية" to "المتبقي حالياً: ${MoneyUtils.formatCurrency(remaining, currency)}",
        "مراجعة مخزون المنزل" to "أصناف وصلت لحد النقص: $lowInv صنف",
        "اعتماد مشتريات الأسبوع القادم" to "بنود قائمة المشتريات المفتوحة: $pendingShopping بند",
        "اكتشاف التجاوزات والالتزامات" to "تصنيفات تجاوزت ميزانيتها: $overBudgetCats تصنيف"
    )

    val mins = secondsLeft / 60
    val secs = secondsLeft % 60

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("المراجعة المالية الأسبوعية (15 دقيقة)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", mins, secs),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(onClick = { running = !running }, shape = RoundedCornerShape(14.dp)) {
                        Icon(if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (running) "إيقاف" else "بدء الجلسة")
                    }
                }
            }
        }

        items(steps.indices.toList()) { idx ->
            val (title, subtitle) = steps[idx]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checklist[idx],
                        onCheckedChange = { checklist[idx] = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${idx + 1}. $title", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            val allChecked = checklist.all { it }
            Button(
                onClick = onCompleteWeeklyReview,
                enabled = allChecked,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (allChecked) "إتمام وحفظ المراجعة الأسبوعية" else "أكمل الخطوات الخمس أعلاه للحفظ")
            }
        }
    }
}
