# spring-petclinic-selenium-cucumber-tests

Selenium + Cucumber + JUnit Platform functional tests for the Spring Petclinic application, with Parasoft CTP integration for runtime coverage collection and test-result publishing.

Sequential execution is the default. Parallel scenario execution is also supported when both Cucumber parallel execution and coverage parallel IDs are enabled.

## Prerequisites

- The Petclinic application must be running and accessible.
- Parasoft CTP must be running.
- The selected CTP environment must contain coverage-agent connections for the Petclinic services.
- The Petclinic services must be started with their coverage agents enabled.
- The `coverage-integration` artifacts must be available from the configured Maven repository or installed in the local Maven repository.

Build the project before running the tests:

```bash
mvn -ntp clean install -DskipTests
```

To publish static coverage to DTP with Jtest, ensure `jtest.settings` contains the correct paths and run:

```bash
mvn -ntp clean package jtest:monitor \
  -DskipTests=true \
  -Djtest.settings=jtest.settings \
  -Djtest.showSettings=true \
  -Dproperty.report.dtp.publish=true
```

## Running the tests

### Sequential execution

```bash
mvn -ntp verify \
  -pl spring-petclinic-selenium-cucumber-tests \
  -am
```

The Petclinic URL defaults to `http://localhost:8099`. Override it with:

```bash
-DPETCLINIC_URL=<Petclinic URL>
```

For a developer-specific CTP environment, override the environment ID configured in `coverage-integration.properties`:

```bash
-Dparasoft.coverage.integration.ctp.envId=<environment ID>
```

Example:

```bash
mvn -ntp verify \
  -pl spring-petclinic-selenium-cucumber-tests \
  -am \
  -DPETCLINIC_URL=http://localhost:8099 \
  -Dparasoft.coverage.integration.ctp.envId=1
```

### Parallel execution

Enable both Cucumber parallel execution and coverage parallel IDs:

```bash
mvn -ntp verify \
  -pl spring-petclinic-selenium-cucumber-tests \
  -am \
  -Dparasoft.coverage.integration.parallel.test.enabled=true \
  -Dcucumber.execution.parallel.enabled=true \
  -Dcucumber.execution.parallel.config.strategy=fixed \
  -Dcucumber.execution.parallel.config.fixed.parallelism=4 \
  -Dcucumber.execution.parallel.config.fixed.max-pool-size=4
```

The two scenarios that edit the same pet are tagged with `@pet-name`. `RunCucumberIT` maps that tag to a Cucumber exclusive read/write resource, so those two scenarios do not modify the same record concurrently. Other scenarios may execute at the same time.

Keep these settings aligned:

| Setting | Responsibility |
|---|---|
| `cucumber.execution.parallel.enabled` | Allows Cucumber to schedule scenarios concurrently |
| `parasoft.coverage.integration.parallel.test.enabled` | Enables distinct parallel IDs for concurrent coverage tests |

Sequential execution remains the default because both settings default to `false`.

### Headless mode

Add:

```bash
-DHEADLESS=true
```

### Selenium Grid

Run with:

```bash
mvn -ntp verify \
  -pl spring-petclinic-selenium-cucumber-tests \
  -am \
  -DSELENIUM_GRID=true \
  -DSELENIUM_GRID_URL=http://<grid-host>:4444/wd/hub \
  -DPROXY_HOST=<test-runner host visible to the Grid browser>
```

The coverage proxy runs on the test-runner machine. `PROXY_BIND_HOST` controls where the proxy listens; `PROXY_HOST` controls the address supplied to the browser.

Defaults:

| Property | Default |
|---|---|
| `PETCLINIC_URL` | `http://localhost:8099` |
| `PROXY_BIND_HOST` | `0.0.0.0` |
| `PROXY_HOST` | `127.0.0.1` |
| `SELENIUM_GRID` | `false` |
| `SELENIUM_GRID_URL` | `http://localhost:4444/wd/hub` |
| `HEADLESS` | `false` |

For a browser running in Docker or on another machine, set `PROXY_HOST` to an address through which that browser can reach the test runner. Override `PROXY_BIND_HOST` when the proxy must listen on a specific interface.

## Coverage integration

The module uses these `coverage-integration` artifacts:

- `coverage-integration-api`
- `coverage-integration-cucumber`
- `coverage-integration-proxy`
- `coverage-integration-selenium`

The Cucumber runner includes the shared coverage hooks in its glue path:

```java
@ConfigurationParameter(
        key = GLUE_PROPERTY_NAME,
        value = "org.springframework.samples.petclinic.cucumber,"
                + "com.parasoft.coverage.integration.cucumber")
```

The shared hooks perform this lifecycle:

1. Start one CTP coverage session before the Cucumber run.
2. Start one CTP coverage test before each scenario.
3. Store the baggage returned by CTP in the current execution context.
4. Stop the CTP coverage test after each scenario with `PASS`, `FAIL`, or `INCOMPLETE`.
5. Stop the CTP session after the run.
6. Request publication of the completed coverage session and test results to DTP.

Scenario identifiers use this format:

```text
test     = <feature-file-name>#<scenario-name>
testCase = <scenario-name>
```

`PetClinicSteps` reads the current baggage from `CoverageIntegration`. When baggage is present, it starts a `ParasoftHeaderInjectingProxy` and configures Chrome through `SeleniumCoverageIntegration`. The browser and proxy are closed after each scenario.

## Coverage settings

The shared library loads:

```text
src/test/resources/coverage-integration.properties
```

A Java system property supplied with `-D` overrides the corresponding value in that file.

Required settings:

| Property | Purpose |
|---|---|
| `parasoft.coverage.integration.ctp.url` | CTP URL including the context path |
| `parasoft.coverage.integration.ctp.envId` | CTP environment ID |

Common optional settings:

| Property | Purpose |
|---|---|
| `parasoft.coverage.integration.ctp.auth.username` | Basic-auth username |
| `parasoft.coverage.integration.ctp.auth.password` | Basic-auth password |
| `parasoft.coverage.integration.ctp.auth.token` | Bearer-token alternative |
| `parasoft.coverage.integration.ctp.userId` | Multi-user coverage identifier |
| `parasoft.coverage.integration.parallel.test.enabled` | Enables unique parallel IDs for concurrent coverage tests |
| `parasoft.coverage.integration.dtp.sessionTag` | DTP session tag |

Example command-line overrides:

```bash
-Dparasoft.coverage.integration.ctp.url=http://ctp-host:8070/em
-Dparasoft.coverage.integration.ctp.envId=1
-Dparasoft.coverage.integration.ctp.auth.username=<username>
-Dparasoft.coverage.integration.ctp.auth.password=<password>
-Dparasoft.coverage.integration.ctp.userId=<coverage user ID>
-Dparasoft.coverage.integration.dtp.sessionTag=<session tag>
```

The CTP user ID and DTP session tag are explicit values. Changing the authentication username does not automatically change either value.

The shared Cucumber lifecycle requests publication after a successfully started session. It does not expose the legacy `CTP_PUBLISH_COVERAGE`, `CTP_PUBLISH_BASELINE`, or `CTP_BASELINE_BUILD_ID` settings.
