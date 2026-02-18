# spring-petclinic-testcommon

This module provides shared test utilities and support classes for the Spring Petclinic microservices project. It is designed to be used as a dependency by other test modules (such as Selenium and Playwright test modules) to enable integration with Parasoft CTP to facilitate the code coverage and test impact analysis test setup and configuration.

## Key Features

- **Parasoft CTP Integration**: Includes a client for interacting with Parasoft CTP (Continuous Testing Platform) APIs, enabling test session start/stop, test start/stop, publishing coverage to DTP, and setting a baselineBuildId.
- **Header Injection Utilities**: Provides proxies and utilities for injecting Parasoft-specific headers into HTTP requests and Selenium DevTools sessions, which is necessary for the code coverage workflow when the coverage agents are in multi-user mode (e.g., parallel test execution).
- **Centralized Test Settings**: Offers a configuration class for managing Parasoft-related settings across test modules.

## Directory Structure

- `src/main/java/org/springframework/samples/petclinic/testcommon/`
    - `ParasoftCTPApiClient.java`
    - `ParasoftHeaderInjectingProxy.java`
    - `ParasoftHeaderInjectingSeleniumDevTools.java`
    - `ParasoftSettings.java`

## Additional Notes

The `ParasoftHeaderInjectingSeleniumDevTools.java` class is there for reference only.  Using Selenium DevTools API for injecting headers can be problematic in scenarios like when you are running tests on a Grid, and it only works with Chrome.  Using a proxy server to inject the header is a more generally applicable solution and the recommended option.