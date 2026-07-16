Feature: PetClinic browser actions

  Scenario: Navigate to home page
    Given the browser is open
    When I navigate to the home page
    Then I should see the PetClinic welcome message

  @pet-name
  Scenario: Edit pet name
    Given the browser is open
    When I navigate to the owners page
    And I select the first owner
    And I edit the pet name to "Lena"
    Then the pet name should be updated to "Lena"

  @pet-name
  Scenario: Edit pet name again
    Given the browser is open
    When I navigate to the owners page
    And I select the first owner
    And I edit the pet name to "Leo"
    Then the pet name should be updated to "Leo"

  Scenario: Navigate to veterinarians page
    Given the browser is open
    When I navigate to the veterinarians page
    Then I should see the list of veterinarians
