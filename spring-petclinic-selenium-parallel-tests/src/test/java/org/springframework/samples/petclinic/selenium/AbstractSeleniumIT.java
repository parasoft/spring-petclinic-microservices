package org.springframework.samples.petclinic.selenium;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;
import org.springframework.samples.petclinic.selenium.util.ParasoftTestSessionRegistry;

import org.littleshoot.proxy.HttpProxyServer;

import org.junit.jupiter.api.extension.ExtendWith;

import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

@ExtendWith(org.springframework.samples.petclinic.selenium.util.ParasoftWatcher.class)
abstract class AbstractSeleniumIT {
	// BrowserContext wrapper class encapsulates relevant information about the browser instance that must be
    // instantiated for each test class (i.e., WebDriver instance) so that separate references to each of these
    // resources can be maintained when running tests in parallel
    protected static final class BrowserContext {
		final WebDriver driver;
		final HttpProxyServer proxy;
		final String coverageUserId;

		BrowserContext(WebDriver driver, HttpProxyServer proxy, String coverageUserId) {
			this.driver = driver;
			this.proxy = proxy;
			this.coverageUserId = coverageUserId;
		}
	}

	protected static BrowserContext startBrowser(Logger logger, Class<?> testClass) {
		ChromeOptions options = new ChromeOptions();
		HttpProxyServer proxy = null;
		AtomicReference<String> coverageUserIdRef = new AtomicReference<>("");

		// Using LittleProxy for request header injection, required for Parasoft
		// coverage reporting when agents are in multi-user mode
		if (ParasoftSettings.isMultiUserMode()) {
			proxy = ParasoftHeaderInjectingProxy.startProxy(coverageUserIdRef);
			int proxyPort = proxy.getListenAddress().getPort();

			Proxy seleniumProxy = new Proxy();
			seleniumProxy.setHttpProxy(ParasoftSettings.PROXY_HOST + ":" + proxyPort);
			seleniumProxy.setSslProxy(ParasoftSettings.PROXY_HOST + ":" + proxyPort);
			seleniumProxy.setNoProxy("<-loopback>");

			options.setProxy(seleniumProxy);
		}
		if (ParasoftSettings.isHeadless()) {
			options.addArguments("--headless=new");
		}

		WebDriver driver;
		if (ParasoftSettings.isSeleniumGrid()) {
			String gridUrl = ParasoftSettings.SELENIUM_GRID_URL;
			if (ParasoftSettings.CTP_DEBUG) {
				logger.info("[" + testClass.getSimpleName() + "] Using Selenium Grid at " + gridUrl);
			}
			try {
				driver = new RemoteWebDriver(new URL(gridUrl), options, false);
			} catch (MalformedURLException me) {
				throw new RuntimeException("Failed to connect to Selenium Grid at " + gridUrl, me);
			} catch (Exception e) {
				throw new RuntimeException("General failure to initialize RemoteWebDriver for Selenium Grid at " + gridUrl, e);
			}
		} else {
			driver = new ChromeDriver(options);
		}
		
		// In multi-user mode for parallel test execution (class-level / WebDriver instance), the CTP API test session lifecycle
		// should mirror the WebDriver lifecycle, so the test session should be started when the browser is started
		//
		// When multi-user mode is false, see the ParasoftSuiteListener class where the CTP test session lifecycle is at the suite-level
		// and not test class / WebDriver instance
		String webDriverSessionId = getWebDriverSessionId(driver);
		String coverageUserId = null;

		if (ParasoftSettings.isMultiUserMode()) {
			coverageUserId = ParasoftTestSessionRegistry.buildRegisterCoverageUserId(testClass.getName(), webDriverSessionId); // use the WebDriver session ID to build a unique CTP coverage user ID and register it in the registry so that it can be retrieved later by the suite listener to publish coverage for this test session
			coverageUserIdRef.set(coverageUserId); // set the coverage user ID in the AtomicReference passed to the proxy so that it can be included in the headers of intercepted requests
			String ctpTestSessionId = ParasoftCTPApiClient.startSession(coverageUserId); // start a CTP test session using the coverage user ID as the identifier for the session
			ParasoftTestSessionRegistry.registerCtpTestSession(ctpTestSessionId, coverageUserId); // register the CTP test session in the registry so that it can be retrieved later by the suite listener to publish coverage for this test session
		}
		return new BrowserContext(driver, proxy, coverageUserId);
	}

	protected static void stopBrowser(BrowserContext context) {
		if (context == null) {
			return;
		}
		
		// In multi-user mode for parallel test execution (class-level / WebDriver instance), the CTP API test session lifecycle
		// should mirror the WebDriver lifecycle, so the test session should be stopped when the browser is stopped
		//
		// When multi-user mode is false, see the ParasoftSuiteListener class where the CTP test session lifecycle is at the suite-level
		// and not test class / WebDriver
		if (ParasoftSettings.isMultiUserMode()) {
			ParasoftCTPApiClient.stopSession(context.coverageUserId);
		}
		if (context.driver != null) {
			context.driver.quit();
		}
		if (context.proxy != null) {
			context.proxy.stop();
		}
	}

	private static String getWebDriverSessionId(WebDriver driver) {
		if (driver instanceof RemoteWebDriver remoteDriver && remoteDriver.getSessionId() != null) {
			return remoteDriver.getSessionId().toString();
		}
		return null;
	}
}
