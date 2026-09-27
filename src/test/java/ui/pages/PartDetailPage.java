package ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

/**
 * Part detail page: the stock status badge, the Part Actions menu
 * (Duplicate/Edit/Delete), and the Parameters and Stock tabs.
 */
public class PartDetailPage {

    private final Page page;
    private final Locator partDetailsLoadedMarker;
    private final Locator firstStatusBadge;
    private final Locator partTablist;
    private final Locator partActionsMenuButton;
    private final Locator editMenuItem;
    private final Locator deleteButton;
    private final Locator addRowButton;
    private final Locator createParameterMenuItem;
    private final Locator deleteMenuItem;
    private final Locator addStockItemButton;

    public PartDetailPage(Page page) {
        this.page = page;
        this.partDetailsLoadedMarker = page.locator(":text-is(\"Part Details\")").first();
        this.firstStatusBadge = page.locator("span.mantine-Badge-label").first();
        this.partTablist = page.locator("[aria-label='panel-tabs-part']");
        this.partActionsMenuButton = page.locator("[aria-label='action-menu-part-actions']");
        this.editMenuItem = page.locator(":text-is(\"Edit\")");
        this.deleteButton = page.locator("button:has-text(\"Delete\")");
        this.addRowButton = page.locator("button:has(svg.tabler-icon-plus)").first();
        this.createParameterMenuItem = page.locator(":text-is(\"Create Parameter\")");
        this.deleteMenuItem = page.locator(":text-is(\"Delete\")");
        this.addStockItemButton = page.locator("[aria-label='action-button-add-stock-item']");
        partDetailsLoadedMarker.waitFor();
    }

    /**
     * The stock status badge (e.g. "No Stock", "In Stock: 1") is reliably the
     * first Mantine Badge on the page - verified directly against a running
     * instance, including that the many YES/NO attribute badges further down
     * the page all come after it in document order.
     */
    public String getStatusBadgeText() {
        return firstStatusBadge.textContent();
    }

    /**
     * Polls the badge until it contains the expected text. A successful
     * Submit only guarantees the write completed and the modal closed - the
     * page's own data refetch (and so the badge update) can lag slightly
     * behind that, verified directly against a running instance.
     */
    public boolean waitForStatusBadgeToContain(String expectedSubstring) {
        try {
            page.locator("span.mantine-Badge-label:has-text(\"" + expectedSubstring + "\")")
                    .first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
            return true;
        } catch (TimeoutError notShown) {
            return false;
        }
    }

    /**
     * Scoped to the Part detail page's own left-hand tab list - the top nav
     * bar also has an unrelated "Stock" module link, verified directly
     * against a running instance via each tablist's aria-label.
     */
    private void openTab(String tabName) {
        partTablist.locator(":text-is(\"" + tabName + "\")").click();
    }

    // --- Part Actions menu ---

    public FormModal openEditModal() {
        partActionsMenuButton.click();
        editMenuItem.click();
        return new FormModal(page);
    }

    public boolean isDeleteActionDisabled() {
        partActionsMenuButton.click();
        // :text-is("Delete") would resolve to the innermost element carrying
        // that exact text (an inner label <div>), not the <button> itself
        // that actually carries data-disabled - verified directly against a
        // running instance - so this anchors on the button tag instead.
        deleteButton.first().waitFor();
        String disabled = deleteButton.first().getAttribute("data-disabled");
        page.keyboard().press("Escape");
        return disabled != null;
    }

    // --- Parameters tab ---

    public FormModal openAddParameterModal() {
        openTab("Parameters");
        addRowButton.click();
        createParameterMenuItem.click();
        return new FormModal(page);
    }

    public boolean parametersTableContains(String templateName, String dataValue) {
        openTab("Parameters");
        try {
            page.locator("tr:has-text(\"" + templateName + "\"):has-text(\"" + dataValue + "\")")
                    .first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (TimeoutError notShown) {
            return false;
        }
    }

    /**
     * Opens the row-level action menu for the parameter row at {@code rowIndex}
     * (0-based, in table order) and clicks Edit. Rows carry a stable
     * {@code aria-label="row-action-menu-<index>"} - verified directly against
     * a running instance.
     */
    public FormModal openEditParameterModal(int rowIndex) {
        openTab("Parameters");
        page.locator("[aria-label='row-action-menu-" + rowIndex + "']").click();
        editMenuItem.click();
        return new FormModal(page);
    }

    /**
     * Opens the row-level action menu for the parameter row at {@code rowIndex}
     * and clicks Delete. InvenTree shows a custom (non-native) confirmation
     * dialog - "Are you sure you want to delete this item?" with Cancel/Delete
     * buttons - verified directly against a running instance, so this returns
     * a {@link FormModal} bound to that confirmation dialog; callers confirm
     * via {@link FormModal#confirmDelete()}.
     */
    public FormModal openDeleteParameterModal(int rowIndex) {
        openTab("Parameters");
        page.locator("[aria-label='row-action-menu-" + rowIndex + "']").click();
        deleteMenuItem.click();
        return new FormModal(page);
    }

    // --- Part Details panel (top summary fields) ---

    /**
     * Checks whether the given text appears anywhere in the "Part Details"
     * tab - used for asserting core field values (units, category,
     * description, link, etc.) shown after creating a Part with all optional
     * fields set (UI-DET-002/UI-CRT-002). Verified directly against a running
     * instance: Name/IPN/Description/Category/Units/Link render as plain
     * label-value rows on this tab, so a substring text locator is enough.
     */
    public boolean detailsPanelContains(String text) {
        openTab("Part Details");
        return page.locator("text=" + text).first().isVisible();
    }

    // --- Stock tab ---

    public FormModal openAddStockModal() {
        openTab("Stock");
        addStockItemButton.click();
        return new FormModal(page);
    }
}
