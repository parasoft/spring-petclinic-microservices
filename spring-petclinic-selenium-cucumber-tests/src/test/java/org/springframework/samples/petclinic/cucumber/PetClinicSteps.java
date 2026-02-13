package org.springframework.samples.petclinic.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.After;
import org.junit.jupiter.api.Assertions;

import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.littleshoot.proxy.HttpProxyServer;
import org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy;

public class PetClinicSteps {
    private ChromeDriver driver;
    private HttpProxyServer proxy;

    @Given("the browser is open")
    public void the_browser_is_open() {
        // Start proxy for header injection
        proxy = ParasoftHeaderInjectingProxy.startProxy();
        int proxyPort = proxy.getListenAddress().getPort();

        Proxy seleniumProxy = new Proxy();
        seleniumProxy.setHttpProxy("localhost:" + proxyPort);
        seleniumProxy.setSslProxy("localhost:" + proxyPort);
        seleniumProxy.setNoProxy("<-loopback>");

        ChromeOptions options = new ChromeOptions();
        options.setProxy(seleniumProxy);
        
        driver = new ChromeDriver(options);
    }

    @When("I navigate to the home page")
    public void i_navigate_to_home_page() {
        driver.get("http://localhost:8099/");
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
        driver.findElement(By.xpath("//a[@ui-sref='owners']")).click();
    }

    @When("I select the first owner")
    public void i_select_first_owner() {
        driver.findElement(By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a")).click();
    }

    @When("I edit the pet name to {string}")
    public void i_edit_pet_name(String name) {
        driver.findElement(By.xpath("//dd/a")).click();
        driver.findElement(By.name("name")).clear();
        driver.findElement(By.name("name")).sendKeys(name);
        driver.findElement(By.xpath("//button[@type='submit']")).click();
    }

    @Then("the pet name should be updated to {string}")
    public void pet_name_should_be_updated(String name) {
        Assertions.assertTrue(driver.getPageSource().contains(name));
    }

    @When("I navigate to the veterinarians page")
    public void i_navigate_to_vets_page() {
        driver.findElement(By.xpath("//a[@title='veterinarians']")).click();
    }

    @Then("I should see the list of veterinarians")
    public void i_should_see_vets_list() {
        Assertions.assertTrue(driver.getPageSource().contains("Veterinarians"));
    }
}
