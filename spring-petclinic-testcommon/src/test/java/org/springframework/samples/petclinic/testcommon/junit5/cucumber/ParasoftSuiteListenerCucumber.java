package org.springframework.samples.petclinic.testcommon.junit5.cucumber;

import org.springframework.samples.petclinic.testcommon.ParasoftSessionManager;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

/**
 * Cucumber lifecycle hook that manages Parasoft CTP session lifecycle at the suite level.
 * <p>
 * This is the Cucumber equivalent of
 * {@link org.springframework.samples.petclinic.testcommon.junit5.ParasoftSuiteListener ParasoftSuiteListener}.
 */
public class ParasoftSuiteListenerCucumber {
    @BeforeAll
    public static void testPlanExecutionStarted() {
        ParasoftSessionManager.startSession();
    }

    @AfterAll
    public static void testPlanExecutionFinished() {
        ParasoftSessionManager.stopSession();
        // Trigger CTP to publish coverage if CTP_PUBLISH_COVERAGE is set to true
        if (ParasoftSettings.CTP_PUBLISH_COVERAGE) {
            ParasoftSessionManager.publishCoverageAtSuiteEnd();
        }
        // Trigger CTP to publish baseline if CTP_PUBLISH_BASELINE is set to true
        if (ParasoftSettings.CTP_PUBLISH_BASELINE) {
            ParasoftSessionManager.publishBaselineAtSuiteEnd();
        }
    }
}