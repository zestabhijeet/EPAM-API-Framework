package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.restassured.response.Response;

import java.io.File;
import java.io.IOException;

public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    //Read Value based on Root Node of Json
    public static JsonNode getJsonNode(String path, String key) throws Exception {
        JsonNode root = mapper.readTree(new File(path));
        return root.get(key);
    }
    // Nested key lookup via JSON Pointer, e.g. "/data/person/id"
    public static JsonNode getJsonNodeAtPath(String jsonContent, String jsonPointerExpr) {
        JsonNode node = null;
        try {
            node = mapper.readTree(jsonContent).at(jsonPointerExpr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON content", e);
        }
        if (node.isMissingNode()) {
            throw new IllegalArgumentException("Path not found: " + jsonPointerExpr);
        }
        return node;
    }
    // Read JSON directly from a REST-assured Response body
    public static JsonNode readTree(Response response) {
        try {
            return mapper.readTree(response.asString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON from response body", e);
        }
    }
    // Write any object (JsonNode, POJO, Response from Get Call.) to a file
    public static void writeValue(String path, Object value) {
        try {
            File file = new File(path);
            file.getParentFile().mkdirs();
            mapper.writeValue(file, value);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON file: " + path, e);
        }
    }

}


