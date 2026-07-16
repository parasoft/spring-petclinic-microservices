package org.springframework.samples.petclinic.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@Suite
@SelectClasspathResource("features/petclinic.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.springframework.samples.petclinic.cucumber,com.parasoft.coverage.integration.cucumber")
@ConfigurationParameter(
        key = "cucumber.execution.exclusive-resources.pet-name.read-write",
        value = "petclinic.pet-name")
public class RunCucumberIT {
}
