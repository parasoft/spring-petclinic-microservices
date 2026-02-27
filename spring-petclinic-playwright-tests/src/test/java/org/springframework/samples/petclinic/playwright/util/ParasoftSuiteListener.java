/**
 * ParasoftSuiteListener integrates with Parasoft CTP and DTP during Playwright test execution.
 */
package org.springframework.samples.petclinic.playwright.util;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class ParasoftSuiteListener implements TestExecutionListener {
    private String sessionId;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        // This module uses a fixed coverage user ID per suite.
        ParasoftPlaywrightContext.initForSuite();
        if (ParasoftSettings.isMultiUserMode()) {
            sessionId = ParasoftCTPApiClient.startSession(ParasoftPlaywrightContext.getCoverageUserId());
        } else {
            sessionId = ParasoftCTPApiClient.startSession();
        }
        ParasoftPlaywrightContext.setCtpSessionId(sessionId);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopSession(ParasoftPlaywrightContext.getCoverageUserId());
            if (sessionId != null && !sessionId.isBlank() && ParasoftSettings.CTP_PUBLISH_COVERAGE) {
                ParasoftCTPApiClient.publishCoverage(sessionId, ParasoftPlaywrightContext.getDtpSessionTag(),
                        ParasoftPlaywrightContext.getCoverageUserId());
            }
            return;
        }
        ParasoftCTPApiClient.stopSession();

        if (sessionId != null && !sessionId.isBlank()) {
            ParasoftCTPApiClient.publishCoverage(sessionId, ParasoftPlaywrightContext.getDtpSessionTag());
        }
    }
}
