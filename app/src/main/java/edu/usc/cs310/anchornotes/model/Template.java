package edu.usc.cs310.anchornotes.model;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "templates")
public class Template {

    public static final String DEFAULT_PAGE_COLOR = "#FFFFFF";

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String title;
    private String body;
    private String pageColor = DEFAULT_PAGE_COLOR;
    private String defaultTagsCsv;

    // Optional geofence association
    private Double geoLatitude;
    private Double geoLongitude;
    private Float geoRadius;
    private String geoLocationName;

    public Template() {
        // Required empty constructor for Room.
    }

    public static Template createDefaultTemplate() {
        Template template = new Template();
        template.setName("Blank Page");
        template.setTitle("");
        template.setBody("");
        template.setPageColor(DEFAULT_PAGE_COLOR);
        template.setDefaultTagsCsv(null);
        template.setGeoLatitude(null);
        template.setGeoLongitude(null);
        template.setGeoRadius(null);
        template.setGeoLocationName(null);
        return template;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getPageColor() {
        return pageColor;
    }

    public void setPageColor(String pageColor) {
        this.pageColor = TextUtils.isEmpty(pageColor) ? DEFAULT_PAGE_COLOR : pageColor;
    }

    public String getDefaultTagsCsv() {
        return defaultTagsCsv;
    }

    public void setDefaultTagsCsv(String defaultTagsCsv) {
        this.defaultTagsCsv = TextUtils.isEmpty(defaultTagsCsv) ? null : defaultTagsCsv;
    }

    public Double getGeoLatitude() {
        return geoLatitude;
    }

    public void setGeoLatitude(@Nullable Double geoLatitude) {
        this.geoLatitude = geoLatitude;
    }

    public Double getGeoLongitude() {
        return geoLongitude;
    }

    public void setGeoLongitude(@Nullable Double geoLongitude) {
        this.geoLongitude = geoLongitude;
    }

    public Float getGeoRadius() {
        return geoRadius;
    }

    public void setGeoRadius(@Nullable Float geoRadius) {
        this.geoRadius = geoRadius;
    }

    public String getGeoLocationName() {
        return geoLocationName;
    }

    public void setGeoLocationName(@Nullable String geoLocationName) {
        this.geoLocationName = geoLocationName;
    }

    public boolean hasGeofence() {
        return geoLatitude != null && geoLongitude != null && geoRadius != null && geoRadius > 0;
    }

    @NonNull
    public List<String> getDefaultTagNames() {
        if (TextUtils.isEmpty(defaultTagsCsv)) {
            return new ArrayList<>();
        }
        String[] parts = defaultTagsCsv.split(",");
        List<String> tags = new ArrayList<>();
        for (String part : parts) {
            if (!TextUtils.isEmpty(part)) {
                tags.add(part.trim());
            }
        }
        return tags;
    }
}

