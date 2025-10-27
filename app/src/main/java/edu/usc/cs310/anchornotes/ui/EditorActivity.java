package edu.usc.cs310.anchornotes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import edu.usc.cs310.anchornotes.R;

public class EditorActivity extends AppCompatActivity {

    private EditText titleEditText, bodyEditText;
    private Button saveButton, cancelButton;
    private int noteIndex = -1; // -1 means new note

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        titleEditText = findViewById(R.id.titleEditText);
        bodyEditText  = findViewById(R.id.bodyEditText);
        saveButton    = findViewById(R.id.saveButton);
        cancelButton  = findViewById(R.id.cancelButton);

        // Check if this is editing an existing note
        Intent intent = getIntent();
        if (intent != null) {
            String existingTitle = intent.getStringExtra("note_title");
            String existingBody  = intent.getStringExtra("note_body");
            noteIndex = intent.getIntExtra("note_index", -1);

            if (existingTitle != null) titleEditText.setText(existingTitle);
            if (existingBody != null) bodyEditText.setText(existingBody);
        }

        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String body  = bodyEditText.getText().toString().trim();

            Intent data = new Intent();
            data.putExtra("note_title", title);
            data.putExtra("note_body", body);
            data.putExtra("note_index", noteIndex);
            setResult(RESULT_OK, data);
            finish();
        });

        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}
