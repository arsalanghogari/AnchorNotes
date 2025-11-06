package edu.usc.cs310.anchornotes.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import edu.usc.cs310.anchornotes.model.Note;

@Dao
public interface NoteDao {
    @Insert
    long insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAtEpochMs DESC, id DESC")
    LiveData<List<Note>> getAllNotesOrdered();

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getById(int id);

    // New query for Feature 4: Contextual Reminders
    @Query("SELECT * FROM notes WHERE isRelevant = 1 ORDER BY updatedAtEpochMs DESC")
    LiveData<List<Note>> getRelevantNotes();

    @Query("SELECT n.* FROM notes n " +
            "INNER JOIN note_tags nt ON n.id = nt.noteId " +
            "WHERE nt.tagId = :tagId " +
            "ORDER BY n.isPinned DESC, n.updatedAtEpochMs DESC, n.id DESC")
    LiveData<List<Note>> getNotesByTag(int tagId);

    @Query("SELECT * FROM notes WHERE isPinned = 1 ORDER BY updatedAtEpochMs DESC")
    LiveData<List<Note>> getPinnedNotes();

    @Query("UPDATE notes SET isRelevant = :isRelevant WHERE id = :noteId")
    void setNoteRelevance(int noteId, boolean isRelevant);

    @Query("SELECT * FROM notes " +
            "WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' " +
            "ORDER BY isPinned DESC, updatedAtEpochMs DESC, id DESC")
    LiveData<List<Note>> searchNotes(String query);

    @Query("SELECT * FROM notes " +
            "WHERE (:query IS NULL OR title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%') " +
            "AND (:tagId IS NULL OR id IN (SELECT noteId FROM note_tags WHERE tagId = :tagId)) " +
            "AND (:startDate IS NULL OR updatedAtEpochMs >= :startDate) " +
            "AND (:endDate IS NULL OR updatedAtEpochMs <= :endDate) " +
            "AND (:hasPhoto = 0 OR (photoUri IS NOT NULL AND photoUri != '')) " +
            "AND (:hasVoice = 0 OR (voiceUri IS NOT NULL AND voiceUri != '')) " +
            "AND (:hasLocation = 0 OR (latitude != 0 OR longitude != 0)) " +
            "ORDER BY updatedAtEpochMs DESC")
    LiveData<List<Note>> filterAndSearchNotes(
            String query,
            Integer tagId,
            Long startDate,
            Long endDate,
            int hasPhoto,
            int hasVoice,
            int hasLocation
    );


}