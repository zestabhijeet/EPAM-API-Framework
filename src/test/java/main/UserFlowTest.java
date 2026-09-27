package main;

import client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import files.ConfigManager;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import utils.JsonUtil;
import utils.RetryUtil;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

public class UserFlowTest {

    static {
        // ensure config.properties is loaded before the BASE_URL lookup below runs
        ConfigManager.load(java.nio.file.Paths.get("src/test/resources/config.properties"));
    }

    private static final String USER_API_TEST_DATA_SCHEMA = "src//test//resources//userapitestdata.json";
    private static final String BASE_URL = ConfigManager.getProperty("base.url");

    // Schema Validation and Crud Call without using service layer
    @Test
    public void validateUserSchemaTest() {
        Response createResponse = RetryUtil.executeWithRetry(() -> RestClient.get(BASE_URL, "/users/2"));
        createResponse.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath(".//schema//userapi.json"))
                .log().all();
    }
    @Test
    public void createUserId() throws Exception {
        JsonNode createUser = JsonUtil.getJsonNode(USER_API_TEST_DATA_SCHEMA, "createUser");
        String userId = RestClient.post(BASE_URL, "/users", createUser)
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");

        System.out.println("Created ID: " + userId);
        // PUT
        JsonNode updateUser = JsonUtil.getJsonNode(USER_API_TEST_DATA_SCHEMA, "updateUser");
        String updatedDetails = RestClient.put(BASE_URL, "/users/" + userId, updateUser)
                .then()
                .statusCode(200)
                .extract()
                .asPrettyString();
        System.out.println("Updated User Id with Job details " + updatedDetails);
        // PATCH
        JsonNode patchUser = JsonUtil.getJsonNode(USER_API_TEST_DATA_SCHEMA, "patchUser");
        String updatedPatchDetails = RestClient.patch(BASE_URL, "/users/" + userId, patchUser)
                .then()
                .statusCode(200)
                .extract()
                .asPrettyString();
        System.out.println("Updated User Id Patched Job Details" + updatedPatchDetails);
    }
}
