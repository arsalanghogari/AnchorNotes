package edu.usc.cs310.anchornotes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.usc.cs310.anchornotes.BuildConfig;
import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.model.Template;
import edu.usc.cs310.anchornotes.viewmodel.NoteViewModel;

public class TemplatesActivity extends AppCompatActivity {

    private static final float DEFAULT_GEOFENCE_RADIUS_METERS = 50f;

    private NoteViewModel viewModel;
    private ArrayAdapter<String> adapter;
    private List<Template> templates = new ArrayList<>();
    private TemplateDraft currentDraft;
    private TextView draftLocationStatusView;
    private EditText draftRadiusInputView;

    private final ActivityResultLauncher<Intent> locationPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && currentDraft != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place.getLatLng() != null) {
                        currentDraft.geoLatitude = place.getLatLng().latitude;
                        currentDraft.geoLongitude = place.getLatLng().longitude;
                        currentDraft.geoLocationName = place.getName();
                        if (currentDraft.geoRadius == null || currentDraft.geoRadius <= 0) {
                            currentDraft.geoRadius = DEFAULT_GEOFENCE_RADIUS_METERS;
                        }
                        if (draftLocationStatusView != null) {
                            draftLocationStatusView.setText(String.format(Locale.getDefault(),
                                    "Location: %s", currentDraft.geoLocationName));
                            draftLocationStatusView.setVisibility(View.VISIBLE);
                        }
                        if (draftRadiusInputView != null && (TextUtils.isEmpty(draftRadiusInputView.getText()) || draftRadiusInputView.getText().toString().trim().isEmpty())) {
                            draftRadiusInputView.setText(String.valueOf(currentDraft.geoRadius));
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_templates);

        Toolbar toolbar = findViewById(R.id.templatesToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Templates");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        initializePlacesApi();

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        ListView templatesListView = findViewById(R.id.templatesListView);
        TextView emptyView = findViewById(R.id.templatesEmptyView);
        templatesListView.setEmptyView(emptyView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        templatesListView.setAdapter(adapter);

        templatesListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < templates.size()) {
                Template selected = templates.get(position);
                showTemplateActionsDialog(selected);
            }
        });

        FloatingActionButton addButton = findViewById(R.id.addTemplateButton);
        addButton.setOnClickListener(v -> openTemplateEditorDialog(null));

        viewModel.getTemplatesLiveData().observe(this, templateList -> {
            templates = (templateList != null) ? templateList : new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Template template : templates) {
                String name = TextUtils.isEmpty(template.getName()) ?
                        String.format(Locale.getDefault(), "Template #%d", template.getId()) :
                        template.getName();
                names.add(name);
            }
            adapter.clear();
            adapter.addAll(names);
            adapter.notifyDataSetChanged();
        });
    }

    private void initializePlacesApi() {
        if (!Places.isInitialized()) {
            String apiKey = BuildConfig.MAPS_API_KEY;
            if (!TextUtils.isEmpty(apiKey)) {
                Places.initialize(getApplicationContext(), apiKey);
            }
        }
    }

    private void showTemplateActionsDialog(Template template) {
        if (template == null) {
            return;
        }
        String templateName = TextUtils.isEmpty(template.getName()) ?
                String.format(Locale.getDefault(), "Template #%d", template.getId()) :
                template.getName();
        List<String> options = new ArrayList<>();
        options.add("Edit");
        options.add("Delete");

        new AlertDialog.Builder(this)
                .setTitle(templateName)
                .setItems(options.toArray(new CharSequence[0]), (dialog, which) -> {
                    String selected = options.get(which);
                    if ("Edit".equals(selected)) {
                        openTemplateEditorDialog(template);
                    } else if ("Delete".equals(selected)) {
                        confirmDeleteTemplate(template);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmDeleteTemplate(Template template) {
        if (template == null) {
            return;
        }
        if (templates.size() <= 1) {
            Toast.makeText(this, "At least one template must remain.", Toast.LENGTH_SHORT).show();
            return;
        }
        String templateName = TextUtils.isEmpty(template.getName()) ?
                String.format(Locale.getDefault(), "Template #%d", template.getId()) :
                template.getName();
        new AlertDialog.Builder(this)
                .setTitle("Delete Template")
                .setMessage("Delete '" + templateName + "'? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteTemplate(template);
                    Toast.makeText(this, "Template deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openTemplateEditorDialog(Template existingTemplate) {
        currentDraft = TemplateDraft.from(existingTemplate);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_template_editor, null);

        EditText nameInput = dialogView.findViewById(R.id.templateNameInput);
        EditText titleInput = dialogView.findViewById(R.id.templateTitleInput);
        EditText bodyInput = dialogView.findViewById(R.id.templateBodyInput);
        Spinner colorSpinner = dialogView.findViewById(R.id.templateColorSpinner);
        EditText tagsInput = dialogView.findViewById(R.id.templateTagsInput);
        draftLocationStatusView = dialogView.findViewById(R.id.templateLocationStatusTextView);
        Button setLocationButton = dialogView.findViewById(R.id.templateSetLocationButton);
        Button clearLocationButton = dialogView.findViewById(R.id.templateClearLocationButton);
        draftRadiusInputView = dialogView.findViewById(R.id.templateRadiusInput);

        LinkedHashMap<String, String> colorOptions = getPageColorOptions();
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new ArrayList<>(colorOptions.keySet()));
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(colorAdapter);

        if (!TextUtils.isEmpty(currentDraft.name)) {
            nameInput.setText(currentDraft.name);
        }
        titleInput.setText(currentDraft.title != null ? currentDraft.title : "");
        bodyInput.setText(currentDraft.body != null ? currentDraft.body : "");
        if (!TextUtils.isEmpty(currentDraft.defaultTagsCsv)) {
            tagsInput.setText(currentDraft.defaultTagsCsv);
        }

        String colorNameToSelect = findColorNameForValue(colorOptions, currentDraft.pageColor);
        int colorIndex = Math.max(new ArrayList<>(colorOptions.keySet()).indexOf(colorNameToSelect), 0);
        colorSpinner.setSelection(colorIndex);

        updateDraftLocationStatus();
        if (currentDraft.geoRadius != null && currentDraft.geoRadius > 0) {
            draftRadiusInputView.setText(String.valueOf(currentDraft.geoRadius));
        } else if (currentDraft.geoLatitude != null && currentDraft.geoLongitude != null) {
            currentDraft.geoRadius = DEFAULT_GEOFENCE_RADIUS_METERS;
            draftRadiusInputView.setText(String.valueOf(currentDraft.geoRadius));
        }

        setLocationButton.setOnClickListener(v -> launchLocationPicker());
        clearLocationButton.setOnClickListener(v -> {
            currentDraft.geoLatitude = null;
            currentDraft.geoLongitude = null;
            currentDraft.geoLocationName = null;
            currentDraft.geoRadius = null;
            updateDraftLocationStatus();
            if (draftRadiusInputView != null) {
                draftRadiusInputView.setText("");
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existingTemplate == null ? "Create Template" : "Edit Template")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    nameInput.setError("Name required");
                    return;
                }
                String radiusText = draftRadiusInputView.getText().toString().trim();
                Float radius = null;
                if (!radiusText.isEmpty()) {
                    try {
                        radius = Float.parseFloat(radiusText);
                        if (radius <= 0) {
                            draftRadiusInputView.setError("Radius must be positive");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        draftRadiusInputView.setError("Invalid radius");
                        return;
                    }
                }

                Template template = (existingTemplate != null) ? existingTemplate : new Template();
                template.setName(name);
                template.setTitle(titleInput.getText().toString());
                template.setBody(bodyInput.getText().toString());
                String selectedColorName = (String) colorSpinner.getSelectedItem();
                template.setPageColor(colorOptions.get(selectedColorName));
                template.setDefaultTagsCsv(serializeTagsInput(tagsInput.getText().toString()));

                Double lat = currentDraft.geoLatitude;
                Double lng = currentDraft.geoLongitude;
                template.setGeoLatitude(lat);
                template.setGeoLongitude(lng);
                template.setGeoLocationName(currentDraft.geoLocationName);

                Float effectiveRadius = null;
                if (lat != null && lng != null) {
                    if (radius != null && radius > 0) {
                        effectiveRadius = radius;
                    } else if (currentDraft.geoRadius != null && currentDraft.geoRadius > 0) {
                        effectiveRadius = currentDraft.geoRadius;
                    } else {
                        effectiveRadius = DEFAULT_GEOFENCE_RADIUS_METERS;
                    }
                }

                if (effectiveRadius != null && effectiveRadius > 0) {
                    template.setGeoRadius(effectiveRadius);
                } else {
                    template.setGeoRadius(null);
                    template.setGeoLatitude(null);
                    template.setGeoLongitude(null);
                    template.setGeoLocationName(null);
                }

                if (existingTemplate == null) {
                    viewModel.insertTemplate(template, newId ->
                            Toast.makeText(TemplatesActivity.this, "Template created", Toast.LENGTH_SHORT).show());
                } else {
                    viewModel.updateTemplate(template);
                    Toast.makeText(TemplatesActivity.this, "Template updated", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void launchLocationPicker() {
        List<Place.Field> fields = new ArrayList<>();
        fields.add(Place.Field.ID);
        fields.add(Place.Field.NAME);
        fields.add(Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
        locationPickerLauncher.launch(intent);
    }

    private void updateDraftLocationStatus() {
        if (draftLocationStatusView == null) {
            return;
        }
        if (currentDraft != null && currentDraft.geoLatitude != null && currentDraft.geoLongitude != null) {
            String display = currentDraft.geoLocationName != null ? currentDraft.geoLocationName :
                    String.format(Locale.getDefault(), "%.4f, %.4f", currentDraft.geoLatitude, currentDraft.geoLongitude);
            draftLocationStatusView.setText("Location: " + display);
            draftLocationStatusView.setVisibility(View.VISIBLE);
        } else {
            draftLocationStatusView.setText("");
            draftLocationStatusView.setVisibility(View.GONE);
        }
    }

    private LinkedHashMap<String, String> getPageColorOptions() {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("Classic White", Template.DEFAULT_PAGE_COLOR);
        options.put("Soft Yellow", "#FFF9C4");
        options.put("Sky Blue", "#E3F2FD");
        options.put("Mint Green", "#E8F5E9");
        options.put("Blush Pink", "#FCE4EC");
        options.put("Warm Gray", "#ECEFF1");
        return options;
    }

    private String findColorNameForValue(Map<String, String> options, String value) {
        if (value != null) {
            for (Map.Entry<String, String> entry : options.entrySet()) {
                if (value.equalsIgnoreCase(entry.getValue())) {
                    return entry.getKey();
                }
            }
        }
        return options.keySet().iterator().next();
    }

    private String serializeTagsInput(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }
        String[] parts = input.split(",");
        List<String> cleaned = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        return cleaned.isEmpty() ? null : TextUtils.join(",", cleaned);
    }

    private static class TemplateDraft {
        String name;
        String title;
        String body;
        String pageColor;
        String defaultTagsCsv;
        Double geoLatitude;
        Double geoLongitude;
        String geoLocationName;
        Float geoRadius;

        static TemplateDraft from(Template template) {
            TemplateDraft draft = new TemplateDraft();
            if (template != null) {
                draft.name = template.getName();
                draft.title = template.getTitle();
                draft.body = template.getBody();
                draft.pageColor = template.getPageColor();
                draft.defaultTagsCsv = template.getDefaultTagsCsv();
                draft.geoLatitude = template.getGeoLatitude();
                draft.geoLongitude = template.getGeoLongitude();
                draft.geoLocationName = template.getGeoLocationName();
                draft.geoRadius = template.getGeoRadius();
            } else {
                draft.pageColor = Template.DEFAULT_PAGE_COLOR;
            }
            return draft;
        }
    }
}

