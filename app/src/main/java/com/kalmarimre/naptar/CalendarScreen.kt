package com.kalmarimre.naptar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun CalendarScreen() {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    val holidays = remember(month.year) { Holidays.forYear(month.year) }
    val monthName = month.month.getDisplayName(TextStyle.FULL, HU)

    Column(Modifier.fillMaxSize().padding(12.dp)) {

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
            Button(onClick = { }, modifier = Modifier.weight(1f)) {
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
                // Az adott sor hétfője -> ISO hétszám
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
                            ),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (inMonth) {
                            Text(
                                text = "%02d".format(date.dayOfMonth),
                                modifier = Modifier.padding(6.dp),
                                fontSize = 18.sp,
                                color = if (isHoliday) SUCCESS else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
