package com.katib.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.katib.app.R
import com.katib.app.data.SubscriptionManager
import com.katib.app.ui.theme.KatibGold
import com.katib.app.ui.theme.KatibTeal

@Composable
fun PaywallScreen(
    isPremium: Boolean,
    onSubscribe: (productId: String) -> Unit,
    onRestore: () -> Unit,
    onDebugUnlock: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✦", style = MaterialTheme.typography.displayMedium, color = KatibGold)
        Text(stringResLabel(R.string.paywall_title), style = MaterialTheme.typography.headlineSmall, color = KatibTeal, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(stringResLabel(R.string.paywall_subtitle), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(20.dp)) {
                listOf(
                    R.string.feature_unlimited,
                    R.string.feature_synonyms,
                    R.string.feature_gulf,
                    R.string.feature_stats,
                    R.string.feature_priority,
                ).forEach { res ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = KatibTeal)
                        Spacer(Modifier.height(0.dp))
                        Text("  " + stringResLabel(res))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (isPremium) {
            Text(stringResLabel(R.string.premium_active), color = KatibTeal, fontWeight = FontWeight.Bold)
        } else {
            Button(
                onClick = { onSubscribe(SubscriptionManager.ANNUAL_PRODUCT_ID) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = KatibTeal),
            ) { Text(stringResLabel(R.string.plan_annual)) }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { onSubscribe(SubscriptionManager.MONTHLY_PRODUCT_ID) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResLabel(R.string.plan_monthly)) }

            Spacer(Modifier.height(12.dp))
            Text(stringResLabel(R.string.start_trial), color = KatibGold, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRestore) { Text(stringResLabel(R.string.restore)) }

            // Dev affordance: unlock locally to test premium gating without Play.
            TextButton(onClick = onDebugUnlock) { Text("Debug: unlock premium", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
