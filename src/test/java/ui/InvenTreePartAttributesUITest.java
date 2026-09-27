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
import ui.pages.PartDetailPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * UI-ATT-*: toggling a Part's boolean attribute switches through the Edit
 * modal (Active), and the Locked attribute's effect on that same modal
 * (Submit is disabled entirely, not just individual fields, once a Part is
 * locked) - verified directly against a running instance.
 */
public class InvenTreePartAttributesUITest {

    private Playwright playwright;
    private Browser browser;
    private Page page;
    private Integer categoryId;
    private final List<Integer> createdPartIds = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        UiTestSupport.requireConfigured();

        Response categoryResponse = RestClient.post(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.CATEGORY_PATH, Map.of("name", UiTestSupport.unique("UI Attributes Category")));
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

    private Integer createPart(String name) {
        Response created = RestClient.post(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PART_PATH,
                Map.of("category", categoryId, "name", name));
        Integer partId = created.jsonPath().getInt("pk");
        createdPartIds.add(partId);
        return partId;
    }

    @Test
    public void editingPartTogglesActiveStatus() {
        Integer partId = createPart(UiTestSupport.unique("UI Part Active Toggle"));

        page.navigate(UiTestSupport.BASE_URL + "/web/part/" + partId + "/details");
        PartDetailPage detailPage = new PartDetailPage(page);
        detailPage.openEditModal().setBoolean("active", false).submit();

        Response verify = RestClient.get(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PART_PATH + partId + "/");
        assertFalse(verify.jsonPath().getBoolean("active"), "Toggling the Active switch off in the Edit modal must update the Part via the API");
    }

    /**
     * Locking a Part through the Edit modal disables that same modal's
     * Submit button on every subsequent open - verified directly against a
     * running instance that this is a whole-form disable (Submit
     * data-disabled), not per-field. The Part is unlocked again immediately
     * after the assertion so the class's shared teardown can still delete it
     * - a locked Part rejects DELETE with "Cannot delete this part as it is
     * locked", verified directly against a running instance.
     */
    @Test
    public void lockedPartDisablesEditSubmit() {
        Integer partId = createPart(UiTestSupport.unique("UI Part Locked"));

        page.navigate(UiTestSupport.BASE_URL + "/web/part/" + partId + "/details");
        PartDetailPage detailPage = new PartDetailPage(page);
        detailPage.openEditModal().setBoolean("locked", true).submit();

        page.navigate(UiTestSupport.BASE_URL + "/web/part/" + partId + "/details");
        detailPage = new PartDetailPage(page);
        FormModal editModal = detailPage.openEditModal();
        assertTrue(editModal.isSubmitDisabled(), "Editing a locked Part must disable the Edit modal's Submit button");
        editModal.cancel();

        RestClient.patch(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PART_PATH + partId + "/",
                Map.of("locked", false));
    }
}
