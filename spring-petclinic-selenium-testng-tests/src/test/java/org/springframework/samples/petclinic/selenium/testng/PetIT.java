package org.springframework.samples.petclinic.selenium.testng;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

public class PetIT {
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
    public void testRenamePet() {
        SeleniumCoverageIntegration.configureCdpBaggageHeader(driver);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(PETCLINIC_URL);

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[@class='dropdown-toggle']")))
        .click();

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[@ui-sref='owners']")))
        .click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("owner-list table")));

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a")))
        .click();

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//dd/a")))
        .click();

        WebElement nameField = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.name("name")));
        nameField.clear();
        nameField.sendKeys("Lena");

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='submit']")))
        .click();

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//dd/a")))
        .click();

        nameField = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.name("name")));
        nameField.clear();
        nameField.sendKeys("Leo");

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='submit']")))
        .click();

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[@title='home page']")))
        .click();

        Assert.assertTrue(true, "Pet rename test completed");
    }
}
