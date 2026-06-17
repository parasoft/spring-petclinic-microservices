# spring-petclinic-testcommon

Shared test utilities for the Spring Petclinic microservices project. This module integrates functional test execution with Parasoft CTP (Continuous Testing Platform) and DTP to enable code coverage collection, test result reporting, and Test Impact Analysis for the application under test.

It is consumed by the following test modules:
- `spring-petclinic-selenium-tests` — Selenium + JUnit 5
- `spring-petclinic-selenium-cucumber-tests` — Selenium + Cucumber + JUnit Platform
- `spring-petclinic-selenium-testng-tests` — Selenium + TestNG
- `spring-petclinic-playwright-tests` — Playwright + JUnit 5

## Package Structure

All source lives under `src/test/java/org/springframework/samples/petclinic/testcommon/`. The module is packaged as a `test-jar` (via `maven-jar-plugin`'s `test-jar` goal) so consumer modules can depend on these test-scoped classes.

### Root package (`testcommon`)

| Class | Purpose |
|---|---|
| `ParasoftSettings` | Centralized configuration. Resolves settings from system properties (`-D`), a `parasoft-settings.properties` file, or hardcoded defaults — in that precedence order. Controls CTP connection, multi-user mode, parallel execution, coverage/baseline publishing, proxy, Selenium Grid, and headless settings. |
| `ParasoftCTPApiClient` | REST API client for Parasoft CTP. Provides methods for session start/stop, test start/stop, coverage publishing to DTP, and baseline publishing for Test Impact Analysis. |
| `ParasoftSessionManager` | Manages the single CTP test session for the entire test run. Started once by the SuiteListener at suite start (with `userId` in multi-user mode). Holds the per-test `baggage` header value sourced from the `/test/start` API response in a single `AtomicReference` per test context that is shared by the Selenium proxy (read on its hot path), Playwright `@BeforeEach` (read via `getBaggage()`), and the watcher's `updateBaggage()` / `resetBaggage()` writes. Records per-test parallel IDs (WebDriver session IDs / Playwright UUIDs) when `CTP_PARALLEL_TEST_EXECUTION=true`. |
| `ParasoftHeaderInjectingProxy` | LittleProxy-based HTTP proxy that injects a `baggage` header (e.g. `test-operator-id=<userId>+<parallelId>`) into all proxied requests. The header value is read on every proxied request from an `AtomicReference` passed in at construction (obtained from `ParasoftSessionManager.obtainProxyBaggageRef(testContextKey)`), so updates from the watcher are visible without any further wiring. Required for code coverage attribution when coverage agents are in multi-user mode. Uses dynamic port assignment. |
| `ParasoftWatcherUtil` | Shared per-test start/stop logic invoked by all four watcher classes (`ParasoftWatcher`, `ParasoftWatcherPlaywright`, `ParasoftWatcherCucumber`, `ParasoftWatcherTestNG`). Centralizes the single-user / multi-user / parallel branching so the framework-specific watchers stay thin adapters that only translate framework events into `testId`, `testContextKey`, `passed`, and `failureMessage`. |

### `junit5` subpackage

For JUnit 5 (Jupiter) test modules, like `spring-petclinic-selenium-tests` and `spring-petclinic-playwright-tests`.

| Class | Purpose |
|---|---|
| `ParasoftSuiteListener` | Implements `TestExecutionListener`. On test plan start: starts the single CTP session for the entire run. On test plan finish: stops the session and optionally publishes coverage and/or baseline data. |
| `ParasoftWatcher` | Thin JUnit 5 adapter that translates `BeforeEachCallback` / `TestWatcher` events into calls on `ParasoftWatcherUtil` for per-test CTP start/stop. Used by Selenium JUnit 5 tests. |

### `junit5.cucumber` subpackage

For the Cucumber test module (`spring-petclinic-selenium-cucumber-tests`).

| Class | Purpose |
|---|---|
| `ParasoftSuiteListenerCucumber` | Uses Cucumber `@BeforeAll`/`@AfterAll` hooks (instead of JUnit `TestExecutionListener`) to start/stop the CTP session and publish coverage/baseline data. |
| `ParasoftWatcherCucumber` | Thin Cucumber adapter that translates `@Before`/`@After` scenario hooks into calls on `ParasoftWatcherUtil` for per-scenario CTP start/stop. |
| `ParasoftCucumberUtil` | Utility to derive a CTP test ID from a Cucumber `Scenario` in the format `featurefile.feature#Scenario Name`. |

### `junit5.playwright` subpackage

For the Playwright test module (`spring-petclinic-playwright-tests`).

| Class | Purpose |
|---|---|
| `ParasoftWatcherPlaywright` | Thin JUnit 5 adapter (`BeforeEachCallback` + `TestWatcher`) that translates events into calls on `ParasoftWatcherUtil`, with Playwright-specific failure-message normalization (strips stack traces and `at` lines) before delegating. |

### `testng` subpackage

For the TestNG test module (`spring-petclinic-selenium-testng-tests`).

| Class | Purpose |
|---|---|
| `ParasoftSuiteListenerTestNG` | Implements TestNG `ISuiteListener`. Starts/stops the CTP session at suite boundaries and publishes coverage/baseline data. |
| `ParasoftWatcherTestNG` | Thin TestNG adapter that translates `ITestListener` events into calls on `ParasoftWatcherUtil` for per-test CTP start/stop. |

### `selenium` subpackage

Shared Selenium WebDriver infrastructure used by all Selenium-based test modules.

| Class | Purpose |
|---|---|
| `BrowserType` | Enum: `CHROME`, `FIREFOX`, `EDGE`. |
| `WebDriverConfigurator` | Strategy interface — `void configure(MutableCapabilities options)`. |
| `BasicWebDriverConfigurator` | Configurator for basic browser options: window size/position, plus Chrome startup-quieting flags (background networking, component updater, sync, translate, optimization hints, etc.) that mirror Playwright's bundled-Chromium defaults to suppress Chrome's background startup network traffic. |
| `ParasoftWebDriverConfigurator` | Configurator that starts the `ParasoftHeaderInjectingProxy` (if multi-user mode) and configures the browser's proxy settings to route through it. Also applies headless mode. |
| `WebDriverFactory` | Factory method `create(BrowserType, WebDriverConfigurator...)` that builds a `MutableCapabilities`, applies all configurators, and creates either a local or `RemoteWebDriver` (Selenium Grid). Returns a `ParasoftWebDriverResource`. |
| `ParasoftWebDriverResource` | `AutoCloseable` wrapper around `WebDriver` + proxy. The proxy already holds the per-test-context baggage `AtomicReference` vended by `ParasoftSessionManager.obtainProxyBaggageRef()` (wired in `ParasoftWebDriverConfigurator`), so this constructor only registers the WebDriver session ID as the parallel ID for the test context when parallel mode is enabled. On `close()`: quits the driver, stops the proxy, and unregisters from `ParasoftSessionManager`. |

## How Test Modules Consume testcommon

### 1. POM Dependency

Since all classes in testcommon live in `src/test/java`, the module produces a **test-jar**. Consumer modules must use `<type>test-jar</type>`:

```xml
<dependency>
    <groupId>org.springframework.samples.petclinic.testcommon</groupId>
    <artifactId>spring-petclinic-testcommon</artifactId>
    <version>1.0.0</version>
    <type>test-jar</type>
    <scope>test</scope>
</dependency>
```

Consumer modules must also declare their own direct dependencies for Selenium, Playwright, Cucumber, TestNG, etc., since `test`-scoped transitive dependencies are not inherited.

### 2. Building and Installing the Test-Jar

Consumer modules pick up changes to testcommon by resolving the `spring-petclinic-testcommon` test-jar from your local Maven repository (`~/.m2/repository`). After **any** change to testcommon source, you must rebuild and reinstall the test-jar before consumer modules will see the change.

**Build and install in one step (most common):**

```
mvn -ntp -pl spring-petclinic-testcommon install -DskipTests
```

This compiles the test classes, packages them into both the regular jar and the test-jar, and installs both into your local repo so the four consumer modules can resolve the new version.

**Build the consumer module along with testcommon in one reactor invocation** (no separate install step needed; uses Maven's `--also-make` flag):

```
mvn -ntp -pl spring-petclinic-selenium-tests -am verify
```

With `-am`, Maven rebuilds testcommon as part of the same reactor before the consumer module is tested, so changes are picked up automatically. Replace `spring-petclinic-selenium-tests` with whichever consumer module you want to run.

**When the testcommon dependency graph changes** (e.g. bumping a Selenium/Playwright/JUnit version), make sure the new dependency is declared at default (`compile`) scope rather than `<scope>test</scope>`. Maven does **not** propagate `test`-scope transitive dependencies even through a test-jar dependency, so a `<scope>test</scope>` library will compile cleanly inside testcommon but produce a `NoClassDefFoundError` at consumer test runtime. Compare with `littleproxy`, `netty-all`, `selenium-java`, and `jackson-databind` in `pom.xml` — all are at default scope so they propagate.

**Quick compile check without installing** (validates the source compiles against the existing dependency graph; does not refresh consumer modules' classpath):

```
mvn -ntp -pl spring-petclinic-testcommon -am test-compile
```

### 3. Connecting the SuiteListener

The SuiteListener manages the CTP session lifecycle at the suite level (start/stop session, publish coverage, publish baseline). Each framework registers it differently:

**JUnit 5 (Selenium and Playwright):** Register via the Java ServiceLoader mechanism. Create the file:

```
src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener
```

With the content:

```
org.springframework.samples.petclinic.testcommon.junit5.ParasoftSuiteListener
```

**Cucumber:** The `ParasoftSuiteListenerCucumber` uses Cucumber `@BeforeAll`/`@AfterAll` hooks. It is automatically picked up when the Cucumber glue path includes the testcommon Cucumber package. In your `@Suite` runner class, include the testcommon glue path:

```java
@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "your.steps.package,org.springframework.samples.petclinic.testcommon.junit5.cucumber"
)
```

**TestNG:** Register the listener in `testng.xml`:

```xml
<suite name="Your Suite">
    <listeners>
        <listener class-name="org.springframework.samples.petclinic.testcommon.testng.ParasoftSuiteListenerTestNG"/>
    </listeners>
    <!-- ... -->
</suite>
```

### 4. Connecting the Watcher

The Watcher reports individual test start/stop events to CTP. Each framework connects it differently:

**JUnit 5 Selenium:** Use `@ExtendWith` on each test class:

```java
@ExtendWith(org.springframework.samples.petclinic.testcommon.junit5.ParasoftWatcher.class)
public class NavigateIT {
    // ...
}
```

**JUnit 5 Playwright:** Use the Playwright-specific watcher:

```java
@ExtendWith(org.springframework.samples.petclinic.testcommon.junit5.playwright.ParasoftWatcherPlaywright.class)
public class NavigateIT {
    // ...
}
```

**Cucumber:** The `ParasoftWatcherCucumber` uses Cucumber `@Before`/`@After` hooks and is automatically discovered when the glue path includes `org.springframework.samples.petclinic.testcommon.junit5.cucumber` (same glue config as the SuiteListener).

**TestNG:** Use `@Listeners` on each test class:

```java
@Listeners(org.springframework.samples.petclinic.testcommon.testng.ParasoftWatcherTestNG.class)
public class NavigateIT {
    // ...
}
```

### 5. Using WebDriverFactory (Selenium Modules Only)

Selenium-based test modules use `WebDriverFactory` to create a `ParasoftWebDriverResource` that bundles the per-class `WebDriver`, the header-injecting proxy, and registration with `ParasoftSessionManager` (parallel ID and proxy baggage `AtomicReference`). The single CTP session for the whole run is managed separately by `ParasoftSuiteListener`:

```java
ParasoftWebDriverResource driverResource = WebDriverFactory.create(
        BrowserType.CHROME,
        new BasicWebDriverConfigurator("960,1080", "0,0"),
        new ParasoftWebDriverConfigurator(MyTestClass.class.getName()));
WebDriver driver = driverResource.getDriver();
```

- `BasicWebDriverConfigurator` — sets window size/position (optional, has defaults).
- `ParasoftWebDriverConfigurator` — takes a `testContextKey` (typically the test class name or Cucumber scenario ID) used to associate the WebDriver session and its proxy's baggage `AtomicReference` with `ParasoftSessionManager`. It starts the `ParasoftHeaderInjectingProxy` when multi-user mode is enabled.

Clean up the resource in `@AfterAll` (JUnit 5), `@AfterClass` (TestNG), or `@After` (Cucumber):

```java
driverResource.close(); // quits driver, stops proxy, unregisters from ParasoftSessionManager
```

Playwright tests do **not** use `WebDriverFactory`. Instead, they inject the `baggage` header directly using Playwright's `context.setExtraHTTPHeaders()` API.

## Configuration

Settings are resolved in this order: **system property** → **`parasoft-settings.properties` file** → **hardcoded default**.

The properties file is loaded from the classpath (`src/test/resources/parasoft-settings.properties`) or from a path specified by `-DPARASOFT_SETTINGS_FILE=<path>`.

### Available Settings

| Property | Default | Description |
|---|---|---|
| `CTP_BASE_URL` | `http://localhost:8080` | CTP server URL |
| `CTP_CONTEXT_PATH` | `/em` | Optional: Specify custom CTP context path if non-standard |
| `CTP_ENV_ID` | `1` | CTP environment ID |
| `CTP_USERNAME` | `admin` | CTP authentication username |
| `CTP_PASSWORD` | `admin` | CTP authentication password |
| `CTP_LOG_LEVEL` | `WARN` | Logging verbosity: ERROR, WARN, INFO, DEBUG, TRACE |
| `CTP_MULTI_USER_MODE` | `true` | Whether coverage agents are in multi-user mode |
| `CTP_PARALLEL_TEST_EXECUTION` | `false` | Enable parallel test execution. Requires `CTP_MULTI_USER_MODE=true` |
| `CTP_PUBLISH_COVERAGE` | `false` | Publish coverage data to DTP at suite end |
| `CTP_PUBLISH_BASELINE` | `false` | Publish baseline for Test Impact Analysis at suite end |
| `CTP_BASELINE_BUILD_ID` | `spring-petclinic-baseline` | Baseline build identifier |
| `PROXY_HOST` | `localhost` | Hostname for the header-injecting proxy |
| `PROXY_BIND_HOST` | `0.0.0.0` | Bind address for the proxy (override for containerized Grid on Linux host) |
| `HEADLESS` | `false` | Run browser in headless mode |
| `SELENIUM_GRID` | `false` | Use Selenium Grid (`RemoteWebDriver`) |
| `SELENIUM_GRID_URL` | `http://localhost:4444/wd/hub` | Selenium Grid hub URL |
| `TESTFRAMEWORK` | `defaultTestFramework` | Identifier composed into the run-level `userId` (`<TESTFRAMEWORK>-<CTP_USERNAME>`) and DTP session tag. Acts as the primary uniqueness discriminator between concurrent test runs (e.g. this repo example demonstrates `seleniumJUnit`, `seleniumTestNG`, `cucumber`, `playwright`) so coverage from different frameworks publishing to the same CTP/DTP environment is attributable. Use this property to uniquely identify each test execution process when running distributed test execution using multiple CI nodes (e.g., `n` CI jobs for distributed test execution) |
