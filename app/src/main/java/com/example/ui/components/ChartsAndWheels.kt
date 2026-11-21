package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.ContrastUtils
import com.example.core.MoneyUtils
import com.example.domain.CategorySpendingSummary
import com.example.domain.DashboardSnapshot
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LocalMasroufExtraColors
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun LuxuryDashboardHeroCard(
    snapshot: DashboardSnapshot,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val extra = LocalMasroufExtraColors.current
    val usageFraction = (snapshot.budgetUsagePercent / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = usageFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "budgetProgress"
    )
    val barColor = when {
        snapshot.budgetUsagePercent >= 90f -> DangerRed
        snapshot.budgetUsagePercent >= 70f -> WarningAmber
        else -> extra.accentGold
    }
    val onHero = ContrastUtils.onColorFor(extra.heroStart)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(extra.heroStart, extra.heroEnd)
                    )
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_family_budget_1795291711481),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.24f)
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = snapshot.household.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = onHero.copy(alpha = 0.88f)
                        )
                        Text(
                            text = MoneyUtils.yearMonthLabelAr(snapshot.activeYearMonth),
                            style = MaterialTheme.typography.bodySmall,
                            color = extra.accentGold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = extra.accentGold
                    ) {
                        Text(
                            text = "${snapshot.budgetUsagePercent.toInt()}% مستهلك",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = extra.onAccentGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "المتبقي من الميزانية",
                    style = MaterialTheme.typography.bodySmall,
                    color = onHero.copy(alpha = 0.82f)
                )
                Text(
                    text = MoneyUtils.formatCurrency(snapshot.remainingBudgetPiasters, currencyCode),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = onHero
                )

                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = barColor,
                    trackColor = onHero.copy(alpha = 0.22f)
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroMetricPill(
                        label = "الدخل الشهري",
                        value = MoneyUtils.formatCurrency(snapshot.totalIncomePiasters, currencyCode),
                        onHero = onHero,
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricPill(
                        label = "الميزانية",
                        value = MoneyUtils.formatCurrency(snapshot.effectiveBudgetPiasters, currencyCode),
                        onHero = onHero,
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricPill(
                        label = "المصروفات",
                        value = MoneyUtils.formatCurrency(snapshot.totalExpensePiasters, currencyCode),
                        onHero = onHero,
                        modifier = Modifier.weight(1f)
                    )
                }

                snapshot.monthDeltaPercent?.let { delta ->
                    Spacer(Modifier.height(12.dp))
                    val deltaText = if (delta >= 0f) {
                        "زيادة بنسبة ${delta.toInt()}% مقارنة بالشهر السابق"
                    } else {
                        "وفر بنسبة ${kotlin.math.abs(delta).toInt()}% مقارنة بالشهر السابق"
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = onHero.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = deltaText,
                            style = MaterialTheme.typography.labelMedium,
                            color = onHero,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    value: String,
    onHero: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = onHero.copy(alpha = 0.13f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = onHero.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = onHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SpendingDonutChart(
    summaries: List<CategorySpendingSummary>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val active = summaries.filter { it.spentPiasters > 0L }.sortedByDescending { it.spentPiasters }
    var selectedId by remember(active) { mutableStateOf(active.firstOrNull()?.category?.id) }
    val total = active.sumOf { it.spentPiasters }
    val selected = active.firstOrNull { it.category.id == selectedId } ?: active.firstOrNull()

    if (active.isEmpty()) {
        EmptyStatePanel(
            icon = categoryIconFor("category"),
            title = "لا توجد مصروفات مسجلة هذا الشهر بعد",
            subtitle = "سجل أول مصروف ليظهر توزيع الإنفاق التفاعلي حسب التصنيف.",
            modifier = modifier
        )
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "توزيع المصروفات حسب التصنيف",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(164.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 26.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        var startAngle = -90f
                        active.forEach { item ->
                            val sweep = if (total > 0L) (item.spentPiasters.toFloat() / total.toFloat()) * 360f else 0f
                            val isSelected = item.category.id == selected?.category?.id
                            drawArc(
                                color = Color(item.category.colorHex.toInt()),
                                startAngle = startAngle,
                                sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(
                                    width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                                    cap = StrokeCap.Butt
                                )
                            )
                            startAngle += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = selected?.category?.name ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                        Text(
                            text = "${selected?.sharePercent?.toInt() ?: 0}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = MoneyUtils.formatCompactCurrency(selected?.spentPiasters ?: 0L, currencyCode),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    active.take(5).forEach { item ->
                        val isSel = item.category.id == selected?.category?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    else Color.Transparent
                                )
                                .clickable { selectedId = item.category.id }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(item.category.colorHex.toInt()))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = item.category.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.sharePercent.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ComparisonBarPoint(
    val label: String,
    val incomePiasters: Long,
    val expensePiasters: Long
)

@Composable
fun IncomeExpenseBarChart(
    points: List<ComparisonBarPoint>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val maxVal = points.maxOfOrNull { maxOf(it.incomePiasters, it.expensePiasters) }?.coerceAtLeast(1L) ?: 1L
    val incomeColor = SuccessGreen
    val expenseColor = MaterialTheme.colorScheme.primary
    var selectedIdx by remember(points) { mutableStateOf(points.lastIndex.coerceAtLeast(0)) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مقارنة الدخل بالمصروفات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot(color = incomeColor, label = "الدخل")
                    LegendDot(color = expenseColor, label = "المصروف")
                }
            }
            if (points.isNotEmpty()) {
                val activePoint = points.getOrNull(selectedIdx) ?: points.last()
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${activePoint.label}: دخل ${MoneyUtils.formatCurrency(activePoint.incomePiasters, currencyCode)} | مصروف ${MoneyUtils.formatCurrency(activePoint.expensePiasters, currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty()) {
                                val slot = size.width / points.size.toFloat()
                                selectedIdx = (offset.x / slot).toInt().coerceIn(0, points.lastIndex)
                            }
                        }
                    }
            ) {
                if (points.isEmpty()) return@Canvas
                val slotWidth = size.width / points.size.toFloat()
                val barWidth = (slotWidth * 0.26f).coerceAtMost(28.dp.toPx())
                val chartHeight = size.height - 16.dp.toPx()

                points.forEachIndexed { idx, pt ->
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val incH = (pt.incomePiasters.toFloat() / maxVal.toFloat()) * chartHeight
                    val expH = (pt.expensePiasters.toFloat() / maxVal.toFloat()) * chartHeight

                    drawRoundRect(
                        color = incomeColor.copy(alpha = if (idx == selectedIdx) 1f else 0.72f),
                        topLeft = Offset(centerX - barWidth - 3.dp.toPx(), chartHeight - incH),
                        size = Size(barWidth, incH.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawRoundRect(
                        color = expenseColor.copy(alpha = if (idx == selectedIdx) 1f else 0.72f),
                        topLeft = Offset(centerX + 3.dp.toPx(), chartHeight - expH),
                        size = Size(barWidth, expH.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                points.forEachIndexed { idx, pt ->
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal,
                        color = if (idx == selectedIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { selectedIdx = idx }
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun InteractiveColorWheel(
    selectedColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialHsv = remember(selectedColor) { ContrastUtils.colorToHsv(selectedColor) }
    var hue by remember(selectedColor) { mutableStateOf(initialHsv[0]) }
    var sat by remember(selectedColor) { mutableStateOf(initialHsv[1]) }
    var value by remember(selectedColor) { mutableStateOf(initialHsv[2].coerceAtLeast(0.15f)) }

    val hueSpectrum = remember {
        listOf(
            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
        )
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(230.dp)
                .aspectRatio(1f)
                .testTag("interactive_color_wheel")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(value) {
                        fun updateFromOffset(offset: Offset) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = size.width.coerceAtMost(size.height) / 2f - 10.dp.toPx()
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = hypot(dx, dy).coerceAtMost(radius)
                            val angleDeg = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
                            hue = angleDeg
                            sat = (dist / radius).coerceIn(0f, 1f)
                            onColorChanged(ContrastUtils.hsvToColor(hue, sat, value))
                        }

                        detectTapGestures { offset -> updateFromOffset(offset) }
                    }
                    .pointerInput(value) {
                        fun updateFromOffset(offset: Offset) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = size.width.coerceAtMost(size.height) / 2f - 10.dp.toPx()
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = hypot(dx, dy).coerceAtMost(radius)
                            val angleDeg = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
                            hue = angleDeg
                            sat = (dist / radius).coerceIn(0f, 1f)
                            onColorChanged(ContrastUtils.hsvToColor(hue, sat, value))
                        }

                        detectDragGestures { change, _ ->
                            change.consume()
                            updateFromOffset(change.position)
                        }
                    }
            ) {
                val radius = size.minDimension / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(
                    brush = Brush.sweepGradient(hueSpectrum, center = center),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
                if (value < 1f) {
                    drawCircle(
                        color = Color.Black.copy(alpha = 1f - value),
                        radius = radius,
                        center = center
                    )
                }

                val angleRad = hue * PI.toFloat() / 180f
                val selectorOffset = Offset(
                    x = center.x + cos(angleRad) * sat * radius,
                    y = center.y + sin(angleRad) * sat * radius
                )
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = selectorOffset,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = ContrastUtils.hsvToColor(hue, sat, value),
                    radius = 9.dp.toPx(),
                    center = selectorOffset
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "درجة السطوع والعمق (${(value * 100).toInt()}%)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        val pureColor = ContrastUtils.hsvToColor(hue, sat, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(hue, sat) {
                        detectTapGestures { offset ->
                            value = (offset.x / size.width).coerceIn(0.15f, 1f)
                            onColorChanged(ContrastUtils.hsvToColor(hue, sat, value))
                        }
                    }
                    .pointerInput(hue, sat) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            value = (change.position.x / size.width).coerceIn(0.15f, 1f)
                            onColorChanged(ContrastUtils.hsvToColor(hue, sat, value))
                        }
                    }
            ) {
                drawRect(
                    brush = Brush.horizontalGradient(listOf(Color.Black, pureColor))
                )
                val thumbX = (value * size.width).coerceIn(10.dp.toPx(), size.width - 10.dp.toPx())
                drawCircle(
                    color = Color.White,
                    radius = 11.dp.toPx(),
                    center = Offset(thumbX, size.height / 2f),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}
