package main;

import client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import files.ConfigManager;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.Address;
import pojo.Preferences;
import pojo.UserType;
import utils.ObjectMapperUtil;

import static org.testng.Assert.assertEquals;

/**
 * POJO USAGE GUIDE IN CRUD OPERATIONS WITH RestClient
 * 
 * ============================================================================================================
 * WHAT ARE POJOs?
 * ============================================================================================================
 * POJO = Plain Old Java Object
 * POJOs are Java classes that represent data structures matching your API response/request format.
 * 
 * Benefits:
 * ✅ Type Safety: Compile-time checking instead of string-based JSON navigation
 * ✅ Readability: user.getEmail() vs jsonNode.get("email").asText()
 * ✅ Reusability: Use same POJO across different tests
 * ✅ Maintainability: One place to update field names
 * ✅ IDE Support: Auto-complete, refactoring, null-safety checks
 * ✅ Nested Objects: Handles complex nested structures (Address, Preferences)
 * ✅ Collections: Supports Lists, Maps for array fields (roles, tags, etc.)
 * ✅ Comparison: Can override equals() for object comparison
 * 
 * ============================================================================================================
 * WHERE ARE POJOs USED IN CRUD OPERATIONS?
 * ============================================================================================================
 */
public class PojoUsageGuide {

    static {
        // ensure config.properties is loaded before the BASE_URL lookup below runs
        ConfigManager.load(java.nio.file.Paths.get("src/test/resources/config.properties"));
    }

    private static final ObjectMapper objectMapper = ObjectMapperUtil.getInstance();
    private static final String BASE_URL = ConfigManager.getProperty("base.url");

    /**
     * USE CASE 1: DESERIALIZATION (Response → POJO)
     * 
     * Convert API response JSON into a strongly-typed Java object
     * When to use: After GET/POST/PUT/PATCH calls
     */
    @Test
    public void useCase1_DeserializeApiResponseToPojo() throws Exception {
        System.out.println("\n========== USE CASE 1: DESERIALIZATION (Response → POJO) ==========");

        // Step 1: Make API call
        Response apiResponse = RestClient.get(BASE_URL, "/users/2")
                .then()
                .statusCode(200)
                .extract()
                .response();

        String responseJson = apiResponse.asString();
        System.out.println("📥 Raw API Response:\n" + responseJson);

        // Step 2: WITHOUT POJO (Old way - String-based JSON navigation)
        JsonNode jsonNode = objectMapper.readTree(responseJson);
        String userIdOldWay = jsonNode.get("data").get("id").asText();
        String emailOldWay = jsonNode.get("data").get("email").asText();
        String firstNameOldWay = jsonNode.get("data").get("first_name").asText();

        System.out.println("\n❌ WITHOUT POJO (Error-prone):");
        System.out.println("   ID: " + userIdOldWay);
        System.out.println("   Email: " + emailOldWay);
        System.out.println("   Issues: String-based, typo-prone, no IDE support");

        // Step 3: WITH POJO (New way - Strongly typed)
        JsonNode dataNode = jsonNode.get("data");
        UserType userPojo = objectMapper.readValue(dataNode.toString(), UserType.class);

        System.out.println("\n✅ WITH POJO (Type-safe):");
        System.out.println("   ID: " + userPojo.getId());
        System.out.println("   Email: " + userPojo.getEmail());
        System.out.println("   FirstName: " + userPojo.getFirstname());
        System.out.println("   Benefits: Auto-complete, null-safety, refactoring support");
    }

    /**
     * USE CASE 2: SERIALIZATION (POJO → Request Payload)
     * 
     * Convert Java objects into JSON for POST/PUT/PATCH requests
     * When to use: Before creating/updating requests
     */
    @Test
    public void useCase2_SerializePojoToRequestPayload() throws Exception {
        System.out.println("\n========== USE CASE 2: SERIALIZATION (POJO → Request Payload) ==========");

        // Step 1: Create POJO with data
        UserType newUser = new UserType();
        newUser.setId(999);
        newUser.setUsername("johndoe");
        newUser.setFirstname("John");
        newUser.setLastname("Doe");
        newUser.setEmail("john@example.com");
        newUser.setStatus(true);

        System.out.println("📝 Created POJO:");
        System.out.println("   ID: " + newUser.getId());
        System.out.println("   Email: " + newUser.getEmail());

        // Step 2: Serialize POJO to JSON
        String jsonPayload = objectMapper.writeValueAsString(newUser);
        System.out.println("\n📤 Serialized to JSON:\n" + 
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(newUser));

        // Step 3: Use JSON payload in API request
        JsonNode payload = objectMapper.readTree(jsonPayload);
        Response postResponse = RestClient.post(BASE_URL, "/users", payload)
                .then()
                .statusCode(201)
                .extract()
                .response();

        System.out.println("✅ POST successful with POJO-derived payload");
    }

