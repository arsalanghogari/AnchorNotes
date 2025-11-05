package edu.usc.cs310.anchornotes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.viewmodel.NoteViewModel;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private NoteViewModel viewModel;

    // === NEW: SEPARATE MASTER LISTS FOR EACH MODE ===
    private List<Note> allContextLocationNotes = new ArrayList<>();
    private List<Note> allReminderLocationNotes = new ArrayList<>();

    private List<Note> currentlyDisplayedNotes = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();

    private boolean showRemindersOnly = false;
    private int currentPinIndex = -1;
    private int focusNoteId = -1;

    private RelativeLayout navigationLayout;
    private ImageButton prevButton, nextButton;
    private TextView pinTitleTextView;
    private ImageButton toggleRemindersButton;
    private ImageButton toggleLocationsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Note Map");
        }

        toggleRemindersButton = toolbar.findViewById(R.id.toggleRemindersButton);
        toggleLocationsButton = toolbar.findViewById(R.id.toggleLocationsButton);

        toggleLocationsButton.setOnClickListener(v -> {
            showRemindersOnly = true;
            updateToggleState();
            refreshMapPins();
        });

        toggleRemindersButton.setOnClickListener(v -> {
            showRemindersOnly = false;
            updateToggleState();
            refreshMapPins();
        });

        navigationLayout = findViewById(R.id.navigationLayout);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        pinTitleTextView = findViewById(R.id.pinTitleTextView);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        prevButton.setOnClickListener(v -> navigateToPreviousPin());
        nextButton.setOnClickListener(v -> navigateToNextPin());
        pinTitleTextView.setOnClickListener(v -> { if (!currentlyDisplayedNotes.isEmpty()) showQuickJumpMenu(); });

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("focus_note_id")) {
            focusNoteId = intent.getIntExtra("focus_note_id", -1);
        }
        updateToggleState();
    }

    private void updateToggleState() {
        if (showRemindersOnly) {
            toggleRemindersButton.setVisibility(View.VISIBLE);
            toggleLocationsButton.setVisibility(View.GONE);
        } else {
            toggleRemindersButton.setVisibility(View.GONE);
            toggleLocationsButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        viewModel.getNotesLiveData().observe(this, notes -> {
            if (notes == null) return;

            // === NEW: POPULATE THE SEPARATE MASTER LISTS ===
            allContextLocationNotes = notes.stream()
                    .filter(Note::hasLocation)
                    .collect(Collectors.toList());
            allReminderLocationNotes = notes.stream()
                    .filter(Note::hasGeofenceReminder)
                    .collect(Collectors.toList());

            refreshMapPins();
            if (focusNoteId != -1) focusOnNote(focusNoteId);
        });

        // ... (Map listeners are unchanged)
    }

    // === REFRESH LOGIC NOW CHOOSES THE CORRECT MASTER LIST ===
    private void refreshMapPins() {
        if (mMap == null) return;
        if (showRemindersOnly) {
            currentlyDisplayedNotes = new ArrayList<>(allReminderLocationNotes);
        } else {
            currentlyDisplayedNotes = new ArrayList<>(allContextLocationNotes);
        }
        addPinsToMap(currentlyDisplayedNotes);
    }

    private void addPinsToMap(List<Note> notesToDraw) {
        if (mMap == null) return;
        mMap.clear();
        markers.clear();
        currentPinIndex = -1;
        updateNavigationUI();
        if (notesToDraw.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (Note note : notesToDraw) {
            LatLng location;
            String snippet;

            // === SIMPLIFIED PIN LOGIC ===
            if (showRemindersOnly) {
                location = new LatLng(note.getReminderLatitude(), note.getReminderLongitude());
                snippet = "Reminder: " + note.getReminderLocationName();
            } else {
                location = new LatLng(note.getLatitude(), note.getLongitude());
                snippet = "Context: " + note.getLocationName();
            }

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(location)
                    .title(note.getDisplayTitle())
                    .snippet(snippet);
            Marker marker = mMap.addMarker(markerOptions);
            if (marker != null) {
                marker.setTag(note);
                markers.add(marker);
            }
            boundsBuilder.include(location);

            // Always draw the circle if the note has a geofence reminder, regardless of mode
            if (note.hasGeofenceReminder()) {
                CircleOptions circleOptions = new CircleOptions()
                        .center(new LatLng(note.getReminderLatitude(), note.getReminderLongitude()))
                        .radius(note.getRadius())
                        .strokeColor(0x88FF0000)
                        .fillColor(0x22FF0000)
                        .strokeWidth(3);
                mMap.addCircle(circleOptions);
            }
        }

        if (focusNoteId == -1 && !markers.isEmpty()) {
            if (markers.size() > 1) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150));
            } else {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markers.get(0).getPosition(), 15f));
            }
        }
    }

    // ... (rest of the file is unchanged)

    private void focusOnNote(int noteId) {
        for (int i = 0; i < currentlyDisplayedNotes.size(); i++) {
            if (currentlyDisplayedNotes.get(i).getId() == noteId) {
                focusOnPin(i);
                break;
            }
        }
    }

    private float getZoomLevelForRadius(double radius) {
        if (radius <= 0) return 15f;
        double scale = radius / 500;
        return (float) (16 - Math.log(scale) / Math.log(2));
    }

    private void showQuickJumpMenu() {
        final CharSequence[] noteTitles = currentlyDisplayedNotes.stream()
                .map(Note::getDisplayTitle)
                .toArray(CharSequence[]::new);
        new AlertDialog.Builder(this)
                .setTitle("Jump to Note")
                .setItems(noteTitles, (dialog, which) -> focusOnPin(which))
                .show();
    }

    private void updateNavigationUI() {
        if (currentPinIndex == -1 || markers.isEmpty() || currentPinIndex >= currentlyDisplayedNotes.size()) {
            pinTitleTextView.setText("Select a pin to navigate");
            boolean enableButtons = !markers.isEmpty();
            prevButton.setEnabled(enableButtons);
            nextButton.setEnabled(enableButtons);
        } else {
            Note note = currentlyDisplayedNotes.get(currentPinIndex);
            pinTitleTextView.setText(note.getDisplayTitle());
            boolean enableButtons = markers.size() > 1;
            prevButton.setEnabled(enableButtons);
            nextButton.setEnabled(enableButtons);
        }
    }

    private void navigateToPreviousPin() {
        if (markers.isEmpty()) return;
        int newIndex = (currentPinIndex - 1 + markers.size()) % markers.size();
        focusOnPin(newIndex);
    }

    private void navigateToNextPin() {
        if (markers.isEmpty()) return;
        int newIndex = (currentPinIndex + 1) % markers.size();
        focusOnPin(newIndex);
    }

    private void focusOnPin(int index) {
        if (index < 0 || index >= markers.size()) return;
        currentPinIndex = index;
        Marker marker = markers.get(index);
        updateNavigationUI();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        marker.showInfoWindow();
    }
}