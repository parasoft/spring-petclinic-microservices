package org.springframework.samples.petclinic.selenium.tests;

import java.net.URL;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import com.parasoft.coverage.integration.api.CoverageIntegration;
import com.parasoft.coverage.integration.proxy.ParasoftHeaderInjectingProxy;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

public class ParameterizedPetIT {

    private static final String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099/");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("org.springframework.samples.petclinic.headless", "false"));
    private static final String PROXY_BIND_HOST = System.getProperty("PROXY_BIND_HOST", "127.0.0.1");

    private WebDriver driver;
    private ParasoftHeaderInjectingProxy proxy;

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
        if (proxy != null) {
            proxy.close();
            proxy = null;
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"chrome", "edge", "firefox"})
    public void testRenamePet(String browser) throws Exception {
        createDriver(browser);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
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
            if ("firefox".equalsIgnoreCase(browser)) {
                FirefoxOptions opts = new FirefoxOptions();
                if (HEADLESS) {
                    opts.addArguments("--headless");
                }
                proxy = SeleniumCoverageIntegration.configureFirefoxOptions(opts);
                driver = new FirefoxDriver(opts);
            } else if ("edge".equalsIgnoreCase(browser)) {
                EdgeOptions opts = new EdgeOptions();
                opts.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                if (HEADLESS) {
                    opts.addArguments("--headless=new");
                }
                proxy = SeleniumCoverageIntegration.configureEdgeOptions(opts);
                driver = new EdgeDriver(opts);
            } else {
                ChromeOptions opts = new ChromeOptions();
                opts.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                if (HEADLESS) {
                    opts.addArguments("--headless=new");
                }
                proxy = SeleniumCoverageIntegration.configureChromeOptions(opts);
                driver = new ChromeDriver(opts);
            }
        }
    }
}
