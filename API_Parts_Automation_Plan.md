# InvenTree Part API Automation Plan

## Objective

Create an executable end-to-end API automation flow for an InvenTree instance using the existing Maven, REST Assured, TestNG, and Allure framework.

The complete flow must be implemented and executed from a **single E2E test file**. The test file will retain created Part IDs and response state between scenarios so that creation, retrieval, update, negative validation, and deletion behavior are tested as one business flow.

Reference schema:

<https://docs.inventree.org/en/stable/api/schema/part/>

Target API:

```text
/api/part/
```

## Mandatory Entry-Point Schema Validation

Schema validation is the first condition of the E2E execution. No CRUD scenario or dependent setup should run until this condition passes.

The entry point must:

1. Verify that the configured InvenTree instance is available.
2. Validate the API/OpenAPI contract required by the test.
3. Validate the `/api/part/` list response structure.
4. Validate the Part detail response structure.
5. Verify required fields, types, maximum lengths, and nullable behavior.
6. Verify writable and read-only field metadata used by the tests.
7. Fail immediately if the schema or required endpoint contract is incompatible.

Only after schema validation passes should the test authenticate, discover/configure category and stock-location prerequisites, and begin the CRUD flow.

## Single-File E2E Scenario Flow

All scenarios below must be orchestrated from one test class/file, for example:

```text
src/test/java/inventree/InvenTreePartE2ETest.java
```

The flow may use ordered TestNG methods or one explicit test method with named Allure steps. Shared IDs and response data must be retained between steps.

| ID | Scenario | Expected assertions |
|---|---|---|
| API-PART-001 | Create Part with only the required field | `POST /api/part/` returns `201`; response conforms to the Part schema; `name` is persisted; default values are valid |
| API-PART-002 | Create Part with writable attributes | Create using IPN, revision, keywords, units, category, location, stock flags, and notes; verify supplied values are returned and persisted |
| API-PART-003 | Retrieve Part by ID | `GET /api/part/{id}/` returns `200`; ID and created fields match; detail response validates against schema |
| API-PART-004 | Retrieve Part with category and path details | Use `category_detail=true` and `path_detail=true`; verify requested detail fields and category/path business data |
| API-PART-005 | PATCH only supplied fields | Patch description and/or keywords; verify changed fields persist and omitted fields remain unchanged |
| API-PART-006 | PUT replaces the Part | Send a complete valid replacement; verify the PUT response contains the replacement values and expected server-managed fields |
| API-PART-007 | PUT without required `name` | Send an invalid replacement without `name`; verify client-error status and error payload; confirm the existing record is unchanged |
| API-PART-008 | Read-only and writable attributes | Attempt to submit server-managed/read-only fields with valid writable changes; verify read-only values are not corrupted and valid fields follow documented behavior |
| API-PART-009 | Delete an inactive Part | Set `active=false`, delete the Part, expect `204`, then verify the Part is no longer retrievable |
| API-PART-010 | Block deletion of an active Part | Attempt deletion while active; verify documented client-error status/message and confirm the Part remains retrievable |
| API-PART-011 | Block deletion of a locked Part | Set `locked=true`, attempt deletion, verify the documented client error, and confirm the Part remains available |

## Coverage Requirements

### Positive scenarios

- Minimal Part creation
- Fully populated writable Part creation
- Retrieval by ID
- Retrieval with category/path details
- Valid PATCH
- Valid PUT
- Deletion of an inactive Part

### Negative scenarios

- PUT without the required `name`
- Invalid field types and invalid values where supported by the schema
- Attempts to modify read-only fields
- Deletion of an active Part
- Deletion of a locked Part

### Boundary scenarios

Use parameterized test data within the same E2E file for schema limits, including:

- `name` at its maximum length and above its maximum length
- `IPN` at its maximum length and above its maximum length
- `description` at its maximum length and above its maximum length
- `keywords` at its maximum length and above its maximum length
- `units` at its maximum length and above its maximum length
- `notes` at a valid large value and above its documented maximum
- `default_expiry` at zero, a valid positive value, and an invalid negative value
- Null handling for fields documented as nullable

## Authentication and Configuration

Do not commit credentials or instance-specific IDs. Read configuration from environment variables or an untracked local properties file.

Required configuration:

```bash
export INVENTREE_BASE_URL=http://localhost:8000
export INVENTREE_API_TOKEN=your-token
```

Optional configuration when the test does not discover prerequisites dynamically:

```bash
export INVENTREE_CATEGORY_ID=1
export INVENTREE_LOCATION_ID=1
```

The API token must be sent using the InvenTree token authentication format:

```text
Authorization: Token <token>
```

## Test Data and State

The single E2E file should:

- Generate unique names/IPNs to avoid collisions with existing data.
- Store created Part IDs in fields owned by the test flow.
- Preserve original values before PATCH and PUT operations.
- Use response data for subsequent business assertions.
- Track records created by the test for cleanup.
- Discover category and location prerequisites where possible instead of assuming fixed IDs.

## Cleanup Requirements

Cleanup must run after the flow and must not silently swallow errors.

- Delete inactive test Parts created by the suite.
- Restore or remove temporary records used by negative scenarios where possible.
- Leave active or locked records only when deletion is intentionally blocked and the test cannot safely change their state.
- Report cleanup failures clearly in the test output and Allure report.

## Response and Business Assertions

Every request must assert:

- Expected HTTP status code
- Response content type where applicable
- Response schema or required response fields
- Persistence of submitted writable values
- Protection of server-managed/read-only values
- Record state after rejected operations
- Deletion and retrieval behavior after successful or blocked deletes

Error assertions should validate stable status and business behavior. Exact error text should only be asserted where it is stable across the supported InvenTree version.

## Execution Commands

Compile and execute the single E2E file:

```bash
mvn -Pinventree -Dtest=InvenTreePartE2ETest test
```

Execute with a local InvenTree instance:

```bash
INVENTREE_BASE_URL=http://localhost:8000 \
INVENTREE_API_TOKEN="$INVENTREE_API_TOKEN" \
mvn -Pinventree -Dtest=InvenTreePartE2ETest test
```

Generate the Allure report:

```bash
mvn allure:report
```

Open the generated report:

```text
target/site/allure-maven-plugin/index.html
```

## Implementation Location

The automation plan is stored at the project root beside `DBUtil_Readme.md`:

```text
API_Parts_Automation_Plan.md
```
