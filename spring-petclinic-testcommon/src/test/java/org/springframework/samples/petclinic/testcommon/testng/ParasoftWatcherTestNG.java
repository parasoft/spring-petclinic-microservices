package org.springframework.samples.petclinic.testcommon.testng;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.springframework.samples.petclinic.testcommon.ParasoftSessionManager;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG test listener that reports individual test start and stop events to Parasoft CTP
 * for per-test coverage tracking.
 * <p>
 * This is the TestNG equivalent of
 * {@link org.springframework.samples.petclinic.testcommon.junit5.ParasoftWatcher ParasoftWatcher}.
 */
public class ParasoftWatcherTestNG implements ITestListener {
    @Override
    public void onTestStart(ITestResult result) {
        String testId = getTestId(result);

        // If multi-user mode, the coverage user ID is retrieved from the Session Manager
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.startTest(testId, getCoverageUserId(result));
            return;
        }
        ParasoftCTPApiClient.startTest(testId); // if not running in multi-user mode, the coverage user ID is not needed
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testId = getTestId(result);

        // If multi-user mode, the coverage user ID is retrieved from the Session Manager
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopTest(testId, true, null, getCoverageUserId(result));
            return;
        }
        ParasoftCTPApiClient.stopTest(testId, true, null); // if not running in multi-user mode, the coverage user ID is not needed
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testId = getTestId(result);

        // If multi-user mode, the coverage user ID is retrieved from the Session Manager
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopTest(testId, false, buildFailureMessage(result.getThrowable()), getCoverageUserId(result));
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

    private static String getCoverageUserId(ITestResult result) {
        String testClassName = result.getTestClass().getName();
        String coverageUserId = ParasoftSessionManager.getCoverageUserId(testClassName); // retrieve the coverage user ID for this test class from the Session Manager Map, which was set when the WebDriver instance was created

        return coverageUserId;
    }
}