package org.springframework.samples.petclinic.selenium.testng;

import java.net.URL;
import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

public class NavigateIT {
    private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("org.springframework.samples.petclinic.headless", "false"));

    private static WebDriver driver;

    @BeforeClass
    public void openBrowser() throws Exception {
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

    @AfterClass
    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeMethod
    public void configureCoverage() {
        SeleniumCoverageIntegration.configureCdpBaggageHeader((HasCdp) driver);
    }

    @Test
    public void testPetClinicNavigation() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(PETCLINIC_URL);

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[@title='veterinarians']")))
        .click();

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[@class='dropdown-toggle']")))
        .click();

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[@ui-sref='owners']")))
        .click();

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a")));
        } catch (TimeoutException e) {
            System.out.println("Page did not render, refreshing...");
            driver.navigate().refresh();
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a")));
        }
        
        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a")))
        .click();

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//dd/a")))
        .click();

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[@title='home page']")))
        .click();

        Assert.assertTrue(true, "Navigation test completed");
    }
}
