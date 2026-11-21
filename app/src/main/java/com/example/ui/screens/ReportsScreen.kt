package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.MoneyUtils
import com.example.domain.BudgetTransaction
import com.example.domain.CategorySpendingSummary
import com.example.domain.TransactionType
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.ComparisonBarPoint
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.components.SpendingDonutChart
import com.example.ui.state.BudgetUiState
import com.example.ui.theme.SuccessGreen

@Composable
fun ReportsScreen(
    uiState: BudgetUiState,
    onExportPdf: (title: String, periodLabel: String, transactions: List<BudgetTransaction>) -> Unit,
    onExportCsv: (title: String, transactions: List<BudgetTransaction>) -> Unit,
    modifier: Modifier = Modifier
) {
    var periodIdx by remember { mutableIntStateOf(2) }
    val periods = listOf("يومي", "أسبوعي", "شهري", "سنوي")
    val currency = uiState.preferences.currencyCode
    val activeYm = uiState.snapshot?.activeYearMonth ?: MoneyUtils.currentYearMonth()
    val activeYear = activeYm.take(4)

    val filteredTx = remember(uiState.transactions, periodIdx, activeYm) {
        val now = System.currentTimeMillis()
        when (periodIdx) {
            0 -> {
                val start = MoneyUtils.startOfTodayMillis(now)
                uiState.transactions.filter { it.dateEpochMillis >= start }
            }
            1 -> {
                val start = MoneyUtils.startOfWeekMillis(now)
                uiState.transactions.filter { it.dateEpochMillis >= start }
            }
            2 -> uiState.transactions.filter { it.yearMonth == activeYm }
            else -> uiState.transactions.filter { it.yearMonth.startsWith(activeYear) }
        }
    }

    val totalIncome = filteredTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPiasters }
    val totalExpense = filteredTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPiasters }
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0L) ((netSavings.toFloat() / totalIncome.toFloat()) * 100f) else 0f

    val periodSummaries = remember(filteredTx, uiState.categories, totalExpense) {
        val byCat = filteredTx.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amountPiasters } }
        uiState.categories.map { cat ->
            val spent = byCat[cat.id] ?: 0L
            val share = if (totalExpense > 0L) (spent.toFloat() / totalExpense.toFloat()) * 100f else 0f
            CategorySpendingSummary(cat, spent, cat.budgetPiasters, share)
        }
    }

    val barPoints = remember(uiState.transactions, activeYm) {
        val ym3 = activeYm
        val ym2 = MoneyUtils.previousYearMonth(ym3)
        val ym1 = MoneyUtils.previousYearMonth(ym2)
        listOf(ym1, ym2, ym3).map { ym ->
            val txs = uiState.transactions.filter { it.yearMonth == ym }
            ComparisonBarPoint(
                label = MoneyUtils.yearMonthLabelAr(ym),
                incomePiasters = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPiasters },
                expensePiasters = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPiasters }
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "مركز التقارير والتحليلات المالية",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                periods.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = periodIdx == idx,
                        onClick = { periodIdx = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = periods.size),
                        modifier = Modifier.testTag("report_period_$idx")
                    ) { Text(label) }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportKpiCard("الدخل", MoneyUtils.formatCurrency(totalIncome, currency), Modifier.weight(1f))
                ReportKpiCard("المصروفات", MoneyUtils.formatCurrency(totalExpense, currency), Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportKpiCard("صافي الادخار", MoneyUtils.formatCurrency(netSavings, currency), Modifier.weight(1f))
                ReportKpiCard("معدل الادخار", "${savingsRate.toInt()}%", Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onExportPdf("التقرير ${periods[periodIdx]}", MoneyUtils.yearMonthLabelAr(activeYm), filteredTx) },
                    modifier = Modifier.weight(1f).testTag("export_pdf_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تصدير PDF")
                }
                OutlinedButton(
                    onClick = { onExportCsv("التقرير ${periods[periodIdx]}", filteredTx) },
                    modifier = Modifier.weight(1f).testTag("export_csv_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.TableView, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تصدير Excel")
                }
            }
        }

        item {
            IncomeExpenseBarChart(points = barPoints, currencyCode = currency)
        }

        item {
            SpendingDonutChart(summaries = periodSummaries, currencyCode = currency)
        }

        val topCats = periodSummaries.filter { it.spentPiasters > 0L }.sortedByDescending { it.spentPiasters }
        if (topCats.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Rounded.Description,
                    title = "لا توجد عمليات في هذه الفترة",
                    subtitle = "اختر فترة زمنية أخرى أو سجل مصروفات جديدة لعرض التحليلات."
                )
            }
        } else {
            item {
                Text("أكثر التصنيفات استهلاكاً", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(topCats, key = { it.category.id }) { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryAvatar(iconKey = summary.category.iconKey, colorHex = summary.category.colorHex, sizeDp = 40)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(summary.category.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${summary.sharePercent.toInt()}% من إجمالي الإنفاق", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            MoneyUtils.formatCurrency(summary.spentPiasters, currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (uiState.archives.isNotEmpty()) {
            item {
                Text("أرشيف الشهور المغلقة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(uiState.archives, key = { it.id }) { arch ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(MoneyUtils.yearMonthLabelAr(arch.yearMonth), fontWeight = FontWeight.Bold)
                            Text("المصروفات: ${MoneyUtils.formatCurrency(arch.totalExpensePiasters, currency)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "المرحل: ${MoneyUtils.formatCurrency(arch.rolloverPiasters, currency)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportKpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
