package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.core.MoneyUtils
import com.example.domain.BudgetTransaction
import com.example.domain.Category
import com.example.domain.DashboardSnapshot
import com.example.domain.Household
import com.example.domain.TransactionType
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.LuxuryDashboardHeroCard
import com.example.ui.components.SmartAlertCard
import com.example.ui.components.SpendingDonutChart
import com.example.ui.state.BudgetUiState
import com.example.ui.theme.DangerRed
import com.example.ui.theme.MasroufAlBaitTheme
import com.example.ui.theme.SuccessGreen

@Composable
fun DashboardScreen(
    uiState: BudgetUiState,
    isOnline: Boolean,
    isCloudMode: Boolean,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onEditTransaction: (BudgetTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onNavigateToFunds: () -> Unit,
    onNavigateToWeeklyReview: () -> Unit,
    onNavigateToAiAdvisor: () -> Unit,
    onNavigateToThemeWheel: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = uiState.snapshot
    val currency = uiState.preferences.currencyCode

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مصروف البيت",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "خطط .. سجل .. وفر  •  ${uiState.preferences.activeMemberName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isCloudMode && isOnline) {
                        SuccessGreen.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCloudMode && isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = if (isCloudMode && isOnline) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when {
                                !isCloudMode -> "محلي"
                                isOnline -> "متزامن"
                                else -> "بدون إنترنت"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (snapshot != null) {
            item {
                LuxuryDashboardHeroCard(snapshot = snapshot, currencyCode = currency)
            }

            item {
                val monthTx = uiState.transactions.filter { it.yearMonth == snapshot.activeYearMonth }
                val incomeCount = monthTx.count { it.type == TransactionType.INCOME }
                val expenseCount = monthTx.count { it.type == TransactionType.EXPENSE }
                IncomeAndExpenseSummaryCards(
                    totalIncomeText = MoneyUtils.formatCurrency(snapshot.totalIncomePiasters, currency),
                    totalExpenseText = MoneyUtils.formatCurrency(snapshot.totalExpensePiasters, currency),
                    incomeCount = incomeCount,
                    expenseCount = expenseCount,
                    onIncomeClick = onAddIncomeClick,
                    onExpenseClick = onAddExpenseClick
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickShortcutTile(
                    title = "إضافة مصروف",
                    icon = Icons.Rounded.Add,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    testTag = "quick_add_expense",
                    onClick = onAddExpenseClick
                )
                QuickShortcutTile(
                    title = "تسجيل دخل",
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    testTag = "quick_add_income",
                    onClick = onAddIncomeClick
                )
                QuickShortcutTile(
                    title = "المساعد الذكي",
                    icon = Icons.Rounded.AutoAwesome,
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    testTag = "quick_ai_advisor",
                    onClick = onNavigateToAiAdvisor
                )
                QuickShortcutTile(
                    title = "المخصصات",
                    icon = Icons.Rounded.Savings,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    testTag = "quick_funds",
                    onClick = onNavigateToFunds
                )
                QuickShortcutTile(
                    title = "مراجعة أسبوعية",
                    icon = Icons.AutoMirrored.Rounded.EventNote,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    testTag = "quick_weekly_review",
                    onClick = onNavigateToWeeklyReview
                )
                QuickShortcutTile(
                    title = "تخصيص المظهر",
                    icon = Icons.Rounded.Palette,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    testTag = "quick_theme_wheel",
                    onClick = onNavigateToThemeWheel
                )
                QuickShortcutTile(
                    title = "التقارير",
                    icon = Icons.Rounded.Analytics,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    testTag = "quick_reports",
                    onClick = onNavigateToReports
                )
            }
        }

        if (snapshot != null && snapshot.alerts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "تنبيهات ذكية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    snapshot.alerts.take(3).forEach { alert ->
                        SmartAlertCard(alert = alert)
                    }
                }
            }
        }

        if (snapshot != null) {
            item {
                SpendingDonutChart(
                    summaries = snapshot.categorySummaries,
                    currencyCode = currency
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر العمليات المالية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.transactions.isNotEmpty()) {
                    Text(
                        text = "${uiState.transactions.size} عملية",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        val recent = snapshot?.recentTransactions ?: emptyList()
        if (recent.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "ابدأ بتسجيل أول مصروف أو دخل",
                    subtitle = "اضغط على زر + بالأسفل لتسجيل مصروفات المنزل ومتابعة الميزانية لحظياً."
                )
            }
        } else {
            items(recent, key = { it.id }) { tx ->
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
private fun QuickShortcutTile(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: BudgetTransaction,
    categories: List<Category>,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val category = categories.firstOrNull { it.id == tx.categoryId }
    val isExpense = tx.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) MaterialTheme.colorScheme.onSurface else SuccessGreen
    val prefix = if (isExpense) "-" else "+"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryAvatar(
                iconKey = if (isExpense) (category?.iconKey ?: "category") else "wallet",
                colorHex = if (isExpense) (category?.colorHex ?: 0xFF10B981) else 0xFF16A34A
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isExpense) tx.categoryName else (tx.incomeSource?.labelAr ?: "دخل"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitleParts = listOfNotNull(
                    tx.merchant?.takeIf { it.isNotBlank() },
                    MoneyUtils.formatShortDateAr(tx.dateEpochMillis),
                    "بواسطة: ${tx.addedByName}"
                )
                Text(
                    text = subtitleParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                tx.notes?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${MoneyUtils.formatCurrency(tx.amountPiasters, currencyCode)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "تعديل",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeAndExpenseSummaryCards(
    totalIncomeText: String,
    totalExpenseText: String,
    incomeCount: Int,
    expenseCount: Int,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val stacked = maxWidth < 310.dp
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricSummaryCard(
                    title = "إجمالي الدخل",
                    amountText = totalIncomeText,
                    subtitle = "$incomeCount عمليات مسجلة • اضغط للإضافة",
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    accentColor = SuccessGreen,
                    testTag = "dashboard_total_income_card",
                    onClick = onIncomeClick,
                    modifier = Modifier.fillMaxWidth()
                )
                MetricSummaryCard(
                    title = "إجمالي المصروفات",
                    amountText = totalExpenseText,
                    subtitle = "$expenseCount عمليات مسجلة • اضغط للإضافة",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    accentColor = DangerRed,
                    testTag = "dashboard_total_expense_card",
                    onClick = onExpenseClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    title = "إجمالي الدخل",
                    amountText = totalIncomeText,
                    subtitle = "$incomeCount عمليات دخل",
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    accentColor = SuccessGreen,
                    testTag = "dashboard_total_income_card",
                    onClick = onIncomeClick,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "إجمالي المصروفات",
                    amountText = totalExpenseText,
                    subtitle = "$expenseCount عمليات صرف",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    accentColor = DangerRed,
                    testTag = "dashboard_total_expense_card",
                    onClick = onExpenseClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    amountText: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun DashboardScreenRtlPreview() {
    val sampleHousehold = Household(
        id = "hh_preview",
        ownerId = "u1",
        name = "أسرتي",
        currencyCode = "EGP",
        inviteCode = "FAM1024",
        memberUids = listOf("u1"),
        monthlyBudgetPiasters = 1_500_000L,
        preventBudgetOverflow = false,
        rolloverEnabled = true,
        carriedOverPiasters = 0L,
        alertThreshold1 = 70,
        alertThreshold2 = 90,
        activeYearMonth = "2026-11"
    )
    val sampleSnapshot = DashboardSnapshot(
        household = sampleHousehold,
        activeYearMonth = "2026-11",
        totalIncomePiasters = 2_000_000L,
        effectiveBudgetPiasters = 1_500_000L,
        totalExpensePiasters = 650_000L,
        remainingBudgetPiasters = 850_000L,
        budgetUsagePercent = 43.3f,
        previousMonthExpensePiasters = 720_000L,
        monthDeltaPercent = -9.7f,
        categorySummaries = emptyList(),
        recentTransactions = emptyList(),
        alerts = emptyList()
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MasroufAlBaitTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                DashboardScreen(
                    uiState = BudgetUiState(isLoading = false, snapshot = sampleSnapshot),
                    isOnline = true,
                    isCloudMode = false,
                    onAddExpenseClick = {},
                    onAddIncomeClick = {},
                    onEditTransaction = {},
                    onDeleteTransaction = {},
                    onNavigateToFunds = {},
                    onNavigateToWeeklyReview = {},
                    onNavigateToAiAdvisor = {},
                    onNavigateToThemeWheel = {},
                    onNavigateToReports = {}
                )
            }
        }
    }
}
