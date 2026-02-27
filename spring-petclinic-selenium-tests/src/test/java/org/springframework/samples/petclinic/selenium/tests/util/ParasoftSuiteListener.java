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
package org.springframework.samples.petclinic.selenium.tests.util;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class ParasoftSuiteListener implements TestExecutionListener {
    private String sessionId;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        ParasoftSettings.setTestFramework("seleniumJUnit");
        // This module uses a fixed coverage user ID per suite.
        ParasoftSeleniumContext.initForSuite();
        if (ParasoftSettings.isMultiUserMode()) {
            sessionId = ParasoftCTPApiClient.startSession(ParasoftSeleniumContext.getCoverageUserId());
        } else {
            sessionId = ParasoftCTPApiClient.startSession();
        }
        ParasoftSeleniumContext.setCtpSessionId(sessionId);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopSession(ParasoftSeleniumContext.getCoverageUserId());
            if (sessionId != null && !sessionId.isBlank() && ParasoftSettings.CTP_PUBLISH_COVERAGE) {
                ParasoftCTPApiClient.publishCoverage(sessionId, ParasoftSeleniumContext.getDtpSessionTag(),
                        ParasoftSeleniumContext.getCoverageUserId());
            }
            return;
        }
        ParasoftCTPApiClient.stopSession();
        if (sessionId != null && !sessionId.isBlank()) {
            ParasoftCTPApiClient.publishCoverage(sessionId, ParasoftSeleniumContext.getDtpSessionTag());
        }
    }
}
