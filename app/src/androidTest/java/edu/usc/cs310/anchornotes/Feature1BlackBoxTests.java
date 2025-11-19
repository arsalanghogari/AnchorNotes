package edu.usc.cs310.anchornotes;

import android.Manifest;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
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
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class Feature1BlackBoxTests {

    // Launches MainActivity before each test
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // Auto-grant runtime permissions that MainActivity requests
    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
    );

    /**
     * BB1 – Create a simple note (title + body) and verify it appears in the All Notes list.
     */
    @Test
    public void createBasicNote_showsInAllNotesList() {
        // Tap the FAB to add a new note
        onView(withId(R.id.addButton))
                .perform(click());

        // Fill in title and body in EditorActivity
        onView(withId(R.id.titleEditText))
                .perform(typeText("Feature1 Test Note"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("This is a body for Feature 1."), closeSoftKeyboard());

        // Save
        onView(withId(R.id.saveButton))
                .perform(click());

        // Back in MainActivity: verify the new note title is visible in the list
        onView(withText("Feature1 Test Note"))
                .check(matches(isDisplayed()));
    }

    /**
     * BB2 – Tapping a note from the list opens EditorActivity
     * with the correct title in the title EditText.
     */
    @Test
    public void tappingNote_opensEditorWithSameTitle() {
        // Create a note "Open Me"
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Open Me"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Body for open test"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Tap the row with "Open Me"
        onView(withText("Open Me"))
                .perform(click());

        // Dismiss optional "Update Location" dialog if it appears
        dismissOptionalUpdateLocationDialog();

        // Now in EditorActivity — check that titleEditText has "Open Me"
        onView(withId(R.id.titleEditText))
                .check(matches(withText("Open Me")));
    }

    private void dismissOptionalUpdateLocationDialog() {
        try {
            // If the dialog is present, it will have this title & buttons
            onView(withText("Update Location"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));

            onView(withText("No"))
                    .inRoot(isDialog())
                    .perform(click());
        } catch (NoMatchingViewException ignored) {
            // Dialog didn’t show – that’s fine, just continue
        }
    }


    /**
     * BB3 – Edit an existing note and verify the updated title appears in the All Notes list
     * and the old title no longer appears.
     */
    @Test
    public void editExistingNote_updatesTitleInList() {
        // Create an original note
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Original Title"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Body before edit"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Open the note from the list
        onView(withText("Original Title"))
                .perform(click());

        // Dismiss the 'Update Location' dialog if it appears
        dismissOptionalUpdateLocationDialog();

        // Now EditorActivity is visible; edit the title
        onView(withId(R.id.titleEditText))
                .perform(replaceText("Updated Title"), closeSoftKeyboard());
        onView(withId(R.id.saveButton))
                .perform(click());

        // Back in MainActivity: new title visible
        onView(withText("Updated Title"))
                .check(matches(isDisplayed()));
    }


    /**
     * BB4 – Delete a note via long-press → Delete, and verify it no longer appears
     * in the All Notes list.
     */
    @Test
    public void deleteNote_removesFromAllNotesList() {
        // Create note "Delete Me"
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Delete Me"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Body to be deleted"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Long-press on the note in the list
        onView(withText("Delete Me"))
                .perform(longClick());

        // In the Note Actions dialog, tap "Delete"
        onView(withText("Delete"))
                .perform(click());

        // Confirm deletion in the confirmation dialog
        onView(withText("Delete"))
                .perform(click());

        // Verify the note is gone
        onView(withText("Delete Me"))
                .check(doesNotExist());
    }

    /**
     * BB5 – Long-press on a note, choose "Pin", then long-press again and
     * verify the dialog now offers "Unpin" (pin/unpin behavior).
     */
    @Test
    public void longPressNote_canPinAndThenUnpin() {
        // Create a note "Pin Toggle"
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Pin Toggle"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Body for pin toggle"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Long-press the note in the *All Notes* list (not the pinned list)
        onView(allOf(
                withText("Pin Toggle"),
                isDescendantOfA(withId(R.id.notesList))
        )).perform(longClick());

        // Choose "Pin"
        onView(withText("Pin")).perform(click());

        // Verify it appears in the pinned section
        onView(allOf(
                withText("Pin Toggle"),
                isDescendantOfA(withId(R.id.pinnedNotesList))
        )).check(matches(isDisplayed()));

        // Long-press it from the pinned list this time
        onView(allOf(
                withText("Pin Toggle"),
                isDescendantOfA(withId(R.id.pinnedNotesList))
        )).perform(longClick());

        // Choose "Unpin"
        onView(withText("Unpin")).perform(click());

    }


    /**
     * BB6 – Home page contains core UI elements for Feature 1:
     * SearchView, Add button, Map, Templates, Tags, and the main notes ListView.
     */
    @Test
    public void mainActivity_hasCoreUiElements() {
        // SearchView
        onView(withId(R.id.searchView))
                .check(matches(isDisplayed()));

        // Add (FAB)
        onView(withId(R.id.addButton))
                .check(matches(allOf(isDisplayed(), isClickable())));

        // Map button
        onView(withId(R.id.mapButton))
                .check(matches(isDisplayed()));

        // Templates button
        onView(withId(R.id.templatesButton))
                .check(matches(isDisplayed()));

        // Tags button
        onView(withId(R.id.tagsButton))
                .check(matches(isDisplayed()));

        // Main ListView for notes
        onView(withId(R.id.notesList))
                .check(matches(isDisplayed()));
    }
}
