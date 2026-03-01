package org.springframework.samples.petclinic.selenium.testng;

import org.springframework.samples.petclinic.testcommon.selenium.BasicWebDriverConfigurator;
import org.springframework.samples.petclinic.testcommon.selenium.BrowserType;
import org.springframework.samples.petclinic.testcommon.selenium.ParasoftWebDriverConfigurator;
import org.springframework.samples.petclinic.testcommon.selenium.ParasoftWebDriverResource;
import org.springframework.samples.petclinic.testcommon.selenium.WebDriverFactory;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(org.springframework.samples.petclinic.testcommon.testng.ParasoftWatcherTestNG.class)
public class NavigateIT {
    private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");

    private static ParasoftWebDriverResource driverResource;
    private static WebDriver driver;

    @BeforeClass
    public void openBrowser() {
        driverResource = WebDriverFactory.create(
                BrowserType.CHROME,
                new BasicWebDriverConfigurator("960,1080", "0,0"),
                new ParasoftWebDriverConfigurator(NavigateIT.class.getName()));
        driver = driverResource.getDriver();
    }

    @AfterClass
    public void closeBrowser() {
        if (driverResource != null) {
            driverResource.close();
        }
    }

    @Test
    public void testPetClinicNavigation() throws Exception {
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
