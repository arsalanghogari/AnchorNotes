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
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;

import android.view.View;
import android.widget.EditText;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class Feature3BlackBoxTests {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
    );

    /**
     * Helper: type into the SearchView's internal text field.
     */
    private void typeInSearchView(String query) {
        // Click the SearchView so it behaves like a user interaction
        onView(withId(R.id.searchView)).perform(click());

        // Directly set the text on the internal EditText, ignoring visibility/size constraints
        onView(withId(androidx.appcompat.R.id.search_src_text))
                .perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        // Only require that it's an EditText, no visibility/size requirement
                        return isAssignableFrom(EditText.class);
                    }

                    @Override
                    public String getDescription() {
                        return "Set text on SearchView's search_src_text";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        EditText editText = (EditText) view;
                        editText.setText(query);
                        editText.setSelection(query.length());
                    }
                });
    }

    /**
     * Helper: dismiss optional "Update Location" dialog in EditorActivity, if it appears.
     * (Kept for future reuse; not strictly needed for search-only flows.)
     */
    private void dismissOptionalUpdateLocationDialog() {
        try {
            onView(withText("Update Location"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
            onView(withText("No"))
                    .inRoot(isDialog())
                    .perform(click());
        } catch (NoMatchingViewException ignored) {
            // Dialog did not show, which is fine.
        }
    }

    /**
     * BB-Search-1:
     * Create two notes. Search by a keyword that appears only in the first note's title.
     * Verify the matching note is shown in the All Notes list and the non-matching one is not.
     */
    @Test
    public void searchByTitle_showsOnlyMatchingNote() {
        // Note 1: title contains the unique keyword
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Alpha_Search_Title_Unique"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Body 1"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Note 2: different title that should not match the query
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Completely Different Note"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Body 2"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Search for keyword in first title
        typeInSearchView("Alpha_Search_Title");

        // Matching note is visible in the main notes list
        onView(allOf(
                withText("Alpha_Search_Title_Unique"),
                isDescendantOfA(withId(R.id.notesList))
        )).check(matches(isDisplayed()));

        // Non-matching note should not appear in the filtered list
        onView(allOf(
                withText("Completely Different Note"),
                isDescendantOfA(withId(R.id.notesList))
        )).check(doesNotExist());
    }

    /**
     * BB-Search-2:
     * Search by a keyword that appears only in the body of one note.
     * Verifies that body text is considered by search and the note's title is shown.
     */
    @Test
    public void searchByBodyKeyword_showsNoteWhoseBodyMatches() {
        // Note 1: body contains a unique keyword
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("BodySearchNote"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("This body has KEYWORD_BODY_12345"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Note 2: no matching body
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("OtherNote"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("No special keyword here"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Search by the body keyword
        typeInSearchView("KEYWORD_BODY_12345");

        // The note with that body should appear via its title
        onView(allOf(
                withText("BodySearchNote"),
                isDescendantOfA(withId(R.id.notesList))
        )).check(matches(isDisplayed()));

        // The other note should not appear in filtered list
        onView(allOf(
                withText("OtherNote"),
                isDescendantOfA(withId(R.id.notesList))
        )).check(doesNotExist());
    }

    /**
     * BB-Search-3:
     * Verify that search is case-insensitive.
     * Create a note with mixed-case title and search with different casing.
     */
    @Test
    public void searchIsCaseInsensitive() {
        // Create a note with mixed case
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Case Mixed Title"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Body for case test"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Search using different casing
        typeInSearchView("case mixed");

        // Title should still be found
        onView(allOf(
                withText("Case Mixed Title"),
                isDescendantOfA(withId(R.id.notesList))
        )).check(matches(isDisplayed()));
    }

    /**
     * BB-Search-4:
     * Search that should return multiple notes:
     * create two titles containing the same keyword and verify both appear.
     */
    @Test
    public void searchWithSharedKeyword_showsAllMatchingNotes() {
        // Note 1
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Project Alpha Search"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Body Alpha"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Note 2
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Project Beta Search"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Body Beta"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Search for keyword common to both titles
        typeInSearchView("Project");

        // Both notes containing "Project" in title should be listed
        onView(allOf(
                withText("Project Alpha Search"),
                isDescendantOfA(withId(R.id.notesList))
        )).check(matches(isDisplayed()));

        onView(allOf(
                withText("Project Beta Search"),
                isDescendantOfA(withId(R.id.notesList))
        )).check(matches(isDisplayed()));
    }

    /**
     * BB-Search-5:
     * Search for a keyword that matches no note title or body.
     * Verify that a known note from earlier in this test is not displayed.
     */
    @Test
    public void searchNoResults_showsEmptyListForThatQuery() {
        // Create a note to ensure there is at least one note in the DB
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText))
                .perform(typeText("Some Existing Note For NoResult"), closeSoftKeyboard());
        onView(withId(R.id.noteBodyEditText))
                .perform(typeText("Random text here"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(click());

        // Search for a nonsense string that should not match anything
        typeInSearchView("ZZZ_NO_MATCH_987654321");

        // The known note should not be visible in the filtered notesList
        onView(allOf(
                withText("Some Existing Note For NoResult"),
                isDescendantOfA(withId(R.id.notesList))
        )).check(doesNotExist());
    }
}
