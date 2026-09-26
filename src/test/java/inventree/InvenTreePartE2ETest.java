package inventree;

import client.RestClient;
import constant.ApplicationConstant;
import files.ConfigManager;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pojo.InventreePart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static inventree.InvenTreeApiSupport.PART_PATH;
import static inventree.InvenTreeApiSupport.authenticatedSpec;
import static inventree.InvenTreeApiSupport.isBlank;
import static inventree.InvenTreeApiSupport.requireConfigured;
import static inventree.InvenTreeApiSupport.unique;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Single-file, ordered E2E coverage for the InvenTree Part API.
 *
 * The suite is enabled only when INVENTREE_BASE_URL and INVENTREE_API_TOKEN
 * are supplied, preventing accidental execution against an unknown instance.
 */
public class InvenTreePartE2ETest {

    // SPEC must be initialized first: it forces InvenTreeApiSupport's static
    // block to run, which loads config.properties before CATEGORY_ID/LOCATION_ID
    // below read from it.
    private static final RequestSpecification SPEC = authenticatedSpec();
    private static final String CATEGORY_ID = ConfigManager.getProperty(ApplicationConstant.INVENTREE_CATEGORY_ID_KEY, "");
    private static final String LOCATION_ID = ConfigManager.getProperty(ApplicationConstant.INVENTREE_LOCATION_ID_KEY, "");
    private final List<Integer> createdPartIds = new ArrayList<>();
    private Integer minimalPartId;
    private Integer fullPartId;
    private InventreePart minimalPart;
    private InventreePart fullPart;

    @BeforeClass(alwaysRun = true)
    public void validateSchemaEntryPoint() {
        requireConfigured();
        // limit=1 forces the paginated {count, results} shape; with no pagination
        // params InvenTree returns a bare array instead.
        Response response = RestClient.get(SPEC, PART_PATH + "?limit=1");
        assertEquals(response.statusCode(), 200, "InvenTree Part endpoint must be available");
        response.then()
                .contentType("application/json")
                .body(matchesJsonSchemaInClasspath("schema/inventree-part-list.json"));
        assertNotNull(response.jsonPath().get("count"), "Part list must contain count");
        assertNotNull(response.jsonPath().getList("results"), "Part list must contain a results array");
        Allure.step("InvenTree Part API schema entry condition passed");
    }

    @Test
    public void executeAllPartScenariosAsSingleE2EFlow() {
        apiPart001CreateMinimalPart();
        apiPart002CreatePartWithWritableAttributes();
        apiPart003RetrievePartById();
        apiPart004RetrievePartWithDetailFlags();
        apiPart005PatchOnlySuppliedFields();
        apiPart006PutReplacement();
        apiPart007RejectPutWithoutName();
        apiPart008ProtectReadOnlyFields();
        apiPart009DeleteInactivePart();
        apiPart010BlockDeleteActivePart();
        apiPart011BlockDeleteLockedPart();
    }

    @DataProvider(name = "partNameBoundaries")
    public Object[][] partNameBoundaries() {
        return new Object[][] {
                { "N".repeat(100), 201 },
                { "N".repeat(101), 400 }
        };
    }

    @Test(dataProvider = "partNameBoundaries", dependsOnMethods = "executeAllPartScenariosAsSingleE2EFlow")
    public void validatePartNameBoundary(String name, int expectedStatus) {
        InventreePart payload = new InventreePart();
        payload.setName(name);
        Response response = RestClient.post(SPEC, PART_PATH, payload);
        assertEquals(response.statusCode(), expectedStatus,"Part name boundary should follow the published maxLength=100 contract");
        if (response.statusCode() == 201) {
            Integer id = response.jsonPath().getInt("pk");
            track(id);
            response.then()
                    .body(matchesJsonSchemaInClasspath("schema/inventree-part-detail.json"))
                    .body("name", equalTo(name));
        }
    }

