package ui;

import client.RestClient;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import inventree.InvenTreeApiSupport;
import io.restassured.response.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ui.pages.FormModal;
import ui.pages.LoginPage;
import ui.pages.PartCategoryPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * UI-CAT-001/002: creating a Part Category through the UI, including the
 * client-visible validation error when Name is left blank.
 */
public class InvenTreePartCategoryUITest {

    private Playwright playwright;
    private Browser browser;
    private Page page;
    private final List<Integer> createdCategoryIds = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        UiTestSupport.requireConfigured();
        playwright = UiTestSupport.newPlaywright();
        browser = UiTestSupport.newBrowser(playwright);
        page = UiTestSupport.newPage(browser);
        new LoginPage(page).open(UiTestSupport.BASE_URL).login(UiTestSupport.USERNAME, UiTestSupport.PASSWORD);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        for (Integer id : createdCategoryIds) {
            RestClient.delete(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.CATEGORY_PATH + id + "/",
                    Map.of("delete_parts", false, "delete_child_categories", false));
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    public void createCategoryWithOnlyRequiredName() {
        String name = UiTestSupport.unique("UI Category");
        PartCategoryPage.openList(page, UiTestSupport.BASE_URL)
                .openCreateCategoryModal()
                .setText("name", name)
                .submit();

        Response lookup = RestClient.get(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.CATEGORY_PATH + "?search=" + name + "&limit=10");
        assertTrue(lookup.jsonPath().getInt("count") >= 1, "The category created via the UI must exist via the API");
        createdCategoryIds.add(lookup.jsonPath().getInt("results[0].pk"));
    }

    @Test
    public void categoryCreationRejectsBlankName() {
        FormModal modal = PartCategoryPage.openList(page, UiTestSupport.BASE_URL).openCreateCategoryModal();
        modal.submit();
        assertTrue(modal.hasFormError(), "Submitting with a blank Name must show the form error banner");
        assertTrue(modal.hasText("This field is required."), "The Name field must show its specific validation message");
        modal.cancel();
    }
}
