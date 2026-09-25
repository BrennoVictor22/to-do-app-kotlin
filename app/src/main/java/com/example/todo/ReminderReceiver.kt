package com.example.todo

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "task_reminders"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(channelId, "Lembretes de tarefas", NotificationManager.IMPORTANCE_DEFAULT))
        val title = intent.getStringExtra("title") ?: "Tarefa"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Lembrete da tarefa")
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        manager.notify(intent.getLongExtra("id", 0L).toInt(), notification)
    }
}

object ReminderScheduler {
    fun schedule(context: Context, task: Task) {
        cancel(context, task.id)
        val due = task.dueDateTime ?: return
        if (task.completed || due <= System.currentTimeMillis()) return
        val intent = Intent(context, ReminderReceiver::class.java).apply { putExtra("id", task.id); putExtra("title", task.title) }
        val pending = PendingIntent.getBroadcast(context, task.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending)
    }
    fun cancel(context: Context, id: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pending?.let { context.getSystemService(AlarmManager::class.java).cancel(it); it.cancel() }
    }
}
