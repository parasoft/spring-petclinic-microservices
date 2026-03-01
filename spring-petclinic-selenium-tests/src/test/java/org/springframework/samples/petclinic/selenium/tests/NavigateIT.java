package org.springframework.samples.petclinic.selenium.tests;

import java.time.Duration;

import org.springframework.samples.petclinic.testcommon.selenium.BasicWebDriverConfigurator;
import org.springframework.samples.petclinic.testcommon.selenium.BrowserType;
import org.springframework.samples.petclinic.testcommon.selenium.ParasoftWebDriverConfigurator;
import org.springframework.samples.petclinic.testcommon.selenium.ParasoftWebDriverResource;
import org.springframework.samples.petclinic.testcommon.selenium.WebDriverFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@ExtendWith(org.springframework.samples.petclinic.testcommon.junit5.ParasoftWatcher.class)
public class NavigateIT {
    private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");

    private static ParasoftWebDriverResource driverResource;
    private static WebDriver driver;

    @BeforeAll
    static void openBrowser() {
        driverResource = WebDriverFactory.create(
                BrowserType.CHROME,
                new BasicWebDriverConfigurator("960,1080", "0,0"),
                new ParasoftWebDriverConfigurator(NavigateIT.class.getName()));
        driver = driverResource.getDriver();
    }

    @AfterAll
    static void closeBrowser() {
        if (driverResource != null) {
            driverResource.close();
        }
    }

    @Test
    public void testPetClinicNavigation() throws Exception {
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
