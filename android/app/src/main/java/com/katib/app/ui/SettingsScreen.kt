package com.katib.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.katib.app.R
import com.katib.app.data.WritingMode
import com.katib.app.ui.theme.KatibGold
import com.katib.app.ui.theme.KatibTeal

@Composable
fun SettingsScreen(
    mode: WritingMode,
    isPremium: Boolean,
    onModeChange: (WritingMode) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(16.dp)) {

        SectionCard(stringResLabel(R.string.writing_mode)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WritingMode.entries.forEach { m ->
                    val locked = m == WritingMode.GULF && !isPremium
                    FilterChip(
                        selected = mode == m,
                        onClick = { if (locked) onOpenPaywall() else onModeChange(m) },
                        label = { Text(m.arabicLabel + if (locked) "  🔒" else "") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KatibTeal,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SectionCard(stringResLabel(R.string.subscription)) {
            if (isPremium) {
                Text(stringResLabel(R.string.premium_active), color = KatibTeal, fontWeight = FontWeight.Bold)
            } else {
                TextButton(onClick = onOpenPaywall) {
                    Text(stringResLabel(R.string.manage_sub), color = KatibGold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SectionCard(stringResLabel(R.string.app_name)) {
            TextButton(onClick = {
                val email = Intent(Intent.ACTION_SENDTO, "mailto:feedback@katib.app".toUri())
                context.startActivity(Intent.createChooser(email, null))
            }) { Text(stringResLabel(R.string.feedback)) }
            HorizontalDivider()
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, "https://katib.app/privacy".toUri())
                context.startActivity(intent)
            }) { Text(stringResLabel(R.string.privacy_policy)) }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = KatibTeal, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
