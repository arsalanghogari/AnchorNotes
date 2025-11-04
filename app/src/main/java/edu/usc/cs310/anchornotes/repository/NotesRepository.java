package edu.usc.cs310.anchornotes.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.core.util.Consumer;
import androidx.lifecycle.LiveData;
import java.util.List;
import edu.usc.cs310.anchornotes.database.AppDatabase;
import edu.usc.cs310.anchornotes.database.NoteDao;
import edu.usc.cs310.anchornotes.database.TagDao;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.Tag;
import edu.usc.cs310.anchornotes.model.NoteTag;

public class NotesRepository {
    private final NoteDao noteDao;
    private final TagDao tagDao;
    private final LiveData<List<Note>> allNotes;
    private final LiveData<List<Note>> relevantNotes;

    public NotesRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        noteDao = db.noteDao();
        tagDao = db.tagDao();
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
    public void insertTag(Tag tag, Consumer<Long> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long newId = tagDao.insert(tag);
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(newId));
        });
    }

    public LiveData<List<Tag>> getAllTags() {
        return tagDao.getAllTags();
    }

    public LiveData<List<Tag>> getTagsForNote(int noteId) {
        return tagDao.getTagsForNote(noteId);
    }

    public void assignTagToNote(int noteId, int tagId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tagDao.insert(new NoteTag(noteId, tagId));
        });
    }

    public void removeTagFromNote(int noteId, int tagId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tagDao.deleteByNoteAndTag(noteId, tagId);
        });
    }

    public void deleteTag(Tag tag) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            tagDao.delete(tag);
        });
    }

    public LiveData<List<Note>> getNotesByTag(int tagId) {
        return noteDao.getNotesByTag(tagId);
    }

}