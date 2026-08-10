package org.springframework.samples.petclinic.selenium.junit4.tests;

import java.time.Duration;
import org.junit.Rule;
import com.parasoft.coverage.integration.junit4.ParasoftJUnit4Watcher;
import java.util.Collection;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
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

import com.parasoft.coverage.integration.proxy.ParasoftHeaderInjectingProxy;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

@RunWith(Parameterized.class)
public class ParameterizedPetIT {

    private static final String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099/");

    @Rule
    public ParasoftJUnit4Watcher parasoftJUnit4Watcher = new ParasoftJUnit4Watcher();

    @Parameters(name = "{0}")
    public static Collection<String> browsers() {
        return Arrays.asList("chrome", "edge", "firefox");
    }

    private final String browser;
    private WebDriver driver;
    private ParasoftHeaderInjectingProxy proxy;

    public ParameterizedPetIT(String browser) {
        this.browser = browser;
    }

    @Before
    public void openBrowser() {
        createDriver(browser);
    }

    @After
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
