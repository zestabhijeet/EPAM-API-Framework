package utils;

import com.fasterxml.jackson.databind.JsonNode;
import files.ConfigManager;
import io.restassured.response.Response;

import java.nio.file.Paths;
import org.testng.annotations.Test;
import static io.restassured.RestAssured.given;

public class AuthTest {
    static {ConfigManager.load(Paths.get("src/test/resources/config.properties"));}
    private static String accessToken;

   @Test
   public void verifyToken() {
       String token = getToken();
       System.out.println("Token retrieved: " + maskToken(token));
   }
    public static String getToken() {
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken;
        }
        String requestBody = """
                {
                  "email": "eve.holt@reqres.in",
                  "password": "cityslicka"
                }
                """;
        Response response = given()
                .baseUri(ConfigManager.getProperty("base.url"))
                .contentType("application/json")
                .header("x-api-key", ConfigManager.getProperty("api-key"))
                .body(requestBody)
                .when()
                .post("/login");

        if (response.statusCode() == 401) {
            throw new IllegalStateException("Authentication failed with HTTP 401: credentials or token are unauthorized");
        }
        response.then().statusCode(200);
        JsonNode jsonResponse = JsonUtil.readTree(response);
        accessToken = jsonResponse.get("token").asText();
        //System.out.println("Token: " + accessToken);
        return accessToken;
    }
    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "********";
        }
        return token.substring(0, 4)
                + "********"
                + token.substring(token.length() - 4);
    }
}