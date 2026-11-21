package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AlertSeverity
import com.example.domain.SmartAlert
import com.example.ui.theme.DangerRed
import com.example.ui.theme.WarningAmber

fun categoryIconFor(key: String): ImageVector = when (key) {
    "home" -> Icons.Rounded.Home
    "shopping_basket" -> Icons.Rounded.ShoppingBasket
    "eco" -> Icons.Rounded.Eco
    "restaurant" -> Icons.Rounded.Restaurant
    "local_drink" -> Icons.Rounded.LocalDrink
    "cleaning_services" -> Icons.Rounded.CleaningServices
    "bolt" -> Icons.Rounded.Bolt
    "receipt_long" -> Icons.Rounded.ReceiptLong
    "directions_car" -> Icons.Rounded.DirectionsCar
    "school" -> Icons.Rounded.School
    "medical_services" -> Icons.Rounded.MedicalServices
    "checkroom" -> Icons.Rounded.Checkroom
    "volunteer_activism" -> Icons.Rounded.VolunteerActivism
    "flight" -> Icons.Rounded.Flight
    "celebration" -> Icons.Rounded.Celebration
    "health_and_safety" -> Icons.Rounded.HealthAndSafety
    "shield" -> Icons.Rounded.Shield
    "savings" -> Icons.Rounded.Savings
    "wallet" -> Icons.Rounded.AccountBalanceWallet
    else -> Icons.Rounded.Category
}

@Composable
fun CategoryAvatar(
    iconKey: String,
    colorHex: Long,
    modifier: Modifier = Modifier,
    sizeDp: Int = 44
) {
    val tint = Color(colorHex.toInt())
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = categoryIconFor(iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((sizeDp * 0.52f).dp)
        )
    }
}

@Composable
fun SmartAlertCard(alert: SmartAlert, modifier: Modifier = Modifier) {
    val (bg, iconColor, icon) = when (alert.severity) {
        AlertSeverity.CRITICAL -> Triple(
            DangerRed.copy(alpha = 0.12f),
            DangerRed,
            Icons.Rounded.ErrorOutline
        )
        AlertSeverity.WARNING -> Triple(
            WarningAmber.copy(alpha = 0.14f),
            WarningAmber,
            Icons.Rounded.WarningAmber
        )
        AlertSeverity.INFO -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.primary,
            Icons.Rounded.Info
        )
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyStatePanel(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
