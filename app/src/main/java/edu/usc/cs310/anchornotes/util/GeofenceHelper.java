package edu.usc.cs310.anchornotes.util;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import edu.usc.cs310.anchornotes.broadcast.GeofenceBroadcastReceiver;
import edu.usc.cs310.anchornotes.model.Note;

public class GeofenceHelper {
    private static final String TAG = "GeofenceHelper";
    private final Context context;

    public GeofenceHelper(Context context) {
        this.context = context;
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
    }

    public void addGeofence(Note note) {
        if (note.getId() == 0 || !"geofence".equals(note.getReminderType())) return;

        Geofence geofence = new Geofence.Builder()
                .setRequestId(String.valueOf(note.getId())) // Use note ID as the unique ID
                .setCircularRegion(note.getLatitude(), note.getLongitude(), note.getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getGeofencingClient(context)
                    .addGeofences(geofencingRequest, getPendingIntent())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Geofence added for note ID: " + note.getId());
                        Toast.makeText(context, "Location reminder set for " + note.getTitle(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add geofence", e);
                        Toast.makeText(context, "Failed to set location reminder.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    public void removeGeofence(Note note) {
        if (note.getId() == 0) return;
        LocationServices.getGeofencingClient(context)
                .removeGeofences(java.util.Collections.singletonList(String.valueOf(note.getId())))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence removed for note ID: " + note.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to remove geofence", e));
    }
}