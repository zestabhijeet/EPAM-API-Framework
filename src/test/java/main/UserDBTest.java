package main;

import client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import files.ConfigManager;
import io.restassured.response.Response;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pojo.UserType;
import utils.DBUtil;
import utils.ObjectMapperUtil;
import java.sql.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

/**
 * UserDBTest: Response Validation Test with CRUD Operations
 * 
 * This test combines:
 * 1. API CRUD operations (like UserApiTest)
 * 2. Database operations (INSERT/SELECT/UPDATE/DELETE)
 * 3. Response validation against database data
 * 
 * Flow: API Request → Get Response → Store in DB → Compare API Response with DB Data
 */
public class UserDBTest {

    static {
        // ensure config.properties is loaded before the BASE_URL lookup below runs
        ConfigManager.load(java.nio.file.Paths.get("src/test/resources/config.properties"));
    }

    private static final ObjectMapper objectMapper = ObjectMapperUtil.getInstance();
    private static final String TEST_USER_ID = "2";  // ReqRes pre-existing user
    private static final String BASE_URL = ConfigManager.getProperty("base.url");

    @BeforeClass
    public void setUp() {
        System.out.println("\n========== INITIALIZING DATABASE ==========");
        DBUtil.createSchema();
        DBUtil.insertData();
        DBUtil.fetchData();
    }

    // ======================== TEST 1: CREATE & VALIDATE IN DB =========================
    @Test(priority = 1)
    public void testPostResponseAndStoreInDB() throws Exception {
        System.out.println("\n========== TEST 1: POST & STORE IN DB ==========");
        
        // Step 1: Create payload using API POJO
        UserType createPayload = new UserType();
        createPayload.setFirstname("Test");
        createPayload.setLastname("UserApi");
        createPayload.setEmail("testapi@example.com");
        createPayload.setPhonenumber(5559999);

        System.out.println("📤 POST Payload:");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(createPayload));

        // Step 2: Call API
        Response postResponse = RestClient.post(BASE_URL, "/users", createPayload)
                .then()
                .statusCode(201)
                .extract()
                .response();

        //JsonPath postJsonPath = postResponse.jsonPath();
        String apiUserId = postResponse.jsonPath().getString("id");

        System.out.println("✅ API Response (POST):");
        System.out.println(postResponse.asString());

        assertEquals(postResponse.jsonPath().getString("firstname"), createPayload.getFirstname(), "POST firstname must match POJO");
        assertEquals(postResponse.jsonPath().getString("lastname"), createPayload.getLastname(), "POST lastname must match POJO");
        assertEquals(postResponse.jsonPath().getString("email"), createPayload.getEmail(), "POST email must match POJO");
        assertEquals(postResponse.jsonPath().getInt("phonenumber"), createPayload.getPhonenumber(), "POST phone must match POJO");

