package com.kalmarimre.naptar

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

object Backup {
    private const val APP = "simple-calendar"
    private val REPEATS = setOf("none", "daily", "weekly", "monthly", "quarterly", "yearly")

    /** Mentési fájl szövege (ugyanaz a formátum, mint a web-pilotban) */
    fun toJson(entries: List<Entry>): String {
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
        return JSONObject().apply {
            put("app", APP)
            put("version", 2)
            put("exportedAt", Instant.now().toString())
            put("entries", arr)
        }.toString(2)
    }

    /** Beolvassa a mentési fájlt. Hibás fájlnál IllegalArgumentException. */
    fun fromJson(text: String): List<Entry> {
        val root = runCatching { JSONObject(text) }.getOrNull()
            ?: throw IllegalArgumentException("Ez a fájl nem érvényes biztonsági mentés.")
        if (root.optString("app") != APP) {
            throw IllegalArgumentException("Ez nem naptár-biztonsági mentés.")
        }
        val arr = root.optJSONArray("entries")
            ?: throw IllegalArgumentException("A mentésből hiányoznak a bejegyzések.")

        val out = mutableListOf<Entry>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id")
            val title = o.optString("title")
            val date = o.optString("date")
            if (id.isEmpty() || title.isEmpty()) continue
            if (runCatching { LocalDate.parse(date) }.isFailure) continue

            var time = o.optString("time", "")
            if (time.isNotEmpty() && runCatching { LocalTime.parse(time) }.isFailure) time = ""

            val repeat = o.optString("repeat", "none").let { if (it in REPEATS) it else "none" }

            out.add(
                Entry(
                    id = id,
                    title = title,
                    date = date,
                    time = time,
                    repeat = repeat,
                    remind = o.optBoolean("remind", false)
                )
            )
        }
        return out
    }

    /** Összefésüli a meglévő bejegyzéseket a beolvasottakkal: azonos azonosító felülíródik */
    fun merge(current: List<Entry>, incoming: List<Entry>): List<Entry> {
        val byId = LinkedHashMap<String, Entry>()
        current.forEach { byId[it.id] = it }
        incoming.forEach { byId[it.id] = it }
        return byId.values.toList()
    }
}
