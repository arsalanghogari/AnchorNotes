package edu.usc.cs310.anchornotes;

import static org.junit.Assert.*;

import org.junit.Test;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.Template;

/**
 * White-box tests for the Note Reminders and Note Map features.
 *
 * This test class focuses ONLY on the internal logic of the Note and Template models.
 * It verifies:
 *  - Default values for reminder and location fields.
 *  - Correct behavior of setters for time-based vs. location-based reminders.
 *  - Logic of helper methods like hasLocation() and hasGeofenceReminder().
 *  - Independence of contextual location fields vs. reminder location fields.
 *  - Geofence-related logic within the Template model.
 *
 * No DB, Mockito, or Android framework classes are used here.
 */
public class Feature4WhiteBoxTests {

    /**
     * Test 1: A new Note should have null or zeroed-out values for all
     * reminder and location-specific fields by default.
     */
    @Test
    public void constructor_initializesReminderAndLocationFieldsToDefaults() {
        Note note = new Note("Test Title", "Test Body");

        // Contextual location fields
        assertEquals(0.0, note.getLatitude(), 0.0);
        assertEquals(0.0, note.getLongitude(), 0.0);
        assertNull("Context location name should be null by default", note.getLocationName());

        // Reminder fields
        assertNull("Reminder type should be null by default", note.getReminderType());
        assertEquals("Reminder time should be 0 by default", 0L, note.getReminderTime());
        assertEquals(0.0, note.getReminderLatitude(), 0.0);
        assertEquals(0.0, note.getReminderLongitude(), 0.0);
        assertNull("Reminder location name should be null by default", note.getReminderLocationName());
        assertEquals(0.0f, note.getRadius(), 0.0f);
    }

    /**
     * Test 2: The hasLocation() helper method should correctly reflect the state
     * of the note's *contextual* latitude and longitude, not the reminder coordinates.
     */
    @Test
    public void hasLocation_reflectsContextualCoordinatesOnly() {
        Note note = new Note("Test Title", "Test Body");

        assertFalse("Note should not have a location by default", note.hasLocation());

        // Set contextual location
        note.setLatitude(34.0224);
        assertTrue("hasLocation should be true when latitude is set", note.hasLocation());
        note.setLatitude(0.0);
        assertFalse("hasLocation should be false when latitude is reset to 0", note.hasLocation());

        note.setLongitude(-118.2851);
        assertTrue("hasLocation should be true when longitude is set", note.hasLocation());

        // Set reminder location - should have no effect on hasLocation()
        note.setReminderLatitude(40.7128);
        assertTrue("hasLocation should still be true and unaffected by reminder coordinates", note.hasLocation());

        // Reset contextual location
        note.setLongitude(0.0);
        assertFalse("hasLocation should be false when contextual coordinates are cleared", note.hasLocation());
    }

    /**
     * Test 3: The hasGeofenceReminder() helper should only return true if the reminderType
     * is "geofence" AND reminder coordinates are non-zero.
     */
    @Test
    public void hasGeofenceReminder_validatesTypeAndCoordinates() {
        Note note = new Note("Test Title", "Test Body");

        assertFalse("Should be false with default values", note.hasGeofenceReminder());

        // Set type to "geofence" but no coordinates
        note.setReminderType("geofence");
        assertFalse("Should be false if type is correct but coordinates are zero", note.hasGeofenceReminder());

        // Set coordinates but wrong type
        note.setReminderType("time");
        note.setReminderLatitude(34.0224);
        assertFalse("Should be false if coordinates are set but type is 'time'", note.hasGeofenceReminder());

        // Set both correctly
        note.setReminderType("geofence");
        assertTrue("Should be true when type is 'geofence' and coordinates are set", note.hasGeofenceReminder());
    }

    /**
     * Test 4: Setting a time-based reminder should correctly populate time fields
     * while leaving geofence-related fields at their default values.
     */
    @Test
    public void setTimeReminder_populatesOnlyTimeFields() {
        Note note = new Note("Test Title", "Test Body");
        long reminderTime = System.currentTimeMillis() + 100000;

        note.setReminderType("time");
        note.setReminderTime(reminderTime);

        assertEquals("time", note.getReminderType());
        assertEquals(reminderTime, note.getReminderTime());

        // Assert that geofence fields were not touched
        assertEquals(0.0, note.getReminderLatitude(), 0.0);
        assertEquals(0.0, note.getReminderLongitude(), 0.0);
        assertNull(note.getReminderLocationName());
        assertEquals(0.0f, note.getRadius(), 0.0f);
    }

