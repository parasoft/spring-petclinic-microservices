# spring-petclinic-playwright-tests

Playwright + JUnit 5 functional tests for the Spring Petclinic application, with Parasoft CTP integration for code coverage collection, test result reporting, and Test Impact Analysis.

By default this module runs tests sequentially. It also supports parallel test execution — see [Parallel Test Execution](#parallel-test-execution) below.

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

### Parallel Test Execution

To run tests in parallel, two changes are required:

1. **Enable JUnit 5 parallel execution** — pass the following system properties on the command line:

```
-Djunit.jupiter.execution.parallel.enabled=true -Djunit.jupiter.execution.parallel.mode.default=same_thread -Djunit.jupiter.execution.parallel.mode.classes.default=concurrent
```

This runs test classes concurrently (each gets its own Playwright Browser and CTP session), while tests within a class run sequentially (they share a Browser instance).

2. **Set `CTP_PARALLEL_TEST_EXECUTION=true`** — either in `parasoft-settings.properties` or on the command line:

```
mvn -ntp verify -pl spring-petclinic-playwright-tests -am -DCTP_PARALLEL_TEST_EXECUTION=true -Djunit.jupiter.execution.parallel.enabled=true -Djunit.jupiter.execution.parallel.mode.default=same_thread -Djunit.jupiter.execution.parallel.mode.classes.default=concurrent -DPETCLINIC_URL=<PETCLINIC URL>
```

When parallel execution is enabled, `CTP_MULTI_USER_MODE` must also be `true` (the default). Each test class gets its own Playwright Browser instance and CTP session, so coverage is tracked independently per class.

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

For sequential execution, this starts/stops the CTP session at the test plan level. For parallel execution, each test class manages its own CTP session in `@BeforeAll`/`@AfterAll`. In both modes, the listener publishes coverage and baseline data at suite end.

### Watcher

Each test class uses `@ExtendWith` to register the Playwright-specific per-test watcher:

```java
@ExtendWith(org.springframework.samples.petclinic.testcommon.junit5.playwright.ParasoftWatcherPlaywright.class)
public class NavigateIT { ... }
```

This reports individual test start/stop events (with PASS/FAIL results) to CTP.

### Header Injection and Session Management (Instead of WebDriverFactory)

Playwright tests do **not** use `WebDriverFactory` or the `ParasoftHeaderInjectingProxy`. Instead, they inject the `baggage` header directly using Playwright's `BrowserContext.setExtraHTTPHeaders()` API and manage CTP sessions in the test class lifecycle methods:

```java
@BeforeAll
static void launchBrowser() {
    playwrightSessionId = UUID.randomUUID().toString();
    playwright = Playwright.create();
    browser = playwright.chromium().launch(...);
    if (ParasoftSettings.isParallelTestExecution()) {
        ParasoftSessionManager.startSession(NavigateIT.class.getName(), playwrightSessionId, new AtomicReference<String>(""));
    }
}

@BeforeEach
void createContextAndPage() {
    context = browser.newContext();
    if (ParasoftSettings.isMultiUserMode()) {
        Map<String, String> headers = new HashMap<>();
        headers.put("baggage", "test-operator-id=" + ParasoftSessionManager.getCoverageUserId(NavigateIT.class.getName()));
        context.setExtraHTTPHeaders(headers);
    }
    page = context.newPage();
}

@AfterAll
static void closeBrowser() {
    browser.close();
    playwright.close();
    if (ParasoftSettings.isParallelTestExecution()) {
        ParasoftSessionManager.stopSession(NavigateIT.class.getName());
    }
}
```

This achieves the same coverage attribution as the proxy-based approach used by the Selenium modules, but uses Playwright's native API instead of LittleProxy. The `@BeforeAll`/`@AfterAll` session management is analogous to the `ParasoftWebDriverResource` lifecycle in the Selenium module.

## Configuring Settings

Settings are resolved in order: **system property** (`-D`) → **`parasoft-settings.properties`** → **hardcoded default**.

Edit [`src/test/resources/parasoft-settings.properties`](src/test/resources/parasoft-settings.properties) to configure your CTP environment. Any setting can be overridden on the Maven command line, e.g. `-DCTP_BASE_URL=http://ctp-server:8080`.

For the full list of available settings and their defaults, see the [testcommon README](../spring-petclinic-testcommon/README.md#available-settings) and [ParasoftSettings.java](../spring-petclinic-testcommon/src/test/java/org/springframework/samples/petclinic/testcommon/ParasoftSettings.java).
