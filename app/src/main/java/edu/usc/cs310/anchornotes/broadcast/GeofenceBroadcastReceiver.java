package edu.usc.cs310.anchornotes.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofenceStatusCodes;

import edu.usc.cs310.anchornotes.database.AppDatabase;
import edu.usc.cs310.anchornotes.database.NoteDao;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.util.NotificationHelper;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        NoteDao noteDao = AppDatabase.getInstance(context).noteDao();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                int noteId = Integer.parseInt(geofence.getRequestId());
                boolean isEntering = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER;

                Log.d(TAG, "Geofence transition for note ID " + noteId + ". Is entering: " + isEntering);

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    // Update the note's relevance status.
                    noteDao.setNoteRelevance(noteId, isEntering);

                    // ================================================================= //
                    // === THE CORRECTED LOGIC IS HERE: Use a synchronous DB call ====== //
                    // ================================================================= //
                    if (isEntering) {
                        // We are on a background thread, so we can call getById directly.
                        Note note = noteDao.getById(noteId);
                        if (note != null) {
                            new NotificationHelper(context).showNotification(noteId, note.getTitle(), "Relevant note nearby!");
                        }
                    }
                });
            }
        }
    }
}