package ui;

import client.RestClient;
import inventree.InvenTreeApiSupport;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ui.pages.FormModal;
import ui.pages.LoginPage;
import ui.pages.PartCategoryPage;
import ui.pages.PartDetailPage;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * UI-FLOW-001 (Phase 3's required cross-functional flow): create a Part,
 * add a Parameter (creating its Template inline), add Stock, then verify
 * the stock is reflected both on the Part page and aggregated in the
 * parent Category's Parts table.
 *
 * Every step and assertion here was verified by hand against a live
 * InvenTree instance before being automated (see the aria-label
 * conventions documented in {@link ui.pages.FormModal}).
 */
public class InvenTreeCrossFunctionalFlowUITest {

    private WebDriver driver;
    private Integer categoryId;
    private String categoryName;
    private Integer partId;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        UiTestSupport.requireConfigured();

        categoryName = UiTestSupport.unique("UI Flow Category");
        Response categoryResponse = RestClient.post(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.CATEGORY_PATH, Map.of("name", categoryName));
        categoryId = categoryResponse.jsonPath().getInt("pk");

        driver = UiTestSupport.newDriver();
        new LoginPage(driver).open(UiTestSupport.BASE_URL).login(UiTestSupport.USERNAME, UiTestSupport.PASSWORD);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (partId != null) {
            RestClient.patch(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PART_PATH + partId + "/",
                    Map.of("active", false));
            RestClient.delete(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PART_PATH + partId + "/");
        }
        if (categoryId != null) {
            RestClient.delete(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.CATEGORY_PATH + categoryId + "/",
                    Map.of("delete_parts", false, "delete_child_categories", false));
        }
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void createPartAddParameterAddStockAndVerifyInCategoryView() {
        String partName = UiTestSupport.unique("UI Flow Part");
        String templateName = UiTestSupport.unique("Color");
        String parameterValue = "Red";

        // 1. Create the Part inside the Category
        PartCategoryPage categoryPage = PartCategoryPage.openList(driver, UiTestSupport.BASE_URL);
        categoryPage.openCategoryByName(categoryName);
        categoryPage.openPartsTab();
        categoryPage.openCreatePartModal().setText("name", partName).submit();

        Response partLookup = RestClient.get(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.PART_PATH + "?search=" + partName + "&limit=10");
        assertEquals(partLookup.jsonPath().getInt("count"), 1, "The Part must exist immediately after creation");
        partId = partLookup.jsonPath().getInt("results[0].pk");

        driver.get(UiTestSupport.BASE_URL + "/web/part/" + partId + "/details");
        PartDetailPage partPage = new PartDetailPage(driver);
        assertTrue(partPage.waitForStatusBadgeToContain("NO STOCK"), "A freshly created Part must show NO STOCK");

        // 2. Add a Parameter, creating its Template inline (none exists yet for this unique name)
        FormModal addParameterModal = partPage.openAddParameterModal();
        FormModal newTemplateModal = addParameterModal.openInlineCreate("create-new-parameter-template");
        newTemplateModal.setText("name", templateName).submit();
        addParameterModal.setText("data", parameterValue).submit();

        assertTrue(partPage.parametersTableContains(templateName, parameterValue),
                "The Parameters tab must list the newly added parameter");

        // 3. Add Stock. Submitting this form navigates to the new Stock
        // Item's own page (verified live), not back to the Part - so return
        // to the Part page explicitly before checking its status badge.
        partPage.openAddStockModal().submit();
        driver.get(UiTestSupport.BASE_URL + "/web/part/" + partId + "/details");
        partPage = new PartDetailPage(driver);
        assertTrue(partPage.waitForStatusBadgeToContain("IN STOCK: 1"),
                "Adding one unit of stock must update the status badge to IN STOCK: 1");

        // 4. Verify stock is aggregated in the parent Category's Parts view
        categoryPage = PartCategoryPage.openList(driver, UiTestSupport.BASE_URL);
        categoryPage.openCategoryByName(categoryName);
        categoryPage.openPartsTab();
        assertEquals(categoryPage.getTotalStockForPart(partName), "1",
                "The Category's Parts table must reflect the Part's stock as Total Stock=1");
    }
}
