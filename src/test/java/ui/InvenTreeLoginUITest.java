package ui;

import org.openqa.selenium.WebDriver;
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

    private WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        UiTestSupport.requireConfigured();
        driver = UiTestSupport.newDriver();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void validCredentialsLogInSuccessfully() {
        // AppShell's constructor already waits for the post-login nav bar to
        // render; that (plus the URL) is a more durable signal than the
        // transient success toast, which can auto-dismiss before we check it.
        new LoginPage(driver).open(UiTestSupport.BASE_URL).login(UiTestSupport.USERNAME, UiTestSupport.PASSWORD);
        assertTrue(driver.getCurrentUrl().contains("/web/home"), "A valid login must land on the dashboard");
    }

    @Test(dependsOnMethods = "validCredentialsLogInSuccessfully")
    public void loggedInUserCanLogOut() {
        AppShell shell = new AppShell(driver);
        LoginPage loginPage = shell.logout();
        assertTrue(loginPage.isDisplayed(), "Logging out must return the user to the login page");
    }

    @Test(dependsOnMethods = "loggedInUserCanLogOut")
    public void incorrectPasswordIsRejected() {
        LoginPage loginPage = new LoginPage(driver).open(UiTestSupport.BASE_URL);
        loginPage.submitExpectingFailure(UiTestSupport.USERNAME, "wrong-password-" + System.nanoTime());
        assertTrue(loginPage.hasLoginFailedNotification(), "A 'Login failed' notification should appear");
        assertFalse(driver.getCurrentUrl().contains("/web/home"), "An incorrect password must not reach the dashboard");
    }

}
