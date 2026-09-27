package main;

import files.ConfigManager;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import pojo.UserType;
import specifications.RequestSpecBuilderUtil;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

public class AssertionWithRest {

    /*
     3 different ways for Carrying out validation - Assertion
     1.At RestClient Level configuring the body with pojo class and carrying assertion
     2.At Business Layer setting a.calling objectUtil b.Setting pojo and writevalueAsString
     3.Leveraging Queryparams for Validation a.At Url b. Validation in body as content Type
    */
    private static final String Schema_Template = "schema/userapi.json";
    @Test
    public void RequestValidationwithStatus() {
        UserType user = new UserType();
        user.setId(1001);
        user.setFirstname("Abhijeet");
        user.setLastname("kumar");
        user.setEmail("abc@gmail.com");
        user.setDepartment("emp");
        user.setCompany("zothopia");
        user.setPhonenumber(123456);

        Response response = given()
                .spec(RequestSpecBuilderUtil.getRequestSpecWithAuth(ConfigManager.getProperty("base.url")))
                .body(user)
                .when()
                .post("/users")
                .then()
                .extract()
                .response();
        Assert.assertEquals(response.statusCode(),201,"status received for post");
        // reqres.in echoes the posted body back verbatim, using the exact field
        // names UserType serializes (lowercase, no renaming for these fields).
        Assert.assertEquals(response.jsonPath().getString("firstname"),"Abhijeet","Firstname Matches");
        Assert.assertEquals(response.jsonPath().getString("lastname"),"kumar","last name matches");
        Assert.assertEquals(response.jsonPath().getInt("phonenumber"),123456,"Phone number matched");
    }

    @Test
    public void RequestValidationwithQueryParams() {
        UserType user = new UserType();
        user.setId(1001);
        user.setFirstname("Abhijeet");
        user.setLastname("kumar");
        user.setEmail("abc@gmail.com");
        user.setDepartment("emp");
        user.setCompany("zothopia");
        user.setPhonenumber(123456);

        Map<String,Object> queryparam = new HashMap<>();
        queryparam.put("source", "web");
        queryparam.put("channel","Internal");

        Response response = given()
                .spec(RequestSpecBuilderUtil.getRequestSpecWithAuth(ConfigManager.getProperty("base.url")))
                .body(user)
                .queryParams(queryparam)
                .when()
                .post("/users")
                .then()
                .extract()
                .response();
        Assert.assertEquals(response.statusCode(),201,"status received for post");
        Assert.assertEquals(response.jsonPath().getString("firstname"),"Abhijeet","Firstname Matches");
        Assert.assertEquals(response.jsonPath().getString("lastname"),"kumar","last name matches");
        Assert.assertEquals(response.jsonPath().getInt("phonenumber"),123456,"Phone number matched");

    }
    @Test
    public void ContractRequestValidation() {
        // /users/2 is a known-existing reqres.in user, matching the {data:{...}} shape in schema/userapi.json
        Response response  = given()
                .spec(RequestSpecBuilderUtil.getRequestSpecWithAuth(ConfigManager.getProperty("base.url")))
                .when()
                .get("/users/2")
                .then()
                .body(matchesJsonSchemaInClasspath(Schema_Template ))
                .extract()
                .response();
        Assert.assertNotNull(response.jsonPath().get("data.id"), "id should not be null");
    }
}
