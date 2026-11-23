package com.kalmarimre.naptar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
private val ACCENT = Color(0xFFF26B1D)
private val SUCCESS = Color(0xFF3FB97A)
private val CELL = Color(0x0FFFFFFF)
private val BORDER = Color(0x1AFFFFFF)
private val HOLIDAY_BG = Color(0x2E3FB97A)
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

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<Entry>>(EntryStore.load(context)) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var dialogDate by remember { mutableStateOf(LocalDate.now()) }

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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {

        // Fejléc: hónap léptetés, év | hónap középen
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { month = month.minusMonths(1) }) {
                Text("‹", fontSize = 22.sp)
            }
            Text(
                text = "${month.year} | $monthName",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
            OutlinedButton(onClick = { month = month.plusMonths(1) }) {
                Text("›", fontSize = 22.sp)
            }
        }

        // Ma és Új gombok, azonos szélességben
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { month = YearMonth.now() }, modifier = Modifier.weight(1f)) {
                Text("Ma")
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
                Modifier.fillMaxWidth().height(58.dp).padding(vertical = 2.dp),
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
                            .fillMaxHeight()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    !inMonth -> Color.Transparent
                                    isHoliday -> HOLIDAY_BG
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
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = inMonth) { openNew(date) }
                    ) {
                        if (inMonth) {
                            Text(
                                text = "%02d".format(date.dayOfMonth),
                                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                                fontSize = 18.sp,
                                color = if (isHoliday) SUCCESS else MaterialTheme.colorScheme.onSurface
                            )
                            if (dayEntries.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    dayEntries.take(4).forEach {
                                        Box(Modifier.size(6.dp).clip(CircleShape).background(ACCENT))
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
                entries.filter { it.occursOn(date) }.sortedBy { it.time }.forEach { e ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { openEdit(e) }
                            .padding(vertical = 8.dp)
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
