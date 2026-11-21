package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.core.CurrencyOption
import com.example.core.onGoogleSignInClicked
import com.example.domain.FamilyRole
import com.example.domain.ThemeModePreference
import com.example.ui.state.BudgetUiState
import kotlinx.coroutines.launch

@Composable
fun FamilyManagementScreen(
    uiState: BudgetUiState,
    isCloudMode: Boolean,
    onUpdateHouseholdName: (String) -> Unit,
    onSaveMember: (existingId: String?, name: String, email: String?, role: FamilyRole) -> Unit,
    onDeleteMember: (String) -> Unit,
    onSetActiveMember: (String) -> Unit,
    onJoinByCode: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val hh = uiState.snapshot?.household
    var hhName by remember(hh?.name) { mutableStateOf(hh?.name ?: "أسرتي") }
    var inviteInput by remember { mutableStateOf("") }
    var showAddMember by remember { mutableStateOf(false) }

    if (showAddMember) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(FamilyRole.MEMBER) }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("إضافة فرد من الأسرة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("الصلاحية", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FamilyRole.entries.forEach { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.labelAr) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveMember(null, name, email, role)
                    showAddMember = false
                }) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { showAddMember = false }) { Text("إلغاء") } }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                }
                Text("إدارة الأسرة والصلاحيات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ملف الأسرة المشترك", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = hhName,
                        onValueChange = { hhName = it },
                        label = { Text("اسم الأسرة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onUpdateHouseholdName(hhName) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("حفظ اسم الأسرة") }

                    hh?.let { household ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("كود دعوة الأسرة", style = MaterialTheme.typography.labelSmall)
                                    Text(household.inviteCode, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "انضم إلى ميزانية ${household.name} في تطبيق مصروف البيت عبر كود الدعوة: ${household.inviteCode}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "دعوة فرد للأسرة"))
                                }) {
                                    Icon(Icons.Rounded.Share, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("مشاركة الدعوة")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isCloudMode) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("الانضمام لأسرة قائمة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = inviteInput,
                            onValueChange = { inviteInput = it },
                            label = { Text("أدخل كود الدعوة (مثال: FAM1234)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { onJoinByCode(inviteInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("انضمام ومزامنة")
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
                Text("أفراد الأسرة (${uiState.members.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { showAddMember = true }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("إضافة فرد")
                }
            }
        }

        items(uiState.members, key = { it.id }) { member ->
            val isActive = member.name == uiState.preferences.activeMemberName
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSetActiveMember(member.name) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name + if (isActive) " (المسجل النشط)" else "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = member.role.labelAr + (member.email?.let { " • $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.members.size > 1) {
                        IconButton(onClick = { onDeleteMember(member.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: BudgetUiState,
    isCloudMode: Boolean,
    userEmail: String?,
    onOpenThemeWheel: () -> Unit,
    onSetThemeMode: (ThemeModePreference) -> Unit,
    onSetCurrency: (String) -> Unit,
    onUpdateNotifications: (budget: Boolean, inventory: Boolean, weekly: Boolean) -> Unit,
    onGenerateBackupJson: suspend () -> Result<String>,
    onRestoreBackupJson: (String) -> Unit,
    onSignOut: () -> Unit,
    onSignedInFromLocal: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var budgetAlerts by remember(uiState.preferences.budgetAlertsEnabled) { mutableStateOf(uiState.preferences.budgetAlertsEnabled) }
    var invAlerts by remember(uiState.preferences.inventoryAlertsEnabled) { mutableStateOf(uiState.preferences.inventoryAlertsEnabled) }
    var weeklyAlerts by remember(uiState.preferences.weeklyReviewReminderEnabled) { mutableStateOf(uiState.preferences.weeklyReviewReminderEnabled) }
    var authStatusMsg by remember { mutableStateOf<String?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                onGenerateBackupJson().onSuccess { json ->
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    authStatusMsg = "تم حفظ النسخة الاحتياطية بنجاح"
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!content.isNullOrBlank()) {
                onRestoreBackupJson(content)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                }
                Text("الإعدادات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }

        // Theme & Appearance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("المظهر والألوان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onOpenThemeWheel,
                        modifier = Modifier.fillMaxWidth().testTag("settings_theme_wheel_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("فتح عجلة الألوان وتخصيص المظهر")
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeModePreference.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.preferences.themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(mode.labelAr) }
                            )
                        }
                    }
                }
            }
        }

        // Currency
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("العملة الافتراضية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CurrencyOption.entries.forEach { cur ->
                            FilterChip(
                                selected = uiState.preferences.currencyCode == cur.code,
                                onClick = { onSetCurrency(cur.code) },
                                label = { Text(cur.labelAr) }
                            )
                        }
                    }
                }
            }
        }

        // Notifications
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("إعدادات التنبيهات الذكية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SettingSwitchRow("تنبيهات تجاوز الميزانية (70% / 90%)", budgetAlerts) {
                        budgetAlerts = it
                        onUpdateNotifications(budgetAlerts, invAlerts, weeklyAlerts)
                    }
                    SettingSwitchRow("تنبيهات نقص المخزون وانتهاء الصلاحية", invAlerts) {
                        invAlerts = it
                        onUpdateNotifications(budgetAlerts, invAlerts, weeklyAlerts)
                    }
                    SettingSwitchRow("تذكير المراجعة المالية الأسبوعية", weeklyAlerts) {
                        weeklyAlerts = it
                        onUpdateNotifications(budgetAlerts, invAlerts, weeklyAlerts)
                    }
                }
            }
        }

        // Backup & Restore
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("النسخ الاحتياطي والاستعادة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { backupLauncher.launch("masrouf_albait_backup.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("تصدير نسخة")
                        }
                        OutlinedButton(
                            onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("استعادة")
                        }
                    }
                }
            }
        }

        // Account & Privacy
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الحساب والمزامنة السحابية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (isCloudMode) {
                        Text("مسجل الدخول: ${userEmail ?: "حساب Google"}", style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("تسجيل الخروج")
                        }
                    } else {
                        Text(
                            "أنت تستخدم الوضع المحلي حالياً. اربط حساب Google لتفعيل المزامنة السحابية بين أفراد الأسرة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = onSignedInFromLocal,
                                    onAuthError = { authStatusMsg = it },
                                    scope = scope
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("تسجيل الدخول بحساب Google")
                        }
                    }
                    authStatusMsg?.let { msg ->
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // App Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("مصروف البيت — الإصدار 1.0", fontWeight = FontWeight.Bold)
                        Text("خطط .. سجل .. وفر • بياناتك محفوظة بأمان وعملاتك بدقة القرش.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
