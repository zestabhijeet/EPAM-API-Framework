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
import ui.pages.LoginPage;
import ui.pages.PartDetailPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * UI-PRM-*: editing and deleting an existing Parameter row through its own
 * row-level action menu (as opposed to {@link InvenTreeCrossFunctionalFlowUITest},
 * which only adds a Parameter) - verified directly against a running
 * instance, including the "Delete Parameter" confirmation dialog's exact
 * wording.
 */
public class InvenTreePartParametersUITest {

    private Playwright playwright;
    private Browser browser;
    private Page page;
    private Integer categoryId;
    private Integer templateId;
    private String templateName;
    private final List<Integer> createdPartIds = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        UiTestSupport.requireConfigured();

        Response categoryResponse = RestClient.post(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.CATEGORY_PATH, Map.of("name", UiTestSupport.unique("UI Parameters Category")));
        categoryId = categoryResponse.jsonPath().getInt("pk");

        templateName = UiTestSupport.unique("UI Parameter Template");
        Response templateResponse = RestClient.post(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.PARAMETER_TEMPLATE_PATH, Map.of("name", templateName));
        templateId = templateResponse.jsonPath().getInt("pk");

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
        if (templateId != null) {
            RestClient.delete(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PARAMETER_TEMPLATE_PATH + templateId + "/");
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

    /** Creates a Part with one Parameter already set (via API, per the project's API-driven UI fixture convention) and returns its pk. */
    private Integer createPartWithParameter(String partName, String parameterValue) {
        Response partResponse = RestClient.post(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PART_PATH,
                Map.of("category", categoryId, "name", partName));
        Integer partId = partResponse.jsonPath().getInt("pk");
        createdPartIds.add(partId);

        RestClient.post(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PARAMETER_PATH,
                Map.of("template", templateId, "model_type", "part", "model_id", partId, "data", parameterValue));
        return partId;
    }

    @Test
    public void editParameterValueUpdatesTable() {
        Integer partId = createPartWithParameter(UiTestSupport.unique("UI Part Param Edit"), "Red");

        page.navigate(UiTestSupport.BASE_URL + "/web/part/" + partId + "/details");
        PartDetailPage detailPage = new PartDetailPage(page);
        detailPage.openEditParameterModal(0).setText("data", "Blue").submit();

        Response verify = RestClient.get(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.PARAMETER_PATH + "?part=" + partId + "&limit=10");
        assertEquals(verify.jsonPath().getString("results[0].data"), "Blue",
                "Editing the Parameter's row-level action menu must update its value via the API");
    }

    @Test
    public void deleteParameterRemovesItFromTable() {
        Integer partId = createPartWithParameter(UiTestSupport.unique("UI Part Param Delete"), "Green");

        page.navigate(UiTestSupport.BASE_URL + "/web/part/" + partId + "/details");
        PartDetailPage detailPage = new PartDetailPage(page);
        detailPage.openDeleteParameterModal(0).confirmDelete();

        assertFalse(detailPage.parametersTableContains(templateName, "Green"),
                "The deleted Parameter must no longer appear in the Parameters table");
        Response verify = RestClient.get(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.PARAMETER_PATH + "?part=" + partId + "&limit=10");
        assertEquals(verify.jsonPath().getInt("count"), 0, "The Parameter must be deleted via the API");
    }
}
