package ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * The logged-in application shell: top navigation, user menu/logout, and
 * toast-notification waits shared across every authenticated page.
 */
public class AppShell {

    private static final By PARTS_NAV_TAB = By.xpath("//button[normalize-space()='Parts']");
    private static final By USER_MENU_BUTTON = By.xpath("//button[normalize-space()='admin']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public AppShell(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(PARTS_NAV_TAB));
    }

    public LoginPage logout() {
        driver.findElement(USER_MENU_BUTTON).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[normalize-space()='Logout']"))).click();
        LoginPage loginPage = new LoginPage(driver);
        wait.until(d -> loginPage.isDisplayed());
        return loginPage;
    }

    public void goTo(String baseUrl, String relativePath) {
        driver.get(baseUrl + relativePath);
    }

    /** Waits briefly for a toast/notification containing this text; false if it never appears. */
    public boolean hasToastContaining(String text) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'" + text + "')]")));
            return true;
        } catch (org.openqa.selenium.TimeoutException notShown) {
            return false;
        }
    }
}
