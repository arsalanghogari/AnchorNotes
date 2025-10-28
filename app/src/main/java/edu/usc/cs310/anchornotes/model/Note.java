package edu.usc.cs310.anchornotes.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String body;

    // For 2.1 Note Storage: order by “most recently accessed/updated”.
    private long updatedAtEpochMs;

    // Simple pin flag reserved for organization; harmless for storage MVP
    private boolean isPinned;

    public Note(String title, String body) {
        this.title = title;
        this.body = body;
        this.updatedAtEpochMs = System.currentTimeMillis();
        this.isPinned = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getUpdatedAtEpochMs() { return updatedAtEpochMs; }
    public void setUpdatedAtEpochMs(long updatedAtEpochMs) { this.updatedAtEpochMs = updatedAtEpochMs; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
}