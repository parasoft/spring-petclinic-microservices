# Playwright Web Functional Tests with CTP

In order to run the tests, CTP must be running and configured for both communicating with DTP and for collecting coverage on a running petclinic application with agents properly configured.

To run all tests, use the command
```
mvn test -DenvId=<CTP ENVIRONMENT ID> -DctpBaseUrl=<CTP BASE URL> -DuserId=<TEST USER ID> -DpetclinicUrl=<PETCLINIC URL>
```
or if already built,
```
mvn clean install -DenvId=<CTP ENVIRONMENT ID> -DctpBaseUrl=<CTP BASE URL> -DuserId=<TEST USER ID> -DpetclinicUrl=<PETCLINIC URL>
```
in this current directory.

By default, petclinicUrl will be set to http://localhost:8080 if not provided.

If you want to run the tests in headless mode so the browser does not become visible during test execution, add the option `-Dheadless=true`.

## Test Impact Analysis

The test `testPetClinicNavigation` in the `NavigateTest` class does not run code in the `PetRequest` class in the customers-service project. Modifying the `PetRequest` class (i.e. adding a new field) and creating a new build in DTP and then examining impacted tests via CTP will list only those tests who are impacted by the changes in `PetRequest`; `testPetClinicNavigation` will be excluded from the list. 

To see how Test Impact Analysis works using these tests, take the following steps:

1. Set up DTP to receive coverage from CTP. Configure a CTP environment so that it matches your setup on DTP.
2. Upload static coverage for the petclinic to DTP.
3. Run the Playwright tests. This will publish runtime coverage data to DTP through the CTP REST API. 
4. Using the CTP REST API, create an environment baseline from the used environment.
5. Modify a portion of the petclinic code that will be detected on only some of the tests, rebuild, and then publish new static coverage as a new build to DTP.
6. Invoke the Playwright tests using the `impacted` command line argument set to true and the `baselineBuildId` set to the ID of the baseline created in step 4. Only those tests impacted by code changes will be executed.
    ```
    mvn clean install -DenvId=<CTP ENVIRONMENT ID> -DctpBaseUrl=<CTP BASE URL> -DuserId=<TEST USER ID> -DpetclinicUrl=<PETCLINIC URL> -Dimpacted=true -DbaselineBuildId=<ENVIRONMENT BASELINE BUILD ID>
    ```