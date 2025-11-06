package edu.usc.cs310.anchornotes.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import edu.usc.cs310.anchornotes.BuildConfig;
import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.Tag;
import edu.usc.cs310.anchornotes.model.Template;
import edu.usc.cs310.anchornotes.viewmodel.NoteViewModel;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.widget.ImageView;
import android.net.Uri;
import android.media.MediaRecorder;
import android.media.MediaPlayer;


public class EditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "edu.usc.cs310.anchornotes.EXTRA_NOTE_ID";
    public static final String EXTRA_NOTE_PAGE_COLOR = "edu.usc.cs310.anchornotes.EXTRA_NOTE_PAGE_COLOR";
    public static final String EXTRA_TEMPLATE_ID = "edu.usc.cs310.anchornotes.EXTRA_TEMPLATE_ID";
    public static final String EXTRA_TEMPLATE_TAGS = "edu.usc.cs310.anchornotes.EXTRA_TEMPLATE_TAGS";
    public static final String EXTRA_NOTE_VOICE_URI = "edu.usc.cs310.anchornotes.EXTRA_NOTE_VOICE_URI";
    public static final String EXTRA_NOTE_PHOTO_URI = "edu.usc.cs310.anchornotes.EXTRA_NOTE_PHOTO_URI";
    private static final float GEOFENCE_RADIUS_METERS = 50;
    private static final String TAG = "EditorActivity";

    private LinearLayout rootLayout;
    private EditText titleEditText;
    private Button saveButton, cancelButton;
    private ImageButton reminderButton, locationButton, tagsButton, pinButton, templateButton;
    private TextView reminderStatusTextView, locationStatusTextView, tagsDisplayTextView, templateStatusTextView;
    private LinearLayout tagsDisplayLayout;
    private EditText noteBody;
    private ImageButton boldBtn, italicBtn, checklistBtn, fontSizeBtn;
    private ImageButton photoButton, recordButton, playButton;
    private ImageView photoPreview;
    private TextView voiceStatusTextView;
    private Uri photoUri;
    private String audioFilePath;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private boolean isRecording = false;



    private NoteViewModel viewModel;
    private Note currentNote;
    private boolean isEditing = false;
    private FusedLocationProviderClient fusedLocationClient;
    private List<Tag> allTags = new ArrayList<>();
    private List<Tag> currentNoteTags = new ArrayList<>();
    private List<Template> templates = new ArrayList<>();
    private List<String> pendingTemplateTagNames = new ArrayList<>();

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

    private final ActivityResultLauncher<String> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            Uri storedUri = savePhotoToAppStorage(uri);
                            if (storedUri != null) {
                                photoUri = storedUri;
                                photoPreview.setVisibility(View.VISIBLE);
                                photoPreview.setImageURI(photoUri);
                                if (currentNote == null) {
                                    currentNote = new Note("", "");
                                }
                                currentNote.setPhotoUri(photoUri.toString());
                            } else {
                                Toast.makeText(this, "Unable to attach image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    private final ActivityResultLauncher<String> micPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            toggleRecording();
                        } else {
                            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
                        }
                    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        initializePlacesApi();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        rootLayout = findViewById(R.id.editorRootLayout);
        titleEditText = findViewById(R.id.titleEditText);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        reminderButton = findViewById(R.id.reminderButton);
        locationButton = findViewById(R.id.locationButton);
        tagsButton = findViewById(R.id.tagsButton);
        pinButton = findViewById(R.id.pinButton);
        templateButton = findViewById(R.id.templateButton);
        reminderStatusTextView = findViewById(R.id.reminderStatusTextView);
        locationStatusTextView = findViewById(R.id.locationStatusTextView);
        tagsDisplayLayout = findViewById(R.id.tagsDisplayLayout);
        tagsDisplayTextView = findViewById(R.id.tagsDisplayTextView);
        templateStatusTextView = findViewById(R.id.templateStatusTextView);
        noteBody = findViewById(R.id.noteBodyEditText);
        photoPreview = findViewById(R.id.photoPreview);
        voiceStatusTextView = findViewById(R.id.voiceStatusTextView);

        boldBtn = findViewById(R.id.boldButton);
        italicBtn = findViewById(R.id.italicButton);
        checklistBtn = findViewById(R.id.checklistButton);
        fontSizeBtn = findViewById(R.id.fontSizeButton);
        photoButton = findViewById(R.id.photoButton);
        recordButton = findViewById(R.id.recordButton);
        playButton = findViewById(R.id.playButton);

        boldBtn.setOnClickListener(v -> toggleSpan(new StyleSpan(Typeface.BOLD)));
        italicBtn.setOnClickListener(v -> toggleSpan(new StyleSpan(Typeface.ITALIC)));
        checklistBtn.setOnClickListener(v -> insertChecklistItem());
        fontSizeBtn.setOnClickListener(v -> showFontSizeDialog());

        photoButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Access Your Photos")
                    .setMessage("Would you like to select an image from your gallery to attach to this note?")
                    .setPositiveButton("Yes", (dialog, which) -> requestPhotoPermissionAndOpen())
                    .setNegativeButton("No", null)
                    .show();
        });

        recordButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(this, "Microphone permission needed to record audio.", Toast.LENGTH_LONG).show();
                }
                micPerm.launch(Manifest.permission.RECORD_AUDIO);
            } else {
                toggleRecording();
            }
        });

        playButton.setOnClickListener(v -> playRecording());


        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        viewModel.getTemplatesLiveData().observe(this, templatesList -> {
            templates = (templatesList != null) ? templatesList : new ArrayList<>();
            updateTemplateStatusUI();
        });

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
            applyPageColor(Template.DEFAULT_PAGE_COLOR);
            updateTemplateStatusUI();
        }

        reminderButton.setOnClickListener(v -> showReminderDialog());
        locationButton.setOnClickListener(v -> showLocationDialog());
        tagsButton.setOnClickListener(v -> showTagAssignmentDialog());
        pinButton.setOnClickListener(v -> togglePinStatus());
        templateButton.setOnClickListener(v -> showTemplateSelectionDialog());

        saveButton.setOnClickListener(v -> {
            synchronizeNoteFromInputs();
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
            data.putExtra(EXTRA_NOTE_PAGE_COLOR, currentNote.getPageColor());
            data.putExtra(EXTRA_TEMPLATE_ID, currentNote.getTemplateId());
            if (currentNote.getVoiceUri() != null) {
                data.putExtra(EXTRA_NOTE_VOICE_URI, currentNote.getVoiceUri());
            }
            if (currentNote.getPhotoUri() != null) {
                data.putExtra(EXTRA_NOTE_PHOTO_URI, currentNote.getPhotoUri());
            }
            if (!pendingTemplateTagNames.isEmpty()) {
                data.putStringArrayListExtra(EXTRA_TEMPLATE_TAGS, new ArrayList<>(pendingTemplateTagNames));
            }
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
        noteBody.setText(currentNote.getBody());
        updateReminderStatusUI();
        updateLocationStatusUI();
        updatePinButtonIcon();
        applyPageColor(currentNote.getPageColor());
        updateTemplateStatusUI();
        pendingTemplateTagNames.clear();
        if (currentNote.getVoiceUri() != null) {
            audioFilePath = currentNote.getVoiceUri();
        }
        if (currentNote.getPhotoUri() != null) {
            photoUri = Uri.parse(currentNote.getPhotoUri());
            try {
                photoPreview.setVisibility(View.VISIBLE);
                photoPreview.setImageURI(photoUri);
            } catch (SecurityException | IllegalArgumentException e) {
                photoPreview.setVisibility(View.GONE);
                Toast.makeText(this, "Unable to load attached image", Toast.LENGTH_SHORT).show();
            }
        } else {
            photoPreview.setVisibility(View.GONE);
        }

    }

    private void setupTagObserver() {
        if (!isEditing || currentNote == null) return;
        viewModel.getTagsForNote(currentNote.getId()).observe(this, this::updateTagsDisplay);
    }

    private void updateTagsDisplay(List<Tag> tags) {
        currentNoteTags = (tags != null) ? new ArrayList<>(tags) : new ArrayList<>();
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

    private void showTemplateSelectionDialog() {
        if (templates == null || templates.isEmpty()) {
            Toast.makeText(this, "No templates available yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fusedLocationClient == null || !hasLocationPermissions()) {
            displayTemplateChooser(templates, null);
            return;
        }

        AtomicBoolean handled = new AtomicBoolean(false);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (handled.compareAndSet(false, true)) {
                        displayTemplateChooser(templates, location);
                    }
                })
                .addOnFailureListener(this, e -> {
                    if (handled.compareAndSet(false, true)) {
                        displayTemplateChooser(templates, null);
                    }
                })
                .addOnCompleteListener(this, task -> {
                    if (handled.compareAndSet(false, true)) {
                        Location result = (task.isSuccessful()) ? task.getResult() : null;
                        displayTemplateChooser(templates, result);
                    }
                });
    }

    private void displayTemplateChooser(List<Template> templatesToShow, Location currentLocation) {
        if (templatesToShow == null || templatesToShow.isEmpty()) {
            Toast.makeText(this, "No templates to show.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Template> sorted = new ArrayList<>(templatesToShow);
        Collections.sort(sorted, Comparator.comparing((Template t) -> !isTemplateRelevant(t, currentLocation))
                .thenComparing(t -> {
                    String name = t.getName();
                    return (name == null ? "" : name.toLowerCase(Locale.getDefault()));
                }));

        List<Template> options = new ArrayList<>();
        options.add(null); // Represents clearing template
        options.addAll(sorted);

        List<String> labels = new ArrayList<>();
        labels.add("No Template");
        for (Template template : sorted) {
            String templateName = TextUtils.isEmpty(template.getName()) ? "Untitled Template" : template.getName();
            StringBuilder label = new StringBuilder(templateName);
            if (isTemplateRelevant(template, currentLocation)) {
                label.append(" • recommended");
            }
            labels.add(label.toString());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        new AlertDialog.Builder(this)
                .setTitle("Select Template")
                .setAdapter(adapter, (dialog, which) -> {
                    Template selected = options.get(which);
                    if (selected == null) {
                        clearTemplateAssociation();
                    } else {
                        applyTemplateToCurrentNote(selected);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean isTemplateRelevant(Template template, Location location) {
        if (template == null || location == null || !template.hasGeofence()) {
            return false;
        }
        double templateLat = template.getGeoLatitude() != null ? template.getGeoLatitude() : 0;
        double templateLng = template.getGeoLongitude() != null ? template.getGeoLongitude() : 0;
        float radius = template.getGeoRadius() != null && template.getGeoRadius() > 0 ? template.getGeoRadius() : GEOFENCE_RADIUS_METERS;
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), templateLat, templateLng, results);
        return results[0] <= radius;
    }

    private void applyTemplateToCurrentNote(Template template) {
        if (template == null) {
            return;
        }
        synchronizeNoteFromInputs();
        if (currentNote == null) {
            currentNote = new Note("", "");
        }
        pendingTemplateTagNames.clear();
        currentNote.setTemplateId(template.getId());
        currentNote.setTitle(template.getTitle() != null ? template.getTitle() : "");
        currentNote.setBody(template.getBody() != null ? template.getBody() : "");
        titleEditText.setText(currentNote.getTitle());
        noteBody.setText(currentNote.getBody());
        applyPageColor(template.getPageColor());

        if (template.hasGeofence()) {
            currentNote.setReminderType("geofence");
            currentNote.setReminderLatitude(template.getGeoLatitude() != null ? template.getGeoLatitude() : 0);
            currentNote.setReminderLongitude(template.getGeoLongitude() != null ? template.getGeoLongitude() : 0);
            currentNote.setReminderLocationName(template.getGeoLocationName());
            currentNote.setRadius(template.getGeoRadius() != null && template.getGeoRadius() > 0 ? template.getGeoRadius() : GEOFENCE_RADIUS_METERS);
            currentNote.setReminderTime(0);
        } else {
            currentNote.setReminderType(null);
            currentNote.setReminderLatitude(0);
            currentNote.setReminderLongitude(0);
            currentNote.setReminderLocationName(null);
            currentNote.setRadius(0);
            currentNote.setReminderTime(0);
        }
        updateReminderStatusUI();

        List<String> templateTags = template.getDefaultTagNames();
        if (isEditing && currentNote.getId() != 0) {
            assignTemplateTagsImmediately(templateTags);
        } else {
            showPendingTags(templateTags);
        }

        updateTemplateStatusUI();
        String templateName = TextUtils.isEmpty(template.getName()) ? "Untitled Template" : template.getName();
        Toast.makeText(this, "Template applied: " + templateName, Toast.LENGTH_SHORT).show();
    }

    private void assignTemplateTagsImmediately(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty() || currentNote == null || currentNote.getId() == 0) {
            return;
        }
        viewModel.ensureTagsForNames(tagNames, tags -> {
            if (tags == null) return;
            for (Tag tag : tags) {
                viewModel.assignTagToNote(currentNote.getId(), tag.getId());
            }
        });
    }

    private void showPendingTags(List<String> tagNames) {
        pendingTemplateTagNames.clear();
        if (tagNames == null || tagNames.isEmpty()) {
            tagsDisplayLayout.setVisibility(View.GONE);
            tagsDisplayTextView.setText("");
            return;
        }
        pendingTemplateTagNames.addAll(tagNames);
        tagsDisplayLayout.setVisibility(View.VISIBLE);
        tagsDisplayTextView.setText(TextUtils.join(", ", tagNames));
    }

    private void synchronizeNoteFromInputs() {
        String title = titleEditText.getText().toString().trim();
        String body = noteBody.getText().toString().trim();
        if (currentNote == null) {
            currentNote = new Note(title, body);
        } else {
            currentNote.setTitle(title);
            currentNote.setBody(body);
            if (audioFilePath != null) {
                currentNote.setVoiceUri(audioFilePath);
            }
            if (photoUri != null) {
                currentNote.setPhotoUri(photoUri.toString());
            }

        }
    }

    private void applyPageColor(String colorHex) {
        String effectiveColor = (colorHex == null || colorHex.isEmpty()) ? Template.DEFAULT_PAGE_COLOR : colorHex;
        if (currentNote == null) {
            currentNote = new Note("", "");
        }
        currentNote.setPageColor(effectiveColor);
        if (rootLayout != null) {
            try {
                rootLayout.setBackgroundColor(Color.parseColor(effectiveColor));
            } catch (IllegalArgumentException e) {
                rootLayout.setBackgroundColor(Color.parseColor(Template.DEFAULT_PAGE_COLOR));
                currentNote.setPageColor(Template.DEFAULT_PAGE_COLOR);
            }
        }
    }

    private void updateTemplateStatusUI() {
        if (templateStatusTextView == null) return;
        int templateId = (currentNote != null) ? currentNote.getTemplateId() : 0;
        if (templateId == 0) {
            templateStatusTextView.setVisibility(View.GONE);
            return;
        }
        Template template = findTemplateById(templateId);
        if (template != null) {
            String templateName = TextUtils.isEmpty(template.getName()) ? "Untitled Template" : template.getName();
            templateStatusTextView.setText("Template: " + templateName);
        } else {
            templateStatusTextView.setText("Template ID: " + templateId);
        }
        templateStatusTextView.setVisibility(View.VISIBLE);
    }

    private Template findTemplateById(int templateId) {
        if (templateId == 0 || templates == null) {
            return null;
        }
        for (Template template : templates) {
            if (template.getId() == templateId) {
                return template;
            }
        }
        return null;
    }

    private void clearTemplateAssociation() {
        if (currentNote == null) {
            return;
        }
        currentNote.setTemplateId(0);
        applyPageColor(Template.DEFAULT_PAGE_COLOR);
        pendingTemplateTagNames.clear();
        if (isEditing) {
            updateTagsDisplay(currentNoteTags);
        } else {
            tagsDisplayLayout.setVisibility(View.GONE);
            tagsDisplayTextView.setText("");
        }
        updateTemplateStatusUI();
        Toast.makeText(this, "Template cleared", Toast.LENGTH_SHORT).show();
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

    private void toggleSpan(Object span) {
        int start = noteBody.getSelectionStart();
        int end = noteBody.getSelectionEnd();
        Spannable str = noteBody.getText();
        str.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void insertChecklistItem() {
        int pos = noteBody.getSelectionStart();
        noteBody.getText().insert(pos, "☐ ");
    }

    private void showFontSizeDialog() {
        String[] sizes = {"Small", "Medium", "Large"};
        new AlertDialog.Builder(this)
                .setTitle("Font size")
                .setItems(sizes, (d, i) -> {
                    float size = (i == 0) ? 14f : (i == 1 ? 18f : 24f);
                    noteBody.setTextSize(size);
                })
                .show();
    }

    // -------------------- Voice Recording --------------------
    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        try {
            if (currentNote == null) {
                currentNote = new Note("", "");
            }
            File file = new File(getExternalFilesDir(null),
                    "voice_" + System.currentTimeMillis() + ".m4a");
            audioFilePath = file.getAbsolutePath();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(audioFilePath);
            recorder.prepare();
            recorder.start();

            isRecording = true;
            if (voiceStatusTextView != null) {
                voiceStatusTextView.setText("Recording...");
                voiceStatusTextView.setVisibility(View.VISIBLE);
            }
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            currentNote.setVoiceUri(audioFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            isRecording = false;
            if (voiceStatusTextView != null) {
                voiceStatusTextView.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playRecording() {
        if (audioFilePath == null) {
            Toast.makeText(this, "No recording available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (player != null) {
                player.release();
            }
            player = new MediaPlayer();
            player.setDataSource(audioFilePath);
            player.prepare();
            player.start();
            Toast.makeText(this, "Playing voice memo", Toast.LENGTH_SHORT).show();

            player.setOnCompletionListener(mp -> {
                mp.release();
                Toast.makeText(this, "Playback finished", Toast.LENGTH_SHORT).show();
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to play recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPhotoPermissionAndOpen() {
        if (android.os.Build.VERSION.SDK_INT <= 32 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 201);
        } else {
            pickPhoto.launch("image/*");
        }
    }

    private Uri savePhotoToAppStorage(Uri sourceUri) {
        try (InputStream in = getContentResolver().openInputStream(sourceUri)) {
            if (in == null) {
                return null;
            }
            File dir = getExternalFilesDir("photos");
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            File targetDir = (dir != null) ? dir : getFilesDir();
            File outFile = new File(targetDir, "photo_" + System.currentTimeMillis() + ".jpg");
            try (OutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
            return Uri.fromFile(outFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 201) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickPhoto.launch("image/*");
            } else {
                Toast.makeText(this, "Permission denied — cannot access gallery.", Toast.LENGTH_SHORT).show();
            }
        }
    }




}