    /**
     * Test 5: Setting a geofence-based reminder should correctly populate location fields
     * while leaving the time-based reminder field at its default value.
     */
    @Test
    public void setGeofenceReminder_populatesOnlyGeofenceFields() {
        Note note = new Note("Test Title", "Test Body");

        note.setReminderType("geofence");
        note.setReminderLatitude(34.0224);
        note.setReminderLongitude(-118.2851);
        note.setReminderLocationName("USC");
        note.setRadius(50.0f);

        assertEquals("geofence", note.getReminderType());
        assertEquals(34.0224, note.getReminderLatitude(), 0.0);
        assertEquals(-118.2851, note.getReminderLongitude(), 0.0);
        assertEquals("USC", note.getReminderLocationName());
        assertEquals(50.0f, note.getRadius(), 0.0f);

        // Assert that time field was not touched
        assertEquals(0L, note.getReminderTime());
    }

    /**
     * Test 6: A note's contextual location (latitude/longitude) must be independent
     * of its reminder location (reminderLatitude/reminderLongitude).
     */
    @Test
    public void contextualAndReminderLocations_areIndependent() {
        Note note = new Note("Test Title", "Test Body");

        // Set contextual location
        note.setLatitude(34.0224);
        note.setLongitude(-118.2851);
        note.setLocationName("USC");

        // Set reminder location
        note.setReminderLatitude(40.7128);
        note.setReminderLongitude(-74.0060);
        note.setReminderLocationName("New York City");

        // Verify contextual location is unchanged
        assertEquals(34.0224, note.getLatitude(), 0.0);
        assertEquals(-118.2851, note.getLongitude(), 0.0);
        assertEquals("USC", note.getLocationName());

        // Verify reminder location is also correct
        assertEquals(40.7128, note.getReminderLatitude(), 0.0);
        assertEquals(-74.0060, note.getReminderLongitude(), 0.0);
        assertEquals("New York City", note.getReminderLocationName());
    }

    /**
     * Test 7: The 'isRelevant' flag, which is set by reminders, should function
     * as a simple boolean that can be toggled correctly.
     */
    @Test
    public void isRelevantFlag_storesCorrectly() {
        Note note = new Note("Title", "Body");

        assertFalse("isRelevant should be false by default", note.isRelevant());

        note.setRelevant(true);
        assertTrue("isRelevant should be true after setting", note.isRelevant());

        note.setRelevant(false);
        assertFalse("isRelevant should be false after resetting", note.isRelevant());
    }

    /**
     * Test 8: A newly created Template should have null geofence fields.
     */
    @Test
    public void template_constructor_initializesGeofenceFieldsToNull() {
        Template template = new Template();

        assertNull(template.getGeoLatitude());
        assertNull(template.getGeoLongitude());
        assertNull(template.getGeoRadius());
        assertNull(template.getGeoLocationName());
    }

    /**
     * Test 9: The Template.hasGeofence() helper must only return true if latitude,
     * longitude, AND a radius greater than 0 are all set.
     */
    @Test
    public void template_hasGeofence_validatesAllFields() {
        Template template = new Template();

        assertFalse("Should be false by default", template.hasGeofence());

        // Set location but not radius
        template.setGeoLatitude(34.0224);
        template.setGeoLongitude(-118.2851);
        assertFalse("Should be false without a radius", template.hasGeofence());

        // Set radius to zero
        template.setGeoRadius(0f);
        assertFalse("Should be false with a zero radius", template.hasGeofence());

        // Set radius to negative
        template.setGeoRadius(-50f);
        assertFalse("Should be false with a negative radius", template.hasGeofence());

        // Set everything correctly
        template.setGeoRadius(50f);
        assertTrue("Should be true with lat, lng, and positive radius", template.hasGeofence());
    }

    /**
     * Test 10: The Template setters for geofence information should store
     * the provided values correctly.
     */
    @Test
    public void template_setters_storeGeofenceValues() {
        Template template = new Template();

        template.setGeoLatitude(34.0224);
        template.setGeoLongitude(-118.2851);
        template.setGeoRadius(100.5f);
        template.setGeoLocationName("Test Location");

        assertEquals(Double.valueOf(34.0224), template.getGeoLatitude());
        assertEquals(Double.valueOf(-118.2851), template.getGeoLongitude());
        assertEquals(Float.valueOf(100.5f), template.getGeoRadius());
        assertEquals("Test Location", template.getGeoLocationName());
    }
}