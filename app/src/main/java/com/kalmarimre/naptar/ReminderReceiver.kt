package com.kalmarimre.naptar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

const val ACTION_FIRE = "com.kalmarimre.naptar.FIRE"
const val ACTION_SNOOZE = "com.kalmarimre.naptar.SNOOZE"
const val ACTION_DISMISS = "com.kalmarimre.naptar.DISMISS"
const val EXTRA_ID = "entry_id"
const val EXTRA_TITLE = "entry_title"
const val EXTRA_BODY = "entry_body"
const val SNOOZE_MINUTES = 10

private const val CHANNEL_ID = "reminders"

object Notifier {

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID, "Emlékeztetők", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Bejegyzések időpontra szóló hangjelzései"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 300)
            setSound(sound, attrs)
        }
        nm.createNotificationChannel(channel)
    }

    fun show(ctx: Context, id: String, title: String, body: String) {
        ensureChannel(ctx)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val snoozeIntent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            data = Uri.parse("naptar://notif/snooze/$id")
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
        }
        val dismissIntent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_DISMISS
            data = Uri.parse("naptar://notif/dismiss/$id")
            putExtra(EXTRA_ID, id)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val snoozePi = PendingIntent.getBroadcast(ctx, 0, snoozeIntent, flags)
        val dismissPi = PendingIntent.getBroadcast(ctx, 0, dismissIntent, flags)

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ $title")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(0, "Szundi $SNOOZE_MINUTES perc", snoozePi)
            .addAction(0, "Elvetés", dismissPi)
            .build()

        NotificationManagerCompat.from(ctx).notify(id.hashCode(), notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""

        when (intent.action) {
            ACTION_FIRE -> {
                Notifier.show(context, id, title, body)
                EntryStore.load(context).firstOrNull { it.id == id }?.let { Scheduler.schedule(context, it) }
            }
            ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context).cancel(id.hashCode())
                Scheduler.snooze(context, id, title, SNOOZE_MINUTES)
            }
            ACTION_DISMISS -> NotificationManagerCompat.from(context).cancel(id.hashCode())
        }
    }
}