    @DataProvider(name = "partFieldLengthBoundaries")
    public Object[][] partFieldLengthBoundaries() {
        return new Object[][] {
                { "ipn", "I".repeat(100), 201 },
                { "ipn", "I".repeat(101), 400 },
                { "description", "D".repeat(250), 201 },
                { "description", "D".repeat(251), 400 },
                { "keywords", "K".repeat(250), 201 },
                { "keywords", "K".repeat(251), 400 },
                { "notes", "N".repeat(50000), 201 },
                { "notes", "N".repeat(50001), 400 }
        };
    }

    @Test(dataProvider = "partFieldLengthBoundaries", dependsOnMethods = "executeAllPartScenariosAsSingleE2EFlow")
    public void validatePartFieldLengthBoundary(String field, String value, int expectedStatus) {
        InventreePart payload = new InventreePart();
        payload.setName(unique("E2E Boundary " + field));
        applyBoundaryField(payload, field, value);
        Response response = RestClient.post(SPEC, PART_PATH, payload);
        assertEquals(response.statusCode(), expectedStatus,
                "Field '" + field + "' should follow its published maxLength contract");
        if (response.statusCode() == 201) {
            track(response.jsonPath().getInt("pk"));
        }
    }

    @DataProvider(name = "partDefaultExpiryBoundaries")
    public Object[][] partDefaultExpiryBoundaries() {
        return new Object[][] {
                { 0, 201 },
                { 30, 201 },
                { -1, 400 }
        };
    }

    @Test(dataProvider = "partDefaultExpiryBoundaries", dependsOnMethods = "executeAllPartScenariosAsSingleE2EFlow")
    public void validateDefaultExpiryBoundary(int defaultExpiry, int expectedStatus) {
        InventreePart payload = new InventreePart();
        payload.setName(unique("E2E Expiry"));
        payload.setDefaultExpiry(defaultExpiry);
        Response response = RestClient.post(SPEC, PART_PATH, payload);
        assertEquals(response.statusCode(), expectedStatus, "default_expiry must respect the >= 0 contract");
        if (response.statusCode() == 201) {
            track(response.jsonPath().getInt("pk"));
        }
    }

    /**
     * {@code units} is validated against InvenTree's registered physical-unit
     * vocabulary (e.g. "kg"), not merely a maxLength string constraint, so it
     * is exercised as a valid-value check rather than a length boundary.
     */
    @Test(dependsOnMethods = "executeAllPartScenariosAsSingleE2EFlow")
    public void validateUnitsMustBeARegisteredPhysicalUnit() {
        InventreePart validPayload = new InventreePart();
        validPayload.setName(unique("E2E Valid Units"));
        validPayload.setUnits("kg");
        Response validResponse = RestClient.post(SPEC, PART_PATH, validPayload);
        assertEquals(validResponse.statusCode(), 201, "A registered physical unit (kg) must be accepted");
        track(validResponse.jsonPath().getInt("pk"));

        InventreePart invalidPayload = new InventreePart();
        invalidPayload.setName(unique("E2E Invalid Units"));
        invalidPayload.setUnits("notarealunit");
        Response invalidResponse = RestClient.post(SPEC, PART_PATH, invalidPayload);
        assertEquals(invalidResponse.statusCode(), 400, "An unregistered unit string must be rejected");
    }

