package com.kalmarimre.naptar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

private val WEEKDAYS = listOf("H", "K", "Sz", "Cs", "P", "Szo", "V")
private val ACCENT = Color(0xFF6D4AFF)
private val SUCCESS = Color(0xFF3FB97A)
private val CELL = Color(0x0FFFFFFF)
private val BORDER = Color(0x1AFFFFFF)
private val HU = Locale.forLanguageTag("hu-HU")

/** "22 (vasárnap) – 10:00" vagy "24 (csütörtök) – Karácsony" */
private fun fmtWhen(date: LocalDate, time: String?, name: String?): String {
    val wd = date.dayOfWeek.getDisplayName(TextStyle.FULL, HU)
    val base = "%02d (%s)".format(date.dayOfMonth, wd)
    return when {
        name != null -> "$base – $name"
        !time.isNullOrEmpty() -> "$base – $time"
        else -> base
    }
}

/** Saját rajzolt léptető nyíl. forward = true: jobbra mutat */
@Composable
private fun NavButton(forward: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 64.dp, height = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CELL)
            .border(1.dp, BORDER, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(16.dp, 22.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                if (forward) {
                    moveTo(w * 0.25f, 0f)
                    lineTo(w * 0.75f, h / 2)
                    lineTo(w * 0.25f, h)
                } else {
                    moveTo(w * 0.75f, 0f)
                    lineTo(w * 0.25f, h / 2)
                    lineTo(w * 0.75f, h)
                }
            }
            drawPath(
                path,
                ACCENT,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<Entry>>(EntryStore.load(context)) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var dialogDate by remember { mutableStateOf(LocalDate.now()) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<List<Entry>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(Backup.toJson(entries).toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                message = "Mentés elkészült (${entries.size} bejegyzés)."
            }.onFailure {
                message = "A mentés nem sikerült."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalArgumentException("A fájl nem olvasható.")
                Backup.fromJson(text)
            }.onSuccess {
                pendingImport = it
            }.onFailure {
                message = it.message ?: "A betöltés nem sikerült."
            }
        }
    }

    val today = LocalDate.now()
    val holidays = remember(month.year) { Holidays.forYear(month.year) }
    val monthName = month.month.getDisplayName(TextStyle.FULL, HU)

    fun openNew(date: LocalDate) {
        editing = null
        dialogDate = date
        showDialog = true
    }

    fun openEdit(e: Entry) {
        editing = e
        dialogDate = LocalDate.parse(e.date)
        showDialog = true
    }

    if (showDialog) {
        val target = editing
        EntryDialog(
            initial = target,
            defaultDate = dialogDate,
            onDismiss = { showDialog = false },
            onSave = { e ->
                entries = if (entries.any { it.id == e.id }) {
                    entries.map { if (it.id == e.id) e else it }
                } else {
                    entries + e
                }
                EntryStore.save(context, entries)
                showDialog = false
            },
            onDelete = if (target == null) null else {
                {
                    Scheduler.cancel(context, target.id)
                    entries = entries.filter { it.id != target.id }
                    EntryStore.save(context, entries)
                    showDialog = false
                }
            }
        )
    }

    // Üzenet panel (mentés/betöltés eredménye vagy hiba)
    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            containerColor = Color(0xFF141417),
            tonalElevation = 0.dp,
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text("OK") }
            }
        )
    }

    // Betöltés megerősítése
    pendingImport?.let { incoming ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            containerColor = Color(0xFF141417),
            tonalElevation = 0.dp,
            title = { Text("Betöltés") },
            text = {
                Text(
                    "Betöltöd a ${incoming.size} bejegyzést? " +
                    "Az azonos azonosítójú bejegyzések felülíródnak, a többi megmarad."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    entries = Backup.merge(entries, incoming)
                    EntryStore.save(context, entries)
                    pendingImport = null
                    message = "${incoming.size} bejegyzés betöltve."
                }) { Text("Betöltés") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Mégse") }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {

        // Fejléc: léptető nyilak, év. hónap középen
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavButton(forward = false) { month = month.minusMonths(1) }
            Text(
                text = "${month.year}. $monthName",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
            NavButton(forward = true) { month = month.plusMonths(1) }
        }

        // Ma és Új gombok, azonos szélességben
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { month = YearMonth.now() }, modifier = Modifier.weight(1f)) {
                Text("Ma", color = Color.White)
            }
            Button(
                onClick = {
                    openNew(if (YearMonth.from(today) == month) today else month.atDay(1))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Új")
            }
        }

        // Mentés és Betöltés, azonos szélességben
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { exportLauncher.launch("naptar-mentes-${LocalDate.now()}.json") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Mentés", color = Color.White)
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Betöltés", color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Hét napjainak fejléce (az első oszlop üres)
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(24.dp))
            WEEKDAYS.forEach { w ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(w, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Mindig 6 hét-sor, hétfővel kezdve
        val first = month.atDay(1)
        val offset = (first.dayOfWeek.value + 6) % 7

        for (row in 0 until 6) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val monday = first.plusDays((row * 7 - offset).toLong())
                Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString(),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                for (col in 0 until 7) {
                    val date = monday.plusDays(col.toLong())
                    val inMonth = date.month == month.month
                    val isHoliday = inMonth && holidays.containsKey(date)
                    val isToday = date == today
                    val dayEntries = if (inMonth) entries.filter { it.occursOn(date) } else emptyList()

                    Box(
                        Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    !inMonth -> Color.Transparent
                                    isHoliday -> Color.Transparent
                                    else -> CELL
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isToday -> ACCENT
                                    inMonth -> BORDER
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = inMonth) { openNew(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (inMonth) {
                            Text(
                                text = "%02d".format(date.dayOfMonth),
                                fontSize = 16.sp,
                                color = if (isHoliday) SUCCESS else MaterialTheme.colorScheme.onSurface
                            )
                            if (dayEntries.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    dayEntries.take(4).forEach {
                                        Box(Modifier.size(5.dp).clip(CircleShape).background(ACCENT))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Havi lista: ünnepek és bejegyzések
        val monthDays = (1..month.lengthOfMonth())
            .map { month.atDay(it) }
            .filter { d -> holidays.containsKey(d) || entries.any { it.occursOn(d) } }

        if (monthDays.isEmpty()) {
            Text("Nincs bejegyzés.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            monthDays.forEach { date ->
                holidays[date]?.let { name ->
                    Text(
                        text = fmtWhen(date, null, name),
                        color = SUCCESS,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
                entries.filter { it.occursOn(date) }.sortedBy { it.time }.forEach { e ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { openEdit(e) }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = fmtWhen(date, e.time, null),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = e.title, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
