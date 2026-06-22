package com.katib.app.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.katib.app.R
import com.katib.app.ui.theme.KatibGold
import com.katib.app.ui.theme.KatibTeal

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("✦ كاتب", style = MaterialTheme.typography.displaySmall, color = KatibTeal)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResLabel(R.string.welcome_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = KatibGold,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResLabel(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    stringResLabel(R.string.enable_kb_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = KatibTeal,
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResLabel(R.string.enable_kb_step1))
                Spacer(Modifier.height(6.dp))
                Text(stringResLabel(R.string.enable_kb_step2))
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { openImeSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = KatibTeal),
                ) { Text(stringResLabel(R.string.open_kb_settings)) }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showImePicker(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResLabel(R.string.pick_kb)) }
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onDone) {
            Text(stringResLabel(R.string.done), color = KatibTeal)
        }
    }
}

private fun openImeSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun showImePicker(context: Context) {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        .showInputMethodPicker()
}
