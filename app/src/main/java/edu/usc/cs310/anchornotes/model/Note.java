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
    private boolean isRelevant;
    private String pageColor = Template.DEFAULT_PAGE_COLOR;
    private int templateId;

    // --- CONTEXTUAL LOCATION FIELDS ---
    // (Data about the note itself)
    private double latitude;
    private double longitude;
    private String locationName;

    // --- REMINDER ACTION FIELDS ---
    private String reminderType; // "time" or "geofence"
    private long reminderTime;   // For time reminders

    // --- NEW, SEPARATE FIELDS FOR GEOFENCE REMINDERS ---
    private double reminderLatitude;
    private double reminderLongitude;
    private String reminderLocationName;
    private float radius;

    public Note(String title, String body) {
        this.title = title;
        this.body = body;
        this.updatedAtEpochMs = System.currentTimeMillis();
        // Default values for all other fields will be 0 or null
        this.pageColor = Template.DEFAULT_PAGE_COLOR;
        this.templateId = 0;
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
    public void setPinned(boolean pinned) { this.isPinned = pinned; }
    public boolean isRelevant() { return isRelevant; }
    public void setRelevant(boolean relevant) { this.isRelevant = relevant; }

    public String getPageColor() { return pageColor; }
    public void setPageColor(String pageColor) { this.pageColor = (pageColor == null || pageColor.isEmpty()) ? Template.DEFAULT_PAGE_COLOR : pageColor; }

    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }

    // --- Contextual Location Getters/Setters ---
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    // --- Reminder Action Getters/Setters ---
    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }
    public long getReminderTime() { return reminderTime; }
    public void setReminderTime(long reminderTime) { this.reminderTime = reminderTime; }
    public double getReminderLatitude() { return reminderLatitude; }
    public void setReminderLatitude(double reminderLatitude) { this.reminderLatitude = reminderLatitude; }
    public double getReminderLongitude() { return reminderLongitude; }
    public void setReminderLongitude(double reminderLongitude) { this.reminderLongitude = reminderLongitude; }
    public String getReminderLocationName() { return reminderLocationName; }
    public void setReminderLocationName(String reminderLocationName) { this.reminderLocationName = reminderLocationName; }
    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }

    // --- Helper Methods ---
    public boolean hasLocation() {
        return this.latitude != 0 || this.longitude != 0;
    }
    public boolean hasGeofenceReminder() {
        return "geofence".equals(this.reminderType) && (this.reminderLatitude != 0 || this.reminderLongitude != 0);
    }
    public String getDisplayTitle() {
        return (title == null || title.isEmpty()) ? "(Untitled)" : title;
    }
}