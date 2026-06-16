# spring-petclinic-selenium-tests

Selenium + JUnit 5 functional tests for the Spring Petclinic application, with Parasoft CTP integration for code coverage collection, test result reporting, and Test Impact Analysis.

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
mvn -ntp verify -pl spring-petclinic-selenium-tests -am -DPETCLINIC_URL=<PETCLINIC URL>
```

`PETCLINIC_URL` defaults to `http://localhost:8099` if not provided.

**With Selenium Grid:**

```
mvn -ntp verify -pl spring-petclinic-selenium-tests -am -DSELENIUM_GRID=true -DPROXY_HOST=<PROXY HOST> -DPETCLINIC_URL=<PETCLINIC URL>
```

**Headless mode:** Add `-DHEADLESS=true` to run without a visible browser window.

**Non-default CTP credentials:** Add `-DCTP_USERNAME=<USERNAME> -DCTP_PASSWORD=<PASSWORD>`.

### Parallel Test Execution

To run tests in parallel, pass JUnit 5 parallel execution properties and set `CTP_PARALLEL_TEST_EXECUTION=true`:

```
mvn -ntp verify -pl spring-petclinic-selenium-tests -am \
    -Djunit.jupiter.execution.parallel.enabled=true \
    -Djunit.jupiter.execution.parallel.mode.classes.default=concurrent \
    -DCTP_PARALLEL_TEST_EXECUTION=true \
    -DPETCLINIC_URL=<PETCLINIC URL>
```

When parallel execution is enabled, `CTP_MULTI_USER_MODE` must also be `true` (the default) — running parallel tests in single-user mode is an invalid configuration because concurrent tests cannot be distinguished by the coverage agents when they are in single-user mode. All concurrent test classes share a single CTP session and are distinguished on the server by a per-class `parallelId` (the WebDriver session ID).

**Parallelism scope:** This project supports **class-level parallelism only** — test classes run concurrently, but methods within a class run sequentially and share the class's WebDriver/proxy instance. Method-level parallelism is intentionally not supported because it would require per-method WebDriver and `ParasoftHeaderInjectingProxy` lifecycle management, which is uncommon in production SDET frameworks for UI tests. The flags shown above include `parallel.mode.classes.default=concurrent` (classes concurrent) but deliberately do **not** include `parallel.mode.default=concurrent` (which would also run methods concurrently). If you add the latter, multiple methods of the same class will share one `testContextKey` and race on baggage and `parallelId` registration — coverage attribution will be unreliable.

`CTP_PARALLEL_TEST_EXECUTION` can also be set in `parasoft-settings.properties` instead of the command line.

### Selenium Grid Notes

- If Selenium Grid is running in a container (e.g., Docker Desktop) and the JUnit test runner is on the host, the `host.docker.internal` convention may not work. Use your host's IP address to set `-DPROXY_HOST`. In some instances, like when Selenium Grid is running in a container on certain Linux hosts, the default value for `-DPROXY_BIND_HOST` (0.0.0.0) is insufficient, and you should override it with the same value used for `-DPROXY_HOST`.
- If Selenium Grid is not running in a container, you only need to provide `-DPROXY_HOST` to where Selenium Grid is located.
- If Selenium Grid is running in the cloud, extra considerations (e.g., VPC) may be necessary to ensure connectivity between the test runner + local proxy and the grid service.

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

Each test class uses `@ExtendWith` to register the per-test watcher:

```java
@ExtendWith(org.springframework.samples.petclinic.testcommon.junit5.ParasoftWatcher.class)
public class NavigateIT { ... }
```

This reports individual test start/stop events (with PASS/FAIL results) to CTP.

### WebDriverFactory

Test classes create a `ParasoftWebDriverResource` in `@BeforeAll` and close it in `@AfterAll`:

```java
@BeforeAll
static void openBrowser() {
    driverResource = WebDriverFactory.create(
            BrowserType.CHROME,
            new BasicWebDriverConfigurator("960,1080", "0,0"),
            new ParasoftWebDriverConfigurator(NavigateIT.class.getName()));
    driver = driverResource.getDriver();
}

@AfterAll
static void closeBrowser() {
    if (driverResource != null) {
        driverResource.close();
    }
}
```

The `ParasoftWebDriverConfigurator` takes the test class name as the `testContextKey`, which associates the WebDriver session and its proxy's baggage `AtomicReference` with `ParasoftSessionManager`. It also starts the header-injecting proxy when multi-user mode is enabled.

## Configuring Settings

Settings are resolved in order: **system property** (`-D`) → **`parasoft-settings.properties`** → **hardcoded default**.

Edit [`src/test/resources/parasoft-settings.properties`](src/test/resources/parasoft-settings.properties) to configure your CTP environment. Any setting can be overridden on the Maven command line, e.g. `-DCTP_BASE_URL=http://ctp-server:8080`.

For the full list of available settings and their defaults, see the [testcommon README](../spring-petclinic-testcommon/README.md#available-settings) and [ParasoftSettings.java](../spring-petclinic-testcommon/src/test/java/org/springframework/samples/petclinic/testcommon/ParasoftSettings.java).
