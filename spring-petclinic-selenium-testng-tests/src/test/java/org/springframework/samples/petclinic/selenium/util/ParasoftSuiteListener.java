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

package org.springframework.samples.petclinic.selenium.util;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.testng.ISuite;
import org.testng.ISuiteListener;

public class ParasoftSuiteListener implements ISuiteListener {
    private String sessionId;

    @Override
    public void onStart(ISuite suite) {
        sessionId = ParasoftCTPApiClient.startSession();
    }

    @Override
    public void onFinish(ISuite suite) {
        ParasoftCTPApiClient.stopSession();
        if (sessionId != null) {
            ParasoftCTPApiClient.publishCoverage(sessionId);
        }
    }
}
