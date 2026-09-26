package inventree;

import client.RestClient;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pojo.InventreeCompany;
import pojo.InventreePart;
import pojo.InventreeSupplierPart;

import java.util.ArrayList;
import java.util.List;

import static inventree.InvenTreeApiSupport.COMPANY_PATH;
import static inventree.InvenTreeApiSupport.PART_PATH;
import static inventree.InvenTreeApiSupport.SUPPLIER_PART_PATH;
import static inventree.InvenTreeApiSupport.authenticatedSpec;
import static inventree.InvenTreeApiSupport.requireConfigured;
import static inventree.InvenTreeApiSupport.unique;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Relational-integrity coverage for supplier linkage: a SupplierPart ties a
 * Part to a Company, is only valid against a Company flagged
 * {@code is_supplier=true}, and enforces a unique (part, supplier, SKU) set.
 */
public class InvenTreeSupplierPartE2ETest {

    private static final RequestSpecification SPEC = authenticatedSpec();

    private final List<Integer> createdSupplierPartIds = new ArrayList<>();
    private final List<Integer> createdPartIds = new ArrayList<>();
    private final List<Integer> createdCompanyIds = new ArrayList<>();

    private Integer supplierCompanyId;
    private Integer nonSupplierCompanyId;
    private Integer partId;

    @BeforeClass(alwaysRun = true)
    public void validateEntryPoint() {
        requireConfigured();
        Response response = RestClient.get(SPEC, COMPANY_PATH + "?limit=1");
        assertEquals(response.statusCode(), 200, "InvenTree Company endpoint must be available");
        response.then().body(matchesJsonSchemaInClasspath("schema/inventree-paginated-list.json"));
    }

    @Test
    public void executeSupplierLinkageFlow() {
        createSupplierCompany();
        createNonSupplierCompany();
        createPurchaseablePart();
        createSupplierPartLinkage();
        retrieveSupplierPartById();
        rejectSupplierPartAgainstNonSupplierCompany();
        rejectDuplicateSupplierPartSku();
    }

    private void createSupplierCompany() {
        InventreeCompany payload = new InventreeCompany();
        payload.setName(unique("E2E Supplier Co"));
        payload.setCurrency("USD");
        payload.setIsSupplier(true);
        Response response = RestClient.post(SPEC, COMPANY_PATH, payload);
        response.then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schema/inventree-company-detail.json"))
                .body("is_supplier", equalTo(true));
        supplierCompanyId = response.jsonPath().getInt("pk");
        createdCompanyIds.add(supplierCompanyId);
    }

    private void createNonSupplierCompany() {
        InventreeCompany payload = new InventreeCompany();
        payload.setName(unique("E2E Non-Supplier Co"));
        payload.setCurrency("USD");
        payload.setIsSupplier(false);
        Response response = RestClient.post(SPEC, COMPANY_PATH, payload);
        response.then().statusCode(201).body("is_supplier", equalTo(false));
        nonSupplierCompanyId = response.jsonPath().getInt("pk");
        createdCompanyIds.add(nonSupplierCompanyId);
    }

    private void createPurchaseablePart() {
        InventreePart payload = new InventreePart();
        payload.setName(unique("E2E Supplier-Linked Part"));
        payload.setPurchaseable(true);
        Response response = RestClient.post(SPEC, PART_PATH, payload);
        response.then().statusCode(201);
        partId = response.jsonPath().getInt("pk");
        createdPartIds.add(partId);
    }

    private void createSupplierPartLinkage() {
        InventreeSupplierPart payload = new InventreeSupplierPart();
        payload.setPart(partId);
        payload.setSupplier(supplierCompanyId);
        payload.setSku(unique("SKU"));
        Response response = RestClient.post(SPEC, SUPPLIER_PART_PATH, payload);
        response.then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schema/inventree-supplierpart-detail.json"))
                .body("part", equalTo(partId))
                .body("supplier", equalTo(supplierCompanyId))
                .body("part_detail.pk", equalTo(partId))
                .body("supplier_detail.pk", equalTo(supplierCompanyId));
        createdSupplierPartIds.add(response.jsonPath().getInt("pk"));
    }

    private void retrieveSupplierPartById() {
        Integer supplierPartId = createdSupplierPartIds.get(0);
        RestClient.get(SPEC, SUPPLIER_PART_PATH + supplierPartId + "/").then()
                .statusCode(200)
                .body("pk", equalTo(supplierPartId));
    }

    private void rejectSupplierPartAgainstNonSupplierCompany() {
        InventreeSupplierPart payload = new InventreeSupplierPart();
        payload.setPart(partId);
        payload.setSupplier(nonSupplierCompanyId);
        payload.setSku(unique("SKU"));
        Response response = RestClient.post(SPEC, SUPPLIER_PART_PATH, payload);
        assertEquals(response.statusCode(), 400,
                "A Company that is not flagged is_supplier must be rejected as a supplier link");
        assertTrue(response.asString().contains("supplier"),
                "Rejection must identify the supplier field as invalid");
    }

    private void rejectDuplicateSupplierPartSku() {
        String sku = unique("DUP-SKU");
        InventreeSupplierPart first = new InventreeSupplierPart();
        first.setPart(partId);
        first.setSupplier(supplierCompanyId);
        first.setSku(sku);
        Response firstResponse = RestClient.post(SPEC, SUPPLIER_PART_PATH, first);
        assertEquals(firstResponse.statusCode(), 201, "First (part, supplier, SKU) combination must succeed");
        createdSupplierPartIds.add(firstResponse.jsonPath().getInt("pk"));

        InventreeSupplierPart duplicate = new InventreeSupplierPart();
        duplicate.setPart(partId);
        duplicate.setSupplier(supplierCompanyId);
        duplicate.setSku(sku);
        Response duplicateResponse = RestClient.post(SPEC, SUPPLIER_PART_PATH, duplicate);
        assertEquals(duplicateResponse.statusCode(), 400,
                "Duplicate (part, supplier, SKU) must be rejected as a conflict");
        assertTrue(duplicateResponse.asString().contains("unique"),
                "Rejection must explain the uniqueness constraint");
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() {
        for (Integer id : createdSupplierPartIds) {
            safeDelete(SUPPLIER_PART_PATH + id + "/", "SupplierPart " + id);
        }
        for (Integer id : createdPartIds) {
            InventreePart deactivate = new InventreePart();
            deactivate.setActive(false);
            RestClient.patch(SPEC, PART_PATH + id + "/", deactivate);
            safeDelete(PART_PATH + id + "/", "Part " + id);
        }
        for (Integer id : createdCompanyIds) {
            safeDelete(COMPANY_PATH + id + "/", "Company " + id);
        }
    }

    private void safeDelete(String path, String label) {
        try {
            Response response = RestClient.delete(SPEC, path);
            if (response.statusCode() != 204 && response.statusCode() != 404) {
                Allure.step("Cleanup failed for " + label + ": HTTP " + response.statusCode());
            }
        } catch (RuntimeException cleanupFailure) {
            Allure.step("Cleanup failed for " + label + ": " + cleanupFailure.getMessage());
        }
    }
}
