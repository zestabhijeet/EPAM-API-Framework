package ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import constant.ApplicationConstant;
import files.ConfigManager;
import inventree.InvenTreeApiSupport;
import org.testng.SkipException;

/**
 * Shared Playwright/config plumbing for the InvenTree UI suites: the
 * "skip if not configured" guard and browser/page creation. Reuses
 * {@link InvenTreeApiSupport}'s BASE_URL/isBlank so both the API and UI
 * suites agree on what "not configured" means.
 */
final class UiTestSupport {

    static final String BASE_URL = InvenTreeApiSupport.BASE_URL;
    static final String USERNAME = ConfigManager.getProperty(ApplicationConstant.INVENTREE_UI_USERNAME_KEY, "");
    static final String PASSWORD = ConfigManager.getProperty(ApplicationConstant.INVENTREE_UI_PASSWORD_KEY, "");

    private UiTestSupport() {
    }

    static void requireConfigured() {
        if (InvenTreeApiSupport.isBlank(BASE_URL) || InvenTreeApiSupport.isBlank(USERNAME) || InvenTreeApiSupport.isBlank(PASSWORD)) {
            throw new SkipException("InvenTree UI suite skipped: set INVENTREE_BASE_URL, INVENTREE_UI_USERNAME, INVENTREE_UI_PASSWORD");
        }
    }

    static Playwright newPlaywright() {
        return Playwright.create();
    }

    /** Headless by default (matches CI); pass -Dui.headless=false for a visible browser during local debugging. */
    static Browser newBrowser(Playwright playwright) {
        boolean headless = !"false".equals(System.getProperty("ui.headless"));
        return playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
    }

    static Page newPage(Browser browser) {
        Browser.NewContextOptions options = new Browser.NewContextOptions().setViewportSize(1440, 1000);
        return browser.newContext(options).newPage();
    }

    static String unique(String prefix) {
        return InvenTreeApiSupport.unique(prefix);
    }
}
