package client;

import io.qameta.allure.Allure;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import specifications.RequestSpecBuilderUtil;
import java.util.function.Supplier;
import static io.restassured.RestAssured.given;

public class RestClient {
    // GET (schema, no auth) — caller (main/inventree) supplies baseUrl resolved from config
    public static Response getSchema(String baseUrl, String endpoint) {
        return execute("GET", endpoint, null, () -> given()
                .spec(RequestSpecBuilderUtil.getRequestSpecWithoutAuth(baseUrl))
                .when()
                .get(endpoint));
    }

    // GET — caller supplies baseUrl resolved from config
    public static Response get(String baseUrl, String endpoint) {
        return execute("GET", endpoint, null, () -> given()
                .spec(RequestSpecBuilderUtil.getRequestSpecWithAuth(baseUrl))
                .when()
                .get(endpoint));
    }

    // GET (custom spec, e.g. a differently authenticated service)
    public static Response get(RequestSpecification spec, String endpoint) {
        return execute("GET", endpoint, null, () -> given()
                .spec(spec)
                .when()
                .get(endpoint));
    }

    // POST — caller supplies baseUrl resolved from config
    public static Response post(String baseUrl, String endpoint, Object body) {
        return execute("POST", endpoint, body, () -> given()
                .spec(RequestSpecBuilderUtil.getRequestSpecWithAuth(baseUrl))
                .body(body)
                .when()
                .post(endpoint));
    }

    // POST (custom spec)
    public static Response post(RequestSpecification spec, String endpoint, Object body) {
        return execute("POST", endpoint, body, () -> given()
                .spec(spec)
                .body(body)
                .when()
                .post(endpoint));
    }

    // PUT — caller supplies baseUrl resolved from config
    public static Response put(String baseUrl, String endpoint, Object body) {
        return execute("PUT", endpoint, body, () -> given()
                .spec(RequestSpecBuilderUtil.getRequestSpecWithAuth(baseUrl))
                .body(body)
                .when()
                .put(endpoint));
    }

    // PUT (custom spec)
    public static Response put(RequestSpecification spec, String endpoint, Object body) {
        return execute("PUT", endpoint, body, () -> given()
                .spec(spec)
                .body(body)
                .when()
                .put(endpoint));
    }

    // PATCH — caller supplies baseUrl resolved from config
    public static Response patch(String baseUrl, String endpoint, Object body) {
        return execute("PATCH", endpoint, body, () -> given()
                .spec(RequestSpecBuilderUtil.getRequestSpecWithAuth(baseUrl))
                .body(body)
                .when()
                .patch(endpoint));
    }

    // PATCH (custom spec)
    public static Response patch(RequestSpecification spec, String endpoint, Object body) {
        return execute("PATCH", endpoint, body, () -> given()
                .spec(spec)
                .body(body)
                .when()
                .patch(endpoint));
    }

    // DELETE — caller supplies baseUrl resolved from config
    public static Response delete(String baseUrl, String endpoint) {
        return execute("DELETE", endpoint, null, () -> given()
                .spec(RequestSpecBuilderUtil.getRequestSpecWithAuth(baseUrl))
                .when()
                .delete(endpoint));
    }

    // DELETE (custom spec)
    public static Response delete(RequestSpecification spec, String endpoint) {
        return execute("DELETE", endpoint, null, () -> given()
                .spec(spec)
                .when()
                .delete(endpoint));
    }

    private static Response execute(String  method,String endpoint,Object body,Supplier<Response> request){
        Allure.addAttachment("Request", "text/plain", method + " " + endpoint);
        if (body != null) {
            Allure.addAttachment("Request body", "application/json", String.valueOf(body));
        }
        Response response = request.get();
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Allure.addAttachment("Status", "text/plain", Integer.toString(response.statusCode()));
        return response;
    }
}
