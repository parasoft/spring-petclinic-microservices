package org.springframework.samples.petclinic.testcommon.selenium;

import org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy;
import org.springframework.samples.petclinic.testcommon.ParasoftSessionManager;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Manages the lifecycle of a WebDriver instance and its associated Parasoft header-injecting proxy.
 * <p>
 * The proxy already holds the per-test-context baggage {@link java.util.concurrent.atomic.AtomicReference}
 * vended by {@link ParasoftSessionManager#obtainProxyBaggageRef(String)} (wired in
 * {@link ParasoftWebDriverConfigurator#configure}), so this constructor only needs to register the
 * parallel ID when parallel mode is enabled. Implements {@link AutoCloseable} to ensure the
 * WebDriver and proxy are properly shut down and the session manager state is cleared.
 */
public class ParasoftWebDriverResource implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(ParasoftWebDriverResource.class.getName());

    private final WebDriver driver;
    private final ParasoftHeaderInjectingProxy parasoftProxyHandle;
    private final String testContextKey; // JUnit/TestNG test class name, or Cucumber scenario name
    private final String webDriverSessionId;

    public ParasoftWebDriverResource(WebDriver driver, ParasoftHeaderInjectingProxy parasoftProxyHandle, String testContextKey) {
        this.driver = driver;
        this.parasoftProxyHandle = parasoftProxyHandle;
        this.testContextKey = testContextKey;
        this.webDriverSessionId = (driver instanceof RemoteWebDriver remoteDriver && remoteDriver.getSessionId() != null)
                ? remoteDriver.getSessionId().toString()
                : null;

        if (ParasoftSettings.isMultiUserMode() && ParasoftSettings.isParallelTestExecution()) {
            if (webDriverSessionId != null) {
                ParasoftSessionManager.registerParallelId(testContextKey, webDriverSessionId);
            } else if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftWebDriverResource] webDriverSessionId is null for " + testContextKey
                        + " in parallel mode; parallelId will be absent from API calls and concurrent tests may not be distinguishable by the coverage agents");
            }
        }
    }

    @Override
    public void close() {
        try {
            if (driver != null)
                driver.quit();
            if (parasoftProxyHandle != null)
                parasoftProxyHandle.getProxy().stop();
        } finally {
            ParasoftSessionManager.unregister(testContextKey);
        }
    }

    public WebDriver getDriver() {
        return driver;
    }
}