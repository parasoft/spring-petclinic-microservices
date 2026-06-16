package org.springframework.samples.petclinic.testcommon.testng;

import org.springframework.samples.petclinic.testcommon.ParasoftWatcherUtil;

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
    private static final String WATCHER_TAG = "ParasoftWatcherTestNG";

    @Override
    public void onTestStart(ITestResult result) {
        ParasoftWatcherUtil.startTest(getTestId(result), result.getTestClass().getName(), WATCHER_TAG);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ParasoftWatcherUtil.stopTest(getTestId(result), result.getTestClass().getName(), true, null, WATCHER_TAG);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ParasoftWatcherUtil.stopTest(getTestId(result), result.getTestClass().getName(), false, buildFailureMessage(result.getThrowable()), WATCHER_TAG);
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