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

    public AppShell(Page page) {
        this.page = page;
        page.locator(":text-is(\"Parts\")").first().waitFor();
    }

    public LoginPage logout() {
        page.locator(":text-is(\"admin\")").click();
        page.locator("text=Logout").click();
        LoginPage loginPage = new LoginPage(page);
        page.locator("[aria-label='login-username']").waitFor();
        return loginPage;
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
