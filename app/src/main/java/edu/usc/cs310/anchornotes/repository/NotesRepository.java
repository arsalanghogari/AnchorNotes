package edu.usc.cs310.anchornotes.repository;

import java.util.ArrayList;
import java.util.List;
import edu.usc.cs310.anchornotes.model.Note;

/**
 * Temporary in-memory repository.
 * To be later replaced with database.
 */
public class NotesRepository {

    private static NotesRepository instance;
    private final List<Note> notes = new ArrayList<>();

    private NotesRepository() {}

    public static synchronized NotesRepository getInstance() {
        if (instance == null) {
            instance = new NotesRepository();
        }
        return instance;
    }

    public void addNote(Note note) {
        notes.add(note);
    }

    public List<Note> getAllNotes() {
        return new ArrayList<>(notes);
    }
}
