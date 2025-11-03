package edu.usc.cs310.anchornotes.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import edu.usc.cs310.anchornotes.util.NotificationHelper;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    public static final String EXTRA_NOTE_ID = "edu.usc.cs310.anchornotes.EXTRA_NOTE_ID";
    public static final String EXTRA_NOTE_TITLE = "edu.usc.cs310.anchornotes.EXTRA_NOTE_TITLE";
    public static final String EXTRA_NOTE_BODY = "edu.usc.cs310.anchornotes.EXTRA_NOTE_BODY";

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        String title = intent.getStringExtra(EXTRA_NOTE_TITLE);
        String body = intent.getStringExtra(EXTRA_NOTE_BODY);
        int noteId = intent.getIntExtra(EXTRA_NOTE_ID, 0);

        if (title != null && noteId != 0) {
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.showNotification(noteId, title, body);

            // Here you would also update the note's 'isRelevant' flag in the database
            // We will add this logic in a later step to keep this focused.
        }
    }
}