package edu.usc.cs310.anchornotes.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import edu.usc.cs310.anchornotes.database.AppDatabase;
import edu.usc.cs310.anchornotes.repository.NotesRepository;
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
        NotesRepository repository = new NotesRepository((android.app.Application) context.getApplicationContext());

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                // The note ID was used as the geofence request ID.
                int noteId = Integer.parseInt(geofence.getRequestId());
                boolean isEntering = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER;

                // Update the note's 'isRelevant' status in the database.
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    repository.getById(noteId, note -> {
                        if (note != null) {
                            note.setRelevant(isEntering);
                            repository.update(note);
                            Log.d(TAG, "Note " + noteId + " relevance set to " + isEntering);

                            // Show a notification only when entering.
                            if (isEntering) {
                                NotificationHelper notificationHelper = new NotificationHelper(context);
                                notificationHelper.showNotification(noteId, note.getTitle(), "Relevant note nearby!");
                            }
                        }
                    });
                });
            }
        } else {
            Log.e(TAG, "Unknown geofence transition type: " + geofenceTransition);
        }
    }
}