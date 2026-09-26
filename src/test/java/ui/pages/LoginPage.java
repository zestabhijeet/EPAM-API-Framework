package ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage {

    private static final By USERNAME = By.cssSelector("[aria-label='login-username']");
    private static final By PASSWORD = By.cssSelector("[aria-label='login-password']");
    private static final By LOG_IN_BUTTON = By.xpath("//button[normalize-space()='Log In']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public LoginPage open(String baseUrl) {
        driver.get(baseUrl + "/web/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME));
        return this;
    }

    public AppShell login(String username, String password) {
        driver.findElement(USERNAME).sendKeys(username);
        driver.findElement(PASSWORD).sendKeys(password);
        driver.findElement(LOG_IN_BUTTON).click();
        return new AppShell(driver);
    }

    /** For the invalid-credentials path, where the app deliberately stays on this page. */
    public void submitExpectingFailure(String username, String password) {
        driver.findElement(USERNAME).sendKeys(username);
        driver.findElement(PASSWORD).sendKeys(password);
        driver.findElement(LOG_IN_BUTTON).click();
    }

    public boolean hasLoginFailedNotification() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'Login failed')]")));
            return true;
        } catch (org.openqa.selenium.TimeoutException notShown) {
            return false;
        }
    }

    public boolean isDisplayed() {
        return !driver.findElements(USERNAME).isEmpty();
    }
}
