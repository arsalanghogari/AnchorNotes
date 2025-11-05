package edu.usc.cs310.anchornotes.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.core.util.Consumer;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import edu.usc.cs310.anchornotes.database.AppDatabase;
import edu.usc.cs310.anchornotes.database.NoteDao;
import edu.usc.cs310.anchornotes.database.TagDao;
import edu.usc.cs310.anchornotes.database.TemplateDao;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.NoteTag;
import edu.usc.cs310.anchornotes.model.Tag;
import edu.usc.cs310.anchornotes.model.Template;

public class NotesRepository {
    private final NoteDao noteDao;
    private final TagDao tagDao;
    private final LiveData<List<Note>> allNotes;
    private final LiveData<List<Note>> relevantNotes;
    private final TemplateDao templateDao;
    private final LiveData<List<Template>> allTemplates;

    public NotesRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        noteDao = db.noteDao();
        tagDao = db.tagDao();
        templateDao = db.templateDao();
        allNotes = noteDao.getAllNotesOrdered();
        relevantNotes = noteDao.getRelevantNotes();
        allTemplates = templateDao.getAllTemplates();
        ensureDefaultTemplateExists();
    }

    public LiveData<List<Note>> getAllNotes() { return allNotes; }

    public LiveData<List<Note>> getRelevantNotes() { return relevantNotes; }

    public LiveData<List<Template>> getAllTemplatesLiveData() { return allTemplates; }

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

    public LiveData<List<Note>> getPinnedNotes() {
        return noteDao.getPinnedNotes();
    }

    public void insertTemplate(Template template, Consumer<Long> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long newId = templateDao.insert(template);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) {
                    callback.accept(newId);
                }
            });
        });
    }

    public void updateTemplate(Template template) {
        AppDatabase.databaseWriteExecutor.execute(() -> templateDao.update(template));
    }

    public void deleteTemplate(Template template) {
        AppDatabase.databaseWriteExecutor.execute(() -> templateDao.delete(template));
    }

    public void getTemplateById(int id, Consumer<Template> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Template template = templateDao.getById(id);
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(template));
        });
    }

    public void ensureTagsForNames(List<String> tagNames, Consumer<List<Tag>> callback) {
        if (tagNames == null || tagNames.isEmpty()) {
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(new ArrayList<>()));
            return;
        }
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Set<String> uniqueNames = new HashSet<>();
            List<Tag> resolved = new ArrayList<>();
            for (String rawName : tagNames) {
                if (rawName == null) continue;
                String trimmed = rawName.trim();
                String lowered = trimmed.toLowerCase(Locale.getDefault());
                if (trimmed.isEmpty() || !uniqueNames.add(lowered)) {
                    continue;
                }
                Tag existing = tagDao.getByName(trimmed);
                if (existing == null) {
                    Tag tag = new Tag(trimmed);
                    long newId = tagDao.insert(tag);
                    if (newId > 0) {
                        tag.setId((int) newId);
                        resolved.add(tag);
                    } else {
                        Tag fallback = tagDao.getByName(trimmed);
                        if (fallback != null) {
                            resolved.add(fallback);
                        }
                    }
                } else {
                    resolved.add(existing);
                }
            }
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(resolved));
        });
    }

    private void ensureDefaultTemplateExists() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (templateDao.countTemplates() == 0) {
                templateDao.insert(Template.createDefaultTemplate());
            }
        });
    }

}