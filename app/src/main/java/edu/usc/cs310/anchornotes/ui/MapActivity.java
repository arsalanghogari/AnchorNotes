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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.viewmodel.NoteViewModel;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private NoteViewModel viewModel;
    private List<Note> geoNotes = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();

    private RelativeLayout navigationLayout;
    private ImageButton prevButton, nextButton;
    private TextView pinTitleTextView;
    private int currentPinIndex = -1;

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

        navigationLayout = findViewById(R.id.navigationLayout);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        pinTitleTextView = findViewById(R.id.pinTitleTextView);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        prevButton.setOnClickListener(v -> navigateToPreviousPin());
        nextButton.setOnClickListener(v -> navigateToNextPin());

        // === NEW FEATURE: QUICK JUMP MENU ===
        // Add a click listener to the title text view to show the list of notes.
        pinTitleTextView.setOnClickListener(v -> {
            if (!geoNotes.isEmpty()) {
                showQuickJumpMenu();
            }
        });
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

        viewModel.getNotesLiveData().observe(this, allNotes -> {
            geoNotes = allNotes.stream()
                    .filter(note -> "geofence".equals(note.getReminderType()) && note.getLocationName() != null)
                    .collect(Collectors.toList());
            addPinsToMap();
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            Note clickedNote = (Note) marker.getTag();
            if (clickedNote != null) {
                Intent intent = new Intent(MapActivity.this, EditorActivity.class);
                intent.putExtra(EditorActivity.EXTRA_NOTE_ID, clickedNote.getId());
                startActivity(intent);
            }
        });

        mMap.setOnMarkerClickListener(marker -> {
            Note clickedNote = (Note) marker.getTag();
            if (clickedNote != null) {
                for (int i = 0; i < geoNotes.size(); i++) {
                    if (geoNotes.get(i).getId() == clickedNote.getId()) {
                        focusOnPin(i);
                        break;
                    }
                }
            }
            return false;
        });

        mMap.setOnMapClickListener(latLng -> {
            currentPinIndex = -1;
            updateNavigationUI();
        });

        updateNavigationUI();
    }

    private void addPinsToMap() {
        if (mMap == null) return;

        mMap.clear();
        markers.clear();
        currentPinIndex = -1;
        updateNavigationUI();

        if (geoNotes.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (Note note : geoNotes) {
            LatLng location = new LatLng(note.getLatitude(), note.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(location)
                    .title(note.getTitle())
                    .snippet(note.getLocationName());

            Marker marker = mMap.addMarker(markerOptions);
            if (marker != null) {
                marker.setTag(note);
                markers.add(marker);
            }
            boundsBuilder.include(location);
        }

        if (markers.size() > 1) {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 150;
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } else if (markers.size() == 1) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markers.get(0).getPosition(), 15f));
        }
    }

    // === NEW FEATURE: QUICK JUMP MENU IMPLEMENTATION ===
    private void showQuickJumpMenu() {
        // Create an array of note titles to display in the dialog.
        final CharSequence[] noteTitles = geoNotes.stream()
                .map(note -> note.getTitle().isEmpty() ? "(Untitled)" : note.getTitle())
                .toArray(CharSequence[]::new);

        new AlertDialog.Builder(this)
                .setTitle("Jump to Note")
                .setItems(noteTitles, (dialog, which) -> {
                    // 'which' is the index of the item the user tapped.
                    focusOnPin(which);
                })
                .show();
    }

    private void updateNavigationUI() {
        if (currentPinIndex == -1 || markers.isEmpty()) {
            pinTitleTextView.setText("Select a pin to navigate");

            // === BUG FIX: Only enable buttons if there are pins to navigate to ===
            boolean enableButtons = !markers.isEmpty();
            prevButton.setEnabled(enableButtons);
            nextButton.setEnabled(enableButtons);

        } else {
            Note note = geoNotes.get(currentPinIndex);
            pinTitleTextView.setText(note.getTitle().isEmpty() ? "(Untitled)" : note.getTitle());

            // === BUG FIX: Buttons should always be enabled if there is more than one pin ===
            boolean enableButtons = markers.size() > 1;
            prevButton.setEnabled(enableButtons);
            nextButton.setEnabled(enableButtons);
        }
    }

    private void navigateToPreviousPin() {
        if (markers.isEmpty()) return;

        // === BUG FIX: WRAPAROUND LOGIC ===
        // The + markers.size() ensures the result is always positive before the modulo.
        int newIndex = (currentPinIndex - 1 + markers.size()) % markers.size();
        focusOnPin(newIndex);
    }

    private void navigateToNextPin() {
        if (markers.isEmpty()) return;

        // === BUG FIX: WRAPAROUND LOGIC ===
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