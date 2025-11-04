package edu.usc.cs310.anchornotes.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "note_tags",
        primaryKeys = {"noteId", "tagId"},
        foreignKeys = {
                @ForeignKey(entity = Note.class,
                        parentColumns = "id",
                        childColumns = "noteId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Tag.class,
                        parentColumns = "id",
                        childColumns = "tagId",
                        onDelete = ForeignKey.CASCADE)
        })
public class NoteTag {
    private int noteId;
    private int tagId;

    public NoteTag(int noteId, int tagId) {
        this.noteId = noteId;
        this.tagId = tagId;
    }

    public int getNoteId() { return noteId; }
    public void setNoteId(int noteId) { this.noteId = noteId; }

    public int getTagId() { return tagId; }
    public void setTagId(int tagId) { this.tagId = tagId; }
}