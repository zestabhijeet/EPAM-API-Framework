package inventree;

import client.RestClient;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Single-file, ordered E2E coverage for the InvenTree Part Category API
 * ({@code /api/part/category/}), including the relational-integrity
 * behaviour of deleting a category that still has a Part assigned to it.
 */
public class InvenTreePartCategoryE2ETest {

    private static final RequestSpecification SPEC = authenticatedSpec();

    private final List<Integer> createdCategoryIds = new ArrayList<>();
    private final List<Integer> createdPartIds = new ArrayList<>();
    private Integer categoryId;
    private Integer childCategoryId;

    @BeforeClass(alwaysRun = true)
    public void validateEntryPoint() {
        requireConfigured();
        Response response = RestClient.get(SPEC, CATEGORY_PATH + "?limit=1");
        assertEquals(response.statusCode(), 200, "InvenTree Category endpoint must be available");
        response.then().body(matchesJsonSchemaInClasspath("schema/inventree-paginated-list.json"));
    }

    @Test
    public void executeAllCategoryScenariosAsSingleE2EFlow() {
        catCreateMinimal();
        catRetrieveById();
        catPatchDescription();
        catPutReplacement();
        catRejectPutWithoutName();
        catCreateChildAndVerifyPath();
        catRejectDeleteWithoutCascadeFlags();
        catDeleteOrphansAssignedPart();
    }

    @DataProvider(name = "categoryNameBoundaries")
    public Object[][] categoryNameBoundaries() {
        return new Object[][]{
                {"C".repeat(100), 201},
                {"C".repeat(101), 400}
        };
    }

    @Test(dataProvider = "categoryNameBoundaries", dependsOnMethods = "executeAllCategoryScenariosAsSingleE2EFlow")
    public void validateCategoryNameBoundary(String name, int expectedStatus) {
        InventreePartCategory payload = new InventreePartCategory();
        payload.setName(name);
        Response response = RestClient.post(SPEC, CATEGORY_PATH, payload);
        assertEquals(response.statusCode(), expectedStatus,
                "Category name boundary should follow the published maxLength=100 contract");
        if (response.statusCode() == 201) {
            createdCategoryIds.add(response.jsonPath().getInt("pk"));
        }
    }

    private void catCreateMinimal() {
        InventreePartCategory payload = new InventreePartCategory();
        payload.setName(unique("E2E Category"));
        Response response = RestClient.post(SPEC, CATEGORY_PATH, payload);
        response.then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schema/inventree-category-detail.json"))
                .body("name", equalTo(payload.getName()));
        categoryId = response.jsonPath().getInt("pk");
        createdCategoryIds.add(categoryId);
    }

    private void catRetrieveById() {
        RestClient.get(SPEC, categoryPath(categoryId)).then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schema/inventree-category-detail.json"))
                .body("pk", equalTo(categoryId));
    }

    private void catPatchDescription() {
        String description = "PATCHED-" + unique("desc");
        InventreePartCategory payload = new InventreePartCategory();
        payload.setDescription(description);
        RestClient.patch(SPEC, categoryPath(categoryId), payload).then()
                .statusCode(200)
                .body("description", equalTo(description));
    }

    private void catPutReplacement() {
        InventreePartCategory payload = new InventreePartCategory();
        payload.setName(unique("E2E Category PUT"));
        payload.setDescription("replaced-via-put");
        RestClient.put(SPEC, categoryPath(categoryId), payload).then()
                .statusCode(200)
                .body("pk", equalTo(categoryId))
                .body("name", equalTo(payload.getName()))
                .body("description", equalTo("replaced-via-put"));
    }

    private void catRejectPutWithoutName() {
        InventreePartCategory payload = new InventreePartCategory();
        payload.setDescription("no name supplied");
        Response response = RestClient.put(SPEC, categoryPath(categoryId), payload);
        assertTrue(response.statusCode() >= 400 && response.statusCode() < 500,
                "PUT without name must return a client error");
    }

    private void catCreateChildAndVerifyPath() {
        InventreePartCategory payload = new InventreePartCategory();
        String childName = unique("E2E Child Category");
        payload.setName(childName);
        payload.setParent(categoryId);
        Response response = RestClient.post(SPEC, CATEGORY_PATH, payload);
        response.then().statusCode(201).body("parent", equalTo(categoryId));
        childCategoryId = response.jsonPath().getInt("pk");
        createdCategoryIds.add(childCategoryId);

        RestClient.get(SPEC, categoryPath(childCategoryId)).then()
                .statusCode(200)
                .body("pathstring", containsString(childName));
    }

    private void catRejectDeleteWithoutCascadeFlags() {
        // InvenTree requires an explicit delete_parts/delete_child_categories
        // decision in the request body; a bare DELETE is a 400, not a 204.
        Response response = RestClient.delete(SPEC, categoryPath(childCategoryId));
        assertEquals(response.statusCode(), 400,
                "Deleting a category without delete_parts/delete_child_categories must be rejected");
        assertTrue(response.asString().contains("delete_parts") || response.asString().contains("delete_child_categories"),
                "Rejection must explain which cascade flag is required");
    }

    /**
     * Verified against a live instance: deleting a non-root category with
     * delete_parts=false does NOT orphan its Parts to a null category — it
     * reparents them one level up, to the deleted category's own parent.
     * (A root category with no parent would orphan them to null instead.)
     */
    private void catDeleteOrphansAssignedPart() {
        InventreePart partPayload = new InventreePart();
        partPayload.setName(unique("E2E Part In Category"));
        partPayload.setCategory(childCategoryId);
        Response partResponse = RestClient.post(SPEC, PART_PATH, partPayload);
        assertEquals(partResponse.statusCode(), 201, "Fixture part must be created inside the child category");
        Integer partId = partResponse.jsonPath().getInt("pk");
        createdPartIds.add(partId);
        assertEquals(partResponse.jsonPath().getInt("category"), childCategoryId.intValue());

        Map<String, Object> cascadeFlags = new HashMap<>();
        cascadeFlags.put("delete_parts", false);
        cascadeFlags.put("delete_child_categories", false);
        Response deleteResponse = RestClient.delete(SPEC, categoryPath(childCategoryId), cascadeFlags);
        assertEquals(deleteResponse.statusCode(), 204,
                "Category delete with delete_parts=false must succeed and only reparent its parts");
        createdCategoryIds.remove(childCategoryId);

        RestClient.get(SPEC, PART_PATH + partId + "/").then()
                .statusCode(200)
                .body("category", equalTo(categoryId));
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() {
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
                Response response = RestClient.delete(SPEC, categoryPath(categoryId), cascadeFlags);
                if (response.statusCode() != 204 && response.statusCode() != 404) {
                    Allure.step("Cleanup failed for Category " + categoryId + ": HTTP " + response.statusCode());
                }
            } catch (RuntimeException cleanupFailure) {
                Allure.step("Cleanup failed for Category " + categoryId + ": " + cleanupFailure.getMessage());
            }
        }
    }

    private static String categoryPath(Integer id) {
        return CATEGORY_PATH + id + "/";
    }
}
