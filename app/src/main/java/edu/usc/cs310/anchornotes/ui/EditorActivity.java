package edu.usc.cs310.anchornotes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.viewmodel.NoteViewModel;

public class EditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "edu.usc.cs310.anchornotes.EXTRA_NOTE_ID";
    private static final float GEOFENCE_RADIUS_METERS = 100;

    private EditText titleEditText, bodyEditText;
    private Button saveButton, cancelButton;
    private ImageButton reminderButton;
    private TextView reminderStatusTextView;

    private NoteViewModel viewModel;
    private Note currentNote;

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
                    Log.e("EditorActivity", "Places Autocomplete Error: " + Autocomplete.getStatusFromIntent(result.getData()));
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
        reminderStatusTextView = findViewById(R.id.reminderStatusTextView);

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_NOTE_ID)) {
            int noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1);
            if (noteId != -1) {
                viewModel.getById(noteId, note -> {
                    if (note != null) {
                        currentNote = note;
                        populateUI();
                    }
                });
            }
        }

        reminderButton.setOnClickListener(v -> showReminderDialog());

        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String body  = bodyEditText.getText().toString().trim();

            Intent data = new Intent();
            data.putExtra("note_title", title);
            data.putExtra("note_body", body);

            if (currentNote != null) {
                data.putExtra(EXTRA_NOTE_ID, currentNote.getId());
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
    }


    private void initializePlacesApi() {
        if (!Places.isInitialized()) {
            // Use the key from the generated BuildConfig class
            String apiKey = edu.usc.cs310.anchornotes.BuildConfig.MAPS_API_KEY;

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
            statusText = "Reminder: " + sdf.format(currentNote.getReminderTime());
        } else if ("geofence".equals(reminderType) && !TextUtils.isEmpty(currentNote.getLocationName())) {
            // ================================================================= //
            // === THIS IS THE ONLY LINE THAT HAS CHANGED FOR YOUR REQUEST ===== //
            // ================================================================= //
            statusText = String.format(Locale.getDefault(), "Reminder: %s (%.4f, %.4f)",
                    currentNote.getLocationName(), currentNote.getLatitude(), currentNote.getLongitude());
        }

        if (!statusText.isEmpty()) {
            reminderStatusTextView.setText(statusText);
            reminderStatusTextView.setVisibility(View.VISIBLE);
        } else {
            reminderStatusTextView.setVisibility(View.GONE);
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