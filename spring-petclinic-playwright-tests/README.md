# spring-petclinic-playwright-tests

Playwright + JUnit 5 functional tests for the Spring Petclinic application, with Parasoft CTP integration for code coverage collection, test result reporting, and Test Impact Analysis.

This module currently only supports sequential test execution, so `CTP_PARALLEL_TEST_EXECUTION` must remain `false`.

## Prerequisites

- The Petclinic application must be running and accessible.
- Parasoft CTP must be running and configured with coverage agents deployed on the Petclinic services.
- The project must be built before running tests:

```
mvn -ntp clean install -DskipTests
```

If you need to publish static coverage to DTP with Jtest, use (ensure `jtest.settings` exists with correct paths):

```
mvn -ntp clean package jtest:monitor -DskipTests=true -Djtest.settings=jtest.settings -Djtest.showSettings=true -Dproperty.report.dtp.publish=true
```

## Running Tests

**Basic execution:**

```
mvn -ntp verify -pl spring-petclinic-playwright-tests -am -DPETCLINIC_URL=<PETCLINIC URL>
```

`PETCLINIC_URL` defaults to `http://localhost:8099` if not provided.

**Headless mode:** Add `-DHEADLESS=true` to run without a visible browser window.

**Non-default CTP credentials:** Add `-DCTP_USERNAME=<USERNAME> -DCTP_PASSWORD=<PASSWORD>`.

## How This Module Uses testcommon

This module depends on `spring-petclinic-testcommon` for all Parasoft CTP integration. See the [testcommon README](../spring-petclinic-testcommon/README.md) for full details.

### Dependency

The testcommon module is consumed as a `test-jar` in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.samples.petclinic.testcommon</groupId>
    <artifactId>spring-petclinic-testcommon</artifactId>
    <version>1.0.0</version>
    <type>test-jar</type>
    <scope>test</scope>
</dependency>
```

### SuiteListener

Registered via the JUnit Platform ServiceLoader file at:

```
src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener
```

Containing:

```
org.springframework.samples.petclinic.testcommon.junit5.ParasoftSuiteListener
```

This starts/stops the CTP session at the test plan level and publishes coverage and baseline data at suite end.

### Watcher

Each test class uses `@ExtendWith` to register the Playwright-specific per-test watcher:

```java
@ExtendWith(org.springframework.samples.petclinic.testcommon.junit5.playwright.ParasoftWatcherPlaywright.class)
public class NavigateIT { ... }
```

This reports individual test start/stop events (with PASS/FAIL results) to CTP.

### Header Injection (Instead of WebDriverFactory)

Playwright tests do **not** use `WebDriverFactory` or the `ParasoftHeaderInjectingProxy`. Instead, they inject the `baggage` header directly using Playwright's `BrowserContext.setExtraHTTPHeaders()` API:

```java
@BeforeEach
void createContextAndPage() {
    context = browser.newContext();
    if (ParasoftSettings.isMultiUserMode()) {
        Map<String, String> headers = new HashMap<>();
        headers.put("baggage", "test-operator-id=" + ParasoftSessionManager.getCoverageUserId());
        context.setExtraHTTPHeaders(headers);
    }
    page = context.newPage();
}
```

This achieves the same coverage attribution as the proxy-based approach used by the Selenium modules, but uses Playwright's native API for simplicity.

## Configuring Settings

Settings are resolved in order: **system property** (`-D`) → **`parasoft-settings.properties`** → **hardcoded default**.

Edit [`src/test/resources/parasoft-settings.properties`](src/test/resources/parasoft-settings.properties) to configure your CTP environment. Any setting can be overridden on the Maven command line, e.g. `-DCTP_BASE_URL=http://ctp-server:8080`.

For the full list of available settings and their defaults, see the [testcommon README](../spring-petclinic-testcommon/README.md#available-settings) and [ParasoftSettings.java](../spring-petclinic-testcommon/src/test/java/org/springframework/samples/petclinic/testcommon/ParasoftSettings.java).
