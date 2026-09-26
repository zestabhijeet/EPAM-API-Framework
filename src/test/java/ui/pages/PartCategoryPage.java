package ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Part Category list and detail pages: creating a Category, opening one,
 * and reading its "Parts" sub-tab (including the Total Stock column used by
 * the cross-functional flow to confirm stock aggregation at category level).
 */
public class PartCategoryPage {

    private final Page page;

    private PartCategoryPage(Page page) {
        this.page = page;
    }

    public static PartCategoryPage openList(Page page, String baseUrl) {
        page.navigate(baseUrl + "/web/part/category/index/subcategories");
        PartCategoryPage categoryPage = new PartCategoryPage(page);
        page.locator("text=Part Categories").first().waitFor();
        return categoryPage;
    }

    public FormModal openCreateCategoryModal() {
        plusButton().click();
        return new FormModal(page);
    }

    /**
     * InvenTree's data-table rows render clickable names as plain
     * {@code <div>}/{@code <span>} elements with a React click handler, not
     * real {@code <a>} tags - verified directly against a running instance -
     * so a text-based locator is used instead of a link role/selector.
     */
    public void openCategoryByName(String name) {
        page.locator(":text-is(\"" + name + "\")").first().click();
        page.locator("text=Category Details").first().waitFor();
    }

    public void openPartsTab() {
        // Scoped to the category detail page's own left-hand tab list -
        // the top nav bar also has an unrelated "Parts" link, verified
        // directly against a running instance via each tablist's aria-label.
        page.locator("[aria-label='panel-tabs-partcategory']").locator(":text-is(\"Parts\")").click();
        page.locator(":text-is(\"Parts\")").first().waitFor();
    }

    public FormModal openCreatePartModal() {
        plusButton().click();
        page.locator(":text-is(\"Create Part\")").click();
        return new FormModal(page);
    }

    public void openPartByName(String name) {
        page.locator(":text-is(\"" + name + "\")").first().click();
    }

    private Locator plusButton() {
        return page.locator("button:has(svg.tabler-icon-plus)").first();
    }

    /** Reads the "Total Stock" column of the row for the named Part, by header position (not assumed column order). */
    public String getTotalStockForPart(String partName) {
        page.locator("table thead th:has-text(\"Total Stock\")").waitFor();
        Locator headers = page.locator("table thead th");
        int columnIndex = -1;
        int count = headers.count();
        for (int i = 0; i < count; i++) {
            if (headers.nth(i).textContent().trim().equals("Total Stock")) {
                columnIndex = i;
                break;
            }
        }
        if (columnIndex < 0) {
            throw new IllegalStateException("No 'Total Stock' column header found in the Parts table");
        }

        Locator row = page.locator("tr:has-text(\"" + partName + "\")").first();
        row.waitFor();
        return row.locator("td").nth(columnIndex).textContent();
    }
}
