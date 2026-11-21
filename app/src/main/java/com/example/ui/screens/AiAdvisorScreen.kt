package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.state.AiAdvisorUiState
import com.example.ui.state.AiChatMessage

@Composable
fun AiAdvisorScreen(
    messages: List<AiChatMessage>,
    aiState: AiAdvisorUiState,
    isOnline: Boolean,
    onSendQuestion: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var input by remember { mutableStateOf("") }

    val quickQuestions = listOf(
        "أين ذهب معظم المال هذا الشهر؟",
        "حلل مصروفاتنا واقترح طرقاً للتوفير",
        "اكتشف البنود مرتفعة الإنفاق",
        "قدم ملخصاً مالياً شهرياً للأسرة",
        "اقترح ميزانية متوازنة للشهر القادم"
    )

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
            }
            Image(
                painter = painterResource(id = R.drawable.img_ai_advisor_avatar_1795291711629),
                contentDescription = "المساعد المالي الذكي",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("المساعد المالي الذكي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isOnline) "يحلل بيانات أسرتك لحظياً بأمان" else "غير متصل بالإنترنت",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickQuestions.forEach { q ->
                AssistChip(
                    onClick = { onSendQuestion(q) },
                    label = { Text(q) },
                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && aiState is AiAdvisorUiState.Idle) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("أهلاً بك في المستشار المالي الذكي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "يمكنني تحليل مصروفات بيتك، تحديد أكثر التصنيفات استهلاكاً، واقتراح ميزانية مخصصة وخطط توفير ذكية بناءً على أرقامك الفعلية.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.fromUser) Arrangement.Start else Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (msg.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(0.88f)
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            when (aiState) {
                AiAdvisorUiState.Loading -> item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("جاري تحليل ميزانية الأسرة...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AiAdvisorUiState.Streaming -> item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Text(
                            text = aiState.partialText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is AiAdvisorUiState.Error -> item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                AiAdvisorUiState.Idle -> Unit
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("اسأل المساعد المالي...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f).testTag("ai_question_input")
            )
            FilledIconButton(
                onClick = {
                    val q = input.trim()
                    if (q.isNotEmpty()) {
                        input = ""
                        onSendQuestion(q)
                    }
                },
                enabled = aiState !is AiAdvisorUiState.Loading && aiState !is AiAdvisorUiState.Streaming,
                modifier = Modifier.size(50.dp).testTag("ai_send_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "إرسال")
            }
        }
    }
}
