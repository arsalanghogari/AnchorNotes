package edu.usc.cs310.anchornotes.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.ui.MainActivity;

public class NotificationHelper {
    public static final String NOTIFICATION_NOTE_ID = "edu.usc.cs310.anchornotes.NOTIFICATION_NOTE_ID";
    private static final String CHANNEL_ID = "AnchorNotes_Reminders";
    private static final CharSequence CHANNEL_NAME = "AnchorNotes Reminders";
    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Channel for AnchorNotes reminders");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showNotification(int noteId, String title, String body) {
        // Create an intent that will open MainActivity, which will then route to the correct note.
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(NOTIFICATION_NOTE_ID, noteId);
        intent.setAction("show_note_" + noteId);

        // --- THIS IS THE CORRECTED LINE ---
        PendingIntent pendingIntent = PendingIntent.getActivity(context, noteId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a real icon
                .setContentTitle(title)
                .setContentText("Reminder for your note.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(noteId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}