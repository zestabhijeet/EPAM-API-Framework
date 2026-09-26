package ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

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
 * All lookups are scoped to this instance's own dialog element (captured at
 * construction time), not the whole page - required for flows like "create a
 * new Parameter Template inline while adding a Parameter", where two modals
 * (this one and the nested one) are in the DOM at once and both have their
 * own "Submit" button.
 */
public class FormModal {

    private static final By DIALOG = By.cssSelector("[role='dialog']");
    private static final By FORM_ERROR_BANNER = By.xpath(".//*[contains(text(),'Errors exist for one or more form fields')]");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final WebElement dialog;

    public FormModal(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // The most recently opened dialog is the last one in the DOM - the
        // relevant instance whether this is the only modal open or a nested one.
        this.dialog = wait.until(d -> {
            List<WebElement> dialogs = d.findElements(DIALOG);
            if (dialogs.isEmpty()) {
                return null;
            }
            WebElement last = dialogs.get(dialogs.size() - 1);
            return last.isDisplayed() ? last : null;
        });
    }

    public FormModal setText(String fieldName, String value) {
        WebElement field = fieldFor("text", fieldName);
        field.clear();
        field.sendKeys(value);
        return this;
    }

    public FormModal setNumber(String fieldName, String value) {
        WebElement field = fieldFor("number", fieldName);
        field.clear();
        field.sendKeys(value);
        return this;
    }

    /**
     * Clicks an inline "create new related record" action button, identified
     * by InvenTree's {@code action-button-<action-name>} convention (e.g.
     * {@code action-button-create-new-parameter-template}). Returns a new
     * FormModal bound to the nested dialog this opens.
     */
    public FormModal openInlineCreate(String actionName) {
        By locator = By.cssSelector("[aria-label='action-button-" + actionName + "']");
        waitUntilClickable(locator).click();
        return new FormModal(driver);
    }

    /** Waits briefly for the server-validation error banner; false if it never appears. */
    public boolean hasFormError() {
        return waitForDialogText(FORM_ERROR_BANNER);
    }

    /** Waits briefly for the given text to appear anywhere within this modal. */
    public boolean hasText(String expectedText) {
        return waitForDialogText(By.xpath(".//*[contains(text(),'" + expectedText + "')]"));
    }

    private boolean waitForDialogText(By locator) {
        try {
            wait.until(d -> !dialog.findElements(locator).isEmpty());
            return true;
        } catch (org.openqa.selenium.TimeoutException notShown) {
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
        clickButtonByText("Submit");
        wait.until(d -> {
            try {
                return !dialog.isDisplayed() || !dialog.findElements(FORM_ERROR_BANNER).isEmpty();
            } catch (org.openqa.selenium.StaleElementReferenceException goneFromDom) {
                return true;
            }
        });
    }

    public void cancel() {
        clickButtonByText("Cancel");
    }

    private WebElement fieldFor(String type, String fieldName) {
        return waitUntilClickable(By.cssSelector("[aria-label='" + type + "-field-" + fieldName + "']"));
    }

    private void clickButtonByText(String text) {
        waitUntilClickable(By.xpath(".//button[normalize-space()='" + text + "']")).click();
    }

    private WebElement waitUntilClickable(By locator) {
        return wait.until(d -> {
            try {
                WebElement el = dialog.findElement(locator);
                return (el.isDisplayed() && el.isEnabled()) ? el : null;
            } catch (NoSuchElementException notYetPresent) {
                return null;
            }
        });
    }
}
