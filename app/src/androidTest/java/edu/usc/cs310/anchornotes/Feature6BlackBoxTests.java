package edu.usc.cs310.anchornotes;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import edu.usc.cs310.anchornotes.database.AppDatabase;
import edu.usc.cs310.anchornotes.database.TemplateDao;
import edu.usc.cs310.anchornotes.model.Template;
import edu.usc.cs310.anchornotes.ui.MainActivity;

/**
 * Espresso UI tests for Feature 6 (Note Templates) covering template management flows.
 */
@RunWith(AndroidJUnit4.class)
public class Feature6BlackBoxTests {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        resetTemplatesToDefault();
        openTemplatesScreen();
        // Ensure the list view is visible before each test starts
        onView(withId(R.id.templatesListView)).check(matches(withEffectiveVisibility(VISIBLE)));
    }

    /**
     * Test BB1 – verify the bundled default template is visible when the Templates screen opens.
     */
    @Test
    public void defaultTemplate_visibleOnLaunch() {
        onView(withText("Blank Page")).check(matches(ViewMatchers.isDisplayed()));
    }

    /**
     * Test BB2 – create a new template and confirm it appears in the list; then clean it up.
     */
    @Test
    public void createTemplate_addsTemplateToList() {
        String name = "BB2 Meeting Notes";
        createTemplate(name, "Weekly Sync", "Agenda:\n- Updates\n- Blockers", "Meetings,Team");

        onView(withText(name)).check(matches(ViewMatchers.isDisplayed()));

        deleteTemplate(name);
        onView(withText(name)).check(doesNotExist());
    }

    /**
     * Test BB3 – edit an existing template and confirm the list reflects the new name.
     */
    @Test
    public void editTemplate_updatesNameInList() {
        String originalName = "BB3 Weekly Report";
        String updatedName = "BB3 Weekly Report (Updated)";
        createTemplate(originalName, "Report", "Summary", "Reporting");

        // Open actions, choose Edit, update the name, and save.
        onView(withText(originalName)).perform(click());
        onView(withText("Edit")).perform(click());
        onView(withId(R.id.templateNameInput)).inRoot(isDialog()).perform(replaceText(updatedName), closeSoftKeyboard());
        onView(withText("Save")).inRoot(isDialog()).perform(click());

        onView(withText(updatedName)).check(matches(ViewMatchers.isDisplayed()));

        deleteTemplate(updatedName);
        onView(withText(updatedName)).check(doesNotExist());
    }

    /**
     * Test BB4 – attempting to delete the final remaining template should be blocked with a toast.
     */
    @Test
    public void deleteTemplate_whenOnlyDefault_showsGuardMessage() {
        onView(withText("Blank Page")).perform(click());
        onView(withText("Delete")).perform(click());

        onView(withText("Delete Template")).check(doesNotExist());
        onView(withId(R.id.templatesListView)).check(matches(hasDescendant(withText("Blank Page"))));
    }

    /**
     * Test BB5 – radius validation should prevent saving when a non-positive value is entered.
     */
    @Test
    public void createTemplate_withInvalidRadius_showsValidationError() {
        onView(withId(R.id.addTemplateButton)).perform(click());
        onView(withId(R.id.templateNameInput)).inRoot(isDialog()).perform(replaceText("BB5 Geo Template"), closeSoftKeyboard());
        onView(withId(R.id.templateRadiusInput)).inRoot(isDialog()).perform(replaceText("0"), closeSoftKeyboard());
        onView(withText("Save")).inRoot(isDialog()).perform(click());

        onView(withId(R.id.templateRadiusInput))
                .inRoot(isDialog())
                .check(matches(ViewMatchers.hasErrorText("Radius must be positive")));

        // Dismiss the dialog so subsequent tests start cleanly.
        onView(withText("Cancel")).inRoot(isDialog()).perform(click());
    }

    private void createTemplate(String name, String title, String body, String tagsCsv) {
        onView(withId(R.id.addTemplateButton)).perform(click());
        onView(withId(R.id.templateNameInput)).inRoot(isDialog()).perform(replaceText(name), closeSoftKeyboard());
        onView(withId(R.id.templateTitleInput)).inRoot(isDialog()).perform(replaceText(title), closeSoftKeyboard());
        onView(withId(R.id.templateBodyInput)).inRoot(isDialog()).perform(replaceText(body), closeSoftKeyboard());
        onView(withId(R.id.templateTagsInput)).inRoot(isDialog()).perform(replaceText(tagsCsv), closeSoftKeyboard());
        onView(withText("Save")).inRoot(isDialog()).perform(click());
    }

    private void deleteTemplate(String name) {
        onView(withText(name)).perform(click());
        onView(withText("Delete")).perform(click());
        onView(withText("Delete Template")).check(matches(ViewMatchers.isDisplayed()));
        onView(allOf(withId(android.R.id.button1), withText("Delete"))).inRoot(isDialog()).perform(click());
    }

    private void openTemplatesScreen() {
        onView(withId(R.id.templatesButton)).perform(click());
    }

    private void resetTemplatesToDefault() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TemplateDao dao = AppDatabase.getInstance(context).templateDao();
            List<Template> existing = dao.getAllTemplatesSync();
            for (Template template : existing) {
                boolean isDefault = "Blank Page".equals(template.getName())
                        && (template.getTitle() == null || template.getTitle().isEmpty())
                        && (template.getBody() == null || template.getBody().isEmpty());
                if (!isDefault) {
                    dao.delete(template);
                }
            }
            if (dao.countTemplates() == 0) {
                dao.insert(Template.createDefaultTemplate());
            }
            latch.countDown();
        });
        assertTrue("Timed out restoring default template state",
                latch.await(5, TimeUnit.SECONDS));
    }

}


