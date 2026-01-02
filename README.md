# Project Status

This document outlines the outcome of our sprint.

## Known Issues

The following are known bugs and issues that were resolved during the sprint:

*   **Search Query Persistence:** A bug occurred where a deleted search query persisted invisibly. If a user entered a search term, deleted it to show all notes, created a new note, and then returned to the home screen, the note list would incorrectly display the results of the previously deleted query. The search bar itself is empty, which may cause confusion. This has been **resolved**.
*   **Failing Test Cases:** Several test cases for "Feature 2" were not passing in either white box or black box testing scenarios. Now all pass.
*   **Loss of Text Styling:** Text styling such as italics and bold was not preserved after a note is saved. When a user reopens a note, all formatting was been removed. Post patch, this is no longer an issue.
*   **Places API Functionality:** The Places API was not functioning, but this has been **resolved** by adjusting restrictions on the API key.

## Future Enhancements

This section details features and improvements that we have added in the latest release.

### Additional Features

*   **Expanded Note Editor Options:**
    *   Ability to change font types.
    *   Highlighting functionality.
*   **Enhanced Location Filtering:** An improved location filter that allows users to filter notes by specific locations they have previously attached to their notes.
*   **Drawing Feature:** An integrated drawing tool that will allow users to create and insert their own drawings as images into their notes, similar to the functionality in Google Docs.

### Added Required Features

*   **Rich Text in Templates:** Support for rich text formatting when creating new templates is implemented.
*   **Location-Based Template Prioritization:** The ability to prioritize templates based on the user's location was added.
*   **Attachment Removal:** Users are now able to remove attachments (voice, image).

## Minor Details & UI/UX Refinements (Nits)

These are smaller, non-critical issues and suggestions for improving the user experience which have been added:

*   **UI Alignment:**
    *   In the Tag creation UI, the "Existing Tags:" subtitle is no longer misaligned.
    *   A similar, less pronounced alignment issue was fixed in the filter UI. The filter UI also uses a picker now for cleaner implementation.
*   **Permissions Requests:** The application prompts the user to enable notifications and location now.
*   **Location Update Prompt:** The app no longer asks for confirmation to update the attached location every time a note is opened for editing. This prompt should only appear when the note is being saved.
*   **"Last Edited" Timestamp:** The "last edited" time only displays the hour and minute, which can be confusing when viewing notes on different days. The timestamp now includes the date as well.
