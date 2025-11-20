package edu.usc.cs310.anchornotes;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.usc.cs310.anchornotes.model.Note;

import static org.junit.Assert.*;

/**
 * White-box tests for Feature 3: Search & filter behavior.
 *
 * These tests are JVM-only (no Android, no Activities).
 * They focus on the underlying logic of:
 *  - keyword search on title and body (case-insensitive)
 *  - robustness to null / empty fields
 *  - simple media-based filtering (has photo / has voice / has location)
 *
 * The helper methods in this test emulate the app's search behavior:
 *  - keyword match if query appears in title OR body (case-insensitive)
 *  - optional filters for photo / voice / location flags
 *
 * No Room DB, no ViewModel, no LiveData, no Android UI.
 */
public class Feature3WhiteBoxTests {

    /**
     * Helper: apply keyword search only, like the logic in MainActivity.applyCurrentFilter.
     * A note matches if the query (case-insensitive) is contained in title or body.
     * If the query is null or empty, all notes are returned.
     */
    private List<Note> applyKeywordSearch(List<Note> notes, String query) {
        if (notes == null) {
            return new ArrayList<>();
        }

        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(notes);
        }

        String q = query.toLowerCase();

        return notes.stream()
                .filter(n ->
                        (n.getTitle() != null && n.getTitle().toLowerCase().contains(q)) ||
                                (n.getBody() != null && n.getBody().toLowerCase().contains(q))
                )
                .collect(Collectors.toList());
    }

    /**
     * Helper: filter by media/location flags.
     * - requirePhoto: only keep notes where getPhotoUri() != null
     * - requireVoice: only keep notes where getVoiceUri() != null
     * - requireLocation: only keep notes where hasLocation() is true
     */
    private List<Note> applyMediaFilters(List<Note> notes,
                                         boolean requirePhoto,
                                         boolean requireVoice,
                                         boolean requireLocation) {
        if (notes == null) {
            return new ArrayList<>();
        }

        return notes.stream()
                .filter(n -> {
                    if (requirePhoto && n.getPhotoUri() == null) {
                        return false;
                    }
                    if (requireVoice && n.getVoiceUri() == null) {
                        return false;
                    }
                    if (requireLocation && !n.hasLocation()) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * WB1 – Empty or null query should return all notes unchanged.
     */
    @Test
    public void emptyOrNullQuery_returnsAllNotes() {
        List<Note> original = Arrays.asList(
                new Note("Alpha title", "First body"),
                new Note("Bravo title", "Second body")
        );

        List<Note> resultEmpty = applyKeywordSearch(original, "");
        List<Note> resultNull = applyKeywordSearch(original, null);

        assertEquals("Empty query should return all notes", original.size(), resultEmpty.size());
        assertTrue(resultEmpty.containsAll(original));

        assertEquals("Null query should return all notes", original.size(), resultNull.size());
        assertTrue(resultNull.containsAll(original));
    }

    /**
     * WB2 – Keyword found in title should include that note
     * and exclude nonmatching notes.
     */
    @Test
    public void keywordInTitle_returnsOnlyMatchingNotes() {
        Note match = new Note("Meeting Notes for Alpha Project", "Body content");
        Note nonMatch = new Note("Random Grocery List", "Eggs, milk, bread");

        List<Note> notes = Arrays.asList(match, nonMatch);

        List<Note> result = applyKeywordSearch(notes, "alpha project");

        assertEquals("Only one note should match the keyword in title", 1, result.size());
        assertEquals(match, result.get(0));
    }

    /**
     * WB3 – Keyword found in body should include that note,
     * even if the title does not contain the keyword.
     */
    @Test
    public void keywordInBody_returnsMatchingNoteEvenIfTitleDoesNotMatch() {
        Note match = new Note("Daily Log", "Today I worked on KEYWORD_BODY_12345 tasks.");
        Note nonMatch = new Note("Another Note", "No relevant content here.");

        List<Note> notes = Arrays.asList(match, nonMatch);

        List<Note> result = applyKeywordSearch(notes, "keyword_body_12345");

        assertEquals(1, result.size());
        assertEquals(match, result.get(0));
    }

    /**
     * WB4 – Search should be case-insensitive and tolerate null titles/bodies
     * without throwing exceptions.
     */
    @Test
    public void searchIsCaseInsensitive_andHandlesNullFieldsSafely() {
        Note upperCase = new Note("ALPHA TITLE", "Body");
        Note mixedCase = new Note("some title", "Body with Alpha keyword");
        Note nullTitle = new Note(null, "alpha in body only");
        Note nullBody = new Note("Alpha in title only", null);
        Note noMatch = new Note("Completely different", "No key word here");

        List<Note> notes = Arrays.asList(upperCase, mixedCase, nullTitle, nullBody, noMatch);

        List<Note> result = applyKeywordSearch(notes, "alpha");

        // All notes except the last one should match in some field
        assertEquals(4, result.size());
        assertTrue(result.contains(upperCase));
        assertTrue(result.contains(mixedCase));
        assertTrue(result.contains(nullTitle));
        assertTrue(result.contains(nullBody));
        assertFalse(result.contains(noMatch));
    }

    /**
     * WB5 – Media/location filters should only include notes that satisfy
     * all required flags simultaneously.
     */
    @Test
    public void mediaFilters_requireMatchingFlagsOnNotes() {
        Note withEverything = new Note("All media", "Has photo, voice, location");
        withEverything.setPhotoUri("content://photos/1");
        withEverything.setVoiceUri("content://voice/clip.m4a");
        withEverything.setLatitude(34.0);
        withEverything.setLongitude(-118.0);
        withEverything.setLocationName("Los Angeles");

        Note photoOnly = new Note("Photo only", "Just a photo");
        photoOnly.setPhotoUri("content://photos/2");

        Note voiceOnly = new Note("Voice only", "Just audio");
        voiceOnly.setVoiceUri("content://voice/only.m4a");

        Note noMedia = new Note("Plain", "No attachments");

        List<Note> notes = Arrays.asList(withEverything, photoOnly, voiceOnly, noMedia);

        // Require only photo
        List<Note> photoResults = applyMediaFilters(notes, true, false, false);
        assertEquals(2, photoResults.size());
        assertTrue(photoResults.contains(withEverything));
        assertTrue(photoResults.contains(photoOnly));

        // Require photo AND voice
        List<Note> photoAndVoiceResults = applyMediaFilters(notes, true, true, false);
        assertEquals(1, photoAndVoiceResults.size());
        assertEquals(withEverything, photoAndVoiceResults.get(0));

        // Require location only
        List<Note> locationResults = applyMediaFilters(notes, false, false, true);
        assertEquals(1, locationResults.size());
        assertEquals(withEverything, locationResults.get(0));

        // Require all three: photo, voice, location
        List<Note> allThree = applyMediaFilters(notes, true, true, true);
        assertEquals(1, allThree.size());
        assertEquals(withEverything, allThree.get(0));
    }
}
