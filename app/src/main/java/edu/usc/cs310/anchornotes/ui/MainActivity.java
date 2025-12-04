package edu.usc.cs310.anchornotes.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    private NoteAdapter adapter;
    private List<Note> allNotes = new ArrayList<>();
    private List<Note> filteredNotes = new ArrayList<>();
    private Integer currentFilterTagId = null;
    private String currentFilterTagName = null;
    private String currentSearchQuery = "";
    private SearchView searchView;

    private TextView relevantNotesHeader;
    private ListView relevantNotesList;
    private NoteAdapter relevantAdapter;
    private List<Note> currentRelevantNotes = new ArrayList<>();

    private TextView pinnedNotesHeader;
    private ListView pinnedNotesList;
    private NoteAdapter pinnedAdapter;
    private List<Note> currentPinnedNotes = new ArrayList<>();

    private Long currentStartDate = null;
    private Long currentEndDate = null;
    private boolean currentHasPhoto = false;
    private boolean currentHasVoice = false;
    private boolean currentHasLocation = false;
    private String currentFilterLocationName = null;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                askLocationPermissions();
            });

    private final ActivityResultLauncher<String[]> requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {});

    private final ActivityResultLauncher<Intent> editorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int noteId = data.getIntExtra(EditorActivity.EXTRA_NOTE_ID, -1);
                    ArrayList<String> templateTagNames = data.getStringArrayListExtra(EditorActivity.EXTRA_TEMPLATE_TAGS);
                    if (noteId != -1) {
                        viewModel.getById(noteId, noteToUpdate -> {
                            if (noteToUpdate != null) {
                                cancelTimeReminder(noteToUpdate);
                                geofenceHelper.removeGeofence(noteToUpdate);
                                updateNoteFromIntent(noteToUpdate, data);
                                viewModel.update(noteToUpdate);
                                scheduleReminders(noteToUpdate);
                                processTemplateTags(noteToUpdate.getId(), templateTagNames);
                            }
                        });
                    } else {
                        Note newNote = new Note("", "");
                        updateNoteFromIntent(newNote, data);
                        viewModel.insert(newNote, newId -> {
                            newNote.setId(newId.intValue());
                            scheduleReminders(newNote);
                            processTemplateTags(newNote.getId(), templateTagNames);
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
        note.setPageColor(data.getStringExtra(EditorActivity.EXTRA_NOTE_PAGE_COLOR));
        note.setTemplateId(data.getIntExtra(EditorActivity.EXTRA_TEMPLATE_ID, 0));
        String voiceUri = data.getStringExtra(EditorActivity.EXTRA_NOTE_VOICE_URI);
        note.setVoiceUri(voiceUri);
        String photoUri = data.getStringExtra(EditorActivity.EXTRA_NOTE_PHOTO_URI);
        note.setPhotoUri(photoUri);
    }

    private void scheduleReminders(Note note) {
        if ("time".equals(note.getReminderType())) {
            scheduleTimeReminder(note);
        } else if ("geofence".equals(note.getReminderType())) {
            geofenceHelper.addGeofence(note);
        }
    }

    private class NoteAdapter extends ArrayAdapter<Note> {
        private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());

        public NoteAdapter(@NonNull Context context, List<Note> notes) {
            super(context, R.layout.list_item_note, notes);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.list_item_note, parent, false);
            }
            TextView titleTextView = view.findViewById(R.id.noteTitleTextView);
            TextView timestampTextView = view.findViewById(R.id.noteTimestampTextView);
            Note currentNote = getItem(position);
            if (currentNote != null) {
                titleTextView.setText(currentNote.getDisplayTitle());
                Date updatedDate = new Date(currentNote.getUpdatedAtEpochMs());
                timestampTextView.setText(fullDateFormat.format(updatedDate));
            }
            return view;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (searchView != null) {
            CharSequence query = searchView.getQuery();
            String queryText = (query != null) ? query.toString() : "";
            currentSearchQuery = queryText;
            applyCurrentFilter();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchView = findViewById(R.id.searchView);
        ImageButton filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> showFilterDialog());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                applyCurrentFilter();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                applyCurrentFilter();
                return true;
            }
        });

        geofenceHelper = new GeofenceHelper(this);
        askNotificationPermission();

        ListView notesList = findViewById(R.id.notesList);
        FloatingActionButton addButton = findViewById(R.id.addButton);
        Button mapButton = findViewById(R.id.mapButton);
        Button templatesButton = findViewById(R.id.templatesButton);
        Button tagsButton = findViewById(R.id.tagsButton);
        relevantNotesHeader = findViewById(R.id.relevantNotesHeader);
        relevantNotesList = findViewById(R.id.relevantNotesList);
        pinnedNotesHeader = findViewById(R.id.pinnedNotesHeader);
        pinnedNotesList = findViewById(R.id.pinnedNotesList);

        adapter = new NoteAdapter(this, new ArrayList<>());
        notesList.setAdapter(adapter);
        relevantAdapter = new NoteAdapter(this, new ArrayList<>());
        relevantNotesList.setAdapter(relevantAdapter);
        pinnedAdapter = new NoteAdapter(this, new ArrayList<>());
        pinnedNotesList.setAdapter(pinnedAdapter);

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        viewModel.getNotesLiveData().observe(this, notes -> {
            allNotes = (notes != null) ? notes : new ArrayList<>();
            applyCurrentFilter();
        });

        viewModel.getRelevantNotesLiveData().observe(this, relevantNotes -> {
            currentRelevantNotes = (relevantNotes != null) ? relevantNotes : new ArrayList<>();
            relevantAdapter.clear();
            relevantAdapter.addAll(currentRelevantNotes);
            relevantNotesHeader.setVisibility(currentRelevantNotes.isEmpty() ? View.GONE : View.VISIBLE);
            relevantNotesList.setVisibility(currentRelevantNotes.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.getPinnedNotesLiveData().observe(this, pinnedNotes -> {
            currentPinnedNotes = (pinnedNotes != null) ? pinnedNotes : new ArrayList<>();
            pinnedAdapter.clear();
            pinnedAdapter.addAll(currentPinnedNotes);
            pinnedNotesHeader.setVisibility(currentPinnedNotes.isEmpty() ? View.GONE : View.VISIBLE);
            pinnedNotesList.setVisibility(currentPinnedNotes.isEmpty() ? View.GONE : View.VISIBLE);
        });

        addButton.setOnClickListener(v -> editorLauncher.launch(new Intent(this, EditorActivity.class)));
        mapButton.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        templatesButton.setOnClickListener(v -> startActivity(new Intent(this, TemplatesActivity.class)));
        tagsButton.setOnClickListener(v -> showTagManagementDialog());
        notesList.setOnItemClickListener((parent, view, position, id) -> openNote(filteredNotes.get(position)));
        relevantNotesList.setOnItemClickListener((parent, view, position, id) -> openNote(currentRelevantNotes.get(position)));
        pinnedNotesList.setOnItemClickListener((parent, view, position, id) -> openNote(currentPinnedNotes.get(position)));
        notesList.setOnItemLongClickListener((parent, view, position, id) -> {
            showNoteActions(filteredNotes.get(position));
            return true;
        });
        pinnedNotesList.setOnItemLongClickListener((parent, view, position, id) -> {
            showNoteActions(currentPinnedNotes.get(position));
            return true;
        });
        relevantNotesList.setOnItemLongClickListener((parent, view, position, id) -> {
            showNoteActions(currentRelevantNotes.get(position));
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
        final TagRowAdapter tagsAdapter = new TagRowAdapter(this, viewModel);
        tagsListView.setAdapter(tagsAdapter);

        viewModel.getAllTags().observe(this, tags -> {
            tagsAdapter.setTags(tags);
        });

        AlertDialog dialog = builder.setPositiveButton("Close", (d, w) -> d.dismiss()).create();

        createTagButton.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateTagDialog();
        });

        tagsListView.setOnItemClickListener((parent, view, position, id) -> {
            Tag selectedTag = tagsAdapter.getTagAt(position);
            if (selectedTag != null) {
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

    private static class TagRowAdapter extends ArrayAdapter<String> {
        private final LayoutInflater inflater;
        private final NoteViewModel viewModel;
        private List<Tag> tags = new ArrayList<>();

        TagRowAdapter(Context context, NoteViewModel viewModel) {
            super(context, 0, new ArrayList<>());
            this.inflater = LayoutInflater.from(context);
            this.viewModel = viewModel;
        }

        void setTags(List<Tag> tags) {
            this.tags = (tags != null) ? new ArrayList<>(tags) : new ArrayList<>();
            clear();
            for (Tag tag : this.tags) {
                add(tag.getName());
            }
            notifyDataSetChanged();
        }

        Tag getTagAt(int position) {
            return (tags != null && position >= 0 && position < tags.size()) ? tags.get(position) : null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_tag_with_delete, parent, false);
            }

            TextView nameView = view.findViewById(R.id.tagNameTextView);
            ImageButton deleteButton = view.findViewById(R.id.deleteTagButton);

            Tag tag = getTagAt(position);
            if (tag != null) {
                nameView.setText(tag.getName());
                deleteButton.setOnClickListener(v -> confirmDelete(tag));
            } else {
                nameView.setText("");
                deleteButton.setOnClickListener(null);
            }

            return view;
        }

        private void confirmDelete(Tag tag) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Tag")
                    .setMessage("Delete tag '" + tag.getName() + "'? This will remove it from all notes.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        viewModel.deleteTag(tag);
                        Toast.makeText(getContext(), "Tag deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void showCreateTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Tag");
        final EditText input = new EditText(this);
        input.setHint("Enter tag name");

        // FIX: Use setView(view, l, t, r, b) to ensure correct margins.
        // 20dp margin + ~4dp internal EditText padding = 24dp (Aligns with Title)
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        builder.setView(input, margin, 0, margin, 0);

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
        List<Note> tempFiltered = new ArrayList<>(allNotes);

        if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
            String lowerCaseQuery = currentSearchQuery.toLowerCase();
            tempFiltered = tempFiltered.stream()
                    .filter(note -> (note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerCaseQuery)) ||
                            (note.getBody() != null && note.getBody().toLowerCase().contains(lowerCaseQuery)))
                    .collect(Collectors.toList());
        }

        if (currentFilterTagId != null) {
            // This is a placeholder for actual tag filtering logic
            // For now, we will just pass the current search-filtered list
        }

        filteredNotes = tempFiltered;
        updateNotesList();
    }

    private void updateNotesList() {
        adapter.clear();
        adapter.addAll(filteredNotes);

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
            } else {
                askLocationPermissions();
            }
        } else {
            askLocationPermissions();
        }
    }

    private void askLocationPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
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

    private void processTemplateTags(int noteId, ArrayList<String> templateTagNames) {
        if (noteId == 0 || templateTagNames == null || templateTagNames.isEmpty()) {
            return;
        }
        viewModel.ensureTagsForNames(templateTagNames, tags -> {
            if (tags == null) {
                return;
            }
            for (Tag tag : tags) {
                viewModel.assignTagToNote(noteId, tag.getId());
            }
        });
    }

    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);


        android.widget.CheckBox photoCheck = dialogView.findViewById(R.id.checkboxPhoto);
        android.widget.CheckBox voiceCheck = dialogView.findViewById(R.id.checkboxVoice);
        android.widget.CheckBox locationCheck = dialogView.findViewById(R.id.checkboxLocation);
        android.widget.DatePicker startPicker = dialogView.findViewById(R.id.startDatePicker);
        android.widget.DatePicker endPicker = dialogView.findViewById(R.id.endDatePicker);
        android.widget.Spinner tagSpinner = dialogView.findViewById(R.id.tagSpinner);
        android.widget.Spinner locationSpinner = dialogView.findViewById(R.id.locationSpinner);



        // Populate the tag spinner
        viewModel.getAllTags().observe(this, tags -> {
            if (tags != null) {
                List<String> tagNames = new ArrayList<>();
                tagNames.add("All Tags"); // Default
                for (edu.usc.cs310.anchornotes.model.Tag t : tags) {
                    tagNames.add(t.getName());
                }
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, tagNames);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                tagSpinner.setAdapter(spinnerAdapter);


                // Preselect current tag if applicable
                if (currentFilterTagName != null) {
                    int pos = tagNames.indexOf(currentFilterTagName);
                    if (pos >= 0) tagSpinner.setSelection(pos);
                }
            }
        });

        // ---- Populate location spinner from existing notes ----
        List<String> locationNames = new ArrayList<>();
        locationNames.add("All Locations"); // default option

        // Use allNotes (already kept up to date in MainActivity)
        if (allNotes != null) {
            // Use a set so we don't get duplicates
            java.util.Set<String> unique = new java.util.LinkedHashSet<>();

            for (Note n : allNotes) {
                if (n.getLocationName() != null && !n.getLocationName().isEmpty()) {
                    unique.add(n.getLocationName());
                }
                // Optional: also include reminder locations if you want
                if (n.getReminderLocationName() != null && !n.getReminderLocationName().isEmpty()) {
                    unique.add(n.getReminderLocationName());
                }
            }

            locationNames.addAll(unique);
        }

        ArrayAdapter<String> locAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                locationNames
        );
        locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locAdapter);

        // Preselect current location filter, if any
        if (currentFilterLocationName != null) {
            int pos = locationNames.indexOf(currentFilterLocationName);
            if (pos >= 0) {
                locationSpinner.setSelection(pos);
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Filter Notes")
                .setView(dialogView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    currentHasPhoto = photoCheck.isChecked();
                    currentHasVoice = voiceCheck.isChecked();
                    currentHasLocation = locationCheck.isChecked();
                    currentStartDate = getEpochFromDatePicker(startPicker);
                    currentEndDate = getEpochFromDatePicker(endPicker);


                    // Tag selection
                    String selectedTagName = tagSpinner.getSelectedItem().toString();
                    if ("All Tags".equals(selectedTagName)) {
                        currentFilterTagId = null;
                        currentFilterTagName = null;
                    } else {
                        List<edu.usc.cs310.anchornotes.model.Tag> tags = viewModel.getAllTags().getValue();
                        if (tags != null) {
                            for (edu.usc.cs310.anchornotes.model.Tag t : tags) {
                                if (t.getName().equals(selectedTagName)) {
                                    currentFilterTagId = t.getId();
                                    currentFilterTagName = t.getName();
                                    break;
                                }
                            }
                        }
                    }

                    // ---- Location selection ----
                    String selectedLocationName = locationSpinner.getSelectedItem().toString();
                    if ("All Locations".equals(selectedLocationName)) {
                        currentFilterLocationName = null;
                    } else {
                        currentFilterLocationName = selectedLocationName;
                    }



                    applyAdvancedFilter();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyAdvancedFilter() {
        String query = ""; // Empty string means no search filter (you can tie this to SearchView text)
        Long inclusiveStart = currentStartDate;
        Long inclusiveEnd = (currentEndDate != null) ? currentEndDate + 24L * 60L * 60L * 1000L - 1L : null;


        LiveData<List<Note>> filteredLiveData = viewModel.filterAndSearchNotes(
                query.isEmpty() ? null : query,
                currentFilterTagId,
                inclusiveStart,
                inclusiveEnd,
                currentHasPhoto,
                currentHasVoice,
                currentHasLocation
        );


        filteredLiveData.observe(this, notes -> {
            filteredNotes = (notes != null) ? notes : new ArrayList<>();

            // Extra pass: filter by specific location name, if selected
            if (currentFilterLocationName != null && !currentFilterLocationName.isEmpty()) {
                List<Note> locationFiltered = new ArrayList<>();
                for (Note n : filteredNotes) {
                    String noteLoc = n.getLocationName();
                    String reminderLoc = n.getReminderLocationName();

                    if ((noteLoc != null && noteLoc.equals(currentFilterLocationName)) ||
                            (reminderLoc != null && reminderLoc.equals(currentFilterLocationName))) {
                        locationFiltered.add(n);
                    }
                }
                filteredNotes = locationFiltered;
            }

            updateNotesList();

            String filterSummary = "Applied filters: ";
            if (currentFilterTagName != null) filterSummary += "Tag = " + currentFilterTagName + "; ";
            if (currentHasPhoto) filterSummary += "Has Photo; ";
            if (currentHasVoice) filterSummary += "Has Voice; ";
            if (currentHasLocation) filterSummary += "Has Location; ";
            if (currentFilterLocationName != null) filterSummary += "Location = " + currentFilterLocationName + "; ";
            if (currentStartDate != null || currentEndDate != null) filterSummary += "Date range set; ";

            Toast.makeText(this, filterSummary, Toast.LENGTH_SHORT).show();
        });
    }


    private Long getEpochFromDatePicker(android.widget.DatePicker picker) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(picker.getYear(), picker.getMonth(), picker.getDayOfMonth(), 0, 0, 0);
        return cal.getTimeInMillis();
    }

    private void showNoteActions(Note note) {
        if (note == null) return;
        String[] options = new String[] {
                note.isPinned() ? "Unpin" : "Pin",
                "Delete",
                "Cancel"
        };
        new AlertDialog.Builder(this)
                .setTitle(note.getDisplayTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            togglePinStatus(note);
                            break;
                        case 1:
                            new AlertDialog.Builder(this)
                                    .setTitle("Delete note")
                                    .setMessage("Delete \"" + note.getDisplayTitle() + "\"?")
                                    .setPositiveButton("Delete", (d, w) -> {
                                        cancelTimeReminder(note);
                                        geofenceHelper.removeGeofence(note);
                                        viewModel.deleteNote(note);
                                        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            break;
                        default:
                            dialog.dismiss();
                    }
                })
                .show();
    }
}