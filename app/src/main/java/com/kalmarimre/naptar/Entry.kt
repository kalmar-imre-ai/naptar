package com.kalmarimre.naptar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** Egy bejegyzés. A dátum "éééé-hh-nn", az időpont "óó:pp" formátumú. */
data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val time: String,
    val repeat: String,   // none, daily, weekly, monthly, quarterly, yearly
    val remind: Boolean
) {
    /** Megadja, hogy a bejegyzés az adott napra esik-e */
    fun occursOn(day: LocalDate): Boolean {
        val start = LocalDate.parse(date)
        if (day.isBefore(start)) return false
        return when (repeat) {
            "none" -> day == start
            "daily" -> true
            "weekly" -> day.dayOfWeek == start.dayOfWeek
            "monthly" -> day.dayOfMonth == start.dayOfMonth
            "quarterly" -> {
                val months = (day.year - start.year) * 12 + (day.monthValue - start.monthValue)
                months % 3 == 0 && day.dayOfMonth == start.dayOfMonth
            }
            "yearly" -> day.monthValue == start.monthValue && day.dayOfMonth == start.dayOfMonth
            else -> false
        }
    }
}

/** Mentés és betöltés az alkalmazás saját tárhelyére (JSON fájl) */
object EntryStore {
    private const val FILE = "entries.json"

    fun load(ctx: Context): MutableList<Entry> {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return mutableListOf()
        val arr = JSONArray(f.readText())
        return MutableList(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Entry(
                id = o.getString("id"),
                title = o.getString("title"),
                date = o.getString("date"),
                time = o.optString("time", ""),
                repeat = o.optString("repeat", "none"),
                remind = o.optBoolean("remind", false)
            )
        }
    }

    fun save(ctx: Context, entries: List<Entry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("date", e.date)
                put("time", e.time)
                put("repeat", e.repeat)
                put("remind", e.remind)
            })
        }
        File(ctx.filesDir, FILE).writeText(arr.toString())

        // Mentés után minden emlékeztető következő időpontját újraszámoljuk
        entries.forEach { Scheduler.schedule(ctx, it) }
    }
}
