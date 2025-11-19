package edu.usc.cs310.anchornotes;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.Template;

/**
 * White-box tests for Feature 1 core note behavior.
 *
 * Focuses ONLY on:
 *  - title / body
 *  - updatedAtEpochMs
 *  - default flags (pinned, relevant)
 *  - pageColor + defaulting logic
 *  - templateId default
 *  - display title helper
 *  - photo / voice URI fields staying separate
 *
 * No DB / Room / repository / geofence stuff here.
 */
public class Feature1WhiteBoxTests {

    /**
     * Constructor should set title, body, and a non-zero updatedAtEpochMs.
     * It should also initialize defaults for flags and template/page color.
     */
    @Test
    public void constructor_setsTitleBodyAndDefaults() {
        long before = System.currentTimeMillis();
        Note note = new Note("My title", "My body");
        long after = System.currentTimeMillis();

        // Title / body
        assertEquals("My title", note.getTitle());
        assertEquals("My body", note.getBody());

        // updatedAtEpochMs is set in constructor and within a reasonable range
        long updated = note.getUpdatedAtEpochMs();
        assertTrue("updatedAtEpochMs should be >= before", updated >= before);
        assertTrue("updatedAtEpochMs should be <= after", updated <= after);

        // Feature 1 flags + template defaults
        assertFalse("Notes should not be pinned by default", note.isPinned());
        assertFalse("Notes should not be marked relevant by default", note.isRelevant());
        assertEquals("Default templateId should be 0", 0, note.getTemplateId());

        // Page color default: Template.DEFAULT_PAGE_COLOR
        assertEquals("Page color should default to Template.DEFAULT_PAGE_COLOR",
                Template.DEFAULT_PAGE_COLOR, note.getPageColor());

        // Photo / voice URIs should start null
        assertNull("Photo URI should be null by default", note.getPhotoUri());
        assertNull("Voice URI should be null by default", note.getVoiceUri());
    }

    /**
     * getDisplayTitle() should return the title if present,
     * or "(Untitled)" if the title is null or empty.
     */
    @Test
    public void getDisplayTitle_handlesNullAndEmptyTitles() {
        // Normal title
        Note withTitle = new Note("Actual title", "body");
        assertEquals("Actual title", withTitle.getDisplayTitle());

        // Empty title
        Note emptyTitle = new Note("", "body");
        assertEquals("(Untitled)", emptyTitle.getDisplayTitle());

        // Null title
        Note nullTitle = new Note("temp", "body");
        nullTitle.setTitle(null);
        assertEquals("(Untitled)", nullTitle.getDisplayTitle());
    }

    /**
     * setPageColor(null) or setPageColor("") should reset to DEFAULT_PAGE_COLOR.
     */
    @Test
    public void setPageColor_nullOrEmptyResetsToDefault() {
        Note note = new Note("Title", "Body");

        // Explicitly set some non-default first
        note.setPageColor("#FF0000");
        assertEquals("#FF0000", note.getPageColor());

        // Passing null -> should go back to default
        note.setPageColor(null);
        assertEquals("null page color should revert to default",
                Template.DEFAULT_PAGE_COLOR, note.getPageColor());

        // Passing empty string -> should also go back to default
        note.setPageColor("");
        assertEquals("empty page color should revert to default",
                Template.DEFAULT_PAGE_COLOR, note.getPageColor());
    }

    /**
     * setPageColor with a non-empty value should use that exact value.
     */
    @Test
    public void setPageColor_nonEmptyUsesGivenColor() {
        Note note = new Note("Title", "Body");

        note.setPageColor("#ABCDEF");
        assertEquals("#ABCDEF", note.getPageColor());

        note.setPageColor("#123456");
        assertEquals("#123456", note.getPageColor());
    }

    /**
     * photoUri and voiceUri should be independent fields.
     * Setting one should not affect the other.
     */
    @Test
    public void photoAndVoiceUris_areIndependent() {
        Note note = new Note("Title", "Body");

        // Initially null
        assertNull(note.getPhotoUri());
        assertNull(note.getVoiceUri());

        // Set photo only
        note.setPhotoUri("content://photos/1");
        assertEquals("content://photos/1", note.getPhotoUri());
        assertNull("Voice URI must remain null when only photo is set", note.getVoiceUri());

        // Set voice only
        note.setVoiceUri("content://voice/clip.m4a");
        assertEquals("content://photos/1", note.getPhotoUri());
        assertEquals("content://voice/clip.m4a", note.getVoiceUri());

        // Overwrite photo, voice stays the same
        note.setPhotoUri("content://photos/2");
        assertEquals("content://photos/2", note.getPhotoUri());
        assertEquals("content://voice/clip.m4a", note.getVoiceUri());
    }

    /**
     * updatedAtEpochMs should be explicitly changeable and stored.
     * (Repository will set this before DB writes; this just checks the field itself.)
     */
    @Test
    public void setUpdatedAtEpochMs_overwritesTimestamp() {
        Note note = new Note("Title", "Body");
        long original = note.getUpdatedAtEpochMs();

        long newTime = original + 10_000L;
        note.setUpdatedAtEpochMs(newTime);

        assertEquals("updatedAtEpochMs should match the new value",
                newTime, note.getUpdatedAtEpochMs());
    }

    /**
     * Pinned / relevant flags should be simple booleans that store correctly.
     */
    @Test
    public void pinnedAndRelevantFlags_storeCorrectly() {
        Note note = new Note("Title", "Body");

        assertFalse(note.isPinned());
        assertFalse(note.isRelevant());

        note.setPinned(true);
        note.setRelevant(true);

        assertTrue(note.isPinned());
        assertTrue(note.isRelevant());

        note.setPinned(false);
        note.setRelevant(false);

        assertFalse(note.isPinned());
        assertFalse(note.isRelevant());
    }
}
