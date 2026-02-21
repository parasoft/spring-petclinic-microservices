/**
 * ParasoftWatcher integrates TestNG execution with Parasoft CTP for test-level coverage tracking.
 * <p>
 * Implements TestNG listener callbacks to:
 * <ul>
 *   <li>Tell the CTP coverage agents when a test is starting</li>
 *   <li>Tell the CTP coverage agents when a test has passed or failed</li>
 * </ul>
 * Handles authentication, error logging, and multi-user mode support.
 */

package org.springframework.samples.petclinic.selenium.testng.util;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class ParasoftWatcher implements ITestListener {
    @Override
    public void onTestStart(ITestResult result) {
        String testId = getTestId(result);
        // When in multi-user mode, the coverage user ID is required to associate the test with the correct CTP test session for coverage reporting
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.startTest(testId, ParasoftSeleniumContext.getCoverageUserId());
            return;
        }
        ParasoftCTPApiClient.startTest(testId); // if not running in multi-user mode, the coverage user ID is not needed
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testId = getTestId(result);
        // When in multi-user mode, the coverage user ID is required to associate the test with the correct CTP test session for coverage reporting
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopTest(testId, true, null, ParasoftSeleniumContext.getCoverageUserId());
            return;
        }
        ParasoftCTPApiClient.stopTest(testId, true, null); // if not running in multi-user mode, the coverage user ID is not needed
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testId = getTestId(result);
        // When in multi-user mode, the coverage user ID is required to associate the test with the correct CTP test session for coverage reporting
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopTest(testId, false,
                    buildFailureMessage(result.getThrowable()),
                    ParasoftSeleniumContext.getCoverageUserId());
            return;
        }
        ParasoftCTPApiClient.stopTest(testId, false, buildFailureMessage(result.getThrowable())); // if not running in multi-user mode, the coverage user ID is not needed
    }

    private static String getTestId(ITestResult result) {
        return result.getTestClass().getName() + '#' + result.getMethod().getMethodName();
    }

    private static String buildFailureMessage(Throwable cause) {
        if (cause == null) {
            return null;
        }
        String message = cause.getMessage();
        if (message != null) {
            int newlineIndex = message.indexOf('\n');
            if (newlineIndex >= 0) {
                message = message.substring(0, newlineIndex);
            }
            message = message.replace('\r', ' ');
        }
        StringBuilder summary = new StringBuilder();
        summary.append(cause.getClass().getSimpleName());
        if (message != null && !message.isBlank()) {
            summary.append(": ").append(message);
        }
        String sanitized = summary.toString()
                .replace('"', '\'')
                .replace("\n", " ")
                .replace("\t", " ");
        int maxLength = 500;
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}