    /**
     * keywords/units/notes are nullable; a Jackson NON_NULL POJO would drop an
     * explicit null instead of sending it, so this uses a raw map to prove the
     * API accepts a real JSON null distinct from simply omitting the field.
     */
    @Test(dependsOnMethods = "executeAllPartScenariosAsSingleE2EFlow")
    public void validateNullableFieldsAcceptExplicitNull() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", unique("E2E Nullable Fields"));
        payload.put("keywords", null);
        payload.put("units", null);
        payload.put("notes", null);
        Response response = RestClient.post(SPEC, PART_PATH, payload);
        response.then()
                .statusCode(201)
                .body("keywords", nullValue())
                .body("units", nullValue())
                .body("notes", nullValue());
        track(response.jsonPath().getInt("pk"));
    }

    private static void applyBoundaryField(InventreePart part, String field, String value) {
        switch (field) {
            case "ipn": part.setIpn(value); break;
            case "description": part.setDescription(value); break;
            case "keywords": part.setKeywords(value); break;
            case "notes": part.setNotes(value); break;
            default: throw new IllegalArgumentException("Unknown boundary field: " + field);
        }
    }

    private void apiPart001CreateMinimalPart() {
        String name = unique("E2E Minimal Part");
        InventreePart payload = InventreePartTestData.minimalPart(name);
        Response response = RestClient.post(SPEC, PART_PATH, payload);
        response.then()
                .statusCode(201)
                .contentType("application/json")
                .body(matchesJsonSchemaInClasspath("schema/inventree-part-detail.json"))
                .body("name", equalTo(name));
        minimalPartId = response.jsonPath().getInt("pk");
        minimalPart = response.as(InventreePart.class);
        track(minimalPartId);
    }

    private void apiPart002CreatePartWithWritableAttributes() {
        String name = unique("E2E Full Part");
        InventreePart payload = InventreePartTestData.fullPart(name, unique("IPN"));
        applyConfiguredCategoryAndLocation(payload);

        Response response = RestClient.post(SPEC, PART_PATH, payload);
        response.then()
                .statusCode(201)
                .contentType("application/json")
                .body(matchesJsonSchemaInClasspath("schema/inventree-part-detail.json"))
                .body("name", equalTo(name))
                .body("IPN", equalTo(payload.getIpn()));

        fullPartId = response.jsonPath().getInt("pk");
        fullPart = response.as(InventreePart.class);
        track(fullPartId);
    }

    private void apiPart003RetrievePartById() {
        Response response = RestClient.get(SPEC, partPath(minimalPartId));
        response.then()
                .statusCode(200)
                .contentType("application/json")
                .body(matchesJsonSchemaInClasspath("schema/inventree-part-detail.json"))
                .body("pk", equalTo(minimalPartId))
                .body("name", equalTo(minimalPart.getName()));
    }

    private void apiPart004RetrievePartWithDetailFlags() {
        Response response = RestClient.get(
                SPEC,
                partPath(fullPartId) + "?category_detail=true&path_detail=true");
        response.then()
                .statusCode(200)
                .contentType("application/json")
                .body(matchesJsonSchemaInClasspath("schema/inventree-part-detail.json"))
                .body("pk", equalTo(fullPartId))
                .body("$", hasKey("category_detail"))
                .body("$", hasKey("category_path"));
    }

    private void apiPart005PatchOnlySuppliedFields() {
        String originalName = fullPart.getName();
        String description = "PATCHED-" + UUID.randomUUID();
        InventreePart payload = new InventreePart();
        payload.setDescription(description);

        Response response = RestClient.patch(SPEC, partPath(fullPartId), payload);
        response.then()
                .statusCode(200)
                .body("description", equalTo(description))
                .body("name", equalTo(originalName));

        Response persisted = RestClient.get(SPEC, partPath(fullPartId));
        persisted.then().statusCode(200).body("description", equalTo(description));
    }

    private void apiPart006PutReplacement() {
        InventreePart payload = InventreePartTestData.putReplacement(unique("E2E PUT Part"));
        applyConfiguredCategoryAndLocation(payload);

        Response response = RestClient.put(SPEC, partPath(fullPartId), payload);
        response.then()
                .statusCode(200)
                .contentType("application/json")
                .body(matchesJsonSchemaInClasspath("schema/inventree-part-detail.json"))
                .body("pk", equalTo(fullPartId))
                .body("name", equalTo(payload.getName()))
                .body("description", equalTo(payload.getDescription()));
        fullPart = response.as(InventreePart.class);
    }

    private void apiPart007RejectPutWithoutName() {
        InventreePart payload = InventreePartTestData.invalidPutWithoutName();
        Response response = RestClient.put(SPEC, partPath(fullPartId), payload);
        assertTrue(response.statusCode() >= 400 && response.statusCode() < 500,"PUT without name must return a client error");
        assertTrue(response.asString().contains("name"),"Validation response should identify the missing name field");
        RestClient.get(SPEC, partPath(fullPartId)).then()
                .statusCode(200)
                .body("name", equalTo(fullPart.getName()));
    }

    private void apiPart008ProtectReadOnlyFields() {
        Integer originalPk = fullPart.getPk();
        InventreePart payload = InventreePartTestData.readOnlyFieldAttempt(fullPart.getName());
        Response response = RestClient.patch(SPEC, partPath(fullPartId), payload);
        assertTrue(response.statusCode() == 200 || response.statusCode() == 400,"Read-only field handling must be documented by a success or validation response");
        if (response.statusCode() == 200) {
            assertEquals(response.jsonPath().getInt("pk"), originalPk.intValue(),
                    "Primary key must remain server-managed");
            assertEquals(response.jsonPath().getString("description"),
                    payload.getDescription());
        }
    }
    private void apiPart009DeleteInactivePart() {
        InventreePart deactivate = new InventreePart();
        deactivate.setActive(false);
        RestClient.patch(SPEC, partPath(minimalPartId), deactivate)
                .then().statusCode(200);
        Response delete = RestClient.delete(SPEC, partPath(minimalPartId));
        delete.then().statusCode(204);
        RestClient.get(SPEC, partPath(minimalPartId)).then().statusCode(404);
        createdPartIds.remove(minimalPartId);
    }
    private void apiPart010BlockDeleteActivePart() {
        Response response = RestClient.delete(SPEC, partPath(fullPartId));
        assertTrue(response.statusCode() >= 400 && response.statusCode() < 500,
                "Deleting an active Part must be blocked");
        RestClient.get(SPEC, partPath(fullPartId)).then().statusCode(200);
    }
    private void apiPart011BlockDeleteLockedPart() {
        InventreePart lock = new InventreePart();
        lock.setLocked(true);
        RestClient.patch(SPEC, partPath(fullPartId), lock)
                .then().statusCode(200);
        Response response = RestClient.delete(SPEC, partPath(fullPartId));
        assertTrue(response.statusCode() >= 400 && response.statusCode() < 500,
                "Deleting a locked Part must be blocked");
        RestClient.get(SPEC, partPath(fullPartId)).then().statusCode(200);
    }

    @AfterClass(alwaysRun = true)
    public void cleanupCreatedParts() {
        for (Integer partId : new ArrayList<>(createdPartIds)) {
            try {
                Response current = RestClient.get(SPEC, partPath(partId));
                if (current.statusCode() == 200) {
                    if (Boolean.TRUE.equals(current.jsonPath().getBoolean("locked"))) {
                        InventreePart unlock = new InventreePart();
                        unlock.setLocked(false);
                        RestClient.patch(SPEC, partPath(partId), unlock);
                    }
                    if (Boolean.TRUE.equals(current.jsonPath().getBoolean("active"))) {
                        InventreePart deactivate = new InventreePart();
                        deactivate.setActive(false);
                        RestClient.patch(SPEC, partPath(partId), deactivate);
                    }
                    Response delete = RestClient.delete(SPEC, partPath(partId));
                    if (delete.statusCode() != 204 && delete.statusCode() != 404) {
                        Allure.step("Cleanup failed for Part " + partId + ": HTTP " + delete.statusCode());
                    }
                }
            } catch (RuntimeException cleanupFailure) {
                Allure.step("Cleanup failed for Part " + partId + ": " + cleanupFailure.getMessage());
            }
        }
    }

    private static String partPath(Integer id) {
        return PART_PATH + id + "/";
    }

    private void track(Integer id) {
        assertNotNull(id, "Created Part response must contain pk");
        createdPartIds.add(id);
    }

    private static void applyConfiguredCategoryAndLocation(InventreePart payload) {
        if (!isBlank(CATEGORY_ID)) {
            payload.setCategory(Integer.parseInt(CATEGORY_ID));
        }
        if (!isBlank(LOCATION_ID)) {
            payload.setDefaultLocation(Integer.parseInt(LOCATION_ID));
        }
    }
}
