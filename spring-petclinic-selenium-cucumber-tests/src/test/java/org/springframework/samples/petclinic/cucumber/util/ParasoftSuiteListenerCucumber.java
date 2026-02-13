/**
 * ParasoftSuiteListener integrates with Parasoft CTP and DTP during test execution.
 * <p>
 * This class implements the Cucumber BeforeAll and AfterAll hooks to automate
 * the following actions:
 * <ul>
 *   <li>Starts a coverage session in Parasoft CTP when the test plan begins</li>
 *   <li>Stops the session and publishes coverage data to Parasoft DTP when the test plan ends</li>
 *   <li>Optionally publishes baseline build information if configured</li>
 * </ul>
 * It handles REST API calls, authentication, and error logging for these operations.
 */
package org.springframework.samples.petclinic.cucumber.util;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

public class ParasoftSuiteListenerCucumber {
    private static String sessionId;

    @BeforeAll
    public static void testPlanExecutionStarted() {
        sessionId = ParasoftCTPApiClient.startSession();
    }

    @AfterAll
    public static void testPlanExecutionFinished() {
        ParasoftCTPApiClient.stopSession();
        
        if (sessionId != null) {
            ParasoftCTPApiClient.publishCoverage(sessionId);
        }
    }
}
