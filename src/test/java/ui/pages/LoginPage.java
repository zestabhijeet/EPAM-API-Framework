package ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

public class LoginPage {

    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public LoginPage open(String baseUrl) {
        page.navigate(baseUrl + "/web/login");
        page.locator("[aria-label='login-username']").waitFor();
        return this;
    }

    public AppShell login(String username, String password) {
        page.locator("[aria-label='login-username']").fill(username);
        page.locator("[aria-label='login-password']").fill(password);
        page.locator(":text-is(\"Log In\")").click();
        return new AppShell(page);
    }

    /** For the invalid-credentials path, where the app deliberately stays on this page. */
    public void submitExpectingFailure(String username, String password) {
        page.locator("[aria-label='login-username']").fill(username);
        page.locator("[aria-label='login-password']").fill(password);
        page.locator(":text-is(\"Log In\")").click();
    }

    public boolean hasLoginFailedNotification() {
        try {
            page.locator("text=Login failed").first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (TimeoutError notShown) {
            return false;
        }
    }

    public boolean isDisplayed() {
        return page.locator("[aria-label='login-username']").count() > 0;
    }
}