    /**
     * USE CASE 3: NESTED OBJECT HANDLING
     * 
     * Handle complex nested structures (Address, Preferences inside UserType)
     * When to use: When response contains nested objects
     */
    @Test
    public void useCase3_HandleNestedObjects() throws Exception {
        System.out.println("\n========== USE CASE 3: NESTED OBJECT HANDLING ==========");

        // Load user.json which has nested objects (departments[0].employees[0] holds the user record)
        JsonNode root = objectMapper.readTree(new java.io.File("src/test/resources/user.json"));
        JsonNode userJson = root.get("departments").get(0).get("employees").get(0);

        System.out.println("📥 API Response with nested objects:\n" +
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userJson));

        // Step 1: WITHOUT POJO (Deep navigation nightmare)
        String streetOldWay = userJson.get("address").get("street").asText();
        String cityOldWay = userJson.get("address").get("city").asText();
        String themeOldWay = userJson.get("preferences").get("theme").asText();

        System.out.println("\n❌ WITHOUT POJO (Deep nested navigation):");
        System.out.println("   userJson.get(\"address\").get(\"street\") → " + streetOldWay);
        System.out.println("   userJson.get(\"preferences\").get(\"theme\") → " + themeOldWay);

        // Step 2: WITH POJO (Clean object access)
        UserType userPojo = objectMapper.readValue(userJson.toString(), UserType.class);

