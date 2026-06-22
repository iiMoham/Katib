package com.katib.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katib.app.R
import com.katib.app.data.WritingMode
import com.katib.app.data.WritingStats
import com.katib.app.net.CorrectResponse
import com.katib.app.ui.theme.KatibErrorOrange
import com.katib.app.ui.theme.KatibGold
import com.katib.app.ui.theme.KatibTeal
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    stats: WritingStats,
    mode: WritingMode,
    runCorrection: suspend (String, String) -> CorrectResponse?,
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("انشاء الله بكرة نروح المدرسه") }
    var result by remember { mutableStateOf<CorrectResponse?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stats.correctionsThisWeek.toString(), stringResLabel(R.string.stat_corrections), Modifier.weight(1f))
            StatCard(stats.suggestionsAccepted.toString(), stringResLabel(R.string.stat_suggestions), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        StatCard(stats.topErrorType, stringResLabel(R.string.stat_top_error), Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        Text(stringResLabel(R.string.try_it), style = MaterialTheme.typography.titleMedium, color = KatibTeal)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    result = runCorrection(input, mode.wire)
                    loading = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = KatibTeal),
        ) { Text("صحّح") }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(color = KatibTeal)
        }

        result?.let { res ->
            if (res.corrections.isEmpty() && res.suggestions.isEmpty()) {
                Text("لا توجد أخطاء ✓", color = KatibTeal)
            }
            res.corrections.forEach { c ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row {
                            Text(c.original, color = KatibErrorOrange, fontWeight = FontWeight.Bold)
                            Text("  ←  ", color = MaterialTheme.colorScheme.onSurface)
                            Text(c.corrected, color = KatibTeal, fontWeight = FontWeight.Bold)
                        }
                        if (c.reason.isNotBlank()) {
                            Text(c.reason, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            res.suggestions.forEach { g ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("بدائل لـ «${g.target}»:", color = KatibGold, fontWeight = FontWeight.Bold)
                        g.options.forEach { o ->
                            Text("• ${o.text}  (${o.register})", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = KatibTeal)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
