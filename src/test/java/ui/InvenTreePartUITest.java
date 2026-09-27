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
import ui.pages.PartDetailPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * UI-PART-001..005: Part creation (positive/negative), editing, and the
 * Part Actions menu's Delete-disabled-while-active behaviour.
 */
public class InvenTreePartUITest {

    private Playwright playwright;
    private Browser browser;
    private Page page;
    private Integer categoryId;
    private String categoryName;
    private final List<Integer> createdPartIds = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        UiTestSupport.requireConfigured();

        categoryName = UiTestSupport.unique("UI Part Test Category");
        Response categoryResponse = RestClient.post(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.CATEGORY_PATH, Map.of("name", categoryName));
        categoryId = categoryResponse.jsonPath().getInt("pk");

        playwright = UiTestSupport.newPlaywright();
        browser = UiTestSupport.newBrowser(playwright);
        page = UiTestSupport.newPage(browser);
        new LoginPage(page).open(UiTestSupport.BASE_URL).login(UiTestSupport.USERNAME, UiTestSupport.PASSWORD);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        for (Integer partId : createdPartIds) {
            RestClient.patch(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PART_PATH + partId + "/",
                    Map.of("active", false));
            RestClient.delete(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PART_PATH + partId + "/");
        }
        if (categoryId != null) {
            RestClient.delete(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.CATEGORY_PATH + categoryId + "/",
                    Map.of("delete_parts", false, "delete_child_categories", false));
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    private PartCategoryPage openCategoryParts() {
        PartCategoryPage categoryPage = PartCategoryPage.openList(page, UiTestSupport.BASE_URL);
        categoryPage.openCategoryByName(categoryName);
        categoryPage.openPartsTab();
        return categoryPage;
    }

    @Test
    public void createPartWithOnlyRequiredName() {
        String name = UiTestSupport.unique("UI Part");
        openCategoryParts().openCreatePartModal().setText("name", name).submit();

        Response lookup = RestClient.get(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.PART_PATH + "?search=" + name + "&limit=10");
        assertEquals(lookup.jsonPath().getInt("count"), 1, "The part created via the UI must exist via the API");
        createdPartIds.add(lookup.jsonPath().getInt("results[0].pk"));

        PartCategoryPage categoryPage = openCategoryParts();
        assertEquals(categoryPage.getTotalStockForPart(name), "No stock", "A freshly created Part shows 'No stock' in the Category's Parts table");
    }

    @Test
    public void partCreationRejectsBlankName() {
        FormModal modal = openCategoryParts().openCreatePartModal();
        modal.submit();
        assertTrue(modal.hasFormError(), "Submitting with a blank Name must show the form error banner");
        assertTrue(modal.hasText("This field is required."), "The Name field must show its specific validation message");
        modal.cancel();
    }

    @Test(dependsOnMethods = "createPartWithOnlyRequiredName")
    public void editPartNameUpdatesDetailPage() {
        Integer partId = createdPartIds.get(0);
        String newName = UiTestSupport.unique("UI Part Renamed");

        page.navigate(UiTestSupport.BASE_URL + "/web/part/" + partId + "/details");
        PartDetailPage detailPage = new PartDetailPage(page);
        detailPage.openEditModal().setText("name", newName).submit();

        Response verify = RestClient.get(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PART_PATH + partId + "/");
        assertEquals(verify.jsonPath().getString("name"), newName, "The Part's name must be updated via the Edit modal");
    }

    @Test(dependsOnMethods = "createPartWithOnlyRequiredName")
    public void deleteIsDisabledForAnActivePart() {
        Integer partId = createdPartIds.get(0);
        page.navigate(UiTestSupport.BASE_URL + "/web/part/" + partId + "/details");
        PartDetailPage detailPage = new PartDetailPage(page);
        assertTrue(detailPage.isDeleteActionDisabled(),
                "Delete must be disabled in the Part Actions menu while the Part is active, matching the API's block on deleting active Parts");
    }
}
