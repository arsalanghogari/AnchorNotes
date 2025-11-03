package edu.usc.cs310.anchornotes.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.core.util.Consumer;
import androidx.lifecycle.LiveData;
import java.util.List;
import edu.usc.cs310.anchornotes.database.AppDatabase;
import edu.usc.cs310.anchornotes.database.NoteDao;
import edu.usc.cs310.anchornotes.model.Note;

public class NotesRepository {
    private final NoteDao noteDao;
    private final LiveData<List<Note>> allNotes;
    private final LiveData<List<Note>> relevantNotes;

    public NotesRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        noteDao = db.noteDao();
        allNotes = noteDao.getAllNotesOrdered();
        relevantNotes = noteDao.getRelevantNotes();
    }

    public LiveData<List<Note>> getAllNotes() { return allNotes; }

    public LiveData<List<Note>> getRelevantNotes() { return relevantNotes; }

    public void insert(Note note, Consumer<Long> callback) {
        note.setUpdatedAtEpochMs(System.currentTimeMillis());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long newId = noteDao.insert(note);
            // Post the result back to the main thread
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(newId));
        });
    }

    /**
     * Fetches a single note by its ID from the database on a background thread.
     * @param id The ID of the note to fetch.
     * @param callback The callback to be invoked on the main thread with the result.
     */
    public void getById(int id, Consumer<Note> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Note note = noteDao.getById(id);
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(note));
        });
    }

    public void update(Note note) {
        note.setUpdatedAtEpochMs(System.currentTimeMillis());
        AppDatabase.databaseWriteExecutor.execute(() -> noteDao.update(note));
    }

    public void delete(Note note) {
        AppDatabase.databaseWriteExecutor.execute(() -> noteDao.delete(note));
    }
}