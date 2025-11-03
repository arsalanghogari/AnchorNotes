package edu.usc.cs310.anchornotes.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.broadcast.ReminderBroadcastReceiver;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.util.GeofenceHelper;
import edu.usc.cs310.anchornotes.util.NotificationHelper;
import edu.usc.cs310.anchornotes.viewmodel.NoteViewModel;

public class MainActivity extends AppCompatActivity {

    private NoteViewModel viewModel;
    private GeofenceHelper geofenceHelper;

    // ... (UI field declarations)

    private final ActivityResultLauncher<Intent> editorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String title = data.getStringExtra("note_title");
                    String body  = data.getStringExtra("note_body");
                    int noteId   = data.getIntExtra(EditorActivity.EXTRA_NOTE_ID, -1);
                    String reminderType = data.getStringExtra("note_reminder_type");

                    if (noteId != -1) {
                        // EDIT EXISTING NOTE (This logic was always correct)
                        viewModel.getById(noteId, noteToUpdate -> {
                            if (noteToUpdate != null) {
                                cancelTimeReminder(noteToUpdate);
                                geofenceHelper.removeGeofence(noteToUpdate);
                                noteToUpdate.setTitle(title);
                                noteToUpdate.setBody(body);
                                noteToUpdate.setReminderType(reminderType);
                                if ("time".equals(reminderType)) {
                                    long reminderTime = data.getLongExtra("note_reminder_time", 0);
                                    noteToUpdate.setReminderTime(reminderTime);
                                    scheduleTimeReminder(noteToUpdate);
                                } else if ("geofence".equals(reminderType)) {
                                    noteToUpdate.setLatitude(data.getDoubleExtra("note_latitude", 0));
                                    noteToUpdate.setLongitude(data.getDoubleExtra("note_longitude", 0));
                                    noteToUpdate.setRadius(data.getFloatExtra("note_radius", 0));
                                    noteToUpdate.setLocationName(data.getStringExtra("note_location_name"));
                                    geofenceHelper.addGeofence(noteToUpdate);
                                }
                                viewModel.update(noteToUpdate);
                            }
                        });
                    } else {
                        // ====================================================================== //
                        // === THE NEW, SIMPLIFIED, AND CORRECT LOGIC FOR CREATING A NEW NOTE === //
                        // ====================================================================== //

                        // Step 1: Create the note object. It has all the reminder data from the
                        // EditorActivity, but its ID is currently 0.
                        Note newNote = new Note(title, body);
                        newNote.setReminderType(reminderType);
                        if ("time".equals(reminderType)) {
                            newNote.setReminderTime(data.getLongExtra("note_reminder_time", 0));
                        } else if ("geofence".equals(reminderType)) {
                            newNote.setLatitude(data.getDoubleExtra("note_latitude", 0));
                            newNote.setLongitude(data.getDoubleExtra("note_longitude", 0));
                            newNote.setRadius(data.getFloatExtra("note_radius", 0));
                            newNote.setLocationName(data.getStringExtra("note_location_name"));
                        }

                        // Step 2: Insert the note. This saves it and generates a new ID.
                        viewModel.insert(newNote, newId -> {
                            // Step 3: Inside the callback, we get the REAL ID from the database.

                            // Step 4 (THE FIX): We take our original 'newNote' object (which still has all
                            // the correct reminder data) and we update its ID to match the real ID
                            // from the database.
                            newNote.setId(newId.intValue());

                            // Step 5: Now, we pass this fully correct 'newNote' object (with both the
                            // correct reminder data AND the correct ID) to our helper methods.
                            if ("time".equals(newNote.getReminderType())) {
                                scheduleTimeReminder(newNote);
                            } else if ("geofence".equals(newNote.getReminderType())) {
                                geofenceHelper.addGeofence(newNote);
                            }
                        });
                    }
                }
            });

    // ... (All other methods are unchanged and provided below for completeness)

    // UI for All Notes
    private ArrayAdapter<String> adapter;
    private ArrayList<String> noteTitles;
    private List<Note> currentNotes;
    // UI for Relevant Notes
    private TextView relevantNotesHeader;
    private ListView relevantNotesList;
    private ArrayAdapter<String> relevantAdapter;
    private ArrayList<String> relevantNoteTitles;
    private List<Note> currentRelevantNotes;
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {});
    private final ActivityResultLauncher<String[]> requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        geofenceHelper = new GeofenceHelper(this);
        askNotificationPermission();
        askLocationPermissions();
        ListView notesList = findViewById(R.id.notesList);
        FloatingActionButton addButton = findViewById(R.id.addButton);
        Button mapButton = findViewById(R.id.mapButton);
        relevantNotesHeader = findViewById(R.id.relevantNotesHeader);
        relevantNotesList = findViewById(R.id.relevantNotesList);
        noteTitles = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, noteTitles);
        notesList.setAdapter(adapter);
        relevantNoteTitles = new ArrayList<>();
        relevantAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, relevantNoteTitles);
        relevantNotesList.setAdapter(relevantAdapter);
        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        viewModel.getNotesLiveData().observe(this, notes -> {
            currentNotes = notes;
            noteTitles.clear();
            for (Note note : notes) {
                noteTitles.add(note.getTitle().isEmpty() ? "(Untitled)" : note.getTitle());
            }
            adapter.notifyDataSetChanged();
        });
        viewModel.getRelevantNotesLiveData().observe(this, relevantNotes -> {
            currentRelevantNotes = relevantNotes;
            relevantNoteTitles.clear();
            if (relevantNotes == null || relevantNotes.isEmpty()) {
                relevantNotesHeader.setVisibility(View.GONE);
                relevantNotesList.setVisibility(View.GONE);
            } else {
                relevantNotesHeader.setVisibility(View.VISIBLE);
                relevantNotesList.setVisibility(View.VISIBLE);
                for (Note note : relevantNotes) {
                    relevantNoteTitles.add(note.getTitle().isEmpty() ? "(Untitled)" : note.getTitle());
                }
            }
            relevantAdapter.notifyDataSetChanged();
        });
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditorActivity.class);
            editorLauncher.launch(intent);
        });
        mapButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        });
        notesList.setOnItemClickListener((parent, view, position, id) -> {
            Note note = currentNotes.get(position);
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra(EditorActivity.EXTRA_NOTE_ID, note.getId());
            editorLauncher.launch(intent);
        });
        relevantNotesList.setOnItemClickListener((parent, view, position, id) -> {
            Note note = currentRelevantNotes.get(position);
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra(EditorActivity.EXTRA_NOTE_ID, note.getId());
            editorLauncher.launch(intent);
        });
        handleIntent(getIntent());
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(getIntent());
    }
    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra(NotificationHelper.NOTIFICATION_NOTE_ID)) {
            int noteId = intent.getIntExtra(NotificationHelper.NOTIFICATION_NOTE_ID, -1);
            if (noteId != -1) {
                intent.removeExtra(NotificationHelper.NOTIFICATION_NOTE_ID);
                Intent editorIntent = new Intent(this, EditorActivity.class);
                editorIntent.putExtra(EditorActivity.EXTRA_NOTE_ID, noteId);
                editorLauncher.launch(editorIntent);
            }
        }
    }
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
    private void askLocationPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            requestLocationPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }
    private void scheduleTimeReminder(Note note) {
        if (note.getId() == 0 || !"time".equals(note.getReminderType()) || note.getReminderTime() <= System.currentTimeMillis()) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_NOTE_ID, note.getId());
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_NOTE_TITLE, note.getTitle());
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_NOTE_BODY, note.getBody());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, note.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, note.getReminderTime(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, note.getReminderTime(), pendingIntent);
        }
        Toast.makeText(this, "Reminder set for " + note.getTitle(), Toast.LENGTH_SHORT).show();
    }
    private void cancelTimeReminder(Note note) {
        if (note.getId() == 0) return;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, note.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }
}