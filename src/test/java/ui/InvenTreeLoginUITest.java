package ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ui.pages.AppShell;
import ui.pages.LoginPage;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * UI-AUTH-001..004: login with valid/invalid credentials, and logout.
 */
public class InvenTreeLoginUITest {

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        UiTestSupport.requireConfigured();
        playwright = UiTestSupport.newPlaywright();
        browser = UiTestSupport.newBrowser(playwright);
        page = UiTestSupport.newPage(browser);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    public void validCredentialsLogInSuccessfully() {
        new LoginPage(page).open(UiTestSupport.BASE_URL).login(UiTestSupport.USERNAME, UiTestSupport.PASSWORD);
        assertTrue(page.url().contains("/web/home"), "A valid login must land on the dashboard");
    }

    @Test(dependsOnMethods = "validCredentialsLogInSuccessfully")
    public void loggedInUserCanLogOut() {
        AppShell shell = new AppShell(page);
        LoginPage loginPage = shell.logout();
        assertTrue(loginPage.isDisplayed(), "Logging out must return the user to the login page");
    }

    @Test(dependsOnMethods = "loggedInUserCanLogOut")
    public void incorrectPasswordIsRejected() {
        LoginPage loginPage = new LoginPage(page).open(UiTestSupport.BASE_URL);
        loginPage.submitExpectingFailure(UiTestSupport.USERNAME, "wrong-password-" + System.nanoTime());
        assertTrue(loginPage.hasLoginFailedNotification(), "A 'Login failed' notification should appear");
        assertFalse(page.url().contains("/web/home"), "An incorrect password must not reach the dashboard");
    }
}
