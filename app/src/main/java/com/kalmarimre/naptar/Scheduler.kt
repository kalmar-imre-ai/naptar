package com.kalmarimre.naptar

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

object Scheduler {

    private val HU = Locale.forLanguageTag("hu-HU")

    /** A következő előfordulás, ami még a jövőben van (legfeljebb ~2 év előre) */
    fun nextTrigger(e: Entry, now: LocalDateTime): LocalDateTime? {
        if (e.time.isEmpty()) return null
        val t = LocalTime.parse(e.time)
        var day: LocalDate = now.toLocalDate()
        repeat(800) {
            if (e.occursOn(day)) {
                val trig = LocalDateTime.of(day, t)
                if (trig.isAfter(now)) return trig
            }
            day = day.plusDays(1)
        }
        return null
    }

    /** Beállítja az adott bejegyzés következő emlékeztetőjét */
    fun schedule(ctx: Context, e: Entry) {
        cancel(ctx, e.id)
        if (!e.remind || e.time.isEmpty()) return
        val next = nextTrigger(e, LocalDateTime.now()) ?: return
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = buildIntent(
            ctx, "naptar://entry/${e.id}", e.id, e.title, fmt(next.toLocalDate(), e.time)
        )
        setAlarm(ctx, millis, pending(ctx, intent))
    }

    /** Minden bejegyzés emlékeztetőjét újra beállítja (indításkor) */
    fun scheduleAll(ctx: Context, entries: List<Entry>) {
        entries.forEach { schedule(ctx, it) }
    }

    /** Megszünteti a bejegyzés riasztását (törléskor is) */
    fun cancel(ctx: Context, id: String) {
        val intent = buildIntent(ctx, "naptar://entry/$id", id, "", "")
        alarm(ctx).cancel(pending(ctx, intent))
    }

    /** Szundi: újra szól a megadott idő múlva */
    fun snooze(ctx: Context, id: String, title: String, minutes: Int) {
        val millis = System.currentTimeMillis() + minutes * 60_000L
        val intent = buildIntent(ctx, "naptar://snooze/$id", id, title, "Szundi után")
        setAlarm(ctx, millis, pending(ctx, intent))
    }

    private fun buildIntent(ctx: Context, uri: String, id: String, title: String, body: String): Intent {
        return Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse(uri)
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
        }
    }

    private fun pending(ctx: Context, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarm(ctx: Context): AlarmManager {
        return ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun setAlarm(ctx: Context, millis: Long, pi: PendingIntent) {
        val am = alarm(ctx)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            // Pontos engedély nélkül is működik, csak kis eltéréssel
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    /** "22 (vasárnap) – 10:00" */
    private fun fmt(date: LocalDate, time: String): String {
        val wd = date.dayOfWeek.getDisplayName(TextStyle.FULL, HU)
        return "%02d (%s) – %s".format(date.dayOfMonth, wd, time)
    }
}
