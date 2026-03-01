package org.springframework.samples.petclinic.testcommon.selenium;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy;
import org.springframework.samples.petclinic.testcommon.ParasoftSessionManager;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Manages the lifecycle of a WebDriver instance and its associated Parasoft header-injecting proxy.
 * <p>
 * On creation, binds a coverage user ID to the proxy: for parallel execution, starts a new
 * CTP session; for sequential execution, reuses the suite-level session. Implements
 * {@link AutoCloseable} to ensure the WebDriver and proxy are properly shut down.
 */
public class ParasoftWebDriverResource implements AutoCloseable {
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

        if (ParasoftSettings.isParallelTestExecution()) {
            // If parallel test execution, then start a CTP session for this WebDriver instance, where the session will register the coverageUserIdRef of the proxy for this WebDriver instance
            AtomicReference<String> coverageUserIdRef = parasoftProxyHandle.getCoverageUserIdRef();
            ParasoftSessionManager.startSession(testContextKey, webDriverSessionId, coverageUserIdRef);
        } else {
            // If sequential test execution, instead bind the sequentialCoverageUserIdRef from the ParasoftSessionManager to the proxy
            AtomicReference<String> sequentialCoverageUserIdRef = ParasoftSessionManager.getCoverageUserIdRef(testContextKey);
            parasoftProxyHandle.setCoverageUserId(sequentialCoverageUserIdRef);
        }
    }

    @Override
    public void close() {
        if (driver != null)
            driver.quit();
        if (parasoftProxyHandle != null)
            parasoftProxyHandle.getProxy().stop();
        
        // If parallel test execution, then stop the CTP session for this WebDriver instance
        if (ParasoftSettings.isParallelTestExecution()) {
            ParasoftSessionManager.stopSession(testContextKey);
        }
    }

    public WebDriver getDriver() {
        return driver;
    }
}