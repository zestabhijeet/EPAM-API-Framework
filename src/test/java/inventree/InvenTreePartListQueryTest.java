package inventree;

import client.RestClient;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pojo.InventreePart;
import pojo.InventreePartCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static inventree.InvenTreeApiSupport.CATEGORY_PATH;
import static inventree.InvenTreeApiSupport.PART_PATH;
import static inventree.InvenTreeApiSupport.authenticatedSpec;
import static inventree.InvenTreeApiSupport.requireConfigured;
import static inventree.InvenTreeApiSupport.unique;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Filtering, search, and pagination coverage for {@code GET /api/part/}.
 * Fixtures are created (and cleaned up) by this suite so assertions are
 * independent of whatever else exists on the InvenTree instance.
 */
public class InvenTreePartListQueryTest {

    private static final RequestSpecification SPEC = authenticatedSpec();

    private final List<Integer> createdPartIds = new ArrayList<>();
    private final List<Integer> createdCategoryIds = new ArrayList<>();
    private String searchTag;
    private Integer categoryId;
    private Integer partInCategoryId;
    private Integer partOutsideCategoryId;

    @BeforeClass(alwaysRun = true)
    public void setUpFixtures() {
        requireConfigured();
        searchTag = unique("ListQuery");

        InventreePartCategory category = new InventreePartCategory();
        category.setName(unique("ListQuery Category"));
        Response categoryResponse = RestClient.post(SPEC, CATEGORY_PATH, category);
        assertEquals(categoryResponse.statusCode(), 201, "Fixture category must be created");
        categoryId = categoryResponse.jsonPath().getInt("pk");
        createdCategoryIds.add(categoryId);

        partInCategoryId = createPart(searchTag + "-InCategory", categoryId);
        partOutsideCategoryId = createPart(searchTag + "-Outside", null);
    }

    private Integer createPart(String name, Integer categoryIdOrNull) {
        InventreePart part = new InventreePart();
        part.setName(name);
        if (categoryIdOrNull != null) {
            part.setCategory(categoryIdOrNull);
        }
        Response response = RestClient.post(SPEC, PART_PATH, part);
        assertEquals(response.statusCode(), 201, "Fixture part must be created");
        Integer id = response.jsonPath().getInt("pk");
        createdPartIds.add(id);
        return id;
    }

    @Test
    public void searchFindsPartsByNameSubstring() {
        Response response = RestClient.get(SPEC, PART_PATH + "?search=" + searchTag + "&limit=10");
        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schema/inventree-paginated-list.json"));
        assertEquals(response.jsonPath().getInt("count"), 2, "Search must find exactly the two fixture parts");
        List<String> names = response.jsonPath().getList("results.name", String.class);
        assertTrue(names.stream().allMatch(n -> n.contains(searchTag)),
                "All returned parts must match the search term");
    }

    @Test
    public void filteringByCategoryReturnsOnlyMatchingParts() {
        Response response = RestClient.get(SPEC, PART_PATH + "?category=" + categoryId + "&limit=10");
        response.then().statusCode(200);
        List<Integer> ids = response.jsonPath().getList("results.pk", Integer.class);
        assertTrue(ids.contains(partInCategoryId), "Category filter must include the part assigned to it");
        assertTrue(!ids.contains(partOutsideCategoryId), "Category filter must exclude parts outside it");
    }

    @Test
    public void paginationLimitAndOffsetControlResultWindow() {
        Response page1 = RestClient.get(SPEC, PART_PATH + "?search=" + searchTag + "&limit=1&offset=0");
        Response page2 = RestClient.get(SPEC, PART_PATH + "?search=" + searchTag + "&limit=1&offset=1");

        page1.then().statusCode(200);
        page2.then().statusCode(200);

        assertEquals(page1.jsonPath().getList("results").size(), 1, "First page must contain exactly one result");
        assertEquals(page2.jsonPath().getList("results").size(), 1, "Second page must contain exactly one result");
        assertNotNull(page2.jsonPath().getString("previous"), "Second page must expose a previous link");

        String firstPagePartName = page1.jsonPath().getString("results[0].name");
        String secondPagePartName = page2.jsonPath().getString("results[0].name");
        assertTrue(!firstPagePartName.equals(secondPagePartName),
                "Paginated pages must return distinct results, not the same record twice");
    }

    @AfterClass(alwaysRun = true)
    public void cleanupFixtures() {
        for (Integer partId : createdPartIds) {
            try {
                InventreePart deactivate = new InventreePart();
                deactivate.setActive(false);
                RestClient.patch(SPEC, PART_PATH + partId + "/", deactivate);
                Response response = RestClient.delete(SPEC, PART_PATH + partId + "/");
                if (response.statusCode() != 204 && response.statusCode() != 404) {
                    Allure.step("Cleanup failed for Part " + partId + ": HTTP " + response.statusCode());
                }
            } catch (RuntimeException cleanupFailure) {
                Allure.step("Cleanup failed for Part " + partId + ": " + cleanupFailure.getMessage());
            }
        }
        for (Integer categoryId : createdCategoryIds) {
            try {
                Map<String, Object> cascadeFlags = new HashMap<>();
                cascadeFlags.put("delete_parts", false);
                cascadeFlags.put("delete_child_categories", false);
                Response response = RestClient.delete(SPEC, CATEGORY_PATH + categoryId + "/", cascadeFlags);
                if (response.statusCode() != 204 && response.statusCode() != 404) {
                    Allure.step("Cleanup failed for Category " + categoryId + ": HTTP " + response.statusCode());
                }
            } catch (RuntimeException cleanupFailure) {
                Allure.step("Cleanup failed for Category " + categoryId + ": " + cleanupFailure.getMessage());
            }
        }
    }
}
