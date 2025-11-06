package edu.usc.cs310.anchornotes.model;

public class NoteFilter {
    private Integer tagId;
    private Long startDate;
    private Long endDate;
    private boolean hasPhoto;
    private boolean hasVoice;
    private boolean hasLocation;

    public NoteFilter() { }

    public NoteFilter(Integer tagId, Long startDate, Long endDate,
                      boolean hasPhoto, boolean hasVoice, boolean hasLocation) {
        this.tagId = tagId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.hasPhoto = hasPhoto;
        this.hasVoice = hasVoice;
        this.hasLocation = hasLocation;
    }

    // Getters and setters
    public Integer getTagId() { return tagId; }
    public void setTagId(Integer tagId) { this.tagId = tagId; }

    public Long getStartDate() { return startDate; }
    public void setStartDate(Long startDate) { this.startDate = startDate; }

    public Long getEndDate() { return endDate; }
    public void setEndDate(Long endDate) { this.endDate = endDate; }

    public boolean hasPhoto() { return hasPhoto; }
    public void setHasPhoto(boolean hasPhoto) { this.hasPhoto = hasPhoto; }

    public boolean hasVoice() { return hasVoice; }
    public void setHasVoice(boolean hasVoice) { this.hasVoice = hasVoice; }

    public boolean hasLocation() { return hasLocation; }
    public void setHasLocation(boolean hasLocation) { this.hasLocation = hasLocation; }
}

