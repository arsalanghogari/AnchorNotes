package edu.usc.cs310.anchornotes.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import edu.usc.cs310.anchornotes.database.AppDatabase;
import edu.usc.cs310.anchornotes.database.NoteDao;
import edu.usc.cs310.anchornotes.model.Note;

public class NotesRepository {
    private final NoteDao noteDao;
    private final LiveData<List<Note>> allNotes;

    public NotesRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        noteDao = db.noteDao();
        allNotes = noteDao.getAllNotesOrdered();
    }

    public LiveData<List<Note>> getAllNotes() { return allNotes; }

    public void insert(Note note) {
        note.setUpdatedAtEpochMs(System.currentTimeMillis());
        AppDatabase.databaseWriteExecutor.execute(() -> noteDao.insert(note));
    }

    public void update(Note note) {
        note.setUpdatedAtEpochMs(System.currentTimeMillis());
        AppDatabase.databaseWriteExecutor.execute(() -> noteDao.update(note));
    }

    public void delete(Note note) {
        AppDatabase.databaseWriteExecutor.execute(() -> noteDao.delete(note));
    }
}