# spring-petclinic-selenium-testng-tests

Selenium + TestNG functional tests for the Spring Petclinic application, with Parasoft CTP integration for code coverage collection, test result reporting, and Test Impact Analysis.

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
mvn -ntp verify -pl spring-petclinic-selenium-testng-tests -am -DPETCLINIC_URL=<PETCLINIC URL>
```

`PETCLINIC_URL` defaults to `http://localhost:8099` if not provided.

**Headless mode:** Add `-Dorg.springframework.samples.petclinic.headless=true` to run without a visible browser window.

**Non-default CTP credentials:** Add `-Dparasoft.coverage.integration.ctp.auth.username=<USERNAME> -Dparasoft.coverage.integration.ctp.auth.password=<PASSWORD>`.

### Parallel Test Execution

To run tests in parallel, two changes are required:

1. **Enable parallel execution in `testng.xml`** — swap the `<suite>` element to use `parallel="tests"` and set the desired `thread-count`:

```xml
<!-- Sequential (default): -->
<suite name="PetClinic Selenium TestNG Suite">

<!-- Parallel: -->
<suite name="PetClinic Selenium TestNG Suite" parallel="tests" thread-count="2">
```

2. **Set `parasoft.coverage.integration.parallel.test.enabled=true`** — either in `coverage-integration.properties` or on the command line:

```
mvn -ntp verify -pl spring-petclinic-selenium-testng-tests -am -Dparasoft.coverage.integration.parallel.test.enabled=true -DPETCLINIC_URL=<PETCLINIC URL>
```

When parallel execution is enabled, the CTP coverage agents must be running in **multi-user mode** — running parallel tests against single-user agents is an invalid configuration because concurrent tests cannot be distinguished by the agents. All concurrent test classes share a single CTP session and are distinguished on the server by a per-class `parallelId` (managed by the `coverage-integration-selenium` module).

**Parallelism scope:** This project supports **class-level parallelism only** — test classes run concurrently, but methods within a class run sequentially and share the class's WebDriver instance. Method-level parallelism is intentionally not supported because it would require per-method WebDriver lifecycle management, which is uncommon in production SDET frameworks for UI tests. The `parallel="tests"` value above runs each `<test>` (and therefore each class via the implicit test grouping) on its own thread; do **not** change it to `parallel="methods"` — multiple methods of the same class would then share one `testContextKey` and race on `parallelId` registration, making coverage attribution unreliable.

## Coverage Integration

This module uses the `coverage-integration` library (`com.parasoft:coverage-integration-testng` and `com.parasoft:coverage-integration-selenium`) for Parasoft CTP session management, baggage header injection via `SeleniumCoverageIntegration.configureCdpBaggageHeader()`, and test lifecycle reporting.

The `coverage-integration` artifacts are vendored in `jtest/.m2/repository` and are resolved via the `jtest/.m2/settings.xml` file repository.

## Configuring Settings

Settings are resolved in order: **system property** (`-D`) → **`coverage-integration.properties`** → **hardcoded default**.

Edit [`src/test/resources/coverage-integration.properties`](src/test/resources/coverage-integration.properties) to configure your CTP environment. Any setting can be overridden on the Maven command line, e.g. `-Dparasoft.coverage.integration.ctp.url=http://ctp-server:8080/em`.
