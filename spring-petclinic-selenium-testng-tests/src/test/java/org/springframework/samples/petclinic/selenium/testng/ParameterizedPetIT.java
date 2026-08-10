package org.springframework.samples.petclinic.selenium.testng;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.parasoft.coverage.integration.proxy.ParasoftHeaderInjectingProxy;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

public class ParameterizedPetIT {

    private static final String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");

    private WebDriver driver;
    private ParasoftHeaderInjectingProxy proxy;

    @DataProvider(name = "browsers")
    public Object[][] browsers() {
        return new Object[][] { {"chrome"}, {"edge"}, {"firefox"} };
    }

    @AfterMethod
    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
        if (proxy != null) {
            proxy.close();
            proxy = null;
        }
    }

    @Test(dataProvider = "browsers")
    public void testRenamePet(String browser) {
        createDriver(browser);

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

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name"))).clear();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name"))).sendKeys("Lena");

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='submit']")))
        .click();

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//dd/a")))
        .click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name"))).clear();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name"))).sendKeys("Leo");

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='submit']")))
        .click();

        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[@title='home page']")))
        .click();
    }

    private void createDriver(String browser) {
        if ("firefox".equalsIgnoreCase(browser)) {
            FirefoxOptions firefoxOptions = new FirefoxOptions();
            proxy = SeleniumCoverageIntegration.configureFirefoxOptions(firefoxOptions);
            driver = new FirefoxDriver(firefoxOptions);
        } else if ("edge".equalsIgnoreCase(browser)) {
            EdgeOptions edgeOptions = new EdgeOptions();
            proxy = SeleniumCoverageIntegration.configureEdgeOptions(edgeOptions);
            driver = new EdgeDriver(edgeOptions);
        } else {
            // Default: Chrome
            ChromeOptions chromeOptions = new ChromeOptions();
            proxy = SeleniumCoverageIntegration.configureChromeOptions(chromeOptions);
            driver = new ChromeDriver(chromeOptions);
        }
    }
}
