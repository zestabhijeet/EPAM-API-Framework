package specifications;

import files.ConfigManager;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import utils.AuthTest;

import java.nio.file.Paths;

public class RequestSpecBuilderUtil {

    static {
        // load properties from test resources
        ConfigManager.load(Paths.get("src/test/resources/config.properties"));
    }

    public static RequestSpecification getRequestSpecWithoutAuth(String baseUrl) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType("application/json")
                .build();
    }
    public static RequestSpecification getRequestSpecWithAuth(String baseUrl) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType("application/json")
                .addHeader("x-api-key",ConfigManager.getProperty("api-key"))
                .build();
    }
    public static RequestSpecification getRequestSpecWithOAuth(String baseUrl) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + AuthTest.getToken())
                .build();
    }

    /**
     * Same intent as {@link #getRequestSpecWithAuth(String)}, parametrised further
     * for services with a different auth header/scheme than this project's
     * default {@code x-api-key} header.
     */
    public static RequestSpecification getRequestSpecWithAuth(String baseUrl, String headerName, String headerValue) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType("application/json")
                .setAccept("application/json")
                .addHeader(headerName, headerValue)
                .build();
    }


}