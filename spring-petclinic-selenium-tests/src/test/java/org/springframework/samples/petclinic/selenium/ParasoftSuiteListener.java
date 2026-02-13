/**
 * ParasoftSuiteListener integrates with Parasoft CTP and DTP during test execution.
 * <p>
 * This class implements the JUnit Platform TestExecutionListener interface to automate
 * the following actions:
 * <ul>
 *   <li>Starts a coverage session in Parasoft CTP when the test plan begins</li>
 *   <li>Stops the session and publishes coverage data to Parasoft DTP when the test plan ends</li>
 *   <li>Optionally publishes baseline build information if configured</li>
 * </ul>
 * It handles REST API calls, authentication, and error logging for these operations.
 */
package org.springframework.samples.petclinic.selenium;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class ParasoftSuiteListener implements TestExecutionListener {
    private String sessionId;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        // Start the CTP test session and store sessionId
        sessionId = ParasoftCTPApiClient.startSession();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        // Stop the CTP test session
        ParasoftCTPApiClient.stopSession();
        // Publish coverage data to DTP
        if (sessionId != null) {
            ParasoftCTPApiClient.publishCoverage(sessionId);
        }
        // Publish baseline if enabled
        ParasoftCTPApiClient.publishBaseline();
    }
}