        // Step 3: Store API response data in DB
        int dbId = Integer.parseInt(apiUserId) + 100;  // Avoid ID conflicts
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO USERS (ID, EMAIL, FIRST_NAME, LAST_NAME, AVATAR) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, dbId);
            ps.setString(2, postResponse.jsonPath().getString("email"));
            ps.setString(3, postResponse.jsonPath().getString("firstname"));
            ps.setString(4, postResponse.jsonPath().getString("lastname"));
            ps.setString(5, "https://api.example.com/avatar/posted.jpg");
            ps.executeUpdate();
            System.out.println("✅ Stored API response data in DB with ID: " + dbId);
        }

        // Step 4: Verify stored data
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM USERS WHERE ID = " + dbId)) {
            assertTrue(rs.next(), "Data must exist in DB");
            assertEquals(rs.getString("EMAIL"), postResponse.jsonPath().getString("email"), "Email must match");
            assertEquals(rs.getString("FIRST_NAME"), postResponse.jsonPath().getString("firstname"), "First name must match");
            assertEquals(rs.getString("LAST_NAME"), postResponse.jsonPath().getString("lastname"), "Last name must match");
            System.out.println("✅ POST → DB validation passed");
        }
    }

    // ======================== TEST 2: GET & VALIDATE RESPONSE =========================
    @Test(priority = 2)
    public void testGetResponseValidation() throws Exception {
        System.out.println("\n========== TEST 2: GET & VALIDATE RESPONSE ==========");

        // Step 1: Call GET API
        Response getResponse = RestClient.get(BASE_URL, "/users/" + TEST_USER_ID)
                .then()
                .statusCode(200)
                .extract()
                .response();

        //JsonPath getJsonPath = getResponse.jsonPath();
        JsonNode getResponseJson = objectMapper.readTree(getResponse.asString());
        System.out.println("📥 GET Response:");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(getResponseJson));

        // Step 2: Validate response structure with response.jsonPath()
        assertNotNull(getResponse.jsonPath().get("data"), "Response must contain data");
        assertNotNull(getResponse.jsonPath().get("data.id"), "Response must have id");
        assertNotNull(getResponse.jsonPath().get("data.email"), "Response must have email");
        assertNotNull(getResponse.jsonPath().get("data.first_name"), "Response must have first_name");
        assertNotNull(getResponse.jsonPath().get("data.last_name"), "Response must have last_name");
        System.out.println("✅ Response structure validation passed");

        // Step 3: Store API response in POJO for comparison
        JsonNode userData = getResponseJson.get("data");
        UserType userFromApi = objectMapper.readValue(userData.toString(), UserType.class);
        assertEquals(userFromApi.getId(), getResponse.jsonPath().getInt("data.id"));
        assertEquals(userFromApi.getEmail(), getResponse.jsonPath().getString("data.email"));
        assertEquals(userFromApi.getFirstname(), getResponse.jsonPath().getString("data.first_name"));
        assertEquals(userFromApi.getLastname(), getResponse.jsonPath().getString("data.last_name"));
        System.out.println("✅ Mapped response to UserType POJO");
        System.out.println("   ID: " + userFromApi.getId());
        System.out.println("   Email: " + userFromApi.getEmail());
    }

    // ======================== TEST 3: PUT & COMPARE WITH DB =========================
    @Test(priority = 3)
    public void testPutResponseAndCompareWithDB() throws Exception {
        System.out.println("\n========== TEST 3: PUT & COMPARE WITH DB ==========");

        // Step 1: Create update payload using API POJO
        UserType updatePayload = new UserType();
        updatePayload.setFirstname("Updated");
        updatePayload.setLastname("UserPut");
        updatePayload.setEmail("updated.put@example.com");

        System.out.println("📤 PUT Payload:");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(updatePayload));

        // Step 2: Call PUT API
        Response putResponse = RestClient.put(BASE_URL, "/users/" + TEST_USER_ID, updatePayload)
                .then()
                .statusCode(200)
                .extract()
                .response();

        //JsonPath putJsonPath = putResponse.jsonPath();
        System.out.println("📥 PUT Response:");
        System.out.println(putResponse.asString());

        // Step 3: Verify response matches POJO payload with response.jsonPath()
        assertEquals(putResponse.jsonPath().getString("firstname"), updatePayload.getFirstname(), "PUT firstname must match payload");
        assertEquals(putResponse.jsonPath().getString("lastname"), updatePayload.getLastname(), "PUT lastname must match payload");
        assertEquals(putResponse.jsonPath().getString("email"), updatePayload.getEmail(), "PUT email must match payload");
        System.out.println("✅ PUT response matches payload");

        // Step 4: Store update in DB
        int dbTestId = 300;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO USERS (ID, EMAIL, FIRST_NAME, LAST_NAME, AVATAR) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, dbTestId);
            ps.setString(2, putResponse.jsonPath().getString("email"));
            ps.setString(3, putResponse.jsonPath().getString("firstname"));
            ps.setString(4, putResponse.jsonPath().getString("lastname"));
            ps.setString(5, "https://api.example.com/avatar/updated.jpg");
            ps.executeUpdate();
        }

        // Step 5: Compare API response with DB data
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM USERS WHERE ID = " + dbTestId)) {
            assertTrue(rs.next(), "Data must exist in DB");
            
            // Compare each field
            assertEquals(rs.getString("EMAIL"), putResponse.jsonPath().getString("email"), "DB email must match API response");
            assertEquals(rs.getString("FIRST_NAME"), putResponse.jsonPath().getString("firstname"), "DB first_name must match API response");
            assertEquals(rs.getString("LAST_NAME"), putResponse.jsonPath().getString("lastname"), "DB last_name must match API response");
            
            System.out.println("✅ PUT response vs DB comparison passed");
        }
    }

    // ======================== TEST 4: PATCH & VALIDATE RESPONSE =========================
    @Test(priority = 4)
    public void testPatchResponseValidation() throws Exception {
        System.out.println("\n========== TEST 4: PATCH & VALIDATE RESPONSE ==========");

        // Step 1: Create patch payload using API POJO
        UserType patchPayload = new UserType();
        patchPayload.setPhonenumber(5551234);

        System.out.println("📤 PATCH Payload:");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(patchPayload));

        // Step 2: Call PATCH API
        Response patchResponse = RestClient.patch(BASE_URL, "/users/" + TEST_USER_ID, patchPayload)
                .then()
                .statusCode(200)
                .extract()
                .response();

        JsonPath patchJsonPath = patchResponse.jsonPath();
        System.out.println("📥 PATCH Response:");
        System.out.println(patchResponse.asString());

        // Step 3: Verify patched field with response.jsonPath()
        assertEquals(patchJsonPath.getInt("phonenumber"), patchPayload.getPhonenumber(),
                "PATCH response must contain patched phone");
        assertNotNull(patchJsonPath.getString("updatedAt"), "PATCH response must contain updatedAt");
        System.out.println("✅ PATCH response validation passed");
    }

    // ======================== TEST 5: DELETE & VALIDATE =========================
    @Test(priority = 5)
    public void testDeleteResponseValidation() throws Exception {
        System.out.println("\n========== TEST 5: DELETE & VALIDATE RESPONSE ==========");

        // Step 1: Delete a test user from DB first
        int deleteTestId = 300;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM USERS WHERE ID = ?")) {
            ps.setInt(1, deleteTestId);
            int deletedRows = ps.executeUpdate();
            System.out.println("🗑️  Deleted " + deletedRows + " row(s) from DB");
        }

        // Step 2: Call DELETE API
        Response deleteResponse = RestClient.delete(BASE_URL, "/users/" + TEST_USER_ID)
                .then()
                .statusCode(204)
                .extract()
                .response();

        System.out.println("✅ DELETE API Response Status: 204 (No Content)");

        // Step 3: Verify deletion in DB (simulate - ReqRes doesn't actually delete)
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM USERS WHERE ID = " + deleteTestId)) {
            assertTrue(rs.next(), "Query must return result");
            assertEquals(rs.getInt(1), 0, "Deleted user must not exist in DB");
            System.out.println("✅ DELETE validation passed");
        }
    }

    // ======================== TEST 6: COMPLETE CRUD FLOW WITH RESPONSE VALIDATION ==========
    @Test(priority = 6)
    public void testCompleteCRUDFlowWithResponseValidation() throws Exception {
        System.out.println("\n========== TEST 6: COMPLETE CRUD FLOW WITH RESPONSE VALIDATION ==========");

        JsonNode userJson = objectMapper.readTree(new java.io.File("src/test/resources/user.json"))
                .get("departments")
                .get(0)
                .get("employees")
                .get(0);

        // ---- CREATE ----
        UserType createPayload = new UserType();
        createPayload.setFirstname(userJson.get("firstName").asText());
        createPayload.setLastname(userJson.get("lastName").asText());
        createPayload.setEmail(userJson.get("email").asText());
        createPayload.setPhonenumber(Integer.parseInt(userJson.get("phoneNumber").asText().replaceAll("[^0-9]", "")));

        Response postResp = RestClient.post(BASE_URL, "/users", createPayload)
                .then()
                .statusCode(201)
                .extract()
                .response();

        JsonPath postRespJsonPath = postResp.jsonPath();
        assertEquals(postRespJsonPath.getString("firstname"), createPayload.getFirstname(), "POST firstname must match request");
        assertEquals(postRespJsonPath.getString("lastname"), createPayload.getLastname(), "POST lastname must match request");
        assertEquals(postRespJsonPath.getString("email"), createPayload.getEmail(), "POST email must match request");
        System.out.println("✅ POST: " + postRespJsonPath.getString("firstname"));

        // ---- READ ----
        Response getResp = RestClient.get(BASE_URL, "/users/" + TEST_USER_ID)
                .then()
                .statusCode(200)
                .extract()
                .response();

        JsonPath getRespJsonPath = getResp.jsonPath();
        String dbStoredEmail = getRespJsonPath.getString("data.email");
        assertNotNull(dbStoredEmail, "GET email must exist");
        System.out.println("✅ GET: " + getRespJsonPath.getString("data.first_name"));

        // ---- UPDATE ----
        UserType updatePayload = new UserType();
        updatePayload.setFirstname("Final");
        updatePayload.setLastname("UpdatedName");
        updatePayload.setEmail("final@example.com");

        Response putResp = RestClient.put(BASE_URL, "/users/" + TEST_USER_ID, updatePayload)
                .then()
                .statusCode(200)
                .extract()
                .response();

        JsonPath putRespJsonPath = putResp.jsonPath();
        System.out.println("✅ PUT: " + putRespJsonPath.getString("firstname"));

        // ---- VALIDATE CONSISTENCY ----
        assertEquals(putRespJsonPath.getString("firstname"), updatePayload.getFirstname(), "Update first name must match request");
        assertEquals(putRespJsonPath.getString("lastname"), updatePayload.getLastname(), "Update last name must match request");
        assertEquals(putRespJsonPath.getString("email"), updatePayload.getEmail(), "Update email must match request");
        
        System.out.println("✅ COMPLETE CRUD FLOW VALIDATION PASSED");
    }

    // ======================== TEST 7: RESPONSE VS DB DATA COMPARISON =========================
    @Test(priority = 7)
    public void testResponseVsDBDataComparison() throws Exception {
        System.out.println("\n========== TEST 7: RESPONSE VS DB DATA COMPARISON ==========");

        // Step 1: Get API response
        Response apiResponse = RestClient.get(BASE_URL, "/users/" + TEST_USER_ID)
                .then()
                .statusCode(200)
                .extract()
                .response();

        JsonPath apiJsonPath = apiResponse.jsonPath();
        assertNotNull(apiJsonPath.get("data"), "API response must contain user data");

        System.out.println("📤 API Response:");
        System.out.println(apiResponse.asString());

        // Step 2: Insert API response data in DB
        int comparisonTestId = apiJsonPath.getInt("data.id") + 400;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO USERS (ID, EMAIL, FIRST_NAME, LAST_NAME, AVATAR) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, comparisonTestId);
            ps.setString(2, apiJsonPath.getString("data.email"));
            ps.setString(3, apiJsonPath.getString("data.first_name"));
            ps.setString(4, apiJsonPath.getString("data.last_name"));
            ps.setString(5, apiJsonPath.getString("data.avatar"));
            ps.executeUpdate();
            System.out.println("📥 Inserted API response data in DB");
        }

        // Step 3: Fetch from DB as POJO
        UserType dbUser = null;
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT ID, EMAIL, FIRST_NAME, LAST_NAME FROM USERS WHERE ID = " + comparisonTestId)) {
            if (rs.next()) {
                dbUser = new UserType();
                dbUser.setId(rs.getInt("ID"));
                dbUser.setEmail(rs.getString("EMAIL"));
                dbUser.setFirstname(rs.getString("FIRST_NAME"));
                dbUser.setLastname(rs.getString("LAST_NAME"));
            }
        }

        assertNotNull(dbUser, "DB user must exist for API response comparison");

        // Step 4: Serialize DB data to JSON
        String dbJson = objectMapper.writeValueAsString(dbUser);
        JsonNode dbNode = objectMapper.readTree(dbJson);

        System.out.println("📥 DB Data (as JSON):");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dbNode));

        // Step 5: Compare critical fields with response.jsonPath()
        assertEquals(dbUser.getEmail(), apiJsonPath.getString("data.email"), "DB email must match API response");
        assertEquals(dbUser.getFirstname(), apiJsonPath.getString("data.first_name"), "DB first name must match API response");
        assertEquals(dbUser.getLastname(), apiJsonPath.getString("data.last_name"), "DB last name must match API response");

        System.out.println("✅ Response vs DB comparison test passed");
    }
}
