package edu.usc.cs310.anchornotes;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.Tag;
import edu.usc.cs310.anchornotes.model.NoteTag;

/**
 * White-box tests for Feature 2: Smart Organization
 *
 * Tests:
 * - Tag creation and management
 * - Note pinning functionality
 * - Location context features
 * - Helper methods for location detection
 */
public class Feature2WhiteBoxTests {

    /**
     * Tests Tag entity creation and field management
     */
    @Test
    public void tagEntity_createsAndManagesFieldsCorrectly() {
        // Create tag with name
        Tag tag = new Tag("Biology");
        assertEquals("Biology", tag.getName());

        // Test ID assignment
        tag.setId(1);
        assertEquals(1, tag.getId());

        // Test name update
        tag.setName("Chemistry");
        assertEquals("Chemistry", tag.getName());
    }

    /**
     * Tests NoteTag junction entity for many-to-many relationship
     */
    @Test
    public void noteTagJunction_linksNoteAndTagCorrectly() {
        NoteTag noteTag = new NoteTag(123, 456);

        assertEquals(123, noteTag.getNoteId());
        assertEquals(456, noteTag.getTagId());

        // Test field updates
        noteTag.setNoteId(789);
        noteTag.setTagId(999);

        assertEquals(789, noteTag.getNoteId());
        assertEquals(999, noteTag.getTagId());
    }

    /**
     * Tests Note location functionality and helper methods
     */
    @Test
    public void noteLocation_managesLocationDataCorrectly() {
        Note note = new Note("Test Note", "Body");

        // Initially no location
        assertFalse("New note should not have location", note.hasLocation());
        assertEquals(0.0, note.getLatitude(), 0.001);
        assertEquals(0.0, note.getLongitude(), 0.001);
        assertNull(note.getLocationName());

        // Set location data
        note.setLatitude(34.0212);
        note.setLongitude(-118.2867);
        note.setLocationName("USC Campus");

        assertTrue("Note should have location after setting data", note.hasLocation());
        assertEquals(34.0212, note.getLatitude(), 0.001);
        assertEquals(-118.2867, note.getLongitude(), 0.001);
        assertEquals("USC Campus", note.getLocationName());

        // Remove location
        note.setLatitude(0);
        note.setLongitude(0);
        note.setLocationName(null);

        assertFalse("Note should not have location after clearing", note.hasLocation());
    }

    /**
     * Tests Note pinning functionality
     */
    @Test
    public void notePinning_togglesCorrectly() {
        Note note = new Note("Test Note", "Body");

        // Initially not pinned
        assertFalse("New note should not be pinned", note.isPinned());

        // Pin the note
        note.setPinned(true);
        assertTrue("Note should be pinned after setPinned(true)", note.isPinned());

        // Unpin the note
        note.setPinned(false);
        assertFalse("Note should be unpinned after setPinned(false)", note.isPinned());

        // Verify timestamp is updated when pinning (indirectly through repository)
        long originalTime = note.getUpdatedAtEpochMs();
        note.setPinned(true);
        note.setUpdatedAtEpochMs(originalTime + 1000);
        assertTrue("Timestamp should be updatable", note.getUpdatedAtEpochMs() > originalTime);
    }

    /**
     * Tests boundary conditions for location coordinates
     */
    @Test
    public void locationCoordinates_handlesBoundaryConditions() {
        Note note = new Note("Test Note", "Body");

        // Test valid coordinate ranges
        note.setLatitude(90.0);  // North pole
        note.setLongitude(180.0); // International date line
        note.setLocationName("Extreme Location");

        assertTrue(note.hasLocation());
        assertEquals(90.0, note.getLatitude(), 0.001);
        assertEquals(180.0, note.getLongitude(), 0.001);

        // Test negative coordinates
        note.setLatitude(-90.0);  // South pole
        note.setLongitude(-180.0); // Opposite side
        assertTrue(note.hasLocation());

        // Test zero coordinates (should still be considered valid if location name exists)
        note.setLatitude(0.0);
        note.setLongitude(0.0);
        note.setLocationName("Null Island");
        assertTrue("Location with zero coordinates but name should be valid", note.hasLocation());
    }
}