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
            chromeOptions.addArguments("--window-size=" + windowSize);
            chromeOptions.addArguments("--window-position=" + windowPosition);
        }
        
        // Add support for other browsers as needed
    }
}
