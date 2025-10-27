package edu.usc.cs310.anchornotes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import edu.usc.cs310.anchornotes.R;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.viewmodel.NoteViewModel;

public class MainActivity extends AppCompatActivity {

    private NoteViewModel viewModel;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> noteTitles;
    private List<Note> currentNotes;

    private final ActivityResultLauncher<Intent> editorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String title = result.getData().getStringExtra("note_title");
                    String body = result.getData().getStringExtra("note_body");
                    int index = result.getData().getIntExtra("note_index", -1);

                    if (index >= 0) {
                        // Editing existing note
                        currentNotes.get(index).setTitle(title);
                        currentNotes.get(index).setBody(body);
                        viewModel.updateNotes(currentNotes);
                    } else {
                        // Creating new note
                        viewModel.addNote(title, body);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView notesList = findViewById(R.id.notesList);
        FloatingActionButton addButton = findViewById(R.id.addButton);

        noteTitles = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, noteTitles);
        notesList.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        viewModel.getNotesLiveData().observe(this, notes -> {
            currentNotes = notes;
            noteTitles.clear();
            for (Note note : notes) {
                noteTitles.add(note.getTitle().isEmpty() ? "(Untitled)" : note.getTitle());
            }
            adapter.notifyDataSetChanged();
        });

        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditorActivity.class);
            editorLauncher.launch(intent);
        });

        notesList.setOnItemClickListener((parent, view, position, id) -> {
            Note note = currentNotes.get(position);
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("note_title", note.getTitle());
            intent.putExtra("note_body", note.getBody());
            intent.putExtra("note_index", position);
            editorLauncher.launch(intent);
        });
    }
}
