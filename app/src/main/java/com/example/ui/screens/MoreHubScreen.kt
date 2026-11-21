package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ThemeModePreference
import com.example.ui.state.AiAdvisorUiState
import com.example.ui.state.AiChatMessage
import com.example.ui.state.BudgetUiState

enum class MoreSubScreen {
    MENU, AI_ADVISOR, THEME_WHEEL, FAMILY, SETTINGS
}

@Composable
fun MoreHubScreen(
    subScreen: MoreSubScreen,
    onSelectSubScreen: (MoreSubScreen) -> Unit,
    uiState: BudgetUiState,
    aiMessages: List<AiChatMessage>,
    aiState: AiAdvisorUiState,
    isOnline: Boolean,
    isCloudMode: Boolean,
    userEmail: String?,
    onAskAi: (String) -> Unit,
    onSaveTheme: (Long, Long, Long, ThemeModePreference) -> Unit,
    onResetTheme: () -> Unit,
    onUpdateHouseholdName: (String) -> Unit,
    onSaveMember: (String?, String, String?, com.example.domain.FamilyRole) -> Unit,
    onDeleteMember: (String) -> Unit,
    onSetActiveMember: (String) -> Unit,
    onJoinByCode: (String) -> Unit,
    onSetThemeMode: (ThemeModePreference) -> Unit,
    onSetCurrency: (String) -> Unit,
    onUpdateNotifications: (Boolean, Boolean, Boolean) -> Unit,
    onGenerateBackupJson: suspend () -> Result<String>,
    onRestoreBackupJson: (String) -> Unit,
    onSignOut: () -> Unit,
    onSignedInFromLocal: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (subScreen) {
        MoreSubScreen.MENU -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("المزيد والأدوات الذكية", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSubScreen(MoreSubScreen.AI_ADVISOR) }
                            .testTag("more_ai_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_ai_advisor_avatar_1795291711629),
                                contentDescription = "المساعد الذكي",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(54.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("المساعد المالي الذكي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("تحليل الإنفاق، خطط التوفير، وتوقع ميزانية الشهر القادم", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                        }
                    }
                }

                item {
                    MoreMenuItemCard(
                        title = "تخصيص المظهر وعجلة الألوان",
                        subtitle = "اختر أي لون بحرية مع المعاينة المباشرة وتباين النصوص",
                        icon = Icons.Rounded.Palette,
                        testTag = "more_theme_card",
                        onClick = { onSelectSubScreen(MoreSubScreen.THEME_WHEEL) }
                    )
                }

                item {
                    MoreMenuItemCard(
                        title = "إدارة الأسرة والصلاحيات",
                        subtitle = "ملف الأسرة، كود الدعوة، وأدوار الأعضاء",
                        icon = Icons.Rounded.Groups,
                        testTag = "more_family_card",
                        onClick = { onSelectSubScreen(MoreSubScreen.FAMILY) }
                    )
                }

                item {
                    MoreMenuItemCard(
                        title = "الإعدادات والنسخ الاحتياطي",
                        subtitle = "العملة، التنبيهات، الوضع الليلي، واستعادة البيانات",
                        icon = Icons.Rounded.Settings,
                        testTag = "more_settings_card",
                        onClick = { onSelectSubScreen(MoreSubScreen.SETTINGS) }
                    )
                }
            }
        }
        MoreSubScreen.AI_ADVISOR -> AiAdvisorScreen(
            messages = aiMessages,
            aiState = aiState,
            isOnline = isOnline,
            onSendQuestion = onAskAi,
            onBack = { onSelectSubScreen(MoreSubScreen.MENU) },
            modifier = modifier
        )
        MoreSubScreen.THEME_WHEEL -> ThemeCustomizerScreen(
            preferences = uiState.preferences,
            onSave = onSaveTheme,
            onReset = onResetTheme,
            onBack = { onSelectSubScreen(MoreSubScreen.MENU) },
            modifier = modifier
        )
        MoreSubScreen.FAMILY -> FamilyManagementScreen(
            uiState = uiState,
            isCloudMode = isCloudMode,
            onUpdateHouseholdName = onUpdateHouseholdName,
            onSaveMember = onSaveMember,
            onDeleteMember = onDeleteMember,
            onSetActiveMember = onSetActiveMember,
            onJoinByCode = onJoinByCode,
            onBack = { onSelectSubScreen(MoreSubScreen.MENU) },
            modifier = modifier
        )
        MoreSubScreen.SETTINGS -> SettingsScreen(
            uiState = uiState,
            isCloudMode = isCloudMode,
            userEmail = userEmail,
            onOpenThemeWheel = { onSelectSubScreen(MoreSubScreen.THEME_WHEEL) },
            onSetThemeMode = onSetThemeMode,
            onSetCurrency = onSetCurrency,
            onUpdateNotifications = onUpdateNotifications,
            onGenerateBackupJson = onGenerateBackupJson,
            onRestoreBackupJson = onRestoreBackupJson,
            onSignOut = onSignOut,
            onSignedInFromLocal = onSignedInFromLocal,
            onBack = { onSelectSubScreen(MoreSubScreen.MENU) },
            modifier = modifier
        )
    }
}

@Composable
private fun MoreMenuItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
        }
    }
}
