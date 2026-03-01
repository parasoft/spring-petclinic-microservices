package org.springframework.samples.petclinic.testcommon.selenium;

import org.openqa.selenium.MutableCapabilities;

/**
 * Strategy interface for applying configuration to WebDriver capabilities.
 */
public interface WebDriverConfigurator {
    void configure(MutableCapabilities options);
}