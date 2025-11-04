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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.broadcast.ReminderBroadcastReceiver;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.Tag;
import edu.usc.cs310.anchornotes.util.GeofenceHelper;
import edu.usc.cs310.anchornotes.util.NotificationHelper;
import edu.usc.cs310.anchornotes.viewmodel.NoteViewModel;

public class MainActivity extends AppCompatActivity {

    private NoteViewModel viewModel;
    private GeofenceHelper geofenceHelper;

    // UI for All Notes
    private ArrayAdapter<String> adapter;
    private ArrayList<String> noteTitles;
    private List<Note> currentNotes;
    private List<Note> allNotes = new ArrayList<>();
    private List<Note> filteredNotes = new ArrayList<>();
    private Integer currentFilterTagId = null;
    private String currentFilterTagName = null;

    // UI for Relevant Notes
    private TextView relevantNotesHeader;
    private ListView relevantNotesList;
    private ArrayAdapter<String> relevantAdapter;
    private ArrayList<String> relevantNoteTitles;
    private List<Note> currentRelevantNotes;

    // UI for Pinned Notes
    private TextView pinnedNotesHeader;
    private ListView pinnedNotesList;
    private ArrayAdapter<String> pinnedAdapter;
    private ArrayList<String> pinnedNoteTitles;
    private List<Note> currentPinnedNotes;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {});
    private final ActivityResultLauncher<String[]> requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {});

    private final ActivityResultLauncher<Intent> editorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String title = data.getStringExtra("note_title");
                    String body  = data.getStringExtra("note_body");
                    int noteId   = data.getIntExtra(EditorActivity.EXTRA_NOTE_ID, -1);
                    String reminderType = data.getStringExtra("note_reminder_type");

                    if (noteId != -1) {
                        // EDIT EXISTING NOTE
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
                        // CREATE NEW NOTE
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

                        viewModel.insert(newNote, newId -> {
                            newNote.setId(newId.intValue());
                            if ("time".equals(newNote.getReminderType())) {
                                scheduleTimeReminder(newNote);
                            } else if ("geofence".equals(newNote.getReminderType())) {
                                geofenceHelper.addGeofence(newNote);
                            }
                        });
                    }
                }
            });

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
        Button tagsButton = findViewById(R.id.tagsButton);
        relevantNotesHeader = findViewById(R.id.relevantNotesHeader);
        relevantNotesList = findViewById(R.id.relevantNotesList);
        pinnedNotesHeader = findViewById(R.id.pinnedNotesHeader);
        pinnedNotesList = findViewById(R.id.pinnedNotesList);

        noteTitles = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, noteTitles);
        notesList.setAdapter(adapter);

        relevantNoteTitles = new ArrayList<>();
        relevantAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, relevantNoteTitles);
        relevantNotesList.setAdapter(relevantAdapter);

        pinnedNoteTitles = new ArrayList<>();
        pinnedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pinnedNoteTitles);
        pinnedNotesList.setAdapter(pinnedAdapter);

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        viewModel.getNotesLiveData().observe(this, notes -> {
            allNotes = notes;
            applyCurrentFilter();
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

        // Observe pinned notes
        viewModel.getPinnedNotesLiveData().observe(this, pinnedNotes -> {
            currentPinnedNotes = pinnedNotes;
            pinnedNoteTitles.clear();
            if (pinnedNotes == null || pinnedNotes.isEmpty()) {
                pinnedNotesHeader.setVisibility(View.GONE);
                pinnedNotesList.setVisibility(View.GONE);
            } else {
                pinnedNotesHeader.setVisibility(View.VISIBLE);
                pinnedNotesList.setVisibility(View.VISIBLE);
                for (Note note : pinnedNotes) {
                    pinnedNoteTitles.add(note.getTitle().isEmpty() ? "(Untitled)" : note.getTitle());
                }
            }
            pinnedAdapter.notifyDataSetChanged();
        });

        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditorActivity.class);
            editorLauncher.launch(intent);
        });

        mapButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        });

        tagsButton.setOnClickListener(v -> showTagManagementDialog());

        notesList.setOnItemClickListener((parent, view, position, id) -> {
            Note note = filteredNotes.get(position);
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

        pinnedNotesList.setOnItemClickListener((parent, view, position, id) -> {
            Note note = currentPinnedNotes.get(position);
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra(EditorActivity.EXTRA_NOTE_ID, note.getId());
            editorLauncher.launch(intent);
        });

        // Add long press to toggle pin status
        notesList.setOnItemLongClickListener((parent, view, position, id) -> {
            Note note = filteredNotes.get(position);
            togglePinStatus(note);
            return true;
        });

        pinnedNotesList.setOnItemLongClickListener((parent, view, position, id) -> {
            Note note = currentPinnedNotes.get(position);
            togglePinStatus(note);
            return true;
        });

        relevantNotesList.setOnItemLongClickListener((parent, view, position, id) -> {
            Note note = currentRelevantNotes.get(position);
            togglePinStatus(note);
            return true;
        });

        handleIntent(getIntent());
    }

    private void togglePinStatus(Note note) {
        viewModel.togglePin(note);
        String message = note.isPinned() ? "Note pinned" : "Note unpinned";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showTagManagementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Tags");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_manage_tags, null);
        builder.setView(dialogView);

        ListView tagsListView = dialogView.findViewById(R.id.tagsListView);
        Button createTagButton = dialogView.findViewById(R.id.createTagButton);
        Button clearFilterButton = dialogView.findViewById(R.id.clearFilterButton);

        // Show clear filter button only if a filter is active
        if (currentFilterTagId != null) {
            clearFilterButton.setVisibility(View.VISIBLE);
        } else {
            clearFilterButton.setVisibility(View.GONE);
        }

        // Setup tags list with custom adapter
        ArrayAdapter<String> tagsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                List<Tag> currentTags = viewModel.getAllTags().getValue();
                if (currentTags != null && position < currentTags.size()) {
                    Tag tag = currentTags.get(position);
                    textView.setText(tag.getName());
                }

                return view;
            }
        };

        tagsListView.setAdapter(tagsAdapter);

        // Observe tags and update the list
        viewModel.getAllTags().observe(this, tags -> {
            List<String> tagNames = new ArrayList<>();
            for (Tag tag : tags) {
                tagNames.add(tag.getName());
            }
            tagsAdapter.clear();
            tagsAdapter.addAll(tagNames);
            tagsAdapter.notifyDataSetChanged();
        });

        createTagButton.setOnClickListener(v -> {
            showCreateTagDialog();
        });

        // Handle tag selection for filtering
        tagsListView.setOnItemClickListener((parent, view, position, id) -> {
            List<Tag> currentTags = viewModel.getAllTags().getValue();
            if (currentTags != null && position < currentTags.size()) {
                Tag selectedTag = currentTags.get(position);
                currentFilterTagId = selectedTag.getId();
                currentFilterTagName = selectedTag.getName();
                applyCurrentFilter();
                Toast.makeText(this, "Showing notes with tag: " + selectedTag.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        clearFilterButton.setOnClickListener(v -> {
            currentFilterTagId = null;
            currentFilterTagName = null;
            applyCurrentFilter();
            Toast.makeText(this, "Showing all notes", Toast.LENGTH_SHORT).show();
        });

        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showCreateTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Tag");

        final EditText input = new EditText(this);
        input.setHint("Enter tag name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String tagName = input.getText().toString().trim();
            if (!tagName.isEmpty()) {
                Tag newTag = new Tag(tagName);
                viewModel.insertTag(newTag, newId -> {
                    Toast.makeText(MainActivity.this, "Tag created: " + tagName, Toast.LENGTH_SHORT).show();
                });
            } else {
                Toast.makeText(MainActivity.this, "Tag name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void applyCurrentFilter() {
        if (currentFilterTagId == null) {
            // Show all notes
            filteredNotes = new ArrayList<>(allNotes);
            updateNotesList();
        } else {
            // Filter notes by tag using the ViewModel
            viewModel.getNotesByTag(currentFilterTagId).observe(this, notes -> {
                filteredNotes = notes;
                updateNotesList();
            });
        }
    }

    private void updateNotesList() {
        noteTitles.clear();
        for (Note note : filteredNotes) {
            noteTitles.add(note.getTitle().isEmpty() ? "(Untitled)" : note.getTitle());
        }
        adapter.notifyDataSetChanged();

        // Update the header to show current filter
        TextView allNotesHeader = findViewById(R.id.allNotesHeader);
        if (currentFilterTagName != null) {
            allNotesHeader.setText("Notes tagged: " + currentFilterTagName);
        } else {
            allNotesHeader.setText("All Notes");
        }
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