package ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

/**
 * The logged-in application shell: top navigation, user menu/logout, and
 * toast-notification waits shared across every authenticated page.
 */
public class AppShell {

    private final Page page;
    private final Locator partsNavLink;
    private final Locator userMenuButton;
    private final Locator logoutMenuItem;

    public AppShell(Page page) {
        this.page = page;
        this.partsNavLink = page.locator(":text-is(\"Parts\")").first();
        this.userMenuButton = page.locator(":text-is(\"admin\")");
        this.logoutMenuItem = page.locator("text=Logout");
        partsNavLink.waitFor();
    }

    public LoginPage logout() {
        userMenuButton.click();
        logoutMenuItem.click();
        return new LoginPage(page).waitUntilDisplayed();
    }

    /** Waits briefly for a toast/notification containing this text; false if it never appears. */
    public boolean hasToastContaining(String text) {
        try {
            page.locator("text=" + text).first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
            return true;
        } catch (TimeoutError notShown) {
            return false;
        }
    }
}
