package ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

/**
 * Generic driver for InvenTree's Mantine-based create/edit modals.
 *
 * Every InvenTree form field carries a stable {@code aria-label} of the form
 * {@code <type>-field-<name>} (e.g. {@code text-field-name},
 * {@code number-field-minimum_stock}, {@code boolean-field-structural}) that
 * survives across page reloads, unlike Mantine's own randomly-generated
 * element ids - verified directly against a running instance. One generic
 * driver against that convention covers every InvenTree form instead of a
 * bespoke Page Object per modal.
 *
 * All lookups are scoped to this instance's own dialog locator (the last
 * "[role=dialog]" in the DOM at construction time) - required for flows
 * like "create a new Parameter Template inline while adding a Parameter",
 * where two modals (this one and the nested one) are open at once and both
 * have their own "Submit" button.
 */
public class FormModal {

    private static final String FORM_ERROR_TEXT = "Errors exist for one or more form fields";

    private final Page page;
    private final Locator dialog;

    public FormModal(Page page) {
        this.page = page;
        this.dialog = page.locator("[role='dialog']").last();
        dialog.waitFor();
    }

    public FormModal setText(String fieldName, String value) {
        dialog.locator("[aria-label='text-field-" + fieldName + "']").fill(value);
        return this;
    }

    public FormModal setNumber(String fieldName, String value) {
        dialog.locator("[aria-label='number-field-" + fieldName + "']").fill(value);
        return this;
    }

    /** Sets a Mantine switch/checkbox field (e.g. {@code boolean-field-structural}) to the given state. */
    public FormModal setBoolean(String fieldName, boolean value) {
        dialog.locator("[aria-label='boolean-field-" + fieldName + "']").setChecked(value);
        return this;
    }

    /**
     * Clicks an inline "create new related record" action button, identified
     * by InvenTree's {@code action-button-<action-name>} convention (e.g.
     * {@code action-button-create-new-parameter-template}). Returns a new
     * FormModal bound to the nested dialog this opens.
     */
    public FormModal openInlineCreate(String actionName) {
        dialog.locator("[aria-label='action-button-" + actionName + "']").click();
        return new FormModal(page);
    }

    /** Waits briefly for the server-validation error banner; false if it never appears. */
    public boolean hasFormError() {
        return waitForDialogText(FORM_ERROR_TEXT);
    }

    /** Waits briefly for the given text to appear anywhere within this modal. */
    public boolean hasText(String expectedText) {
        return waitForDialogText(expectedText);
    }

    private boolean waitForDialogText(String text) {
        try {
            dialog.locator("text=" + text).first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (TimeoutError notShown) {
            return false;
        }
    }

    /**
     * Clicks Submit and waits for the outcome: on success the modal closes
     * (this method returns only once that async request has actually
     * completed, so callers can safely verify the result immediately
     * afterward); on validation failure the error banner appears instead and
     * the modal stays open, which {@link #hasFormError()}/{@link #hasText}
     * can then assert on.
     */
    public void submit() {
        submitAs("Submit");
    }

    /**
     * For destructive-action confirmation dialogs whose confirm button reads
     * "Delete" rather than "Submit" (e.g. the Parameter row delete
     * confirmation) - same wait-for-outcome contract as {@link #submit()}.
     */
    public void confirmDelete() {
        submitAs("Delete");
    }

    private void submitAs(String buttonText) {
        dialog.locator(":text-is(\"" + buttonText + "\")").click();
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            if (dialog.isHidden() || dialog.locator("text=" + FORM_ERROR_TEXT).count() > 0) {
                return;
            }
            page.waitForTimeout(200);
        }
    }

    public void cancel() {
        dialog.locator(":text-is(\"Cancel\")").click();
    }

    /**
     * Whether the Submit button is currently disabled - e.g. editing a
     * locked Part disables the entire form instead of individual fields,
     * verified directly against a running instance. Anchored on the button
     * tag (not {@code :text-is()}) for the same reason as
     * {@link PartDetailPage#isDeleteActionDisabled()}.
     */
    public boolean isSubmitDisabled() {
        Locator submitButton = dialog.locator("button:has-text(\"Submit\")").first();
        return submitButton.getAttribute("data-disabled") != null;
    }
}
