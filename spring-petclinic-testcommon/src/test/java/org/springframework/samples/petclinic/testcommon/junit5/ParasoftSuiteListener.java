package org.springframework.samples.petclinic.testcommon.junit5;

import org.springframework.samples.petclinic.testcommon.ParasoftSessionManager;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * JUnit 5 {@link TestExecutionListener} that manages Parasoft CTP session lifecycle at the suite level.
 * Starts a coverage session when the test plan begins and stops it when the plan ends,
 * with optional coverage and baseline publishing.
 */
public class ParasoftSuiteListener implements TestExecutionListener {
    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        ParasoftSessionManager.startSession();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
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