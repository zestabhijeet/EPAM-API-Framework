package inventree;

import client.RestClient;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pojo.InventreePart;

import static inventree.InvenTreeApiSupport.BASE_URL;
import static inventree.InvenTreeApiSupport.PART_PATH;
import static inventree.InvenTreeApiSupport.requireConfigured;
import static inventree.InvenTreeApiSupport.unique;
import static org.testng.Assert.assertEquals;

/**
 * Unauthorised-access edge cases for the InvenTree Part API: requests with
 * no credentials and requests with an invalid token must both be rejected,
 * on both read and write operations.
 */
public class InvenTreeSecurityTest {

    private static RequestSpecification unauthenticatedSpec;
    private static RequestSpecification invalidTokenSpec;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        requireConfigured();
        unauthenticatedSpec = new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType("application/json")
                .build();
        invalidTokenSpec = new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType("application/json")
                .addHeader("Authorization", "Token invalid-token-value")
                .build();
    }

    @Test
    public void getWithoutCredentialsIsRejected() {
        Response response = RestClient.get(unauthenticatedSpec, PART_PATH);
        assertEquals(response.statusCode(), 401, "GET without credentials must be rejected");
    }

    @Test
    public void getWithInvalidTokenIsRejected() {
        Response response = RestClient.get(invalidTokenSpec, PART_PATH);
        assertEquals(response.statusCode(), 401, "GET with an invalid token must be rejected");
    }

    @Test
    public void postWithoutCredentialsIsRejected() {
        InventreePart payload = new InventreePart();
        payload.setName(unique("Unauthorised Create Attempt"));
        Response response = RestClient.post(unauthenticatedSpec, PART_PATH, payload);
        assertEquals(response.statusCode(), 401, "POST without credentials must be rejected");
    }

    @Test
    public void postWithInvalidTokenIsRejected() {
        InventreePart payload = new InventreePart();
        payload.setName(unique("Unauthorised Create Attempt"));
        Response response = RestClient.post(invalidTokenSpec, PART_PATH, payload);
        assertEquals(response.statusCode(), 401, "POST with an invalid token must be rejected");
    }
}
