package org.springframework.samples.petclinic.selenium.testng;

import java.net.URL;
import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.parasoft.coverage.integration.api.CoverageIntegration;
import com.parasoft.coverage.integration.proxy.ParasoftHeaderInjectingProxy;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

public class ParameterizedPetIT {

    private static final String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("org.springframework.samples.petclinic.headless", "false"));
    private static final String PROXY_BIND_HOST = System.getProperty("PROXY_BIND_HOST", "127.0.0.1");

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
    public void testRenamePet(String browser) throws Exception {
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

    private void createDriver(String browser) throws Exception {
        String gridUrl = System.getProperty("SELENIUM_GRID_URL", "");
        if (!gridUrl.isEmpty()) {
            String baggage = CoverageIntegration.getBaggageHeader();
            if ("firefox".equalsIgnoreCase(browser)) {
                FirefoxOptions opts = new FirefoxOptions();
                if (HEADLESS) {
                    opts.addArguments("--headless");
                }
                if (baggage != null && !baggage.isBlank()) {
                    // proxy must bind to a routable address when the browser runs in a separate container
                    proxy = new ParasoftHeaderInjectingProxy(PROXY_BIND_HOST, 0, baggage);
                    SeleniumCoverageIntegration.configureFirefoxOptions(opts, proxy);
                }
                driver = new RemoteWebDriver(new URL(gridUrl), opts);
            } else if ("edge".equalsIgnoreCase(browser)) {
                EdgeOptions opts = new EdgeOptions();
                if (HEADLESS) {
                    opts.addArguments("--headless=new");
                }
                if (baggage != null && !baggage.isBlank()) {
                    proxy = new ParasoftHeaderInjectingProxy(PROXY_BIND_HOST, 0, baggage);
                    SeleniumCoverageIntegration.configureEdgeOptions(opts, proxy);
                }
                driver = new RemoteWebDriver(new URL(gridUrl), opts);
            } else {
                ChromeOptions opts = new ChromeOptions();
                if (HEADLESS) {
                    opts.addArguments("--headless=new");
                }
                if (baggage != null && !baggage.isBlank()) {
                    proxy = new ParasoftHeaderInjectingProxy(PROXY_BIND_HOST, 0, baggage);
                    SeleniumCoverageIntegration.configureChromeOptions(opts, proxy);
                }
                driver = new RemoteWebDriver(new URL(gridUrl), opts);
            }
        } else {
            // Local: use local drivers directly
            if ("firefox".equalsIgnoreCase(browser)) {
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (HEADLESS) {
                    firefoxOptions.addArguments("--headless");
                }
                proxy = SeleniumCoverageIntegration.configureFirefoxOptions(firefoxOptions);
                driver = new FirefoxDriver(firefoxOptions);
            } else if ("edge".equalsIgnoreCase(browser)) {
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                if (HEADLESS) {
                    edgeOptions.addArguments("--headless=new");
                }
                proxy = SeleniumCoverageIntegration.configureEdgeOptions(edgeOptions);
                driver = new EdgeDriver(edgeOptions);
            } else {
                // Default: Chrome
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                if (HEADLESS) {
                    chromeOptions.addArguments("--headless=new");
                }
                proxy = SeleniumCoverageIntegration.configureChromeOptions(chromeOptions);
                driver = new ChromeDriver(chromeOptions);
            }
        }
    }
}
