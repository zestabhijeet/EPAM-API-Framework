package inventree;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pojo.InventreePart;
import utils.JsonUtil;
import utils.ObjectMapperUtil;

/**
 * Loads the static (non-unique) InvenTree Part fixtures from
 * {@code inventree-part-testdata.json} and hands back {@link InventreePart}
 * instances, with the caller supplying only the values that must be
 * generated at runtime (name, IPN, or a value read back from a prior call).
 */
public class InventreePartTestData {

    private static final String DATA_FILE = "src/test/resources/inventree-part-testdata.json";
    private static final ObjectMapper MAPPER = ObjectMapperUtil.getInstance();

    private InventreePartTestData() {
    }

    public static InventreePart minimalPart(String name) {
        InventreePart part = load("minimalPart");
        part.setName(name);
        return part;
    }

    public static InventreePart fullPart(String name, String ipn) {
        InventreePart part = load("fullPart");
        part.setName(name);
        part.setIpn(ipn);
        return part;
    }

    public static InventreePart putReplacement(String name) {
        InventreePart part = load("putReplacement");
        part.setName(name);
        return part;
    }

    public static InventreePart invalidPutWithoutName() {
        return load("invalidPutWithoutName");
    }

    public static InventreePart readOnlyFieldAttempt(String existingName) {
        InventreePart part = load("readOnlyFieldAttempt");
        part.setName(existingName);
        return part;
    }

    private static InventreePart load(String key) {
        try {
            JsonNode node = JsonUtil.getJsonNode(DATA_FILE, key);
            return MAPPER.treeToValue(node, InventreePart.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load InvenTree Part test data for key: " + key, e);
        }
    }
}
