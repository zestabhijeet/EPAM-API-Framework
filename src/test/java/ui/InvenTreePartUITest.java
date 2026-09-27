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
 * UI-CRT-001..003, UI-DET-002, UI-NEG-002/004: Part creation (required-only,
 * all optional fields, full validation flow), editing, the Part Actions
 * menu's Delete-disabled-while-active behaviour, and negative/boundary
 * validation (duplicate name+IPN+revision, HTML rejection).
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

    /**
     * UI-CRT-003: a single modal instance driven through three submit
     * attempts - blank Name (required-field error), an invalid Link value
     * (field-specific format error), then a corrected resubmit that
     * succeeds - verified directly against a running instance.
     */
    @Test
    public void partCreationFullValidationFlow() {
        FormModal modal = openCategoryParts().openCreatePartModal();
        modal.submit();
        assertTrue(modal.hasFormError(), "Submitting with a blank Name must show the form error banner");
        assertTrue(modal.hasText("This field is required."), "The Name field must show its specific validation message");

        String name = UiTestSupport.unique("UI Part Corrected");
        modal.setText("name", name).setText("link", "not-a-url").submit();
        assertTrue(modal.hasFormError(), "Submitting an invalid Link must show the form error banner");
        assertTrue(modal.hasText("Enter a valid URL."), "The Link field must show its specific validation message");

        modal.setText("link", "https://example.com").submit();

        Response lookup = RestClient.get(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.PART_PATH + "?search=" + name + "&limit=10");
        assertEquals(lookup.jsonPath().getInt("count"), 1, "The corrected resubmit must create the Part");
        createdPartIds.add(lookup.jsonPath().getInt("results[0].pk"));
    }

    /** UI-CRT-002: every optional field set at once, then read back from the Part Details tab. */
    @Test
    public void createPartWithAllOptionalFields() {
        String name = UiTestSupport.unique("UI Part Full");
        openCategoryParts().openCreatePartModal()
                .setText("name", name)
                .setText("IPN", "IPN-FULL-01")
                .setText("description", "Full field description")
                .setText("units", "each")
                .setText("link", "https://example.com/spec-sheet")
                .submit();

        Response lookup = RestClient.get(InvenTreeApiSupport.authenticatedSpec(),
                InvenTreeApiSupport.PART_PATH + "?search=" + name + "&limit=10");
        assertEquals(lookup.jsonPath().getInt("count"), 1, "The Part must be created with all optional fields");
        Integer partId = lookup.jsonPath().getInt("results[0].pk");
        createdPartIds.add(partId);
        assertEquals(lookup.jsonPath().getString("results[0].IPN"), "IPN-FULL-01", "IPN must be saved as entered");
        assertEquals(lookup.jsonPath().getString("results[0].description"), "Full field description", "Description must be saved as entered");
        assertEquals(lookup.jsonPath().getString("results[0].units"), "each", "Units must be saved as entered");

        page.navigate(UiTestSupport.BASE_URL + "/web/part/" + partId + "/details");
        PartDetailPage detailPage = new PartDetailPage(page);
        assertTrue(detailPage.detailsPanelContains("IPN-FULL-01"), "The Part Details tab must display the IPN");
        assertTrue(detailPage.detailsPanelContains("Full field description"), "The Part Details tab must display the Description");
        assertTrue(detailPage.detailsPanelContains("each"), "The Part Details tab must display the Units");
    }

    /**
     * UI-NEG-002: name+IPN+revision must make a unique set, enforced
     * server-side and surfaced in the modal. Verified directly against a
     * running instance that a pure {@code non_field_errors} violation (as
     * opposed to a per-field error like a blank Name) renders only its own
     * specific message as the banner - the generic "Errors exist for one or
     * more form fields" text that {@link FormModal#hasFormError()} checks
     * for does not appear in this case, so this assertion relies on
     * {@link FormModal#hasText} alone.
     */
    @Test
    public void rejectsDuplicateNameIpnRevision() {
        String name = UiTestSupport.unique("UI Part Dup");
        Response created = RestClient.post(InvenTreeApiSupport.authenticatedSpec(), InvenTreeApiSupport.PART_PATH,
                Map.of("category", categoryId, "name", name, "IPN", "DUP-1", "revision", "A"));
        createdPartIds.add(created.jsonPath().getInt("pk"));

        FormModal modal = openCategoryParts().openCreatePartModal();
        modal.setText("name", name).setText("IPN", "DUP-1").setText("revision", "A").submit();
        assertTrue(modal.hasText("The fields name, IPN, revision must make a unique set."),
                "The non-field error must explain which fields must be unique together");
        modal.cancel();
    }

    /** UI-NEG-004: HTML/script content in the Name field is rejected server-side. */
    @Test
    public void rejectsHtmlInNameField() {
        FormModal modal = openCategoryParts().openCreatePartModal();
        modal.setText("name", "<script>alert(1)</script>").submit();
        assertTrue(modal.hasFormError(), "HTML in the Name field must show the form error banner");
        assertTrue(modal.hasText("Remove HTML tags from this value"), "The Name field must show its specific validation message");
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
