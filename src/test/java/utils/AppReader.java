package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pojo.UserType;

import java.io.File;

public class AppReader {

    public static UserType readuser() throws Exception {
        ObjectMapper omapper = ObjectMapperUtil.getInstance();
        JsonNode root = omapper.readTree(new File("src/test/resources/user.json"));
        JsonNode employee = root
                .path("departments")
                .get(0)
                .path("employees")
                .get(0);

        UserType user = omapper.treeToValue(employee, UserType.class);
        System.out.println(user);
        return user;
    }
}