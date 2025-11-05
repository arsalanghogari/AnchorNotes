package edu.usc.cs310.anchornotes.broadcast;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import edu.usc.cs310.anchornotes.database.AppDatabase;
import edu.usc.cs310.anchornotes.database.NoteDao;
import edu.usc.cs310.anchornotes.util.NotificationHelper;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    public static final String EXTRA_NOTE_ID = "edu.usc.cs310.anchornotes.EXTRA_NOTE_ID";
    public static final String EXTRA_NOTE_TITLE = "edu.usc.cs310.anchornotes.EXTRA_NOTE_TITLE";
    public static final String EXTRA_NOTE_BODY = "edu.usc.cs310.anchornotes.EXTRA_NOTE_BODY";

    @Override
    public void onReceive(Context context, Intent intent) {
        int noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1);
        String title = intent.getStringExtra(EXTRA_NOTE_TITLE);
        String body = intent.getStringExtra(EXTRA_NOTE_BODY);

        if (noteId != -1 && title != null) {
            Log.d("ReminderReceiver", "Received time reminder for note ID: " + noteId);

            // 1. Show the notification
            new NotificationHelper(context).showNotification(noteId, title, body);

            NoteDao noteDao = AppDatabase.getInstance(context).noteDao();

            // 2. Set the note to be relevant
            AppDatabase.databaseWriteExecutor.execute(() -> {
                noteDao.setNoteRelevance(noteId, true);
            });

            // 3. Schedule the relevance to be cleared in 60 minutes
            scheduleRelevanceClear(context, noteId);
        }
    }

    private void scheduleRelevanceClear(Context context, int noteId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ClearRelevanceBroadcastReceiver.class);
        intent.putExtra(ClearRelevanceBroadcastReceiver.EXTRA_NOTE_ID, noteId);

        // Use a different request code to avoid collision with the main reminder alarm
        // A simple way is to use a negative noteId or add a large constant.
        int requestCode = noteId + 100000;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Calculate time: 60 minutes from now
        long triggerAtMillis = System.currentTimeMillis() + (60 * 60 * 1000);

        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        Log.d("ReminderReceiver", "Scheduled to clear relevance for note ID " + noteId + " in 60 minutes.");
    }
}