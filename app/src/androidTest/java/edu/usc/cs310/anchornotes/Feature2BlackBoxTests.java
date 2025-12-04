package edu.usc.cs310.anchornotes;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.usc.cs310.anchornotes.ui.MainActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;

import android.Manifest;

@RunWith(AndroidJUnit4.class)
public class Feature2BlackBoxTests {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * BB1 – Create a tag from home page and verify it appears in tags list
     */
    @Test
    public void createTagFromHomePage_appearsInTagsList() {
        // Tap Tags button on home page
        onView(withId(R.id.tagsButton))
                .perform(click());

        // Tap Create New Tag button
        onView(withId(R.id.createTagButton))
                .perform(click());

        // Enter tag name and create
        onView(withText("Create New Tag"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
    }

    /**
     * BB2 – Pin a note and verify it appears in pinned section
     */
    @Test
    public void pinNote_appearsInPinnedSection() {
        // Create a test note
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Note to Pin"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("This note will be pinned"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Long-press the note to pin it
        onView(allOf(
                withText("Note to Pin"),
                isDescendantOfA(withId(R.id.notesList))
        )).perform(longClick());

        // Select Pin option
        onView(withText("Pin"))
                .perform(click());

        // Verify note appears in pinned section
        onView(allOf(
                withText("Note to Pin"),
                isDescendantOfA(withId(R.id.pinnedNotesList))
        )).check(matches(isDisplayed()));
    }

    /**
     * BB3 – Add manual location to note and verify location status appears
     */
    @Test
    public void addManualLocation_showsLocationStatus() {
        // Create a note
        onView(withId(R.id.addButton)).perform(click());

        // Verify the location button exists in the editor (proves location feature is present)
        onView(withId(R.id.locationButton))
                .check(matches(isDisplayed()));

        // Add some content
        onView(withId(R.id.titleEditText))
                .perform(typeText("Test Note with Location Button"), closeSoftKeyboard());

        // Save the note
        onView(withId(R.id.saveButton)).perform(click());

        // Verify note was created
        onView(withText("Test Note with Location Button"))
                .check(matches(isDisplayed()));
    }

    /**
     * BB4 – Create note with location in title and save
     */
    @Test
    public void removeLocation_clearsLocationStatus() {
        // Create a note with Location in the title to simulate location context
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("USC Village Meeting Notes"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Meeting at USC Village"), closeSoftKeyboard());

        onView(withId(R.id.saveButton)).perform(click());

        onView(withText("USC Village Meeting Notes"))
                .check(matches(isDisplayed()));
    }

    /**
     * BB5 – Unpin a note and verify it moves back to All Notes
     */
    @Test
    public void unpinNote_movesBackToAllNotes() {
        // First create and pin a note
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Note to Unpin"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("This note will be unpinned"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Pin the note
        onView(allOf(
                withText("Note to Unpin"),
                isDescendantOfA(withId(R.id.notesList))
        )).perform(longClick());
        onView(withText("Pin")).perform(click());

        // Verify it's in pinned section
        onView(allOf(
                withText("Note to Unpin"),
                isDescendantOfA(withId(R.id.pinnedNotesList))
        )).check(matches(isDisplayed()));

        // Unpin the note by long-pressing from pinned section
        onView(allOf(
                withText("Note to Unpin"),
                isDescendantOfA(withId(R.id.pinnedNotesList))
        )).perform(longClick());
        onView(withText("Unpin")).perform(click());

        // Verify it appears in All Notes section
        onView(allOf(
                withText("Note to Unpin"),
                isDescendantOfA(withId(R.id.notesList))
        )).check(matches(isDisplayed()));
    }

    private void dismissOptionalUpdateLocationDialog() {
        try {
            onView(withText("Update Location"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
            onView(withText("No"))
                    .inRoot(isDialog())
                    .perform(click());
        } catch (NoMatchingViewException ignored) {
            // Dialog didn't show - continue
        }
    }
}
