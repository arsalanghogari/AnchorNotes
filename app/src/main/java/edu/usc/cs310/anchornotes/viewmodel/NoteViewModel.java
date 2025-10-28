package edu.usc.cs310.anchornotes.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.repository.NotesRepository;

public class NoteViewModel extends AndroidViewModel {
    private final NotesRepository repository;
    private final LiveData<List<Note>> allNotes;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NotesRepository(application);
        allNotes = repository.getAllNotes();
    }

    public LiveData<List<Note>> getNotesLiveData() { return allNotes; }

    public void addNote(String title, String body) { repository.insert(new Note(title, body)); }

    public void update(Note note) { repository.update(note); }

    public void delete(Note note) { repository.delete(note); }
}