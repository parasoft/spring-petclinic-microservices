# spring-petclinic-selenium-cucumber-tests

Selenium + Cucumber + JUnit Platform functional tests for the Spring Petclinic application, with Parasoft CTP integration for code coverage collection, test result reporting, and Test Impact Analysis.

This module only supports sequential test execution. Parallel test execution is not yet implemented for this module, so `CTP_PARALLEL_TEST_EXECUTION` must remain `false`.

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
mvn -ntp verify -pl spring-petclinic-selenium-cucumber-tests -am -DPETCLINIC_URL=<PETCLINIC URL>
```

`PETCLINIC_URL` defaults to `http://localhost:8099` if not provided.

**With Selenium Grid:**

```
mvn -ntp verify -pl spring-petclinic-selenium-cucumber-tests -am -DSELENIUM_GRID=true -DPROXY_HOST=<PROXY HOST> -DPETCLINIC_URL=<PETCLINIC URL>
```

**Headless mode:** Add `-DHEADLESS=true` to run without a visible browser window.

**Non-default CTP credentials:** Add `-DCTP_USERNAME=<USERNAME> -DCTP_PASSWORD=<PASSWORD>`.

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

### SuiteListener and Watcher

Both the `ParasoftSuiteListenerCucumber` and `ParasoftWatcherCucumber` are Cucumber hook classes that are automatically discovered via the Cucumber glue path. The `@Suite` runner class includes the testcommon Cucumber package in its glue configuration:

```java
@Suite
@SelectClasspathResource("features/petclinic.feature")
@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "org.springframework.samples.petclinic.cucumber,org.springframework.samples.petclinic.testcommon.junit5.cucumber")
public class RunCucumberIT { }
```

This causes Cucumber to discover:
- `ParasoftSuiteListenerCucumber` — uses `@BeforeAll`/`@AfterAll` hooks to start/stop the CTP session and publish coverage and baseline data at suite end.
- `ParasoftWatcherCucumber` — uses `@Before`/`@After` hooks to report individual scenario start/stop events (with PASS/FAIL results) to CTP.

### WebDriverFactory

Step definition classes create a `ParasoftWebDriverResource` and close it per scenario:

```java
@Given("the browser is open")
public void the_browser_is_open() {
    driverResource = WebDriverFactory.create(
            BrowserType.CHROME,
            new BasicWebDriverConfigurator(),
            new ParasoftWebDriverConfigurator(ParasoftCucumberUtil.getTestId(scenario)));
    driver = driverResource.getDriver();
}

@After
public void cleanup() {
    if (driverResource != null) {
        driverResource.close();
    }
}
```

The `ParasoftWebDriverConfigurator` takes the Cucumber test ID (derived from `ParasoftCucumberUtil.getTestId(scenario)`) as the `testContextKey`, which associates the WebDriver session and its proxy's baggage `AtomicReference` with `ParasoftSessionManager`. It also starts the header-injecting proxy when multi-user mode is enabled.

## Configuring Settings

Settings are resolved in order: **system property** (`-D`) → **`parasoft-settings.properties`** → **hardcoded default**.

Edit [`src/test/resources/parasoft-settings.properties`](src/test/resources/parasoft-settings.properties) to configure your CTP environment. Any setting can be overridden on the Maven command line, e.g. `-DCTP_BASE_URL=http://ctp-server:8080`.

For the full list of available settings and their defaults, see the [testcommon README](../spring-petclinic-testcommon/README.md#available-settings) and [ParasoftSettings.java](../spring-petclinic-testcommon/src/test/java/org/springframework/samples/petclinic/testcommon/ParasoftSettings.java).
