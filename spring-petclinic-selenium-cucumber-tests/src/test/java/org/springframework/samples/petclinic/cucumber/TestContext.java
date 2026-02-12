package org.springframework.samples.petclinic.cucumber;

import org.openqa.selenium.chrome.ChromeDriver;

public class TestContext {
    private ChromeDriver driver;

    public ChromeDriver getDriver() {
        return driver;
    }

    public void setDriver(ChromeDriver driver) {
        this.driver = driver;
    }
}
