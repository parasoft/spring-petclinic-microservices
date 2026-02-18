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
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class ParasoftSuiteListener implements TestExecutionListener {
    private String ctpTestSessionId; // only used by the suite listener when not running in multi-user mode
    private String dtpSessionTag = "spring-petclinic-selenium-parallel-tests"; // only used by the suite listener when not running in multi-user mode

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        ParasoftSettings.setTestFramework("seleniumJUnit");
		
        // If the CTP coverage agents are in multi-user mode, then parallel test execution must be handled at the
        // test level (i.e., WebDriver instance).  However, for sequential test execution where the coverage agents
        // are not in multi-user mode, a single CTP test session can be created for the entire suite here in the suite listener
        if (!ParasoftSettings.isMultiUserMode()) {
			ctpTestSessionId = ParasoftCTPApiClient.startSession();
		}
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        // If running the coverage agents in multi-user mode and running tests in parallel, CTP test sessions are
        // created for each web driver instance.  In this case, the suite listener must retrieve all of the test
        // sessions that were created during parallel test execution so that coverage can be published for each session
        // at the end of the test run.  This avoids delays between each test class where coverage data is published, and
        // pushes coverage publishing for all test sessions to the end of the suite execution.
        if (ParasoftSettings.isMultiUserMode()) {
            for (var session : ParasoftTestSessionRegistry.getSessionInfos()) {
                if (session.ctpTestSessionId == null || session.ctpTestSessionId.isBlank()) {
                    continue;
                }
                ParasoftCTPApiClient.publishCoverage(session.ctpTestSessionId, session.dtpSessionTag, session.coverageUserId);
            }
            return;
        }

        // If not running in multi-user mode, there will be a single CTP test session for the entire suite that can be
        //  stopped and published here.
        if (ctpTestSessionId != null && !ctpTestSessionId.isBlank()) {
            ParasoftCTPApiClient.stopSession();
            ParasoftCTPApiClient.publishCoverage(ctpTestSessionId, dtpSessionTag);
        }
    }
}
