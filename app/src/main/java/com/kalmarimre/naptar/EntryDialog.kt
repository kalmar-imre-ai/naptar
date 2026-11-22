package com.kalmarimre.naptar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.UUID

private val REPEATS = listOf(
    "none" to "Nem ismétlődik",
    "daily" to "Naponta",
    "weekly" to "Hetente",
    "monthly" to "Havonta",
    "quarterly" to "Negyedévente",
    "yearly" to "Évente"
)

/**
 * Szerkesztő panel. initial = null esetén új bejegyzés.
 * onDelete = null esetén nincs törlés gomb.
 */
@Composable
fun EntryDialog(
    initial: Entry?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Entry) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: defaultDate.toString()) }
    var time by remember { mutableStateOf(initial?.time ?: "10:00") }
    var repeat by remember { mutableStateOf(initial?.repeat ?: "none") }
    var remind by remember { mutableStateOf(initial?.remind ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Új bejegyzés" else "Bejegyzés szerkesztése") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Cím") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Dátum (éééé-hh-nn)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Időpont (óó:pp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Koppintásra a következő ismétlési módra vált
                val label = REPEATS.first { it.first == repeat }.second
                OutlinedButton(
                    onClick = {
                        val idx = REPEATS.indexOfFirst { it.first == repeat }
                        repeat = REPEATS[(idx + 1) % REPEATS.size].first
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ismétlés: $label")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remind, onCheckedChange = { remind = it })
                    Text("Hangjelzés az időpontban")
                }
                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                val validTime = time.isEmpty() || Regex("""\d{2}:\d{2}""").matches(time)
                if (title.isBlank()) {
                    errorMsg = "Adj meg címet."
                } else if (parsedDate == null) {
                    errorMsg = "Érvénytelen dátum (éééé-hh-nn)."
                } else if (!validTime) {
                    errorMsg = "Érvénytelen időpont (óó:pp)."
                } else {
                    onSave(
                        Entry(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            date = parsedDate.toString(),
                            time = time,
                            repeat = repeat,
                            remind = remind
                        )
                    )
                }
            }) {
                Text("Mentés")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Törlés", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Mégse")
                }
            }
        }
    )
}
