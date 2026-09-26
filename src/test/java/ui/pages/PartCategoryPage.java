package ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Part Category list and detail pages: creating a Category, opening one,
 * and reading its "Parts" sub-tab (including the Total Stock column used by
 * the cross-functional flow to confirm stock aggregation at category level).
 */
public class PartCategoryPage {

    private static final By PLUS_ICON_BUTTON = By.xpath("(//button[.//*[local-name()='svg' and contains(@class,'tabler-icon-plus')]])[1]");

    private final WebDriver driver;
    private final WebDriverWait wait;

    private PartCategoryPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public static PartCategoryPage openList(WebDriver driver, String baseUrl) {
        driver.get(baseUrl + "/web/part/category/index/subcategories");
        PartCategoryPage page = new PartCategoryPage(driver);
        page.wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[normalize-space()='Part Categories']")));
        return page;
    }

    public FormModal openCreateCategoryModal() {
        wait.until(ExpectedConditions.elementToBeClickable(PLUS_ICON_BUTTON)).click();
        return new FormModal(driver);
    }

    public void openCategoryByName(String name) {
        clickByExactText(name);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[normalize-space()='Category Details']")));
    }

    public void openPartsTab() {
        // Scoped to the category detail page's own left-hand tab list -
        // the top nav bar also has an unrelated "Parts" link, verified
        // directly against a running instance via each tablist's aria-label.
        By tab = By.xpath("//*[@aria-label='panel-tabs-partcategory']//*[normalize-space()='Parts']");
        wait.until(ExpectedConditions.elementToBeClickable(tab)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[normalize-space()='Parts']")));
    }

    public FormModal openCreatePartModal() {
        wait.until(ExpectedConditions.elementToBeClickable(PLUS_ICON_BUTTON)).click();
        clickByExactText("Create Part");
        return new FormModal(driver);
    }

    public void openPartByName(String name) {
        clickByExactText(name);
    }

    /**
     * InvenTree's data-table rows render clickable names as plain
     * {@code <div>}/{@code <span>} elements with a React click handler, not
     * real {@code <a>} tags - verified directly against a running instance -
     * so a text-based XPath match is used instead of {@code By.linkText}.
     */
    private void clickByExactText(String text) {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//*[normalize-space()='" + text + "'])[1]"))).click();
    }

    /** Reads the "Total Stock" column of the row for the named Part, by header position (not assumed column order). */
    public String getTotalStockForPart(String partName) {
        List<WebElement> headers = driver.findElements(By.cssSelector("table thead th"));
        int columnIndex = -1;
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).getText().trim().equals("Total Stock")) {
                columnIndex = i;
                break;
            }
        }
        if (columnIndex < 0) {
            throw new IllegalStateException("No 'Total Stock' column header found in the Parts table");
        }

        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//tr[.//*[normalize-space()='" + partName + "']]")));
        List<WebElement> cells = row.findElements(By.tagName("td"));
        return cells.get(columnIndex).getText();
    }
}
