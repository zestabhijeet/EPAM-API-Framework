package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;

import java.io.File;
import java.io.IOException;

public class JsonUtil {

    private static final ObjectMapper mapper = ObjectMapperUtil.getInstance();

    //Read Value based on Root Node of Json
    public static JsonNode getJsonNode(String path, String key) throws Exception {
        JsonNode root = mapper.readTree(new File(path));
        return root.get(key);
    }

    // Read JSON directly from a REST-assured Response body
    public static JsonNode readTree(Response response) {
        try {
            return mapper.readTree(response.asString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON from response body", e);
        }
    }

}

