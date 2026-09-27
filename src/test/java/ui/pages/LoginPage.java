package ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

public class LoginPage {

    private final Page page;
    private final Locator usernameField;
    private final Locator passwordField;
    private final Locator logInButton;
    private final Locator loginFailedNotification;

    public LoginPage(Page page) {
        this.page = page;
        this.usernameField = page.locator("[aria-label='login-username']");
        this.passwordField = page.locator("[aria-label='login-password']");
        this.logInButton = page.locator(":text-is(\"Log In\")");
        this.loginFailedNotification = page.locator("text=Login failed").first();
    }

    public LoginPage open(String baseUrl) {
        page.navigate(baseUrl + "/web/login");
        usernameField.waitFor();
        return this;
    }

    /** Waits for this page to be displayed without navigating - e.g. after a client-side redirect (logout). */
    public LoginPage waitUntilDisplayed() {
        usernameField.waitFor();
        return this;
    }

    public AppShell login(String username, String password) {
        usernameField.fill(username);
        passwordField.fill(password);
        logInButton.click();
        return new AppShell(page);
    }

    /** For the invalid-credentials path, where the app deliberately stays on this page. */
    public void submitExpectingFailure(String username, String password) {
        usernameField.fill(username);
        passwordField.fill(password);
        logInButton.click();
    }

    public boolean hasLoginFailedNotification() {
        try {
            loginFailedNotification.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (TimeoutError notShown) {
            return false;
        }
    }

    public boolean isDisplayed() {
        return usernameField.count() > 0;
    }
}
