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
import com.google.android.gms.maps.model.Circle; // <-- Import this
import com.google.android.gms.maps.model.CircleOptions; // <-- Import this
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
    private List<Circle> geofenceCircles = new ArrayList<>(); // <-- New list to track circles

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
        geofenceCircles.clear(); // <-- Clear old circles
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

            // --- NEW: DRAW THE GEOFENCE CIRCLE ---
            // Only draw if the note has a radius set (i.e., it's a geofence)
            if (note.getRadius() > 0 && "geofence".equals(note.getReminderType())) {
                CircleOptions circleOptions = new CircleOptions()
                        .center(location)
                        .radius(note.getRadius()) // Use the note's radius (50m)
                        .strokeColor(0x88FF0000)   // Semi-transparent Red border
                        .fillColor(0x22FF0000)     // Very transparent Red fill
                        .strokeWidth(3);           // Border width

                Circle circle = mMap.addCircle(circleOptions);
                geofenceCircles.add(circle); // Add to our list to clear later if needed
            }
        }

        if (markers.size() > 1) {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 150;
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } else if (markers.size() == 1) {
            // Adjust zoom level if there's only one pin with a visible circle
            float zoomLevel = getZoomLevelForRadius(geoNotes.get(0).getRadius());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markers.get(0).getPosition(), zoomLevel));
        }
    }

    // --- NEW HELPER METHOD TO CALCULATE ZOOM LEVEL ---
    private float getZoomLevelForRadius(double radius) {
        // Approximate conversion from meters to zoom level.
        // This is not precise but gives a good estimation for typical map views.
        // Google Maps SDK doesn't provide a direct API for this, so this is a heuristic.
        // A smaller radius means a higher zoom level (closer).
        double scale = radius / 500; // Reference 500m to 14 zoom level
        return (float) (16 - Math.log(scale) / Math.log(2));
    }

    private void showQuickJumpMenu() {
        final CharSequence[] noteTitles = geoNotes.stream()
                .map(note -> note.getTitle().isEmpty() ? "(Untitled)" : note.getTitle())
                .toArray(CharSequence[]::new);

        new AlertDialog.Builder(this)
                .setTitle("Jump to Note")
                .setItems(noteTitles, (dialog, which) -> {
                    focusOnPin(which);
                })
                .show();
    }

    private void updateNavigationUI() {
        if (currentPinIndex == -1 || markers.isEmpty()) {
            pinTitleTextView.setText("Select a pin to navigate");
            boolean enableButtons = !markers.isEmpty();
            prevButton.setEnabled(enableButtons);
            nextButton.setEnabled(enableButtons);
        } else {
            Note note = geoNotes.get(currentPinIndex);
            pinTitleTextView.setText(note.getTitle().isEmpty() ? "(Untitled)" : note.getTitle());
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