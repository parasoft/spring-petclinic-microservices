# Playwright-JUnit Web Functional Tests with CTP

In order to run the tests, CTP must be running and configured for both communicating with DTP and for collecting coverage on a running petclinic application with agents properly configured.

This module assumes sequential test execution.  For an example of a parallel test execution implementation, see the `spring-petclinic-selenium-parallel-tests` module.

First, make sure you've had a successful build, use the command 
```
mvn -ntp clean install -DskipTests
```

Or if you need to publish static coverage to DTP with Jtest, you can use the command (ensure jtest.settings file exists and has correct path)
```
mvn -ntp clean package jtest:monitor -DskipTests=true -Djtest.settings=jtest.settings -Djtest.showSettings=true -Dproperty.report.dtp.publish=true
```

To run all tests in this module, use the command
```
mvn verify -pl spring-petclinic-playwright-tests -am -DPETCLINIC_URL=<PETCLINIC URL>
```

By default, PETCLINIC_URL will be set to http://localhost:8099 if not provided.

If you want to run the tests in headless mode so the browser does not become visible during test execution, add the option `-DHEADLESS=true`.

If your CTP instance uses non-default credentials, set `-DCTP_USERNAME=<USERNAME>` and `-DCTP_PASSWORD=<PASSWORD>`.

## Configuring Settings

The Parasoft-related settings can be read from a properties file on the classpath with the name `parasoft-settings.properties`, see [spring-petclinic-playwright-tests/src/test/resources/parasoft-settings.properties](spring-petclinic-playwright-tests/src/test/resources/parasoft-settings.properties).  You will want to configure these settings to point to your instance of CTP with the correct environment ID.  These settings can also be overridden with equivalently named System variables on the Maven commandline with -D, like: `-DCTPBASEURL=http://localhost:8080`

The Petclinic BASEURL must be set via System variable, using `-DPETCLINIC_URL=<url>` for example: `-DPETCLINIC_URL=http://localhost:8099`

For additional context and comments about these settings, see [spring-petclinic-testcommon/src/test/java/org/springframework/samples/petclinic/testcommon/ParasoftSettings.java](spring-petclinic-testcommon/src/test/java/org/springframework/samples/petclinic/testcommon/ParasoftSettings.java).

- `HEADLESS` (default: `false`)

- `CTP_ENABLED` (default: `false`)
- `CTP_BASE_URL` (default: `http://localhost:8080/em`)
- `CTP_ENV_ID` (default: `1`)
- `CTP_USERNAME` (default: `admin`)
- `CTP_PASSWORD` (default: `admin`)
- `CTP_DEBUG` (default: `true`)

- `CTP_MULTI_USER_MODE` (default: `true`)

- `PROXY_HOST` (default: `localhost`)
- `PROXY_BIND_HOST` (default: `0.0.0.0`)

- `CTP_PUBLISH_BASELINE` (default: `false`)
- `CTP_BASELINE_BUILD_ID` (default: `spring-petclinic-baseline`)

When `CTP_MULTI_USER_MODE` is `true`, this module uses a fixed coverage user ID format of `{testFramework}-{ctpUsername}-1`.