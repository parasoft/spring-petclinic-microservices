package org.springframework.samples.petclinic.testcommon.selenium;

import java.net.URL;

import org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Factory for creating {@link ParasoftWebDriverResource} instances with configurable
 * browser type, options, and optional Parasoft CTP integration.
 */
public class WebDriverFactory {
    public static ParasoftWebDriverResource create(BrowserType browser, WebDriverConfigurator... configurators) {
        MutableCapabilities options = null;
        ParasoftHeaderInjectingProxy parasoftProxyHandle = null;
        String testContextKey = null;

        // Create the appropriate MutableCapabilities based on the browser type
        switch (browser) {
            case CHROME:
                options = new ChromeOptions();
                break;
            case FIREFOX:
                throw new UnsupportedOperationException("WebDriver for " + browser + " is not yet implemented.");    
                // options = new FirefoxOptions();
            case EDGE:
                throw new UnsupportedOperationException("WebDriver for " + browser + " is not yet implemented.");    
                // options = new EdgeOptions();
            default:
                throw new IllegalArgumentException("Unsupported browser type: " + browser);
        }

        // Apply all configurators to the options
        for (WebDriverConfigurator configurator : configurators) {
            configurator.configure(options);
            // If this configurator is a ParasoftWebDriverConfigurator, get the proxy and testContextKey from it
            if (configurator instanceof ParasoftWebDriverConfigurator) {
                parasoftProxyHandle = ((ParasoftWebDriverConfigurator) configurator).getParasoftProxyHandle();
                testContextKey = ((ParasoftWebDriverConfigurator) configurator).getTestContextKey();
            }
        }

        return createWebDriverResource(browser, options, parasoftProxyHandle, testContextKey);
    }

    // Create appropriate WebDriver based on the browser type, whether Selenium Grid
    // is used, and the provided options
    private static ParasoftWebDriverResource createWebDriverResource(BrowserType browser, MutableCapabilities options,
            ParasoftHeaderInjectingProxy parasoftProxyHandle, String testContextKey) {
        WebDriver driver = null;
        if (ParasoftSettings.isSeleniumGrid()) {
            try {
                driver = new RemoteWebDriver(new URL(ParasoftSettings.SELENIUM_GRID_URL), options);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize RemoteWebDriver", e);
            }
        } else {
            switch (browser) {
                case CHROME:
                    driver = new ChromeDriver((ChromeOptions) options);
                    break;
                case FIREFOX:
                    throw new UnsupportedOperationException("WebDriver for " + browser + " is not yet implemented.");    
                    // driver = new FirefoxDriver((FirefoxOptions)options);
                case EDGE:
                    throw new UnsupportedOperationException("WebDriver for " + browser + " is not yet implemented.");    
                    // driver = new EdgeDriver((EdgeOptions)options);
                default:
                    throw new IllegalArgumentException("Unsupported browser type: " + browser);
            }
        }
        return new ParasoftWebDriverResource(driver, parasoftProxyHandle, testContextKey);
    }
}