package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonNodeReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]+(?:\\[[0-9]+\\])?");

    public static JsonNode read(String filePath, String expression) throws IOException {
        JsonNode root = MAPPER.readTree(new File(filePath));
        return read(root, expression);
    }

    public static JsonNode read(JsonNode root, String expression) {
        if (root == null) {
            throw new IllegalArgumentException("JSON root is null");
        }
        if (expression == null || expression.trim().isEmpty()) {
            return root;
        }
        String normalized = expression.trim();
        if (normalized.startsWith("/")) {
            JsonNode node = root.at(normalized);
            if (node.isMissingNode()) {
                throw new IllegalArgumentException("Path not found: " + normalized);
            }
            return node;
        }

        JsonNode current = root;
        for (String token : tokenize(normalized)) {
            if (token == null || token.isBlank()) {
                continue;
            }
            Matcher matcher = Pattern.compile("([A-Za-z0-9_-]+)(?:\\[(\\d+)\\])?").matcher(token);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Unsupported JSON path token: " + token);
            }

            String fieldName = matcher.group(1);
            String indexValue = matcher.group(2);
            if (current == null || current.isMissingNode()) {
                throw new IllegalArgumentException("Path not found: " + expression);
            }

            if (current.isArray()) {
                if (indexValue == null) {
                    throw new IllegalArgumentException("Expected array index in token: " + token);
                }
                int index = Integer.parseInt(indexValue);
                current = current.get(index);
            } else {
                current = current.get(fieldName);
                if (indexValue != null && current != null && current.isArray()) {
                    int index = Integer.parseInt(indexValue);
                    current = current.get(index);
                }
            }

            if (current == null || current.isMissingNode()) {
                throw new IllegalArgumentException("Path not found: " + expression);
            }
        }
        return current;
    }

    private static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(expression);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Invalid JSON path: " + expression);
        }
        return tokens;
    }
}
