package org.springframework.samples.petclinic.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.After;
import org.junit.jupiter.api.Assertions;

import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.logging.Logger;
import java.time.Duration;

import org.littleshoot.proxy.HttpProxyServer;
import org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;
import org.springframework.samples.petclinic.cucumber.util.ParasoftSeleniumContext;

public class PetClinicSteps {
	private static final Logger LOGGER = Logger.getLogger(PetClinicSteps.class.getName());
	private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");    
    
    private WebDriver driver;
    private HttpProxyServer proxy;
    private WebDriverWait wait;

    @Given("the browser is open")
    public void the_browser_is_open() {
        ChromeOptions options = new ChromeOptions();

        // Using LittleProxy for request header injection, required for Parasoft
        // coverage reporting when agents are in multi-user mode
        if (ParasoftSettings.isMultiUserMode()) {
            proxy = ParasoftHeaderInjectingProxy.startProxy(ParasoftSeleniumContext.getCoverageUserIdRef());
            int proxyPort = proxy.getListenAddress().getPort();

            Proxy seleniumProxy = new Proxy();
            seleniumProxy.setHttpProxy(ParasoftSettings.PROXY_HOST + ":" + proxyPort);
            seleniumProxy.setSslProxy(ParasoftSettings.PROXY_HOST + ":" + proxyPort);
            seleniumProxy.setNoProxy("<-loopback>");

            options.setProxy(seleniumProxy);
        }
        if (ParasoftSettings.isHeadless()) {
            options.addArguments("--headless=new");
        }
        // Calling code to retrieve the Selenium Grid node ID to dynamically set the CTP coverage user ID
		if (ParasoftSettings.isSeleniumGrid()) {
			String gridUrl = ParasoftSettings.SELENIUM_GRID_URL;
			try {
				driver = new RemoteWebDriver(new URL(gridUrl), options, false);
				if (ParasoftSettings.CTP_DEBUG) {
					LOGGER.info("[NavigateIT] Using Selenium Grid at " + gridUrl);
				}
			} catch (MalformedURLException me) {
				throw new RuntimeException("Failed to connect to Selenium Grid at " + gridUrl, me);
			} catch (Exception e) {
				throw new RuntimeException("General failure to initialize RemoteWebDriver for Selenium Grid at " + gridUrl, e);
			}
		} else {
			driver = new ChromeDriver(options);
		}

        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @When("I navigate to the home page")
    public void i_navigate_to_home_page() {
        openHomePage();
    }

    @After
    public void cleanup() {
        if (driver != null) {
            driver.quit();
        }
        if (proxy != null) {
            proxy.stop();
        }
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
