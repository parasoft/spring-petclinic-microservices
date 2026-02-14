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
        sessionId = ParasoftCTPApiClient.startSession();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        ParasoftCTPApiClient.stopSession();

        if (sessionId != null) {
            ParasoftCTPApiClient.publishCoverage(sessionId);
        }
    }
}