        System.out.println("\n✅ WITH POJO (Clean access):");
        System.out.println("   Address Street: " + userPojo.getAddress().getStreet());
        System.out.println("   Address City: " + userPojo.getAddress().getCity());
        System.out.println("   Preferences Theme: " + userPojo.getPreferences().getTheme());
        System.out.println("   Much cleaner! No deep navigation needed");
    }

    /**
     * USE CASE 4: TYPE CONVERSION & VALIDATION
     * 
     * Automatically convert types (e.g., String to int, date parsing)
     * When to use: When response has type-sensitive fields
     */
    @Test
    public void useCase4_TypeConversionAndValidation() throws Exception {
        System.out.println("\n========== USE CASE 4: TYPE CONVERSION & VALIDATION ==========");

        UserType user = new UserType();
        user.setId(100);                          // Auto converts to int
        user.setPhonenumber(5551234);             // Auto converts to int
        user.setStatus(true);                     // Auto converts to boolean
        user.setEmail("user@example.com");        // String

        System.out.println("📝 POJO with Type Safety:");
        System.out.println("   ID (int): " + user.getId());
        System.out.println("   Phone (int): " + user.getPhonenumber());
        System.out.println("   Status (boolean): " + user.isStatus());
        System.out.println("   Email (String): " + user.getEmail());

        // Step 1: Serialize to JSON
        String json = objectMapper.writeValueAsString(user);
        System.out.println("\n✅ Types maintained during serialization:\n" + 
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(json)));

        // Step 2: Deserialize back (maintains types)
        UserType deserialized = objectMapper.readValue(json, UserType.class);
        System.out.println("✅ Types maintained during deserialization:");
       // System.out.println("   ID is int: " + (deserialized.getId() instanceof Integer));
       // System.out.println("   Status is boolean: " + (deserialized.isStatus() instanceof Boolean));
    }

    /**
     * USE CASE 5: COMPARISON & ASSERTION
     * 
     * Compare POJOs for test assertions
     * When to use: Validating request/response match or DB vs API comparison
     */
    @Test
    public void useCase5_ComparisonAndAssertion() throws Exception {
        System.out.println("\n========== USE CASE 5: COMPARISON & ASSERTION ==========");

        // Create expected POJO
        UserType expectedUser = new UserType();
        expectedUser.setId(1);
        expectedUser.setEmail("expected@example.com");
        expectedUser.setFirstname("John");
        expectedUser.setLastname("Doe");

        // Create actual POJO from API response
        UserType actualUser = new UserType();
        actualUser.setId(1);
        actualUser.setEmail("expected@example.com");
        actualUser.setFirstname("John");
        actualUser.setLastname("Doe");

        System.out.println("✅ POJO Assertion Examples:");
        System.out.println("   Expected ID: " + expectedUser.getId());
        System.out.println("   Actual ID: " + actualUser.getId());

        assertEquals(expectedUser.getId(), actualUser.getId(), "IDs must match");
        assertEquals(expectedUser.getEmail(), actualUser.getEmail(), "Emails must match");
        assertEquals(expectedUser.getFirstname(), actualUser.getFirstname(), "First names must match");

        System.out.println("   ✅ All assertions passed using POJOs");
    }

    /**
     * USE CASE 6: CRUD OPERATION WORKFLOW
     * 
     * Complete CRUD flow using POJOs at each step
     * When to use: Full integration testing
     */
    @Test
    public void useCase6_CompleteCrudWorkflowWithPojo() throws Exception {
        System.out.println("\n========== USE CASE 6: COMPLETE CRUD WORKFLOW WITH POJO ==========");

        // ===== CREATE (POST) =====
        System.out.println("\n📝 CREATE:");
        UserType newUser = new UserType();
        newUser.setFirstname("Alice");
        newUser.setLastname("Smith");
        newUser.setEmail("alice@example.com");
        newUser.setStatus(true);

        JsonNode createPayload = objectMapper.readTree(objectMapper.writeValueAsString(newUser));
        Response createResponse = RestClient.post(BASE_URL, "/users", createPayload)
                .then()
                .statusCode(201)
                .extract()
                .response();

        JsonNode createdJson = objectMapper.readTree(createResponse.asString());
        UserType createdUser = objectMapper.readValue(createdJson.toString(), UserType.class);
        System.out.println("✅ Created: " + createdUser.getFirstname() + " " + createdUser.getLastname());

        // ===== READ (GET) =====
        System.out.println("\n📖 READ:");
        Response readResponse = RestClient.get(BASE_URL, "/users/2")
                .then()
                .statusCode(200)
                .extract()
                .response();

        JsonNode readJson = objectMapper.readTree(readResponse.asString());
        UserType readUser = objectMapper.readValue(readJson.get("data").toString(), UserType.class);
        System.out.println("✅ Read: " + readUser.getFirstname() + " (ID: " + readUser.getId() + ")");

        // ===== UPDATE (PUT) =====
        System.out.println("\n✏️  UPDATE:");
        UserType updateUser = new UserType();
        updateUser.setFirstname("Alice Updated");
        updateUser.setEmail("alice.updated@example.com");

        JsonNode updatePayload = objectMapper.readTree(objectMapper.writeValueAsString(updateUser));
        Response updateResponse = RestClient.put(BASE_URL, "/users/2", updatePayload)
                .then()
                .statusCode(200)
                .extract()
                .response();

        JsonNode updatedJson = objectMapper.readTree(updateResponse.asString());
        UserType updatedUser = objectMapper.readValue(updatedJson.toString(), UserType.class);
        System.out.println("✅ Updated: " + updatedUser.getFirstname());

        // ===== DELETE =====
        System.out.println("\n🗑️  DELETE:");
        Response deleteResponse = RestClient.delete(BASE_URL, "/users/2")
                .then()
                .statusCode(204)
                .extract()
                .response();
        System.out.println("✅ Deleted successfully");

        System.out.println("\n✅ COMPLETE CRUD WORKFLOW USING POJO SUCCESSFUL");
    }

    /**
     * USE CASE 7: POJO WITH ADDRESS (Nested POJO)
     * 
     * Use nested POJOs for complex objects
     * When to use: API responses with nested structures
     */
    @Test
    public void useCase7_NestedPojoUsage() throws Exception {
        System.out.println("\n========== USE CASE 7: NESTED POJO USAGE ==========");

        // Create user with address
        UserType user = new UserType();
        user.setId(500);
        user.setFirstname("Bob");
        user.setLastname("Johnson");
        user.setEmail("bob@example.com");

        // Create nested Address POJO
        Address address = new Address();
        address.setStreet("123 Main St");
        address.setCity("New York");
        address.setState("NY");
        address.setZipcode("10001");

        user.setAddress(address);

        System.out.println("📝 User with nested Address POJO:");
        System.out.println("   Name: " + user.getFirstname() + " " + user.getLastname());
        System.out.println("   Address: " + user.getAddress().getStreet() + 
                ", " + user.getAddress().getCity() + ", " + user.getAddress().getState());

        // Serialize with nested objects
        String json = objectMapper.writeValueAsString(user);
        System.out.println("\n✅ Serialized (including nested):\n" +
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(json)));

        // Deserialize back
        UserType deserializedUser = objectMapper.readValue(json, UserType.class);
        System.out.println("✅ Deserialized - Full Address access:");
        System.out.println("   City: " + deserializedUser.getAddress().getCity());
        System.out.println("   State: " + deserializedUser.getAddress().getState());
    }

    /**
     * COMPARISON: WITH POJO vs WITHOUT POJO
     */
    @Test
    public void comparisonPojoVsNoPojo() throws Exception {
        System.out.println("\n========== COMPARISON: POJO vs NO POJO ==========");

        Response response = RestClient.get(BASE_URL, "/users/2")
                .then()
                .statusCode(200)
                .extract()
                .response();

        JsonNode jsonNode = objectMapper.readTree(response.asString());
        JsonNode dataNode = jsonNode.get("data");

        System.out.println("\n❌ WITHOUT POJO (Error-Prone):");
        System.out.println("   String-based access: jsonNode.get(\"email\").asText()");
        System.out.println("   No type safety: Typos cause runtime errors");
        System.out.println("   Deep nesting: jsonNode.get(\"address\").get(\"street\")");
        System.out.println("   No IDE support: Can't see available fields");
        System.out.println("   Manual assertions: assertEquals(obj1, obj2)");
        System.out.println("   Example: " + dataNode.get("email").asText());

        UserType userPojo = objectMapper.readValue(dataNode.toString(), UserType.class);

        System.out.println("\n✅ WITH POJO (Production Quality):");
        System.out.println("   Type-safe: user.getEmail()");
        System.out.println("   Compile-time checking: IDE catches errors");
        System.out.println("   Clean access: user.getAddress().getCity()");
        System.out.println("   Full IDE support: Auto-complete, refactoring");
        System.out.println("   Can override equals(): userPojo.equals(other)");
        System.out.println("   Example: " + userPojo.getEmail());

        System.out.println("\n🎯 RECOMMENDATION: Use POJOs for all CRUD operations!");
    }
}
