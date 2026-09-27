# CLAUDE.md — Automation Test Suite Ruleset

Rules for generating and extending this repo's test automation. This is a Java/Maven/TestNG
project with two test layers over the InvenTree application: **REST Assured** for API tests and
**Playwright (Java bindings)** for UI tests. Follow the conventions below rather than inventing new
ones — every rule here reflects a pattern already used somewhere in `src/test/java`.

## Stack

- Java 15, Maven, TestNG 7.9 (not JUnit)
- REST Assured 5.4 + `json-schema-validator` + Hamcrest for API tests
- Playwright (Java) 1.47 for UI tests — **not** Playwright Test/JS. No `@playwright/test`, no `.spec.ts`.
- Allure (`allure-testng`) for reporting; attach request/response/step evidence, don't just assert silently
- Jackson (`jackson-databind`) for POJOs

## Module layout (put new files here, not elsewhere)

```
src/test/java/
  client/          RestClient — thin static HTTP wrapper, all methods go through it
  specifications/  RequestSpecBuilderUtil — builds RequestSpecification objects
  constant/        ApplicationConstant — config KEY names and defaults only
  files/           ConfigManager — property/env loading
  pojo/            Request/response POJOs, one per API resource
  inventree/       InvenTree API support + E2E test classes (one per resource)
  ui/              InvenTree UI test classes (one per feature/flow)
  ui/pages/        Playwright Page Object classes
  utils/           Misc helpers (DB, JSON, retry, auth)
  main/            Pre-existing ReqRes/SWAPI/H2 practice exercises — unrelated to InvenTree,
                   out of scope for new work; don't add new InvenTree code here
src/test/resources/
  config.properties       default/local config (see Config & secrets below)
  schema/*.json           JSON Schema files, one per API resource shape (list + detail)
```

## Config & secrets

