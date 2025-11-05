package edu.usc.cs310.anchornotes.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import edu.usc.cs310.anchornotes.BuildConfig;
import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.Tag;
import edu.usc.cs310.anchornotes.viewmodel.NoteViewModel;

public class EditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "edu.usc.cs310.anchornotes.EXTRA_NOTE_ID";
    private static final float GEOFENCE_RADIUS_METERS = 50;
    private static final String TAG = "EditorActivity";

    private EditText titleEditText, bodyEditText;
    private Button saveButton, cancelButton;
    private ImageButton reminderButton, locationButton, tagsButton, pinButton;
    private TextView reminderStatusTextView, locationStatusTextView, tagsDisplayTextView;
    private LinearLayout tagsDisplayLayout;

    private NoteViewModel viewModel;
    private Note currentNote;
    private boolean isEditing = false;
    private FusedLocationProviderClient fusedLocationClient;
    private List<Tag> allTags = new ArrayList<>();

    private final ActivityResultLauncher<Intent> reminderPlacePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place.getLatLng() != null && place.getName() != null) {
                        if (currentNote == null) currentNote = new Note("", "");
                        currentNote.setReminderType("geofence");
                        currentNote.setReminderLatitude(place.getLatLng().latitude);
                        currentNote.setReminderLongitude(place.getLatLng().longitude);
                        currentNote.setReminderLocationName(place.getName());
                        currentNote.setRadius(GEOFENCE_RADIUS_METERS);
                        currentNote.setReminderTime(0);
                        updateReminderStatusUI();
                        Toast.makeText(this, "Location Reminder set to " + place.getName(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> contextPlacePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place.getLatLng() != null && place.getName() != null) {
                        if (currentNote == null) currentNote = new Note("", "");
                        currentNote.setLatitude(place.getLatLng().latitude);
                        currentNote.setLongitude(place.getLatLng().longitude);
                        currentNote.setLocationName(place.getName());
                        updateLocationStatusUI();
                        Toast.makeText(this, "Location set to " + place.getName(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        initializePlacesApi();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        titleEditText = findViewById(R.id.titleEditText);
        bodyEditText = findViewById(R.id.bodyEditText);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        reminderButton = findViewById(R.id.reminderButton);
        locationButton = findViewById(R.id.locationButton);
        tagsButton = findViewById(R.id.tagsButton);
        pinButton = findViewById(R.id.pinButton);
        reminderStatusTextView = findViewById(R.id.reminderStatusTextView);
        locationStatusTextView = findViewById(R.id.locationStatusTextView);
        tagsDisplayLayout = findViewById(R.id.tagsDisplayLayout);
        tagsDisplayTextView = findViewById(R.id.tagsDisplayTextView);

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_NOTE_ID)) {
            isEditing = true;
            int noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1);
            if (noteId != -1) {
                viewModel.getById(noteId, note -> {
                    if (note != null) {
                        currentNote = note;
                        populateUI();
                        askToUpdateLocation();
                        setupTagObserver();
                    }
                });
            }
        } else {
            isEditing = false;
            attachInitialLocation();
        }

        reminderButton.setOnClickListener(v -> showReminderDialog());
        locationButton.setOnClickListener(v -> showLocationDialog());
        tagsButton.setOnClickListener(v -> showTagAssignmentDialog());
        pinButton.setOnClickListener(v -> togglePinStatus());

        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String body = bodyEditText.getText().toString().trim();
            if (currentNote == null) {
                currentNote = new Note(title, body);
            } else {
                currentNote.setTitle(title);
                currentNote.setBody(body);
            }
            Intent data = new Intent();
            data.putExtra("note_title", currentNote.getTitle());
            data.putExtra("note_body", currentNote.getBody());
            if (isEditing) {
                data.putExtra(EXTRA_NOTE_ID, currentNote.getId());
            }
            data.putExtra("is_pinned", currentNote.isPinned());
            data.putExtra("note_latitude", currentNote.getLatitude());
            data.putExtra("note_longitude", currentNote.getLongitude());
            data.putExtra("note_location_name", currentNote.getLocationName());
            data.putExtra("note_reminder_type", currentNote.getReminderType());
            data.putExtra("note_reminder_time", currentNote.getReminderTime());
            data.putExtra("note_reminder_latitude", currentNote.getReminderLatitude());
            data.putExtra("note_reminder_longitude", currentNote.getReminderLongitude());
            data.putExtra("note_reminder_location_name", currentNote.getReminderLocationName());
            data.putExtra("note_radius", currentNote.getRadius());
            setResult(RESULT_OK, data);
            finish();
        });
        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        viewModel.getAllTags().observe(this, tags -> {
            if (tags != null) allTags = tags;
        });
    }

    private void populateUI() {
        if (currentNote == null) return;
        titleEditText.setText(currentNote.getTitle());
        bodyEditText.setText(currentNote.getBody());
        updateReminderStatusUI();
        updateLocationStatusUI();
        updatePinButtonIcon();
    }

    private void setupTagObserver() {
        if (!isEditing || currentNote == null) return;
        viewModel.getTagsForNote(currentNote.getId()).observe(this, this::updateTagsDisplay);
    }

    private void updateTagsDisplay(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            tagsDisplayLayout.setVisibility(View.GONE);
        } else {
            String tagNames = tags.stream().map(Tag::getName).collect(Collectors.joining(", "));
            tagsDisplayTextView.setText(tagNames);
            tagsDisplayLayout.setVisibility(View.VISIBLE);
        }
    }

    private void updateLocationStatusUI() {
        if (currentNote != null && currentNote.hasLocation()) {
            locationStatusTextView.setText(String.format(Locale.getDefault(), "Location: %s", currentNote.getLocationName()));
            locationStatusTextView.setVisibility(View.VISIBLE);
        } else {
            locationStatusTextView.setVisibility(View.GONE);
        }
    }

    private void attachInitialLocation() {
        if (hasLocationPermissions()) addAutomaticLocation();
    }

    private void askToUpdateLocation() {
        if (isEditing && hasLocationPermissions()) {
            new AlertDialog.Builder(this)
                    .setTitle("Update Location")
                    .setMessage("Would you like to update this note's location to your current one?")
                    .setPositiveButton("Yes", (d, w) -> addAutomaticLocation())
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    private void showLocationDialog() {
        List<CharSequence> options = new ArrayList<>();
        options.add("Update with Current Location");
        options.add("Set a Manual Location");
        if (currentNote != null && currentNote.hasLocation()) {
            options.add("Remove Location");
        }
        new AlertDialog.Builder(this)
                .setTitle("Note Location")
                .setItems(options.toArray(new CharSequence[0]), (dialog, which) -> {
                    String selected = options.get(which).toString();
                    switch (selected) {
                        case "Update with Current Location": addAutomaticLocation(); break;
                        case "Set a Manual Location": addManualLocation(); break;
                        case "Remove Location": removeLocation(); break;
                    }
                }).show();
    }

    private void addAutomaticLocation() {
        if (!hasLocationPermissions()) {
            Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) setLocationFromCoords(location.getLatitude(), location.getLongitude());
            else Toast.makeText(this, "Could not get current location.", Toast.LENGTH_SHORT).show();
        });
    }

    private void addManualLocation() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
        contextPlacePickerLauncher.launch(intent);
    }

    private void removeLocation() {
        if (currentNote == null) return;
        if ("geofence".equals(currentNote.getReminderType())) {
            new AlertDialog.Builder(this)
                    .setTitle("Remove Location")
                    .setMessage("This will also remove the active location reminder for this note. Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        currentNote.setLatitude(0);
                        currentNote.setLongitude(0);
                        currentNote.setLocationName(null);
                        clearReminder();
                        updateLocationStatusUI();
                        Toast.makeText(this, "Location and reminder removed.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            currentNote.setLatitude(0);
            currentNote.setLongitude(0);
            currentNote.setLocationName(null);
            updateLocationStatusUI();
            Toast.makeText(this, "Location removed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLocationFromCoords(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            String locationName = String.format(Locale.getDefault(), "%.4f, %.4f", latitude, longitude);
            if (addresses != null && !addresses.isEmpty()) locationName = addresses.get(0).getAddressLine(0);
            if (currentNote == null) currentNote = new Note("", "");
            currentNote.setLatitude(latitude);
            currentNote.setLongitude(longitude);
            currentNote.setLocationName(locationName);
            updateLocationStatusUI();
            Toast.makeText(this, "Location updated.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Geocoder failed", e);
        }
    }

    private void showReminderDialog() {
        if (currentNote == null) currentNote = new Note("", "");
        List<CharSequence> options = new ArrayList<>();
        options.add("Set Time Reminder");
        options.add("Set Location Reminder");
        if (currentNote.getReminderType() != null) {
            options.add("Clear Reminder");
        }
        new AlertDialog.Builder(this)
                .setTitle("Reminder Options")
                .setItems(options.toArray(new CharSequence[0]), (dialog, which) -> {
                    String selected = options.get(which).toString();
                    switch (selected) {
                        case "Set Time Reminder": setTimeReminder(); break;
                        case "Set Location Reminder": launchReminderPlacePicker(); break;
                        case "Clear Reminder": clearReminder(); break;
                    }
                }).show();
    }

    private void launchReminderPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
        reminderPlacePickerLauncher.launch(intent);
    }

    private void clearReminder() {
        if (currentNote == null) return;
        currentNote.setReminderType(null);
        currentNote.setReminderTime(0);
        currentNote.setRadius(0);
        currentNote.setReminderLatitude(0);
        currentNote.setReminderLongitude(0);
        currentNote.setReminderLocationName(null);
        updateReminderStatusUI();
        Toast.makeText(this, "Reminder cleared.", Toast.LENGTH_SHORT).show();
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
        } else if ("geofence".equals(reminderType) && currentNote.hasGeofenceReminder()) {
            statusText = String.format(Locale.getDefault(), "Location Reminder: %s", currentNote.getReminderLocationName());
        }
        if (!statusText.isEmpty()) {
            reminderStatusTextView.setText(statusText);
            reminderStatusTextView.setVisibility(View.VISIBLE);
        } else {
            reminderStatusTextView.setVisibility(View.GONE);
        }
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
                if (currentNote == null) currentNote = new Note("", "");
                currentNote.setReminderType("time");
                currentNote.setReminderTime(calendar.getTimeInMillis());
                currentNote.setRadius(0);
                currentNote.setReminderLatitude(0);
                currentNote.setReminderLongitude(0);
                currentNote.setReminderLocationName(null);
                updateReminderStatusUI();
            };
            new TimePickerDialog(EditorActivity.this, timeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        };
        new DatePickerDialog(EditorActivity.this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void togglePinStatus() {
        if (currentNote == null) currentNote = new Note("", "");
        currentNote.setPinned(!currentNote.isPinned());
        updatePinButtonIcon();
        Toast.makeText(this, currentNote.isPinned() ? "Note pinned" : "Note unpinned", Toast.LENGTH_SHORT).show();
    }

    private void updatePinButtonIcon() {
        if (currentNote != null && currentNote.isPinned()) {
            pinButton.setImageResource(android.R.drawable.star_on);
        } else {
            pinButton.setImageResource(android.R.drawable.star_off);
        }
    }

    private void showTagAssignmentDialog() {
        if (!isEditing || currentNote == null) {
            Toast.makeText(this, "Please save the note before adding tags.", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] tagNames = allTags.stream().map(Tag::getName).toArray(CharSequence[]::new);
        final boolean[] checkedItems = new boolean[allTags.size()];

        // This is a one-time fetch for the dialog, so we don't need to permanently observe.
        viewModel.getTagsForNote(currentNote.getId()).observe(this, noteTags -> {
            if (noteTags != null) {
                for (int i = 0; i < allTags.size(); i++) {
                    Tag currentGlobalTag = allTags.get(i);
                    for (Tag noteTag : noteTags) {
                        if (currentGlobalTag.getId() == noteTag.getId()) {
                            checkedItems[i] = true;
                            break;
                        }
                    }
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Assign Tags")
                .setMultiChoiceItems(tagNames, checkedItems, (dialog, which, isChecked) -> {
                    Tag selectedTag = allTags.get(which);
                    if (isChecked) {
                        viewModel.assignTagToNote(currentNote.getId(), selectedTag.getId());
                    } else {
                        viewModel.removeTagFromNote(currentNote.getId(), selectedTag.getId());
                    }
                })
                .setPositiveButton("Done", null)
                .show();
    }
}