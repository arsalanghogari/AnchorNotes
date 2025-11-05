package edu.usc.cs310.anchornotes.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String body;

    private long updatedAtEpochMs;
    private boolean isPinned;

    // --- Reminder Fields ---
    private String reminderType;
    private long reminderTime;
    private double latitude;
    private double longitude;
    private float radius;
    private boolean isRelevant;

    // --- Location Fields (for context) ---
    private String locationName;

    public Note(String title, String body) {
        this.title = title;
        this.body = body;
        this.updatedAtEpochMs = System.currentTimeMillis();
        this.isPinned = false;
        this.isRelevant = false;
        this.latitude = 0;
        this.longitude = 0;
    }

    // --- Getters and Setters ---
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

    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }

    public long getReminderTime() { return reminderTime; }
    public void setReminderTime(long reminderTime) { this.reminderTime = reminderTime; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }

    public boolean isRelevant() { return isRelevant; }
    public void setRelevant(boolean relevant) { isRelevant = relevant; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    // --- Helper Methods ---
    public boolean hasLocation() {
        return latitude != 0 && longitude != 0 && locationName != null;
    }

    public String getDisplayTitle() {
        return title.isEmpty() ? "(Untitled)" : title;
    }
}