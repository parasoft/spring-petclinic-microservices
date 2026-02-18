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
        ParasoftSettings.setTestFramework("playwrightJUnit");
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
            if (sessionId != null && !sessionId.isBlank()) {
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
