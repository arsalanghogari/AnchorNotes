package edu.usc.cs310.anchornotes.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import edu.usc.cs310.anchornotes.database.AppDatabase;
import edu.usc.cs310.anchornotes.database.NoteDao;

public class ClearRelevanceBroadcastReceiver extends BroadcastReceiver {

    public static final String EXTRA_NOTE_ID = "edu.usc.cs310.anchornotes.EXTRA_NOTE_ID_CLEAR";

    @Override
    public void onReceive(Context context, Intent intent) {
        int noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1);
        if (noteId != -1) {
            Log.d("ClearRelevance", "Clearing relevance for note ID: " + noteId);
            NoteDao noteDao = AppDatabase.getInstance(context).noteDao();
            AppDatabase.databaseWriteExecutor.execute(() -> {
                noteDao.setNoteRelevance(noteId, false);
            });
        }
    }
}