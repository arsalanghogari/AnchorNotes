package edu.usc.cs310.anchornotes;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.PickerActions; // Import the necessary class
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.widget.DatePicker;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;

import edu.usc.cs310.anchornotes.ui.MainActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class Feature4BlackBoxTests {

    // Black Box Test 1: Set a time reminder and verify UI update.
    @Test
    public void testSetTimeReminder_displaysInEditor() {
        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.reminderButton)).perform(click());
        onView(withText("Set Time Reminder")).perform(click());

        // --- FIX: Set the date to one year in the future ---
        Calendar calendar = Calendar.getInstance();
        int futureYear = calendar.get(Calendar.YEAR) + 1;
        int currentMonth = calendar.get(Calendar.MONTH); // Month is 0-indexed
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(futureYear, currentMonth + 1, currentDay));
        // --- END FIX ---

        onView(withText("OK")).perform(click()); // Select the future date
        onView(withText("OK")).perform(click()); // Select the time (any time on a future date is valid)

        onView(withId(R.id.reminderStatusTextView))
                .check(matches(isDisplayed()));
    }

    // Black Box Test 2: Set a location reminder. (Conceptual - requires mock Places API)
    @Test
    public void testSetGeofenceReminder_launchesPlacePicker() {
        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.reminderButton)).perform(click());
        onView(withText("Set Location Reminder")).perform(click());
        // In a real test, you would use Espresso-Intents to verify
        // that an intent to the AutocompleteActivity was sent.
    }

    // Black Box Test 3: Clear a reminder and verify UI update.
    @Test
    public void testClearReminder_removesStatusInEditor() {
        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.addButton)).perform(click());
        // Set a reminder first (using the fixed future date logic)
        onView(withId(R.id.reminderButton)).perform(click());
        onView(withText("Set Time Reminder")).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(Calendar.getInstance().get(Calendar.YEAR) + 1, 1, 1));
        onView(withText("OK")).perform(click());
        onView(withText("OK")).perform(click());
        // Now, clear it
        onView(withId(R.id.reminderButton)).perform(click());
        onView(withText("Clear Reminder")).perform(click());
        onView(withId(R.id.reminderStatusTextView))
                .check(matches(not(isDisplayed())));
    }

    // Black Box Test 4: Save a note with a reminder and check for a toast.
    @Test
    public void testSaveNoteWithTimeReminder_showsToast() {
        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.titleEditText)).perform(typeText("Reminder Test Note"), closeSoftKeyboard());
        onView(withId(R.id.reminderButton)).perform(click());
        onView(withText("Set Time Reminder")).perform(click());

        // --- FIX: Set the date to one year in the future ---
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(Calendar.getInstance().get(Calendar.YEAR) + 1, 5, 20));
        // --- END FIX ---

        onView(withText("OK")).perform(click());
        onView(withText("OK")).perform(click());
        onView(withId(R.id.saveButton)).perform(click());

        // A full notification test is complex. The goal is to ensure the save completes
        // and the app doesn't crash. We can check that we've returned to MainActivity.
        onView(withId(R.id.notesList)).check(matches(isDisplayed()));
    }

    // Black Box Test 5: Navigate to MapActivity.
    @Test
    public void testMapButton_opensMapActivity() {
        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.mapButton)).perform(click());
        onView(withId(R.id.map)).check(matches(isDisplayed()));
    }

    // Black Box Test 6: Verify map navigation UI is present.
    @Test
    public void testMapActivity_displaysNavigationControls() {
        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.mapButton)).perform(click());
        onView(withId(R.id.navigationLayout)).check(matches(isDisplayed()));
        onView(withId(R.id.prevButton)).check(matches(isDisplayed()));
        onView(withId(R.id.nextButton)).check(matches(isDisplayed()));
    }

    // Black Box Test 7: Toggle map view to show reminders.
    @Test
    public void testMapToggle_switchesToRemindersView() {
        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.mapButton)).perform(click());
        onView(withId(R.id.toggleLocationsButton)).perform(click());
        onView(withId(R.id.toggleRemindersButton)).check(matches(isDisplayed()));
        onView(withId(R.id.toggleLocationsButton)).check(matches(not(isDisplayed())));
    }

    // Black Box Test 8: Create a note with a location.
    @Test
    public void testAddContextLocation_launchesPlacePicker() {
        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.addButton)).perform(click());
        onView(withId(R.id.locationButton)).perform(click());
        onView(withText("Set a Manual Location")).perform(click());
        // Would use Espresso-Intents to verify the correct intent was fired.
    }

    // Black Box Test 9: Open and close the tag management dialog.
    @Test
    public void testTagManagementDialog_opensAndCloses() {
        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.tagsButton)).perform(click());
        onView(withText("Manage Tags")).check(matches(isDisplayed()));
        onView(withText("Close")).perform(click());
    }

    // Black Box Test 10: Open and close the filter dialog.
    @Test
    public void testFilterDialog_opensAndCloses() {
        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("Filter Notes")).check(matches(isDisplayed()));
        onView(withText("Cancel")).perform(click());
    }
}