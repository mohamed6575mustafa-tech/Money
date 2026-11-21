package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ContrastUtils
import com.example.data.local.UserPreferences
import com.example.domain.ThemeModePreference
import com.example.ui.components.InteractiveColorWheel

@Composable
fun ThemeCustomizerScreen(
    preferences: UserPreferences,
    onSave: (primaryArgb: Long, secondaryArgb: Long, accentArgb: Long, mode: ThemeModePreference) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var primaryColor by remember(preferences.primaryColorArgb) {
        mutableStateOf(Color(preferences.primaryColorArgb.toInt()))
    }
    var secondaryColor by remember(preferences.secondaryColorArgb) {
        mutableStateOf(Color(preferences.secondaryColorArgb.toInt()))
    }
    var accentColor by remember(preferences.accentColorArgb) {
        mutableStateOf(Color(preferences.accentColorArgb.toInt()))
    }
    var selectedMode by remember(preferences.themeMode) { mutableStateOf(preferences.themeMode) }
    var activeSlot by remember { mutableIntStateOf(0) } // 0=Primary, 1=Buttons(Secondary), 2=Accent

    val activeWheelColor = when (activeSlot) {
        0 -> primaryColor
        1 -> secondaryColor
        else -> accentColor
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                }
                Text(
                    text = "تخصيص المظهر",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live Preview Card BEFORE saving
        item {
            val onPrimary = ContrastUtils.onColorFor(primaryColor)
            val onSecondary = ContrastUtils.onColorFor(secondaryColor)
            val onAccent = ContrastUtils.onColorFor(accentColor)

            Card(
                modifier = Modifier.fillMaxWidth().testTag("theme_live_preview"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(primaryColor, secondaryColor)))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "معاينة مباشرة للمظهر",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = onPrimary
                            )
                            Surface(shape = RoundedCornerShape(50), color = accentColor) {
                                Text(
                                    text = "عنصر مميز",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = onAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "مصروف البيت • خطط .. سجل .. وفر",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onPrimary.copy(alpha = 0.88f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = secondaryColor
                            ) {
                                Text(
                                    text = "زر تفاعلي",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = onSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = onPrimary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "تباين تلقائي للنص",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = onPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Target Color Slot Selector + Interactive HSV Color Wheel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "عجلة الألوان التفاعلية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("الأساسي", "الأزرار", "المميز").forEachIndexed { idx, label ->
                            SegmentedButton(
                                selected = activeSlot == idx,
                                onClick = { activeSlot = idx },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3)
                            ) { Text(label) }
                        }
                    }

                    InteractiveColorWheel(
                        selectedColor = activeWheelColor,
                        onColorChanged = { newColor ->
                            when (activeSlot) {
                                0 -> primaryColor = newColor
                                1 -> secondaryColor = newColor
                                else -> accentColor = newColor
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ColorChipIndicator("الأساسي", primaryColor, activeSlot == 0) { activeSlot = 0 }
                        ColorChipIndicator("الأزرار", secondaryColor, activeSlot == 1) { activeSlot = 1 }
                        ColorChipIndicator("المميز", accentColor, activeSlot == 2) { activeSlot = 2 }
                    }
                }
            }
        }

        // Curated Presets
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ثيمات جاهزة مقترحة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ContrastUtils.Presets.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    primaryColor = Color(preset.primaryArgb.toInt())
                                    secondaryColor = Color(preset.secondaryArgb.toInt())
                                    accentColor = Color(preset.accentArgb.toInt())
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(16.dp).clip(CircleShape).background(Color(preset.primaryArgb.toInt())))
                                    Spacer(Modifier.width(4.dp))
                                    Box(Modifier.size(16.dp).clip(CircleShape).background(Color(preset.secondaryArgb.toInt())))
                                    Spacer(Modifier.width(4.dp))
                                    Box(Modifier.size(16.dp).clip(CircleShape).background(Color(preset.accentArgb.toInt())))
                                    Spacer(Modifier.width(8.dp))
                                    Text(preset.nameAr, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Light / Dark Mode
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("وضع الإضاءة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        ThemeModePreference.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.labelAr) }
                            )
                        }
                    }
                }
            }
        }

        // Save & Reset Actions
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        primaryColor = Color(ContrastUtils.DEFAULT_PRIMARY_ARGB.toInt())
                        secondaryColor = Color(ContrastUtils.DEFAULT_SECONDARY_ARGB.toInt())
                        accentColor = Color(ContrastUtils.DEFAULT_ACCENT_ARGB.toInt())
                        onReset()
                    },
                    modifier = Modifier.weight(1f).height(52.dp).testTag("reset_theme_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("الافتراضي")
                }
                Button(
                    onClick = {
                        onSave(
                            primaryColor.toArgb().toLong() and 0xFFFFFFFFL,
                            secondaryColor.toArgb().toLong() and 0xFFFFFFFFL,
                            accentColor.toArgb().toLong() and 0xFFFFFFFFL,
                            selectedMode
                        )
                    },
                    modifier = Modifier.weight(1.4f).height(52.dp).testTag("save_theme_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = secondaryColor, contentColor = ContrastUtils.onColorFor(secondaryColor))
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("حفظ وتطبيق المظهر")
                }
            }
        }
    }
}

@Composable
private fun ColorChipIndicator(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
