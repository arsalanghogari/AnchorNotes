package edu.usc.cs310.anchornotes.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.repository.NotesRepository;

public class NoteViewModel extends AndroidViewModel {
    private final NotesRepository repository;
    private final LiveData<List<Note>> notesLiveData;
    private final LiveData<List<Note>> relevantNotesLiveData;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NotesRepository(application);
        notesLiveData = repository.getAllNotes();
        relevantNotesLiveData = repository.getRelevantNotes();
    }

    public LiveData<List<Note>> getNotesLiveData() {
        return notesLiveData;
    }

    public LiveData<List<Note>> getRelevantNotesLiveData() {
        return relevantNotesLiveData;
    }

    public void insert(Note note, Consumer<Long> callback) {
        repository.insert(note, callback);
    }

    /**
     * Exposes the repository's method to get a single note by its ID.
     */
    public void getById(int id, Consumer<Note> callback) {
        repository.getById(id, callback);
    }

    public void update(Note note) {
        repository.update(note);
    }
}