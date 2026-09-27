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
    private final Locator partCategoriesHeading;
    private final Locator plusButton;
    private final Locator categoryDetailsHeading;
    private final Locator partsTabInPartCategoryTablist;
    private final Locator partsTabAnywhere;
    private final Locator createPartMenuItem;
    private final Locator totalStockColumnHeader;
    private final Locator tableHeaders;

    private PartCategoryPage(Page page) {
        this.page = page;
        this.partCategoriesHeading = page.locator("text=Part Categories").first();
        this.plusButton = page.locator("button:has(svg.tabler-icon-plus)").first();
        this.categoryDetailsHeading = page.locator("text=Category Details").first();
        this.partsTabInPartCategoryTablist = page.locator("[aria-label='panel-tabs-partcategory']").locator(":text-is(\"Parts\")");
        this.partsTabAnywhere = page.locator(":text-is(\"Parts\")").first();
        this.createPartMenuItem = page.locator(":text-is(\"Create Part\")");
        this.totalStockColumnHeader = page.locator("table thead th:has-text(\"Total Stock\")");
        this.tableHeaders = page.locator("table thead th");
    }

    public static PartCategoryPage openList(Page page, String baseUrl) {
        page.navigate(baseUrl + "/web/part/category/index/subcategories");
        PartCategoryPage categoryPage = new PartCategoryPage(page);
        categoryPage.partCategoriesHeading.waitFor();
        return categoryPage;
    }

    public FormModal openCreateCategoryModal() {
        plusButton.click();
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
        categoryDetailsHeading.waitFor();
    }

    public void openPartsTab() {
        // Scoped to the category detail page's own left-hand tab list -
        // the top nav bar also has an unrelated "Parts" link, verified
        // directly against a running instance via each tablist's aria-label.
        partsTabInPartCategoryTablist.click();
        partsTabAnywhere.waitFor();
    }

    public FormModal openCreatePartModal() {
        plusButton.click();
        createPartMenuItem.click();
        return new FormModal(page);
    }

    public void openPartByName(String name) {
        page.locator(":text-is(\"" + name + "\")").first().click();
    }

    /** Reads the "Total Stock" column of the row for the named Part, by header position (not assumed column order). */
    public String getTotalStockForPart(String partName) {
        totalStockColumnHeader.waitFor();
        int columnIndex = -1;
        int count = tableHeaders.count();
        for (int i = 0; i < count; i++) {
            if (tableHeaders.nth(i).textContent().trim().equals("Total Stock")) {
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