- All config goes through `files.ConfigManager` — never read `System.getenv`/hardcode a URL or
  token directly in a test. Add new keys to `constant.ApplicationConstant` as `..._KEY` constants
  (and a `..._DEFAULT` if there's a sane default), mirroring the existing `INVENTREE_*` constants.
- `ConfigManager.getProperty` resolves dotted keys (`inventree.base.url`) and their uppercase
  env-var form (`INVENTREE_BASE_URL`) automatically, with env vars taking priority. Don't add a
  second config-loading mechanism.
- New suites that hit a real/external instance must guard themselves like
  `InvenTreeApiSupport.requireConfigured()` / `UiTestSupport.requireConfigured()`: throw
  `org.testng.SkipException` in `@BeforeClass` when required config is blank, so `mvn test` never
  fails just because a local instance isn't running.
- Do not commit real credentials or instance-specific IDs for a new external target. Prefer values
  overridable by environment variable, and treat `REPLACE_WITH_...`-prefixed values as "unset"
  (see `InvenTreeApiSupport.isBlank`).

## API test conventions (REST Assured)

- All HTTP calls go through `client.RestClient` static methods (`get`/`post`/`put`/`patch`/`delete`),
  which already attach the request/body/response/status to Allure. Don't call `RestAssured.given()`
  directly from a test. (`utils/AuthTest.java` predates this rule and still calls `given()` directly —
  known legacy exception, not a pattern to copy; migrate it to `RestClient` rather than extend it.)
- Build request specs via `specifications.RequestSpecBuilderUtil`, or a resource-specific
  `<Resource>ApiSupport.authenticatedSpec()` (see `inventree.InvenTreeApiSupport`) that wraps it.
  One `...ApiSupport` class per external system, holding: `BASE_URL`, auth token/spec, endpoint path
  constants, `requireConfigured()`, and a `unique(prefix)` test-data-naming helper.
- One POJO per resource under `pojo/`, annotated `@JsonInclude(JsonInclude.Include.NON_NULL)` +
  `@JsonIgnoreProperties(ignoreUnknown = true)`, so the same class serves full create, partial
  PATCH, and full PUT bodies. Use `@JsonProperty` for fields whose API name isn't camelCase
  (`IPN`, `default_location`, ...). For proving a real explicit JSON `null` is sent (nullable-field
  tests), use a raw `Map<String,Object>` instead of the POJO — `NON_NULL` will drop an explicit null.
- Validate response shape with `matchesJsonSchemaInClasspath("schema/<resource>-detail.json")` /
  `-list.json`. Add a new schema file under `src/test/resources/schema/` for any new resource
  before writing CRUD tests against it — schema validation is the **entry gate**: a `@BeforeClass`
  method validates the endpoint/schema is available before any CRUD scenario runs (see
  `InvenTreePartE2ETest.validateSchemaEntryPoint`).
- Structure resource coverage as **one E2E test class per resource**
  (`InvenTree<Resource>E2ETest`). Two shapes, depending on whether the resource has CRUD state to
  chain:
  - **Stateful CRUD resource** (Part, Category, SupplierPart) — single-flow shape:
    - Numbered scenario IDs in Javadoc/comments and private method names, e.g.
      `API-PART-001` ↔ `apiPart001CreateMinimalPart()`.
    - One `@Test` method (`executeAll...AsSingleE2EFlow`) that calls the numbered private methods in
      order, so create/retrieve/update/delete share state (created IDs, prior response values) as one
      flow, plus separate `@Test(dependsOnMethods = ...)` methods for boundary/negative cases via
      `@DataProvider`.
  - **Stateless checks with no CRUD state to share** (list/search/pagination, unauthorised-access
    security checks) — plain independent `@Test` methods instead, one per scenario, no shared-flow
    method (see `InvenTreePartListQueryTest`, `InvenTreeSecurityTest`). Don't force these into the
    single-flow shape just for consistency — there's no state for them to chain.
  - Unique test-data names/IPNs via the support class's `unique(prefix)` — never a fixed literal
    name, to avoid collisions on shared instances.
  - Track every created record's ID in a field-owned `List<Integer>`, and clean up in
    `@AfterClass(alwaysRun = true)`: reverse any state blocking deletion (unlock, deactivate) then
    delete, logging (not swallowing) failures via `Allure.step(...)`.
  - Assert: HTTP status, content-type, schema conformance, persistence of submitted writable
    fields, and non-corruption of read-only/server-managed fields. Only assert exact error text
    when it's stable across the supported API version — prefer status code + key presence checks.
- Keep call chains for a single request/assert on `response.then().statusCode(...).body(...)`
  (REST Assured + Hamcrest), and use TestNG `assertEquals`/`assertTrue`/`assertNotNull` for anything
  computed outside that chain.

## UI test conventions (Playwright Java)

- Page Object Model under `ui/pages/`. Each page/modal gets its own class holding a `Page` field,
  constructed against an already-open state (constructors that need to confirm the page loaded call
  `.waitFor()` in the constructor, e.g. `PartDetailPage`).
- Prefer InvenTree's stable `aria-label` attributes over CSS classes or generated Mantine IDs:
  - Form fields: `[aria-label='<type>-field-<name>']` (`text-field-name`, `number-field-...`,
    `boolean-field-...`).
  - Action buttons/menus: `[aria-label='action-button-<name>']`, `[aria-label='action-menu-<name>']`.
  - Only fall back to `:text-is("...")` / `:has-text("...")` text locators when no stable attribute
    exists, and scope them to the narrowest containing locator (e.g. a specific `[aria-label=...]`
    tablist) to avoid matching an unrelated element with the same text elsewhere on the page.
  - **`tag:text-is("...")` (e.g. `button:text-is("Submit")`) matches zero elements in this
    Playwright version, even on an exact text match** — confirmed by a standalone probe against the
    running app. Use the bare `:text-is("...")` form (no tag prefix) instead; it works. Where you do
    need a tag anchor (e.g. to land on the actual interactive `<button>` rather than an inner label
    `<div>` — `:text-is()` resolves to the *smallest* matching element, which can be a wrapper, not
    the button that carries attributes like `data-disabled`), use `tag:has-text("...")` instead, which
    does combine correctly with a tag.
- Submitting some forms navigates away from the current page rather than back to it — e.g. "Add
  Stock Item" lands on the new Stock Item's own page, not the Part page it was opened from (verified
  live). After such a submit, explicitly re-navigate before asserting on the original page's state;
  don't assume the submit leaves you where you started.
