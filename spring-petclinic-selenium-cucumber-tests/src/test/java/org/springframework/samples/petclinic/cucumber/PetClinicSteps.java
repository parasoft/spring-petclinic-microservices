package org.springframework.samples.petclinic.cucumber;

import java.time.Duration;

import org.springframework.samples.petclinic.testcommon.junit5.cucumber.ParasoftCucumberUtil;
import org.springframework.samples.petclinic.testcommon.selenium.BasicWebDriverConfigurator;
import org.springframework.samples.petclinic.testcommon.selenium.BrowserType;
import org.springframework.samples.petclinic.testcommon.selenium.ParasoftWebDriverConfigurator;
import org.springframework.samples.petclinic.testcommon.selenium.ParasoftWebDriverResource;
import org.springframework.samples.petclinic.testcommon.selenium.WebDriverFactory;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.junit.jupiter.api.Assertions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class PetClinicSteps {
	private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");    
    
    private static ParasoftWebDriverResource driverResource;
    private static WebDriver driver;
    private WebDriverWait wait;
    private Scenario scenario;

    @Before
    public void beforeScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    @Given("the browser is open")
    public void the_browser_is_open() {
        driverResource = WebDriverFactory.create(
                BrowserType.CHROME,
                new BasicWebDriverConfigurator(),
                new ParasoftWebDriverConfigurator(ParasoftCucumberUtil.getTestId(scenario)));
        driver = driverResource.getDriver();

        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @When("I navigate to the home page")
    public void i_navigate_to_home_page() {
        openHomePage();
    }

    @After
    public void cleanup() {
        if (driverResource != null) {
            driverResource.close();
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
