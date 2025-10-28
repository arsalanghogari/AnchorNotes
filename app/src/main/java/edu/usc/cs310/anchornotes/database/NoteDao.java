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
    void insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAtEpochMs DESC, id DESC")
    LiveData<List<Note>> getAllNotesOrdered();

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getById(int id);
}