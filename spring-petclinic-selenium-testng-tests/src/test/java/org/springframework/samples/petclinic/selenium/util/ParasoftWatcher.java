/**
 * ParasoftWatcher integrates JUnit test execution with Parasoft CTP for test-level coverage tracking.
 * <p>
 * Implements BeforeEachCallback and TestWatcher to:
 * <ul>
 *   <li>Tell the CTP coverage agents when a test is starting</li>
 *   <li>Tell the CTP coverage agents when a test has passed or failed</li>
 * </ul>
 * Handles authentication, error logging, and multi-user mode support.
 */

package org.springframework.samples.petclinic.selenium.util;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class ParasoftWatcher implements ITestListener {
    @Override
    public void onTestStart(ITestResult result) {
        String testId = getTestId(result);
        ParasoftCTPApiClient.startTest(testId);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testId = getTestId(result);
        ParasoftCTPApiClient.stopTest(testId, true, null);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testId = getTestId(result);
        ParasoftCTPApiClient.stopTest(testId, false, result.getThrowable() != null ? result.getThrowable().getMessage() : null);
    }

    private static String getTestId(ITestResult result) {
        return result.getTestClass().getName() + '#' + result.getMethod().getMethodName();
    }
}
