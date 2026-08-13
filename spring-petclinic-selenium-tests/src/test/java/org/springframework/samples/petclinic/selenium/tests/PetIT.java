package org.springframework.samples.petclinic.selenium.tests;

import java.net.URL;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

public class PetIT {
    private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099/");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("org.springframework.samples.petclinic.headless", "false"));

    private static WebDriver driver;

    @BeforeAll
    static void openBrowser() throws Exception {
        ChromeOptions opts = new ChromeOptions();
        if (HEADLESS) {
            opts.addArguments("--headless=new");
        }
        String gridUrl = System.getProperty("SELENIUM_GRID_URL", "");
        if (!gridUrl.isEmpty()) {
            driver = new Augmenter().augment(new RemoteWebDriver(new URL(gridUrl), opts));
        } else {
            opts.addArguments("--no-sandbox", "--disable-dev-shm-usage");
            driver = new ChromeDriver(opts);
        }
    }

    @AfterAll
    static void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void configureCoverage() {
        SeleniumCoverageIntegration.configureCdpBaggageHeader((HasCdp) driver);
    }

    @Test
    public void testRenamePet() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.get(PETCLINIC_URL);
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@class=\"dropdown-toggle\"]"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[ui-sref='owners']"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("owner-list table")));
        wait.until(
                driverInstance -> driverInstance.findElements(By.cssSelector("owner-list table tbody tr")).size() > 0);
        wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("owner-list table tbody tr:first-child td a")))
                .click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//dd/a"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name"))).clear();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name"))).sendKeys("Lena");
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type=\"submit\"]"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//dd/a"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name"))).clear();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name"))).sendKeys("Leo");
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type=\"submit\"]"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@title=\"home page\"]"))).click();
    }
}
