package org.springframework.samples.petclinic.selenium.tests;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

public class NavigateIT {
    private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("org.springframework.samples.petclinic.headless", "false"));

    private static ChromeDriver driver;

    @BeforeAll
    static void openBrowser() {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments(
            "--no-sandbox",
                "--disable-dev-shm-usage",
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

    @AfterAll
    static void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testPetClinicNavigation() throws Exception {
        SeleniumCoverageIntegration.configureCdpBaggageHeader(driver);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.get(PETCLINIC_URL);
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@title=\"veterinarians\"]"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@class=\"dropdown-toggle\"]"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[ui-sref='owners']"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("owner-list table")));
        wait.until(driverInstance -> driverInstance.findElements(By.cssSelector("owner-list table tbody tr")).size() > 0);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("owner-list table tbody tr:first-child td a"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//dd/a"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@title=\"home page\"]"))).click();
    }
}
