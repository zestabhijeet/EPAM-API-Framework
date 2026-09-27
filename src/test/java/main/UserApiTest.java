package main;
import client.RestClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import files.ConfigManager;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import pojo.Address;
import pojo.Preferences;
import pojo.UserType;
import utils.JsonNodeReader;
import utils.ObjectMapperUtil;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;

/*
Test Layer (UserApiTest) - Using user.json for comprehensive CRUD testing with POJO mapping
        ↓
RestClient (GET/POST/PUT/PATCH/DELETE)
        ↓
RequestSpecBuilderUtil (config, headers)
        ↓
POJO → JSON serialization for payloads
        ↓
JSON from user.json (comprehensive test data)

 */

public class UserApiTest {

    static {
        // ensure config.properties is loaded before the BASE_URL lookup below runs
        ConfigManager.load(java.nio.file.Paths.get("src/test/resources/config.properties"));
    }

    private static final ObjectMapper objectMapper = ObjectMapperUtil.getInstance();
    private static final String USER_DATA_FILE = "src/test/resources/user.json";
    private static final String SCHEMA_USER_ID = "2";  // ReqRes has pre-existing users 1-12
    private static final String BASE_URL = ConfigManager.getProperty("base.url");

    @Test
    public void userCrudFlowUsingRestClientWithUserJsonAndPojoMapping() throws Exception {
        // Load the selected employee from the nested test-data resource.
        JsonNode departmentJson = JsonNodeReader.read(USER_DATA_FILE, "departments[0]");
        JsonNode userJson = JsonNodeReader.read(USER_DATA_FILE, "departments[0].employees[0]");
        JsonNode firstRoleJson = JsonNodeReader.read(USER_DATA_FILE, "departments[0].employees[0].roles[0]");

        // ================================ POST (Using POJO for payload creation) ====================================
        // Create UserType POJO from user.json data
        UserType createUserPojo = new UserType();
        createUserPojo.setId(999);  // Temp ID for creation
        createUserPojo.setCompany("Global");
        createUserPojo.setDepartment(departmentJson.get("deptname").asText());
        createUserPojo.setUsername(userJson.get("username").asText());
        createUserPojo.setFirstname(userJson.get("firstName").asText());
        createUserPojo.setLastname(userJson.get("lastName").asText());
        createUserPojo.setEmail(userJson.get("email").asText());
        createUserPojo.setPhonenumber(Integer.parseInt(userJson.get("phoneNumber").asText().replaceAll("[^0-9]", "")));
        createUserPojo.setStatus(userJson.get("isActive").asBoolean());
        createUserPojo.setRoles(objectMapper.convertValue(userJson.get("roles"), new TypeReference<>() { }));

        // Create nested Address POJO
        Address address = new Address();
        address.setStreet(userJson.get("address").get("street").asText());
        address.setCity(userJson.get("address").get("city").asText());
        address.setState(userJson.get("address").get("state").asText());
        address.setZipcode(userJson.get("address").get("zipCode").asText());
        createUserPojo.setAddress(address);

        // Create nested Preferences POJO
        Preferences preferences = new Preferences();
        preferences.setTheme(userJson.get("preferences").get("theme").asText());
        createUserPojo.setPreferences(preferences);

        System.out.println("\n📝 Created UserType POJO for POST:");
        System.out.println("   Name: " + createUserPojo.getFirstname() + " " + createUserPojo.getLastname());
        System.out.println("   Email: " + createUserPojo.getEmail());
        System.out.println("   Primary Role: " + firstRoleJson.asText());
        System.out.println("   Address: " + createUserPojo.getAddress().getCity() + ", " + createUserPojo.getAddress().getState());
        System.out.println("   Preferences: " + createUserPojo.getPreferences().getTheme());
        // Serialize POJO to JSON payload
        JsonNode createPayload = objectMapper.readTree(objectMapper.writeValueAsString(createUserPojo));
        //System.out.println("\n📤 POST Payload (from POJO serialization):");
        //System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(createPayload));
        Response postResponse = RestClient.post(BASE_URL, "/users", createPayload)
                .then()
                .statusCode(201)
                .extract()
                .response();
        JsonNode postResponseJson = objectMapper.readTree(postResponse.asString());
        String createdUserId = postResponseJson.get("id").asText();
        System.out.println("✅ Created User ID: " + createdUserId);
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(postResponseJson));

        // ================================= PUT (Using POJO for payload creation) ====================================
        // Create UserType POJO for update
        UserType updateUserPojo = new UserType();
        updateUserPojo.setFirstname("Updated " + userJson.get("firstName").asText());
        updateUserPojo.setLastname(userJson.get("lastName").asText());
        updateUserPojo.setEmail("updated." + userJson.get("email").asText());
        updateUserPojo.setPhonenumber(999999999);  // Updated phone
        updateUserPojo.setStatus(true);

