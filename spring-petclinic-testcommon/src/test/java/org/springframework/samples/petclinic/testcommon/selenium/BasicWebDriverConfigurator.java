package org.springframework.samples.petclinic.testcommon.selenium;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Configures basic WebDriver options such as window size and position.
 */
public class BasicWebDriverConfigurator implements WebDriverConfigurator {
    
    private String windowSize;
    private String windowPosition;

    public BasicWebDriverConfigurator() {
        this.windowSize = "1200,800"; // default window size
        this.windowPosition = "0,0"; //  default window position
    }

    public BasicWebDriverConfigurator(String windowSize, String windowPosition) {
        this.windowSize = windowSize;
        this.windowPosition = windowPosition;
    }

    @Override
    public void configure(MutableCapabilities options) {
        if (options instanceof ChromeOptions) {
            // Add any basic Chrome options here, e.g.:
            ChromeOptions chromeOptions = (ChromeOptions) options;
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--window-size=" + windowSize);
            chromeOptions.addArguments("--window-position=" + windowPosition);
            // Suppress Chrome's background startup network traffic
            // These flags mirror the defaults that Playwright's launcher applies to its
            // bundled Chromium
            chromeOptions.addArguments(
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-background-networking",
                "--disable-component-update",
                "--disable-default-apps",
                "--disable-sync",
                "--disable-translate",
                "--disable-domain-reliability",
                "--disable-client-side-phishing-detection",
                "--metrics-recording-only",
                "--safebrowsing-disable-auto-update",
                "--disable-features=OptimizationHints,InterestFeedContentSuggestions,Translate"
            );
        }
        
        // Add support for other browsers as needed
    }
}
