package ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Part detail page: the stock status badge, the Part Actions menu
 * (Duplicate/Edit/Delete), and the Parameters and Stock tabs.
 */
public class PartDetailPage {

    private static final By PART_ACTIONS_MENU = By.cssSelector("[aria-label='action-menu-part-actions']");
    private static final By ADD_STOCK_BUTTON = By.cssSelector("[aria-label='action-button-add-stock-item']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public PartDetailPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[normalize-space()='Part Details']")));
    }

    /**
     * The stock status badge (e.g. "No Stock", "In Stock: 1") is reliably the
     * first Mantine Badge on the page - verified directly against a running
     * instance, including that the many YES/NO attribute badges further down
     * the page all come after it in document order.
     */
    public String getStatusBadgeText() {
        By badge = By.cssSelector("span.mantine-Badge-label");
        return wait.until(ExpectedConditions.visibilityOfElementLocated(badge)).getText();
    }

    /**
     * Polls the badge until it contains the expected text. A successful
     * Submit only guarantees the write completed and the modal closed - the
     * page's own data refetch (and so the badge update) can lag slightly
     * behind that, verified directly against a running instance.
     */
    public boolean waitForStatusBadgeToContain(String expectedSubstring) {
        try {
            wait.until(d -> getStatusBadgeText().contains(expectedSubstring));
            return true;
        } catch (org.openqa.selenium.TimeoutException notShown) {
            return false;
        }
    }

    /**
     * Scoped to the Part detail page's own left-hand tab list - the top nav
     * bar also has an unrelated "Stock" module link, verified directly
     * against a running instance via each tablist's aria-label.
     */
    private void openTab(String tabName) {
        By tab = By.xpath("//*[@aria-label='panel-tabs-part']//*[normalize-space()='" + tabName + "']");
        wait.until(ExpectedConditions.elementToBeClickable(tab)).click();
    }

    // --- Part Actions menu ---

    public FormModal openEditModal() {
        wait.until(ExpectedConditions.elementToBeClickable(PART_ACTIONS_MENU)).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[normalize-space()='Edit']"))).click();
        return new FormModal(driver);
    }

    public boolean isDeleteActionDisabled() {
        wait.until(ExpectedConditions.elementToBeClickable(PART_ACTIONS_MENU)).click();
        boolean disabled = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//button[normalize-space()='Delete']")))
                .getAttribute("data-disabled") != null;
        driver.findElement(By.tagName("body")).sendKeys(org.openqa.selenium.Keys.ESCAPE);
        return disabled;
    }

    // --- Parameters tab ---

    public FormModal openAddParameterModal() {
        openTab("Parameters");
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//button[.//*[local-name()='svg' and contains(@class,'tabler-icon-plus')]])[1]"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[normalize-space()='Create Parameter']"))).click();
        return new FormModal(driver);
    }

    public boolean parametersTableContains(String templateName, String dataValue) {
        openTab("Parameters");
        By row = By.xpath("//tr[.//*[normalize-space()='" + templateName + "'] and .//*[normalize-space()='" + dataValue + "']]");
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(row));
            return true;
        } catch (org.openqa.selenium.TimeoutException notShown) {
            return false;
        }
    }

    // --- Stock tab ---

    public FormModal openAddStockModal() {
        openTab("Stock");
        wait.until(ExpectedConditions.elementToBeClickable(ADD_STOCK_BUTTON)).click();
        return new FormModal(driver);
    }
}
