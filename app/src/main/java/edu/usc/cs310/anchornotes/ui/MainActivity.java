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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
    private ArrayAdapter<String> adapter;
    private List<Note> allNotes = new ArrayList<>();
    private List<Note> filteredNotes = new ArrayList<>();
    private Integer currentFilterTagId = null;
    private String currentFilterTagName = null;
    private TextView relevantNotesHeader;
    private ListView relevantNotesList;
    private ArrayAdapter<String> relevantAdapter;
    private List<Note> currentRelevantNotes = new ArrayList<>();
    private TextView pinnedNotesHeader;
    private ListView pinnedNotesList;
    private ArrayAdapter<String> pinnedAdapter;
    private List<Note> currentPinnedNotes = new ArrayList<>();

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {});
    private final ActivityResultLauncher<String[]> requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {});

    private final ActivityResultLauncher<Intent> editorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int noteId = data.getIntExtra(EditorActivity.EXTRA_NOTE_ID, -1);
                    if (noteId != -1) {
                        viewModel.getById(noteId, noteToUpdate -> {
                            if (noteToUpdate != null) {
                                cancelTimeReminder(noteToUpdate);
                                geofenceHelper.removeGeofence(noteToUpdate);
                                updateNoteFromIntent(noteToUpdate, data);
                                viewModel.update(noteToUpdate);
                                scheduleReminders(noteToUpdate);
                            }
                        });
                    } else {
                        Note newNote = new Note("", "");
                        updateNoteFromIntent(newNote, data);
                        viewModel.insert(newNote, newId -> {
                            newNote.setId(newId.intValue());
                            scheduleReminders(newNote);
                        });
                    }
                }
            });

    private void updateNoteFromIntent(Note note, Intent data) {
        note.setTitle(data.getStringExtra("note_title"));
        note.setBody(data.getStringExtra("note_body"));
        note.setPinned(data.getBooleanExtra("is_pinned", false));
        note.setLatitude(data.getDoubleExtra("note_latitude", 0));
        note.setLongitude(data.getDoubleExtra("note_longitude", 0));
        note.setLocationName(data.getStringExtra("note_location_name"));
        note.setReminderType(data.getStringExtra("note_reminder_type"));
        note.setReminderTime(data.getLongExtra("note_reminder_time", 0));
        note.setRadius(data.getFloatExtra("note_radius", 0));
        note.setReminderLatitude(data.getDoubleExtra("note_reminder_latitude", 0));
        note.setReminderLongitude(data.getDoubleExtra("note_reminder_longitude", 0));
        note.setReminderLocationName(data.getStringExtra("note_reminder_location_name"));
    }

    private void scheduleReminders(Note note) {
        if ("time".equals(note.getReminderType())) {
            scheduleTimeReminder(note);
        } else if ("geofence".equals(note.getReminderType())) {
            geofenceHelper.addGeofence(note);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("AnchorNotes");
        }
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
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        notesList.setAdapter(adapter);
        relevantAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        relevantNotesList.setAdapter(relevantAdapter);
        pinnedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        pinnedNotesList.setAdapter(pinnedAdapter);
        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        viewModel.getNotesLiveData().observe(this, notes -> {
            allNotes = (notes != null) ? notes : new ArrayList<>();
            applyCurrentFilter();
        });

        viewModel.getRelevantNotesLiveData().observe(this, relevantNotes -> {
            currentRelevantNotes = (relevantNotes != null) ? relevantNotes : new ArrayList<>();
            List<String> titles = currentRelevantNotes.stream().map(Note::getDisplayTitle).collect(Collectors.toList());
            relevantAdapter.clear();
            relevantAdapter.addAll(titles);
            relevantNotesHeader.setVisibility(currentRelevantNotes.isEmpty() ? View.GONE : View.VISIBLE);
            relevantNotesList.setVisibility(currentRelevantNotes.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.getPinnedNotesLiveData().observe(this, pinnedNotes -> {
            currentPinnedNotes = (pinnedNotes != null) ? pinnedNotes : new ArrayList<>();
            List<String> titles = currentPinnedNotes.stream().map(Note::getDisplayTitle).collect(Collectors.toList());
            pinnedAdapter.clear();
            pinnedAdapter.addAll(titles);
            pinnedNotesHeader.setVisibility(currentPinnedNotes.isEmpty() ? View.GONE : View.VISIBLE);
            pinnedNotesList.setVisibility(currentPinnedNotes.isEmpty() ? View.GONE : View.VISIBLE);
        });

        addButton.setOnClickListener(v -> editorLauncher.launch(new Intent(this, EditorActivity.class)));
        mapButton.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        tagsButton.setOnClickListener(v -> showTagManagementDialog());
        notesList.setOnItemClickListener((parent, view, position, id) -> openNote(filteredNotes.get(position)));
        relevantNotesList.setOnItemClickListener((parent, view, position, id) -> openNote(currentRelevantNotes.get(position)));
        pinnedNotesList.setOnItemClickListener((parent, view, position, id) -> openNote(currentPinnedNotes.get(position)));
        notesList.setOnItemLongClickListener((parent, view, position, id) -> {
            togglePinStatus(filteredNotes.get(position));
            return true;
        });
        pinnedNotesList.setOnItemLongClickListener((parent, view, position, id) -> {
            togglePinStatus(currentPinnedNotes.get(position));
            return true;
        });
        relevantNotesList.setOnItemLongClickListener((parent, view, position, id) -> {
            togglePinStatus(currentRelevantNotes.get(position));
            return true;
        });
        handleIntent(getIntent());
    }

    private void openNote(Note note) {
        if (note == null) return;
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra(EditorActivity.EXTRA_NOTE_ID, note.getId());
        editorLauncher.launch(intent);
    }

    private void togglePinStatus(Note note) {
        if (note == null) return;
        viewModel.togglePin(note);
        Toast.makeText(this, note.isPinned() ? "Note unpinned" : "Note pinned", Toast.LENGTH_SHORT).show();
    }

    private void showTagManagementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Tags");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_manage_tags, null);
        builder.setView(dialogView);
        ListView tagsListView = dialogView.findViewById(R.id.tagsListView);
        Button createTagButton = dialogView.findViewById(R.id.createTagButton);
        Button clearFilterButton = dialogView.findViewById(R.id.clearFilterButton);

        clearFilterButton.setVisibility(currentFilterTagId != null ? View.VISIBLE : View.GONE);
        final ArrayAdapter<String> tagsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        tagsListView.setAdapter(tagsAdapter);

        viewModel.getAllTags().observe(this, tags -> {
            if (tags != null) {
                List<String> tagNames = tags.stream().map(Tag::getName).collect(Collectors.toList());
                tagsAdapter.clear();
                tagsAdapter.addAll(tagNames);
            }
        });

        AlertDialog dialog = builder.setPositiveButton("Close", (d, w) -> d.dismiss()).create();

        createTagButton.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateTagDialog();
        });

        tagsListView.setOnItemClickListener((parent, view, position, id) -> {
            List<Tag> currentTags = viewModel.getAllTags().getValue();
            if (currentTags != null && position < currentTags.size()) {
                Tag selectedTag = currentTags.get(position);
                currentFilterTagId = selectedTag.getId();
                currentFilterTagName = selectedTag.getName();
                applyCurrentFilter();
                Toast.makeText(this, "Filtering by tag: " + selectedTag.getName(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        clearFilterButton.setOnClickListener(v -> {
            currentFilterTagId = null;
            currentFilterTagName = null;
            applyCurrentFilter();
            Toast.makeText(this, "Filter cleared", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
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
                viewModel.insertTag(new Tag(tagName), newId -> {
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
            filteredNotes = new ArrayList<>(allNotes);
            updateNotesList();
        } else {
            viewModel.getNotesByTag(currentFilterTagId).observe(this, notes -> {
                filteredNotes = (notes != null) ? notes : new ArrayList<>();
                updateNotesList();
            });
        }
    }

    private void updateNotesList() {
        List<String> noteTitles = new ArrayList<>();
        for (Note note : filteredNotes) {
            noteTitles.add(note.getDisplayTitle());
        }
        adapter.clear();
        adapter.addAll(noteTitles);
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
        if (note.getId() == 0 || !"time".equals(note.getReminderType()) || note.getReminderTime() <= System.currentTimeMillis()) return;
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