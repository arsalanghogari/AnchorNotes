package edu.usc.cs310.anchornotes.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import edu.usc.cs310.anchornotes.model.Tag;
import edu.usc.cs310.anchornotes.model.NoteTag;

@Dao
public interface TagDao {
    @Insert
    long insert(Tag tag);

    @Delete
    void delete(Tag tag);

    @Query("SELECT * FROM tags ORDER BY name")
    LiveData<List<Tag>> getAllTags();

    @Query("SELECT * FROM tags WHERE id = :id")
    Tag getById(int id);

    @Query("SELECT * FROM tags WHERE name = :name")
    Tag getByName(String name);

    // NoteTag methods
    @Insert
    void insert(NoteTag noteTag);

    @Delete
    void delete(NoteTag noteTag);

    @Query("DELETE FROM note_tags WHERE noteId = :noteId AND tagId = :tagId")
    void deleteByNoteAndTag(int noteId, int tagId);

    @Query("SELECT tagId FROM note_tags WHERE noteId = :noteId")
    List<Integer> getTagIdsForNote(int noteId);

    @Transaction
    @Query("SELECT * FROM tags WHERE id IN (SELECT tagId FROM note_tags WHERE noteId = :noteId)")
    LiveData<List<Tag>> getTagsForNote(int noteId);
}