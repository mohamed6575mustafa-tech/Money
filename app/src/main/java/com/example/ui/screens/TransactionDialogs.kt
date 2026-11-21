package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.CurrencyOption
import com.example.core.MoneyUtils
import com.example.domain.BudgetTransaction
import com.example.domain.Category
import com.example.domain.IncomeSource
import com.example.domain.TransactionType
import com.example.ui.components.categoryIconFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditorDialog(
    initial: BudgetTransaction?,
    categories: List<Category>,
    currencyCode: String,
    activeMemberName: String,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        amountText: String,
        category: Category,
        merchant: String?,
        notes: String?,
        dateEpochMillis: Long,
        receiptUri: String?
    ) -> Unit
) {
    val currency = CurrencyOption.fromCode(currencyCode)
    var amountText by remember {
        mutableStateOf(initial?.let { MoneyUtils.piastersToInputString(it.amountPiasters) } ?: "")
    }
    var selectedCategory by remember {
        mutableStateOf(
            categories.firstOrNull { it.id == initial?.categoryId }
                ?: categories.firstOrNull()
        )
    }
    var merchant by remember { mutableStateOf(initial?.merchant ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var dateMillis by remember { mutableLongStateOf(initial?.dateEpochMillis ?: System.currentTimeMillis()) }
    var receiptUri by remember { mutableStateOf(initial?.receiptUri) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "إضافة مصروف جديد" else "تعديل المصروف",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationError = null
                    },
                    label = { Text("المبلغ (${currency.symbolAr})") },
                    placeholder = { Text("مثال: 250.50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                Text(
                    text = "التصنيف",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory?.id == cat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIconFor(cat.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("التاريخ: ${MoneyUtils.formatDateAr(dateMillis)}")
                }

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("اسم المتجر أو جهة الشراء") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_merchant_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (receiptUri == null) "إرفاق صورة الفاتورة" else "تغيير الفاتورة")
                    }
                    if (receiptUri != null) {
                        Box {
                            AsyncImage(
                                model = receiptUri,
                                contentDescription = "صورة الفاتورة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            IconButton(
                                onClick = { receiptUri = null },
                                modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "حذف الفاتورة", tint = Color.Red)
                            }
                        }
                    }
                }

                Text(
                    text = "المسجل: $activeMemberName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                validationError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cat = selectedCategory
                    val parsed = MoneyUtils.parseAmountToPiasters(amountText)
                    if (parsed == null || parsed <= 0L) {
                        validationError = "يرجى إدخال مبلغ صحيح أكبر من صفر"
                    } else if (cat == null) {
                        validationError = "يرجى اختيار التصنيف"
                    } else {
                        onSave(initial?.id, amountText, cat, merchant, notes, dateMillis, receiptUri)
                    }
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("حفظ المصروف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun IncomeEditorDialog(
    initial: BudgetTransaction?,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        amountText: String,
        source: IncomeSource,
        notes: String?,
        dateEpochMillis: Long
    ) -> Unit
) {
    val currency = CurrencyOption.fromCode(currencyCode)
    var amountText by remember {
        mutableStateOf(initial?.let { MoneyUtils.piastersToInputString(it.amountPiasters) } ?: "")
    }
    var selectedSource by remember { mutableStateOf(initial?.incomeSource ?: IncomeSource.SALARY) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var dateMillis by remember { mutableLongStateOf(initial?.dateEpochMillis ?: System.currentTimeMillis()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "تسجيل دخل جديد" else "تعديل الدخل",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorText = null
                    },
                    label = { Text("المبلغ (${currency.symbolAr})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("income_amount_input")
                )
                Text("مصدر الدخل", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IncomeSource.entries.forEach { src ->
                        FilterChip(
                            selected = selectedSource == src,
                            onClick = { selectedSource = src },
                            label = { Text(src.labelAr) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("وصف أو ملاحظات (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = MoneyUtils.parseAmountToPiasters(amountText)
                    if (parsed == null || parsed <= 0L) {
                        errorText = "يرجى إدخال مبلغ دخل صحيح"
                    } else {
                        onSave(initial?.id, amountText, selectedSource, notes, dateMillis)
                    }
                },
                modifier = Modifier.testTag("save_income_button")
            ) { Text("حفظ الدخل") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorDialog(
    initial: Category?,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (existingId: String?, name: String, colorHex: Long, budgetText: String) -> Unit
) {
    val currency = CurrencyOption.fromCode(currencyCode)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var budgetText by remember {
        mutableStateOf(initial?.let { MoneyUtils.piastersToInputString(it.budgetPiasters) } ?: "")
    }
    var selectedColor by remember { mutableLongStateOf(initial?.colorHex ?: 0xFF10B981) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val colorChoices = listOf(
        0xFF064E3B, 0xFF10B981, 0xFFD4AF37, 0xFF3B82F6,
        0xFFEF4444, 0xFF8B5CF6, 0xFFEC4899, 0xFFF59E0B, 0xFF06B6D4
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "إضافة تصنيف جديد" else "تعديل التصنيف",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("اسم التصنيف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("ميزانية التصنيف الشهرية (${currency.symbolAr})") },
                    placeholder = { Text("0 = غير محدد") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("لون التصنيف", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorChoices.forEach { argb ->
                        val isSel = selectedColor == argb
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .border(
                                    width = if (isSel) 3.dp else 1.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = argb }
                        )
                    }
                }
                errorText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.trim().isEmpty()) {
                    errorText = "يرجى إدخال اسم التصنيف"
                } else {
                    onSave(initial?.id, name, selectedColor, budgetText)
                }
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
