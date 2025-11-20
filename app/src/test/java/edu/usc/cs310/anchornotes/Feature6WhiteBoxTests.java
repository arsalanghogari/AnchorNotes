package edu.usc.cs310.anchornotes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import edu.usc.cs310.anchornotes.model.Template;

/**
 * White-box tests for Feature 6 (Note Templates) focusing on model-level behavior and
 * the TemplateDraft helper used by the templates editor dialog.
 */
public class Feature6WhiteBoxTests {

    /**
     * Ensures that Template.createDefaultTemplate() produces the expected "Blank Page" template
     * with empty content, default coloring, and no geofence/tag associations.
     */
    @Test
    public void createDefaultTemplate_hasBlankPageDefaults() {
        Template template = Template.createDefaultTemplate();

        assertEquals("Blank Page", template.getName());
        assertEquals("", template.getTitle());
        assertEquals("", template.getBody());
        assertEquals(Template.DEFAULT_PAGE_COLOR, template.getPageColor());
        assertNull("Default template should not have tags", template.getDefaultTagsCsv());
        assertNull("Default template should not have latitude", template.getGeoLatitude());
        assertNull("Default template should not have longitude", template.getGeoLongitude());
        assertNull("Default template should not have radius", template.getGeoRadius());
        assertNull("Default template should not have a location name", template.getGeoLocationName());
        assertFalse("Default template should not report a geofence", template.hasGeofence());
    }

    /**
     * Verifies that page color assignments respect defaults when null/empty values are supplied.
     */
    @Test
    public void setPageColor_nullOrEmptyRevertsToDefault() {
        Template template = new Template();

        template.setPageColor("#ABCDEF");
        assertEquals("#ABCDEF", template.getPageColor());

        template.setPageColor(null);
        assertEquals(Template.DEFAULT_PAGE_COLOR, template.getPageColor());

        template.setPageColor("");
        assertEquals(Template.DEFAULT_PAGE_COLOR, template.getPageColor());
    }

    /**
     * Ensures the template tag CSV setter normalizes null/empty input to null and preserves
     * meaningful comma-separated values.
     */
    @Test
    public void setDefaultTagsCsv_handlesNullAndEmpty() {
        Template template = new Template();

        template.setDefaultTagsCsv(null);
        assertNull(template.getDefaultTagsCsv());

        template.setDefaultTagsCsv("");
        assertNull(template.getDefaultTagsCsv());

        template.setDefaultTagsCsv("Project, Planning");
        assertEquals("Project, Planning", template.getDefaultTagsCsv());
    }

    /**
     * Validates that getDefaultTagNames() trims whitespace around each CSV segment.
     */
    @Test
    public void getDefaultTagNames_trimsAndSkipsEmptyEntries() {
        Template template = new Template();
        template.setDefaultTagsCsv(" design , planning , standup ");

        List<String> tags = template.getDefaultTagNames();
        assertEquals(Arrays.asList("design", "planning", "standup"), tags);
    }

    /**
     * Uses reflection to exercise TemplateDraft.from(Template) and confirm that the editor dialog
     * receives a faithful copy of an existing template's fields.
     */
    @Test
    public void templateDraft_fromExistingTemplateCopiesAllFields() throws Exception {
        Template source = new Template();
        source.setName("Weekly Review");
        source.setTitle("Weekly Status");
        source.setBody("Summary:\n- Highlights\n- Risks");
        source.setPageColor("#FFF9C4");
        source.setDefaultTagsCsv("status,team");
        source.setGeoLatitude(34.0001);
        source.setGeoLongitude(-118.2501);
        source.setGeoRadius(75f);
        source.setGeoLocationName("HQ");

        Class<?> draftClass = Class.forName("edu.usc.cs310.anchornotes.ui.TemplatesActivity$TemplateDraft");
        Method fromMethod = draftClass.getDeclaredMethod("from", Template.class);
        fromMethod.setAccessible(true);
        Object draft = fromMethod.invoke(null, source);

        assertEquals("Weekly Review", getFieldValue(draft, draftClass, "name"));
        assertEquals("Weekly Status", getFieldValue(draft, draftClass, "title"));
        assertEquals("Summary:\n- Highlights\n- Risks", getFieldValue(draft, draftClass, "body"));
        assertEquals("#FFF9C4", getFieldValue(draft, draftClass, "pageColor"));
        assertEquals("status,team", getFieldValue(draft, draftClass, "defaultTagsCsv"));
        assertEquals(Double.valueOf(34.0001), getFieldValue(draft, draftClass, "geoLatitude"));
        assertEquals(Double.valueOf(-118.2501), getFieldValue(draft, draftClass, "geoLongitude"));
        assertEquals(Float.valueOf(75f), getFieldValue(draft, draftClass, "geoRadius"));
        assertEquals("HQ", getFieldValue(draft, draftClass, "geoLocationName"));
    }

    private Object getFieldValue(Object target, Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}


