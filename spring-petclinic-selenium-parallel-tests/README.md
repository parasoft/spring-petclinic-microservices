# Selenium JUnit Parallel Web Functional Tests with CTP

In order to run the tests, CTP must be running and configured for both communicating with DTP and for collecting coverage on a running petclinic application with agents properly configured.

First, make sure you've had a successful build, use the command
```
mvn -ntp clean install -DskipTests
```

Or if you need to publish static coverage to DTP with Jtest, you can use the command (ensure jtest.settings file exists and has correct path)
```
mvn -ntp clean package jtest:monitor -DskipTests=true -Djtest.settings=jtest.settings -Djtest.showSettings=true -Dproperty.report.dtp.publish=true
```

To run all tests in this module sequentially, use the command
```
mvn -ntp verify -pl spring-petclinic-selenium-parallel-tests -am -DCTP_ENV_ID=<CTP ENVIRONMENT ID> -DCTP_BASE_URL=<CTP BASE URL> -DPETCLINIC_URL=<PETCLINIC URL>
```
To run all the tests in this module using JUnit 5 parallel mode, use the command
```
mvn -ntp verify -pl spring-petclinic-selenium-parallel-tests -am -DCTP_ENV_ID=<CTP ENVIRONMENT ID> -DCTP_BASE_URL=<CTP BASE URL> -DPETCLINIC_URL=<PETCLINIC URL> -Djunit.jupiter.execution.parallel.enabled=true -Djunit.jupiter.execution.parallel.mode.classes.default=concurrent
```
To run all the tests in this module using JUnit 5 parallel mode with Selenium Grid, use the command
```
mvn -ntp verify -pl spring-petclinic-selenium-parallel-tests -am -DCTP_ENV_ID=<CTP ENVIRONMENT ID> -DCTP_BASE_URL=<CTP BASE URL> -DPETCLINIC_URL=<PETCLINIC URL> -DSELENIUM_GRID=true -DPROXY_HOST=<PROXY HOST> -Djunit.jupiter.execution.parallel.enabled=true -Djunit.jupiter.execution.parallel.mode.classes.default=concurrent
```

Notes about Parallel Test Execution:
- This module is setup with the expectation that parallelism will happen at the test class level, which is a common parallelism strategy for E2E/Web functional tests to avoid flaky behavior (e.g., interleaved clicks, navigation races, shared state).  Method-level parallelism, which has different junit arguments to configure, is not represented in this module and would require refactoring to support.
- JUnit 5 parallel execution is enabled via the `junit.jupiter.execution.parallel.*` system properties.
	- `junit.jupiter.execution.parallel.enabled=true` enables parallel test execution.
	- `junit.jupiter.execution.parallel.mode.classes.default=concurrent` controls class-level parallelism.
- If tests still run sequentially, add a fixed parallelism strategy, for example:
	- `-Djunit.jupiter.execution.parallel.config.strategy=fixed`
	- `-Djunit.jupiter.execution.parallel.config.fixed.parallelism=2`
- Selenium Grid capacity limits parallelism. If the grid reports `maxSessions: 1`, tests will still queue even with JUnit parallel enabled.
    - Selenium Grid can be configured to increase the number of max sessions (the default behavior of the Docker container is 1 max session).  For example, if running Selenium Grid in a container, you could use the command: `docker run -d --rm -p 4444:4444 -p 7900:7900 --network=demo-net --shm-size "2g" -e SE_NODE_MAX_SESSIONS=2 -e SE_NODE_OVERRIDE_MAX_SESSIONS=true --name selenium-grid selenium/standalone-chrome:latest`

Notes about Selenium Grid:
- If Selenium Grid is running in a container (e.g., Docker Desktop) and the JUnit test runner is on the host, then the host.docker.internal convention may not work.
- Use your host's ip address and set both `-DPROXY_HOST` and `-DPROXY_BIND_HOST` to that ip address.
- If Selenium Grid is not running in a container, then you only need to provide -DPROXY_HOST to where Selenium Grid is located. If Selenium Grid is running on the cloud, extra considerations (e.g., VPC) may be necessary to ensure connectivity between the test runner + local proxy and grid service.


By default, PETCLINIC_URL will be set to http://localhost:8099 if not provided.

If you want to run the tests in headless mode so the browser does not become visible during test execution, add the option `-DHEADLESS=true`.

If your CTP instance uses non-default credentials, set `-DCTP_USERNAME=<USERNAME>` and `-DCTP_PASSWORD=<PASSWORD>`.

## System Properties

These tests use the shared Parasoft settings from testcommon. You can override the following system properties when running Maven:

For additional context and comments about these settings, see [spring-petclinic-testcommon/src/test/java/org/springframework/samples/petclinic/testcommon/ParasoftSettings.java](spring-petclinic-testcommon/src/test/java/org/springframework/samples/petclinic/testcommon/ParasoftSettings.java).

- `PETCLINIC_URL` (default: `http://localhost:8099`)
- `HEADLESS` (default: `false`)
- `CTP_BASE_URL` (default: `http://localhost:8070/em`)
- `CTP_ENV_ID` (default: `4`)
- `CTP_USERNAME` (default: `admin`)
- `CTP_PASSWORD` (default: `admin`)
- `CTP_MULTI_USER_MODE` (default: `true`)
- `CTP_DEBUG` (default: `true`)
- `PUBLISH_BASELINE` (default: `false`)
- `BASELINE_BUILD_ID` (default: `spring-petclinic-baseline`)

When `CTP_MULTI_USER_MODE` is `true`, the tests generate a coverage user ID using the format `{testFramework}-{ctpUsername}-{sessionId}`.
