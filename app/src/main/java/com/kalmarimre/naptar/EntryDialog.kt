package com.kalmarimre.naptar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

private val REPEATS = listOf(
    "none" to "Nem ismétlődik",
    "daily" to "Naponta",
    "weekly" to "Hetente",
    "monthly" to "Havonta",
    "quarterly" to "Negyedévente",
    "yearly" to "Évente"
)

private val PANEL = Color(0xFF141417)

private fun LocalDate.toMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun fmtDate(d: LocalDate): String =
    "%04d. %02d. %02d.".format(d.year, d.monthValue, d.dayOfMonth)

/** Kuka ikon, rajzolva (nincs külső ikoncsomag) */
@Composable
private fun TrashButton(onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.error
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val sw = 2.dp.toPx()
            val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val lid = Path().apply {
                moveTo(w * 0.1f, h * 0.22f)
                lineTo(w * 0.9f, h * 0.22f)
            }
            val handle = Path().apply {
                moveTo(w * 0.36f, h * 0.22f)
                lineTo(w * 0.36f, h * 0.08f)
                lineTo(w * 0.64f, h * 0.08f)
                lineTo(w * 0.64f, h * 0.22f)
            }
            val bin = Path().apply {
                moveTo(w * 0.2f, h * 0.22f)
                lineTo(w * 0.27f, h * 0.95f)
                lineTo(w * 0.73f, h * 0.95f)
                lineTo(w * 0.8f, h * 0.22f)
            }
            drawPath(lid, tint, style = stroke)
            drawPath(handle, tint, style = stroke)
            drawPath(bin, tint, style = stroke)
            drawLine(tint, Offset(w * 0.42f, h * 0.4f), Offset(w * 0.42f, h * 0.8f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.58f, h * 0.4f), Offset(w * 0.58f, h * 0.8f), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

/**
 * Szerkesztő panel. initial = null esetén új bejegyzés.
 * onDelete = null esetén nincs törlés gomb.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDialog(
    initial: Entry?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Entry) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember { mutableStateOf(initial?.let { LocalDate.parse(it.date) } ?: defaultDate) }
    var time by remember { mutableStateOf(initial?.time?.takeIf { it.isNotEmpty() } ?: "10:00") }
    var repeat by remember { mutableStateOf(initial?.repeat ?: "none") }
    var remind by remember { mutableStateOf(initial?.remind ?: true) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun save() {
        if (title.isBlank()) {
            errorMsg = "Adj meg címet."
            return
        }
        onSave(
            Entry(
                id = initial?.id ?: UUID.randomUUID().toString(),
                title = title.trim(),
                date = date.toString(),
                time = time,
                repeat = repeat,
                remind = remind
            )
        )
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toMillis())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = it.toLocalDate() }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("Mégse") }
            },
            tonalElevation = 0.dp
        ) {
            DatePicker(state = state)
        }
    }

    if (showTime) {
        val parsed = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(10, 0))
        val state = rememberTimePickerState(
            initialHour = parsed.hour,
            initialMinute = parsed.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            containerColor = PANEL,
            tonalElevation = 0.dp,
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    time = "%02d:%02d".format(state.hour, state.minute)
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text("Mégse") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PANEL,
        tonalElevation = 0.dp,
        title = { Text(if (initial == null) "Új bejegyzés" else "Szerkesztés") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Cím") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Dátum (szélesebb) és idő (keskenyebb) egy sorban
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDate = true },
                        modifier = Modifier.weight(2f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(fmtDate(date), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showTime = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(time, maxLines = 1)
                    }
                }
                // Koppintásra a következő ismétlési módra vált
                val label = REPEATS.first { it.first == repeat }.second
                OutlinedButton(
                    onClick = {
                        val idx = REPEATS.indexOfFirst { it.first == repeat }
                        repeat = REPEATS[(idx + 1) % REPEATS.size].first
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(label)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remind, onCheckedChange = { remind = it })
                    Text("Hangjelzés")
                }
                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                // Alsó sor: kuka (ha szerkesztés), Mégse, Mentés egy sorban
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        TrashButton(onClick = onDelete)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("Mégse")
                    }
                    Button(onClick = { save() }) {
                        Text("Mentés")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = null
    )
}
