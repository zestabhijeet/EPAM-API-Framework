package utils;

import com.fasterxml.jackson.databind.JsonNode;
import files.ConfigManager;
import io.restassured.response.Response;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import org.testng.annotations.Test;
import static io.restassured.RestAssured.given;
/*
Additional Oauth layer Created
Its Provides Additional Security Layer from traditional Token
1.Short-lived tokens – Tokens expire automatically, limiting misuse
2.Scoped permissions – scope controls which resources or actions are allowed.
3.Refresh support – Refresh tokens can obtain new access tokens without repeating login.
4.Centralized authentication – Identity and login are handled by an authorization server.
5.Token revocation – Tokens can be invalidated when a user logs out or access is removed.
6.Separation of credentials – The client does not need to expose the user’s password to every API.
*/
public class AuthTest {
    static {ConfigManager.load(Paths.get("src/test/resources/config.properties"));}
    private static String accessToken;
   /*
   For a real OAuth provider, the token request commonly uses client_id, client_secret, and grant_type instead.
   */
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
    private static String encryptToken(String token, String base64Key) throws GeneralSecurityException {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(Base64.getDecoder().decode(base64Key),"AES"),
                new GCMParameterSpec(128, iv));
        String encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        return Base64.getEncoder().encodeToString(iv) + ":" + encrypted;
    }
}

/*
Manual Validation for OAuth Token from Terminal
curl -X POST "https://reqres.in/api/login" \
  -H "Content-Type: application/json" \
  -H "x-api-key: ${REQRES_API_KEY}" \
  -d '{"email":"eve.holt@reqres.in","password":"cityslicka"}'
 */