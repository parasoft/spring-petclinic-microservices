package org.springframework.samples.petclinic.selenium.parallel;

import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openqa.selenium.By;

public class NavigateIT extends AbstractSeleniumIT {
    private static final Logger LOGGER = Logger.getLogger(NavigateIT.class.getName());
    private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");

    private static BrowserContext browser;

    @BeforeAll
    static void openBrowser() {
        browser = startBrowser(LOGGER, NavigateIT.class);
    }

    @AfterAll
    static void closeBrowser() {
        stopBrowser(browser);
    }

    @Test
    public void testPetClinicNavigation() throws Exception {
        browser.driver.get(PETCLINIC_URL);
        Thread.sleep(1000);
        browser.driver.findElement(By.xpath("//a[@title=\"veterinarians\"]")).click();
        Thread.sleep(1000);
        browser.driver.findElement(By.xpath("//a[@class=\"dropdown-toggle\"]")).click();
        Thread.sleep(1000);
        browser.driver.findElement(By.xpath("//a[@ui-sref=\"owners\"]")).click();
        Thread.sleep(1000);
        browser.driver.findElement(By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a")).click();
        Thread.sleep(1000);
        browser.driver.findElement(By.xpath("//dd/a")).click();
        Thread.sleep(1000);
        browser.driver.findElement(By.xpath("//a[@title=\"home page\"]")).click();
    }
}