- **A pure `non_field_errors` server validation error (e.g. "name, IPN, revision must make a unique
  set") renders only its own message as the form banner — `FormModal`'s generic "Errors exist for
  one or more form fields" text does NOT appear in that case**, unlike a per-field error (blank
  Name, invalid Link), which shows both. Confirmed live: an automated assertion on
  `hasFormError()` failed for a non-field-error scenario even though the specific message was
  present. For a non-field error, assert on `hasText(<specific message>)` alone; don't also assert
  `hasFormError()`.
- Reuse the generic `FormModal` driver for any Mantine create/edit dialog instead of writing a new
  bespoke Page Object per modal — it already handles field-fill, submit-and-wait-for-outcome,
  form-error detection, and nested/inline-create modals via the dialog stack (`last()`).
- Where a UI action's outcome lags the request completing (async refetch), poll for the expected
  state with a bounded timeout (see `PartDetailPage.waitForStatusBadgeToContain`,
  `FormModal.submit`) instead of a fixed `sleep`.
- Test classes live in `ui/`, one per feature/flow, following existing naming
  (`InvenTree<Feature>UITest`) with scenario IDs `UI-<FEATURE>-NNN` in the class Javadoc.
- Use `UiTestSupport` for all shared plumbing: `requireConfigured()` guard, `newPlaywright()` /
  `newBrowser()` (headless unless `-Dui.headless=false`) / `newPage()` (1440×1000 viewport), and
  `unique(prefix)`. Always close `Browser` and `Playwright` in `@AfterClass(alwaysRun = true)`.
- **Set up and tear down test fixtures via the API layer** (`RestClient` +
  `InvenTreeApiSupport`/other `...ApiSupport`), not by driving the UI — it's faster and more
  reliable, and keeps UI tests focused on the UI behavior being verified. Only drive the UI for the
  behavior actually under test.
- Assert UI outcomes both in the UI (visible state) and, where meaningful, by cross-checking via the
  API (e.g. a Part created through a modal must also be retrievable via `GET /api/part/`).

## Adding a new suite — checklist

1. New external resource/API? Add a JSON schema file under `resources/schema/`, a POJO under
   `pojo/`, endpoint path constant(s) in the resource's `...ApiSupport` class (or a new one if it's
   a new system), and config keys in `ApplicationConstant` + `config.properties`.
2. New API test class → one E2E class per resource in `inventree/` (or a new package for a new
   system), using whichever of the two structures above fits (single-flow + numbered-scenario for a
   stateful CRUD resource, independent `@Test` methods for stateless checks).
3. New UI test class → a `ui/<Feature>UITest.java` plus any new `ui/pages/*.java` Page Objects it
   needs; reuse `FormModal`/`UiTestSupport` rather than duplicating their logic.
4. Register the new test class in the relevant Maven Surefire profile's `<includes>` in `pom.xml`
   (`inventree`, `ui`, and `all`), matching the existing `**/ClassName.java` pattern.
5. Guard any suite that needs a live external instance with a `requireConfigured()` /
   `SkipException` pattern so `mvn test` stays green with no environment configured.

## Execution

```bash
mvn -Pinventree test   # API E2E suites
mvn -Pui test          # Playwright UI suites
mvn -Pall test         # everything
mvn allure:report && open target/site/allure-maven-plugin/index.html
```

Local Playwright debugging with a visible browser: `mvn -Pui -Dui.headless=false test`.
