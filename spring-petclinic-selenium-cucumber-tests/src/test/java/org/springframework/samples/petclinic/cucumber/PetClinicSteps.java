package org.springframework.samples.petclinic.cucumber;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

import com.parasoft.coverage.integration.api.CoverageIntegration;
import com.parasoft.coverage.integration.proxy.ParasoftHeaderInjectingProxy;
import com.parasoft.coverage.integration.selenium.SeleniumCoverageIntegration;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.junit.jupiter.api.Assertions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class PetClinicSteps {
    private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("org.springframework.samples.petclinic.headless", "false"));
    private static final String SELENIUM_GRID_URL = System.getProperty("SELENIUM_GRID_URL", "");
    private static final String PROXY_BIND_HOST = System.getProperty("PROXY_BIND_HOST", "127.0.0.1");

    private WebDriver driver;
    private WebDriverWait wait;
    private ParasoftHeaderInjectingProxy coverageProxy;

    @Given("the browser is open")
    public void the_browser_is_open() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        if (HEADLESS) {
            options.addArguments("--headless=new");
        }

        if (SELENIUM_GRID_URL.isEmpty()) {
            coverageProxy = SeleniumCoverageIntegration.configureChromeOptions(options);
            driver = new ChromeDriver(options);
        } else {
            String baggageHeader = CoverageIntegration.getBaggageHeader();
            if (baggageHeader != null && !baggageHeader.isBlank()) {
                // proxy must bind to a routable address when the browser runs in a separate container
                coverageProxy = new ParasoftHeaderInjectingProxy(PROXY_BIND_HOST, 0, baggageHeader);
                SeleniumCoverageIntegration.configureChromeOptions(options, coverageProxy);
            }
            try {
                driver = new RemoteWebDriver(URI.create(SELENIUM_GRID_URL).toURL(), options);
            } catch (IllegalArgumentException | MalformedURLException e) {
                throw new WebDriverException("Invalid SELENIUM_GRID_URL: " + SELENIUM_GRID_URL, e);
            }
        }

        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @After
    public void cleanup() {
        try {
            if (driver != null) {
                driver.quit();
            }
        } finally {
            driver = null;
            wait = null;

            if (coverageProxy != null) {
                coverageProxy.close();
                coverageProxy = null;
            }
        }
    }

    @When("I navigate to the home page")
    public void i_navigate_to_home_page() {
        openHomePage();
    }

    @Then("I should see the PetClinic welcome message")
    public void i_should_see_welcome_message() {
        Assertions.assertTrue(driver.getPageSource().contains("Welcome"));
    }

    @When("I navigate to the owners page")
    public void i_navigate_to_owners_page() {
        openHomePage();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@class='dropdown-toggle']"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@ui-sref='owners']"))).click();
    }

    @When("I select the first owner")
    public void i_select_first_owner() {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a"))).click();
    }

    @When("I edit the pet name to {string}")
    public void i_edit_pet_name(String name) {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//dd/a"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name"))).clear();
        driver.findElement(By.name("name")).sendKeys(name);
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type='submit']"))).click();
    }

    @Then("the pet name should be updated to {string}")
    public void pet_name_should_be_updated(String name) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), name));
        Assertions.assertTrue(driver.getPageSource().contains(name));
    }

    @When("I navigate to the veterinarians page")
    public void i_navigate_to_vets_page() {
        openHomePage();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@title='veterinarians']"))).click();
    }

    @Then("I should see the list of veterinarians")
    public void i_should_see_vets_list() {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Veterinarians"));
        Assertions.assertTrue(driver.getPageSource().contains("Veterinarians"));
    }

    private void openHomePage() {
        driver.get(PETCLINIC_URL);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Welcome"));
    }
}