        // Update nested objects
        Address updatedAddress = new Address();
        updatedAddress.setStreet("Updated " + userJson.get("address").get("street").asText());
        updatedAddress.setCity(userJson.get("address").get("city").asText());
        updatedAddress.setState(userJson.get("address").get("state").asText());
        updatedAddress.setZipcode(userJson.get("address").get("zipCode").asText());
        updateUserPojo.setAddress(updatedAddress);

        Preferences updatedPreferences = new Preferences();
        updatedPreferences.setTheme("dark");  // Updated theme
        //updatedPreferences.setNotifications(false);  // Updated notifications
        updateUserPojo.setPreferences(updatedPreferences);

        System.out.println("\n📝 Created UserType POJO for PUT:");
        System.out.println("   Name: " + updateUserPojo.getFirstname() + " " + updateUserPojo.getLastname());
        System.out.println("   Email: " + updateUserPojo.getEmail());
        System.out.println("   Address: " + updateUserPojo.getAddress().getStreet());
        System.out.println("   Preferences: " + updateUserPojo.getPreferences().getTheme() + " theme");

        // Serialize POJO to JSON payload
        JsonNode updatePayload = objectMapper.readTree(objectMapper.writeValueAsString(updateUserPojo));
        System.out.println("\n📤 PUT Payload (from POJO serialization):");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(updatePayload));
        Response putResponse = RestClient.put(BASE_URL, "/users/" + createdUserId, updatePayload)
                .then()
                .statusCode(200)
                .extract()
                .response();
        JsonNode putResponseJson = objectMapper.readTree(putResponse.asString());
        System.out.println("\n📥 PUT Response:");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(putResponseJson));
        assertEquals(putResponseJson.get("firstname").asText(),updateUserPojo.getFirstname(),"Updated first name must match POJO data");
        assertEquals(putResponseJson.get("lastname").asText(),updateUserPojo.getLastname(),"Updated last name must match POJO data");
        System.out.println("✅ PUT verification passed");

        // ==================================== PATCH (Using POJO for payload creation) ================================
        // Create UserType POJO for partial update
        UserType patchUserPojo = new UserType();
        patchUserPojo.setPhonenumber(Integer.parseInt(userJson.get("phoneNumber").asText().replaceAll("[^0-9]", "")));
        patchUserPojo.setStatus(false);  // Partial update
        System.out.println("\n📝 Created UserType POJO for PATCH:");
        System.out.println("   Phone: " + patchUserPojo.getPhonenumber());
        System.out.println("   Status: " + patchUserPojo.isStatus());

        // Serialize POJO to JSON payload
        JsonNode patchPayload = objectMapper.readTree(objectMapper.writeValueAsString(patchUserPojo));
        System.out.println("\n📤 PATCH Payload (from POJO serialization):");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(patchPayload));

        Response patchResponse = RestClient.patch(BASE_URL, "/users/" + createdUserId, patchPayload)
                .then()
                .statusCode(200)
                .extract()
                .response();
        JsonNode patchResponseJson = objectMapper.readTree(patchResponse.asString());
        System.out.println("\n📥 PATCH Response:");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(patchResponseJson));

        assertEquals(patchResponseJson.get("phonenumber").asText(),
                String.valueOf(patchUserPojo.getPhonenumber()),
                "Patched phone must match POJO data");
        System.out.println("✅ PATCH verification passed");

        // ============================== DELETE ======================================================================
        // Note: ReqRes returns 204 but doesn't actually delete
        RestClient.delete(BASE_URL, "/users/" + createdUserId)
                .then()
                .statusCode(204);
        System.out.println("✅ DELETE successful (HTTP 204)");

        // ================= SCHEMA VALIDATION ========================================================================
        // Verify response structure matches userapi.json schema
        RestClient.get(BASE_URL, "/users/" + SCHEMA_USER_ID)  // Use reliable existing user to validate schema
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schema/userapi.json"));
        System.out.println("✅ Schema validation passed");

        // ================= POJO RESPONSE MAPPING DEMONSTRATION ===================================================
        System.out.println("\n🔄 POJO Response Mapping Demonstration:");
        // ReqRes simulates writes; a later GET does not return PUT/PATCH changes.
        UserType responseUserPojo = objectMapper.readValue(putResponseJson.toString(), UserType.class);

        System.out.println("✅ PUT response mapped back to POJO:");
        System.out.println("   ID: " + responseUserPojo.getId());
        System.out.println("   Email: " + responseUserPojo.getEmail());
        System.out.println("   FirstName: " + responseUserPojo.getFirstname());
        System.out.println("   LastName: " + responseUserPojo.getLastname());

        System.out.println("\n✅ All CRUD operations completed successfully using POJO mapping for payloads!");
    }
}
