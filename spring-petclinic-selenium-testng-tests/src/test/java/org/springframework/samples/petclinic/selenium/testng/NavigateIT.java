package org.springframework.samples.petclinic.selenium.testng;

import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

public class NavigateIT {
    private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("org.springframework.samples.petclinic.headless", "false"));

    private static ChromeDriver driver;

    @BeforeClass
    public void openBrowser() {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments(
            "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1200,800",
                "--window-position=0,0",
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
        if (HEADLESS) {
            chromeOptions.addArguments("--headless=new");
        }
        driver = new ChromeDriver(chromeOptions);
    }

    @AfterClass
    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testPetClinicNavigation() throws Exception {
        SeleniumCoverageIntegration.configureCdpBaggageHeader(driver);
        driver.get(PETCLINIC_URL);
        Thread.sleep(1000);
        driver.findElement(By.xpath("//a[@title=\"veterinarians\"]")).click();
        Thread.sleep(1000);
        driver.findElement(By.xpath("//a[@class=\"dropdown-toggle\"]")).click();
        Thread.sleep(1000);
        driver.findElement(By.xpath("//a[@ui-sref=\"owners\"]")).click();
        Thread.sleep(1000);
        driver.findElement(By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a")).click();
        Thread.sleep(1000);
        driver.findElement(By.xpath("//dd/a")).click();
        Thread.sleep(1000);
        driver.findElement(By.xpath("//a[@title=\"home page\"]")).click();
        Assert.assertTrue(true, "Navigation test completed");
    }
}
