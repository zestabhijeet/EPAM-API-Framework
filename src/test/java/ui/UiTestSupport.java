package ui;

import constant.ApplicationConstant;
import files.ConfigManager;
import inventree.InvenTreeApiSupport;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.SkipException;

/**
 * Shared Selenium/config plumbing for the InvenTree UI suites: the
 * "skip if not configured" guard and WebDriver creation. Reuses
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

    /**
     * Headless by default (matches CI); pass -Dui.headless=false for a
     * visible browser during local debugging.
     */
    static WebDriver newDriver() {
        ChromeOptions options = new ChromeOptions();
        if (!"false".equals(System.getProperty("ui.headless"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1440,1000");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        return new ChromeDriver(options);
    }

    static String unique(String prefix) {
        return InvenTreeApiSupport.unique(prefix);
    }
}
