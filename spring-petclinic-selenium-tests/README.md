# spring-petclinic-selenium-tests

Selenium + JUnit 5 functional tests for the Spring Petclinic application, with Parasoft CTP integration for code coverage collection, test result reporting, and Test Impact Analysis.

By default this module runs tests sequentially. It also supports parallel test execution — see [Parallel Test Execution](#parallel-test-execution) below.

## Prerequisites

- Complete the [local deployment setup](../README.md#local-deployment-with-coverage) in the root README before running tests (configures coverage agents, builds images, publishes static coverage to DTP, and starts the services).
- The project must be built before running tests:

```
mvn -ntp clean install -DskipTests
```

## Running Tests

**Basic execution:**

```
mvn -ntp verify -pl spring-petclinic-selenium-tests -am -DPETCLINIC_URL=<PETCLINIC URL>
```

`PETCLINIC_URL` defaults to `http://localhost:8099` if not provided.

**Headless mode:** Add `-Dorg.springframework.samples.petclinic.headless=true` to run without a visible browser window.

**Non-default CTP credentials:** Add `-Dparasoft.coverage.integration.ctp.auth.username=<USERNAME> -Dparasoft.coverage.integration.ctp.auth.password=<PASSWORD>`.

### Parallel Test Execution

To run tests in parallel, pass JUnit 5 parallel execution properties and set `parasoft.coverage.integration.parallel.test.enabled=true`:

```
mvn -ntp verify -pl spring-petclinic-selenium-tests -am \
    -Djunit.jupiter.execution.parallel.enabled=true \
    -Djunit.jupiter.execution.parallel.mode.classes.default=concurrent \
    -Dparasoft.coverage.integration.parallel.test.enabled=true \
    -DPETCLINIC_URL=<PETCLINIC URL>
```

When parallel execution is enabled, the CTP coverage agents must be running in **multi-user mode** — running parallel tests against single-user agents is an invalid configuration because concurrent tests cannot be distinguished by the agents. All concurrent test classes share a single CTP session and are distinguished on the server by a per-class `parallelId` (managed by the `coverage-integration-selenium` module).

**Parallelism scope:** This project supports **class-level parallelism only** — test classes run concurrently, but methods within a class run sequentially and share the class's WebDriver instance. Method-level parallelism is intentionally not supported because it would require per-method WebDriver lifecycle management, which is uncommon in production SDET frameworks for UI tests. The flags shown above include `parallel.mode.classes.default=concurrent` (classes concurrent) but deliberately do **not** include `parallel.mode.default=concurrent` (which would also run methods concurrently). If you add the latter, multiple methods of the same class will share one `testContextKey` and race on `parallelId` registration — coverage attribution will be unreliable.

`parasoft.coverage.integration.parallel.test.enabled` can also be set in `coverage-integration.properties` instead of the command line.

## Coverage Integration

This module uses the `coverage-integration` library (`com.parasoft:coverage-integration-junit5` and `com.parasoft:coverage-integration-selenium`) for Parasoft CTP session management, baggage header injection via `SeleniumCoverageIntegration.configureCdpBaggageHeader()`, and test lifecycle reporting.

The `coverage-integration` artifacts are vendored in `jtest/.m2/repository` and are resolved via the `jtest/.m2/settings.xml` file repository.

## Configuring Settings

Settings are resolved in order: **system property** (`-D`) → **`coverage-integration.properties`** → **hardcoded default**.

Edit [`src/test/resources/coverage-integration.properties`](src/test/resources/coverage-integration.properties) to configure your CTP environment. Any setting can be overridden on the Maven command line, e.g. `-Dparasoft.coverage.integration.ctp.url=http://ctp-server:8080/em`.
