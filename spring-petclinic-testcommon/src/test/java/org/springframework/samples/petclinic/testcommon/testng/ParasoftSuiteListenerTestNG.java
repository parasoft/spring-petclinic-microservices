package org.springframework.samples.petclinic.testcommon.testng;

import org.springframework.samples.petclinic.testcommon.ParasoftSessionManager;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * TestNG suite listener that manages Parasoft CTP session lifecycle at the suite level.
 * <p>
 * This is the TestNG equivalent of
 * {@link org.springframework.samples.petclinic.testcommon.junit5.ParasoftSuiteListener ParasoftSuiteListener}.
 */
public class ParasoftSuiteListenerTestNG implements ISuiteListener {
    @Override
    public void onStart(ISuite suite) {
        // Start session at suite level if sequential test execution
        if (!ParasoftSettings.isParallelTestExecution()) {
            ParasoftSessionManager.startSession();
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        // Stop session at suite level if sequential test execution
        if (!ParasoftSettings.isParallelTestExecution()) {
            ParasoftSessionManager.stopSession(null);
        }
        // Triger CTP to publish coverage if CTP_PUBLISH_COVERAGE is set to true
        if (ParasoftSettings.CTP_PUBLISH_COVERAGE) {
            ParasoftSessionManager.publishCoverageAtSuiteEnd();
        }
        // Trigger CTP to publish baseline if CTP_PUBLISH_BASELINE is set to true
        // Uses CTP_BAESELINE_BUILD_ID
        if (ParasoftSettings.CTP_PUBLISH_BASELINE) {
            ParasoftSessionManager.publishBaselineAtSuiteEnd();
        }
    }
}