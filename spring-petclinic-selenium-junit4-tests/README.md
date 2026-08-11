# spring-petclinic-selenium-junit4-tests

Selenium + JUnit 4 functional tests for the Spring Petclinic application, with Parasoft CTP integration for code coverage collection, test result reporting, and Test Impact Analysis.

## Prerequisites

- Complete the [local deployment setup](../README.md#local-deployment-with-coverage) in the root README before running tests (configures coverage agents, builds images, publishes static coverage to DTP, and starts the services).
- The project must be built before running tests:

```
mvn -ntp clean install -DskipTests
```

## Running Tests

**Basic execution:**

```
mvn -ntp verify -pl spring-petclinic-selenium-junit4-tests -am -DPETCLINIC_URL=<PETCLINIC URL>
```

`PETCLINIC_URL` defaults to `http://localhost:8099` if not provided.

**Headless mode:** Add `-Dorg.springframework.samples.petclinic.headless=true` to run without a visible browser window.

**Non-default CTP credentials:** Add `-Dparasoft.coverage.integration.ctp.auth.username=<USERNAME> -Dparasoft.coverage.integration.ctp.auth.password=<PASSWORD>`.

## Coverage Integration

This module uses the `coverage-integration` library (`com.parasoft:coverage-integration-junit4` and `com.parasoft:coverage-integration-selenium`) for Parasoft CTP session management, baggage injection, and test lifecycle reporting.

Coverage integration is wired in two ways:

- **`ParasoftJUnit4Watcher`** — a JUnit 4 `@Rule` declared on each test class. It handles per-test CTP session start/stop and baggage propagation.
- **`ParasoftJUnit4RunListener`** — registered in the Maven Failsafe plugin configuration (`pom.xml`). It handles the overall CTP session lifecycle for the full test run.

`NavigateIT` and `PetIT` use CDP baggage header injection via `SeleniumCoverageIntegration.configureCdpBaggageHeader()`. `ParameterizedPetIT` uses a `ParasoftHeaderInjectingProxy` to inject baggage headers for multi-browser parameterized tests (Chrome, Edge, Firefox).

The `coverage-integration` artifacts are vendored in `jtest/.m2/repository` and are resolved via the `jtest/.m2/settings.xml` file repository.

## Configuring Settings

Settings are resolved in order: **system property** (`-D`) → **`coverage-integration.properties`** → **hardcoded default**.

Edit [`src/test/resources/coverage-integration.properties`](src/test/resources/coverage-integration.properties) to configure your CTP environment. Any setting can be overridden on the Maven command line, e.g. `-Dparasoft.coverage.integration.ctp.url=http://ctp-server:8080/em`.
