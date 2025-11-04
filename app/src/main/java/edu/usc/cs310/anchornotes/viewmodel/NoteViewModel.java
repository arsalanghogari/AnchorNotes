package edu.usc.cs310.anchornotes.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.Tag;
import edu.usc.cs310.anchornotes.repository.NotesRepository;

public class NoteViewModel extends AndroidViewModel {
    private final NotesRepository repository;
    private final LiveData<List<Note>> notesLiveData;
    private final LiveData<List<Note>> relevantNotesLiveData;
    private final LiveData<List<Tag>> allTags;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NotesRepository(application);
        notesLiveData = repository.getAllNotes();
        relevantNotesLiveData = repository.getRelevantNotes();
        allTags = repository.getAllTags();
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
    public LiveData<List<Tag>> getAllTags() {
        return allTags;
    }

    public void insertTag(Tag tag, Consumer<Long> callback) {
        repository.insertTag(tag, callback);
    }

    public LiveData<List<Tag>> getTagsForNote(int noteId) {
        return repository.getTagsForNote(noteId);
    }

    public void assignTagToNote(int noteId, int tagId) {
        repository.assignTagToNote(noteId, tagId);
    }

    public void removeTagFromNote(int noteId, int tagId) {
        repository.removeTagFromNote(noteId, tagId);
    }

    public void deleteTag(Tag tag) {
        repository.deleteTag(tag);
    }
    public LiveData<List<Note>> getNotesByTag(int tagId) {
        return repository.getNotesByTag(tagId);
    }
}