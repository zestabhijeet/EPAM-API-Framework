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
 */
final class InvenTreeApiSupport {

    static {
        ConfigManager.load(Paths.get(ApplicationConstant.CONFIG_FILE_PATH));
    }

    static final String BASE_URL = ConfigManager.getProperty(ApplicationConstant.INVENTREE_BASE_URL_KEY, "");
    static final String API_TOKEN = ConfigManager.getProperty(ApplicationConstant.INVENTREE_API_TOKEN_KEY, "");

    static final String PART_PATH = ConfigManager.getProperty(ApplicationConstant.INVENTREE_PART_PATH_KEY, ApplicationConstant.INVENTREE_PART_PATH_DEFAULT);
    static final String CATEGORY_PATH = "/api/part/category/";
    static final String COMPANY_PATH = "/api/company/";
    static final String SUPPLIER_PART_PATH = "/api/company/part/";

    private InvenTreeApiSupport() {
    }

    static void requireConfigured() {
        if (isBlank(BASE_URL) || isBlank(API_TOKEN)) {
            throw new SkipException("InvenTree suite skipped: set INVENTREE_BASE_URL and INVENTREE_API_TOKEN");
        }
    }

    static RequestSpecification authenticatedSpec() {
        return RequestSpecBuilderUtil.getRequestSpecWithAuth(BASE_URL, "Authorization", "Token " + API_TOKEN);
    }

    static String unique(String prefix) {
        return prefix + "-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank() || value.startsWith("REPLACE_WITH_");
    }
}
