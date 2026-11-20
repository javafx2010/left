package com.unic.left.feature.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAirport
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unic.left.ui.theme.BrandPrimary
import com.unic.left.ui.theme.Ink
import com.unic.left.ui.theme.MutedInk

/** 把原型里的 iconName 映射到 Material 图标。 */
fun placeIcon(name: String): ImageVector = when (name) {
    "home" -> Icons.Default.Home
    "briefcase" -> Icons.Default.Business
    "plane" -> Icons.Default.LocalAirport
    "flag" -> Icons.Default.Flag
    "bed" -> Icons.Default.Bed
    "star" -> Icons.Default.Star
    "bike" -> Icons.Default.DirectionsBike
    "coffee" -> Icons.Default.Coffee
    else -> Icons.Default.AccountBalanceWallet
}

fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    BrandPrimary
}

@Composable
fun SectionTitle(title: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        if (trailing != null) Text(trailing, fontSize = 11.sp, color = MutedInk)
    }
}

@Composable
fun PageTitle(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(subtitle, fontSize = 11.5.sp, color = MutedInk)
        }
        action?.invoke()
    }
}
