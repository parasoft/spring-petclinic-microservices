package org.springframework.samples.petclinic.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.openqa.selenium.By;
import org.junit.jupiter.api.Assertions;

public class PetClinicSteps {
    private final TestContext context;

    public PetClinicSteps(TestContext context) {
        this.context = context;
    }
    @Given("the browser is open")
    public void the_browser_is_open() {
        // ChromeDriver is initialized in ParasoftWatcherCucumber.beforeScenario()
        Assertions.assertNotNull(context.getDriver());
    }

    @When("I navigate to the home page")
    public void i_navigate_to_home_page() {
        context.getDriver().get("http://localhost:8080/");
    }

    @Then("I should see the PetClinic welcome message")
    public void i_should_see_welcome_message() {
        Assertions.assertTrue(context.getDriver().getPageSource().contains("Welcome"));
    }

    @When("I navigate to the owners page")
    public void i_navigate_to_owners_page() {
        context.getDriver().findElement(By.xpath("//a[@ui-sref='owners']")).click();
    }

    @When("I select the first owner")
    public void i_select_first_owner() {
        context.getDriver().findElement(By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a")).click();
    }

    @When("I edit the pet name to {string}")
    public void i_edit_pet_name(String name) {
        context.getDriver().findElement(By.xpath("//dd/a")).click();
        context.getDriver().findElement(By.name("name")).clear();
        context.getDriver().findElement(By.name("name")).sendKeys(name);
        context.getDriver().findElement(By.xpath("//button[@type='submit']")).click();
    }

    @Then("the pet name should be updated to {string}")
    public void pet_name_should_be_updated(String name) {
        Assertions.assertTrue(context.getDriver().getPageSource().contains(name));
    }

    @When("I navigate to the veterinarians page")
    public void i_navigate_to_vets_page() {
        context.getDriver().findElement(By.xpath("//a[@title='veterinarians']")).click();
    }

    @Then("I should see the list of veterinarians")
    public void i_should_see_vets_list() {
        Assertions.assertTrue(context.getDriver().getPageSource().contains("Veterinarians"));
    }
}
