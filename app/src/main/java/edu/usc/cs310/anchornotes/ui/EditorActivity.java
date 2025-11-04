package edu.usc.cs310.anchornotes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import edu.usc.cs310.anchornotes.BuildConfig;
import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.Tag;
import edu.usc.cs310.anchornotes.viewmodel.NoteViewModel;

public class EditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "edu.usc.cs310.anchornotes.EXTRA_NOTE_ID";
    private static final float GEOFENCE_RADIUS_METERS = 50;

    private EditText titleEditText, bodyEditText;
    private Button saveButton, cancelButton;
    private ImageButton reminderButton, tagsButton, pinButton;
    private TextView reminderStatusTextView;
    private LinearLayout tagsContainer;

    private NoteViewModel viewModel;
    private Note currentNote;
    private boolean isEditing = false;
    private List<Tag> allTags;

    private final ActivityResultLauncher<Intent> placePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place.getLatLng() != null && place.getName() != null) {
                        if (currentNote == null) {
                            currentNote = new Note("", "");
                        }
                        currentNote.setReminderType("geofence");
                        currentNote.setLatitude(place.getLatLng().latitude);
                        currentNote.setLongitude(place.getLatLng().longitude);
                        currentNote.setRadius(GEOFENCE_RADIUS_METERS);
                        currentNote.setLocationName(place.getName());
                        currentNote.setReminderTime(0);
                        updateReminderStatusUI();
                        Toast.makeText(this, "Location set to " + place.getName(), Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                    Toast.makeText(this, "Error picking location.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        initializePlacesApi();

        titleEditText = findViewById(R.id.titleEditText);
        bodyEditText  = findViewById(R.id.bodyEditText);
        saveButton    = findViewById(R.id.saveButton);
        cancelButton  = findViewById(R.id.cancelButton);
        reminderButton = findViewById(R.id.reminderButton);
        tagsButton = findViewById(R.id.tagsButton);
        pinButton = findViewById(R.id.pinButton);
        reminderStatusTextView = findViewById(R.id.reminderStatusTextView);
        tagsContainer = findViewById(R.id.tagsContainer);

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_NOTE_ID)) {
            int noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1);
            if (noteId != -1) {
                viewModel.getById(noteId, note -> {
                    if (note != null) {
                        currentNote = note;
                        isEditing = true;
                        populateUI();
                    }
                });
            }
        }

        reminderButton.setOnClickListener(v -> showReminderDialog());
        tagsButton.setOnClickListener(v -> showTagAssignmentDialog());
        pinButton.setOnClickListener(v -> togglePinStatus());

        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String body  = bodyEditText.getText().toString().trim();

            Intent data = new Intent();
            data.putExtra("note_title", title);
            data.putExtra("note_body", body);

            if (isEditing && currentNote != null) {
                data.putExtra(EXTRA_NOTE_ID, currentNote.getId());
            }

            if (currentNote != null) {
                data.putExtra("note_reminder_type", currentNote.getReminderType());
                if ("time".equals(currentNote.getReminderType())) {
                    data.putExtra("note_reminder_time", currentNote.getReminderTime());
                } else if ("geofence".equals(currentNote.getReminderType())) {
                    data.putExtra("note_latitude", currentNote.getLatitude());
                    data.putExtra("note_longitude", currentNote.getLongitude());
                    data.putExtra("note_radius", currentNote.getRadius());
                    data.putExtra("note_location_name", currentNote.getLocationName());
                }
            }

            setResult(RESULT_OK, data);
            finish();
        });

        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Load all tags
        viewModel.getAllTags().observe(this, tags -> {
            allTags = tags;
            updateTagCheckboxes();
        });
    }

    private void initializePlacesApi() {
        if (!Places.isInitialized()) {
            String apiKey = BuildConfig.MAPS_API_KEY;
            if (TextUtils.isEmpty(apiKey)) {
                Toast.makeText(this, "Error: Maps API key not found.", Toast.LENGTH_LONG).show();
                return;
            }
            Places.initialize(getApplicationContext(), apiKey);
        }
    }

    private void populateUI() {
        if (currentNote == null) return;
        titleEditText.setText(currentNote.getTitle());
        bodyEditText.setText(currentNote.getBody());
        updateReminderStatusUI();
        updateTagCheckboxes();
        updatePinButtonIcon();
    }

    private void updateReminderStatusUI() {
        if (currentNote == null || currentNote.getReminderType() == null) {
            reminderStatusTextView.setVisibility(View.GONE);
            return;
        }

        String statusText = "";
        String reminderType = currentNote.getReminderType();

        if ("time".equals(reminderType) && currentNote.getReminderTime() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
            statusText = "Time Reminder: " + sdf.format(currentNote.getReminderTime());
        } else if ("geofence".equals(reminderType) && !TextUtils.isEmpty(currentNote.getLocationName())) {
            statusText = String.format(Locale.getDefault(), "Location Reminder: %s (%.4f, %.4f)",
                    currentNote.getLocationName(), currentNote.getLatitude(), currentNote.getLongitude());
        }

        if (!statusText.isEmpty()) {
            reminderStatusTextView.setText(statusText);
            reminderStatusTextView.setVisibility(View.VISIBLE);
        } else {
            reminderStatusTextView.setVisibility(View.GONE);
        }
    }

    private void updateTagCheckboxes() {
        tagsContainer.removeAllViews();

        if (allTags == null || allTags.isEmpty()) {
            TextView noTagsText = new TextView(this);
            noTagsText.setText("No tags available. Create tags from the main screen.");
            noTagsText.setPadding(0, 16, 0, 16);
            tagsContainer.addView(noTagsText);
            return;
        }

        if (currentNote == null) return;

        // Get current note's tags
        viewModel.getTagsForNote(currentNote.getId()).observe(this, noteTags -> {
            tagsContainer.removeAllViews();

            for (Tag tag : allTags) {
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(tag.getName());

                // Check if this tag is assigned to the current note
                boolean isAssigned = false;
                for (Tag noteTag : noteTags) {
                    if (noteTag.getId() == tag.getId()) {
                        isAssigned = true;
                        break;
                    }
                }
                checkBox.setChecked(isAssigned);

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        viewModel.assignTagToNote(currentNote.getId(), tag.getId());
                        Toast.makeText(EditorActivity.this, "Added tag: " + tag.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        viewModel.removeTagFromNote(currentNote.getId(), tag.getId());
                        Toast.makeText(EditorActivity.this, "Removed tag: " + tag.getName(), Toast.LENGTH_SHORT).show();
                    }
                });

                tagsContainer.addView(checkBox);
            }
        });
    }

    private void showTagAssignmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Assign Tags to Note");

        // Create a scrollable container for the tags
        ScrollView scrollView = new ScrollView(this);
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(16, 16, 16, 16);
        scrollView.addView(dialogLayout);

        if (allTags == null || allTags.isEmpty()) {
            TextView noTagsText = new TextView(this);
            noTagsText.setText("No tags available. Create tags from the main screen.");
            noTagsText.setPadding(0, 16, 0, 16);
            dialogLayout.addView(noTagsText);
        } else {
            viewModel.getTagsForNote(currentNote.getId()).observe(this, noteTags -> {
                dialogLayout.removeAllViews();

                for (Tag tag : allTags) {
                    CheckBox checkBox = new CheckBox(this);
                    checkBox.setText(tag.getName());

                    boolean isAssigned = false;
                    for (Tag noteTag : noteTags) {
                        if (noteTag.getId() == tag.getId()) {
                            isAssigned = true;
                            break;
                        }
                    }
                    checkBox.setChecked(isAssigned);

                    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            viewModel.assignTagToNote(currentNote.getId(), tag.getId());
                            Toast.makeText(EditorActivity.this, "Added tag: " + tag.getName(), Toast.LENGTH_SHORT).show();
                        } else {
                            viewModel.removeTagFromNote(currentNote.getId(), tag.getId());
                            Toast.makeText(EditorActivity.this, "Removed tag: " + tag.getName(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    dialogLayout.addView(checkBox);
                }
            });
        }

        builder.setView(scrollView);
        builder.setPositiveButton("Done", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void togglePinStatus() {
        if (currentNote == null) return;

        currentNote.setPinned(!currentNote.isPinned());
        currentNote.setUpdatedAtEpochMs(System.currentTimeMillis());

        // Update the pin button icon
        updatePinButtonIcon();

        // Show toast message
        String message = currentNote.isPinned() ? "Note pinned" : "Note unpinned";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updatePinButtonIcon() {
        if (currentNote == null) return;

        if (currentNote.isPinned()) {
            pinButton.setImageResource(android.R.drawable.star_big_on);
        } else {
            pinButton.setImageResource(android.R.drawable.star_big_off);
        }
    }

    private void showReminderDialog() {
        final CharSequence[] options;
        if (currentNote == null || currentNote.getReminderType() == null) {
            options = new CharSequence[]{"Set Time Reminder", "Set Location Reminder"};
        } else {
            options = new CharSequence[]{"Set Time Reminder", "Set Location Reminder", "Clear Reminder"};
        }

        new AlertDialog.Builder(this)
                .setTitle("Reminder Options")
                .setItems(options, (dialog, which) -> {
                    if (currentNote == null) {
                        currentNote = new Note("", "");
                    }
                    switch (options[which].toString()) {
                        case "Set Time Reminder": setTimeReminder(); break;
                        case "Set Location Reminder": setLocationReminder(); break;
                        case "Clear Reminder": clearReminder(); break;
                    }
                }).show();
    }

    private void setTimeReminder() {
        final Calendar calendar = Calendar.getInstance();
        if (currentNote != null && "time".equals(currentNote.getReminderType()) && currentNote.getReminderTime() > 0) {
            calendar.setTimeInMillis(currentNote.getReminderTime());
        }

        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            TimePickerDialog.OnTimeSetListener timeSetListener = (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    Toast.makeText(this, "Cannot set a reminder in the past.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentNote == null) {
                    currentNote = new Note("", "");
                }
                currentNote.setReminderType("time");
                currentNote.setReminderTime(calendar.getTimeInMillis());
                currentNote.setLocationName(null);
                updateReminderStatusUI();
            };

            new TimePickerDialog(EditorActivity.this, timeSetListener,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE), false).show();
        };

        new DatePickerDialog(EditorActivity.this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setLocationReminder() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
        placePickerLauncher.launch(intent);
    }

    private void clearReminder() {
        if (currentNote == null) return;
        currentNote.setReminderType(null);
        currentNote.setReminderTime(0);
        currentNote.setLatitude(0);
        currentNote.setLongitude(0);
        currentNote.setRadius(0);
        currentNote.setLocationName(null);
        updateReminderStatusUI();
        Toast.makeText(this, "Reminder cleared.", Toast.LENGTH_SHORT).show();
    }
}