package inventree;

import constant.ApplicationConstant;
import files.ConfigManager;
import io.restassured.specification.RequestSpecification;
import org.testng.SkipException;
import specifications.RequestSpecBuilderUtil;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Shared InvenTree connection plumbing for the Part, Category, SupplierPart
 * and security suites: config/env resolution, the "skip if not configured"
 * guard, the authenticated request spec, and endpoint paths.
 *
 * Public so the {@code ui} package's Selenium suites can reuse it for fast,
 * reliable API-driven test-data setup/teardown instead of duplicating this
 * config/spec logic or tearing down fixtures by driving the UI.
 */
public final class InvenTreeApiSupport {

    static {
        ConfigManager.load(Paths.get(ApplicationConstant.CONFIG_FILE_PATH));
    }

    public static final String BASE_URL = ConfigManager.getProperty(ApplicationConstant.INVENTREE_BASE_URL_KEY, "");
    public static final String API_TOKEN = ConfigManager.getProperty(ApplicationConstant.INVENTREE_API_TOKEN_KEY, "");

    public static final String PART_PATH = ConfigManager.getProperty(ApplicationConstant.INVENTREE_PART_PATH_KEY, ApplicationConstant.INVENTREE_PART_PATH_DEFAULT);
    public static final String CATEGORY_PATH = "/api/part/category/";
    public static final String COMPANY_PATH = "/api/company/";
    public static final String SUPPLIER_PART_PATH = "/api/company/part/";
    public static final String PARAMETER_TEMPLATE_PATH = "/api/parameter/template/";
    public static final String PARAMETER_PATH = "/api/parameter/";

    private InvenTreeApiSupport() {
    }

    public static void requireConfigured() {
        if (isBlank(BASE_URL) || isBlank(API_TOKEN)) {
            throw new SkipException("InvenTree suite skipped: set INVENTREE_BASE_URL and INVENTREE_API_TOKEN");
        }
    }

    public static RequestSpecification authenticatedSpec() {
        return RequestSpecBuilderUtil.getRequestSpecWithAuth(BASE_URL, "Authorization", "Token " + API_TOKEN);
    }

    public static String unique(String prefix) {
        return prefix + "-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank() || value.startsWith("REPLACE_WITH_");
    }
}
