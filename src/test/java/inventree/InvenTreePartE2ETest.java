package inventree;

import client.RestClient;
import constant.ApplicationConstant;
import files.ConfigManager;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pojo.InventreePart;
import specifications.RequestSpecBuilderUtil;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
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

    static {
        // ensure config.properties is loaded before the property lookups below run
        ConfigManager.load(Paths.get(ApplicationConstant.CONFIG_FILE_PATH));
    }

    private static final String PART_PATH = ConfigManager.getProperty(ApplicationConstant.INVENTREE_PART_PATH_KEY, ApplicationConstant.INVENTREE_PART_PATH_DEFAULT);
    private static final String BASE_URL = ConfigManager.getProperty(ApplicationConstant.INVENTREE_BASE_URL_KEY, "");
    private static final String API_TOKEN = ConfigManager.getProperty(ApplicationConstant.INVENTREE_API_TOKEN_KEY, "");
    private static final String CATEGORY_ID = ConfigManager.getProperty(ApplicationConstant.INVENTREE_CATEGORY_ID_KEY, "");
    private static final String LOCATION_ID = ConfigManager.getProperty(ApplicationConstant.INVENTREE_LOCATION_ID_KEY, "");
    private static final RequestSpecification SPEC = RequestSpecBuilderUtil.getRequestSpecWithAuth(BASE_URL, "Authorization", "Token " + API_TOKEN);
    private final List<Integer> createdPartIds = new ArrayList<>();
    private Integer minimalPartId;
    private Integer fullPartId;
    private InventreePart minimalPart;
    private InventreePart fullPart;

    @BeforeClass(alwaysRun = true)
    public void validateSchemaEntryPoint() {
        if (isBlank(BASE_URL) || isBlank(API_TOKEN)) {
            throw new SkipException("InvenTree E2E skipped: set INVENTREE_BASE_URL and INVENTREE_API_TOKEN");
        }
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

    private static String unique(String prefix) {
        return prefix + "-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static void applyConfiguredCategoryAndLocation(InventreePart payload) {
        if (!isBlank(CATEGORY_ID)) {
            payload.setCategory(Integer.parseInt(CATEGORY_ID));
        }
        if (!isBlank(LOCATION_ID)) {
            payload.setDefaultLocation(Integer.parseInt(LOCATION_ID));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
