package org.springframework.samples.petclinic.testcommon.junit5;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.springframework.samples.petclinic.testcommon.ParasoftSessionManager;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * JUnit 5 extension that reports individual test start and stop events to Parasoft CTP
 * for per-test coverage tracking.
 */
public class ParasoftWatcher implements BeforeEachCallback, TestWatcher  {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String testId = getTestId(context);

        // If multi-user mode, the coverage user ID is retrieved from the Session Manager
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.startTest(testId, getCoverageUserId(context));
            return;
        }
        ParasoftCTPApiClient.startTest(testId); // if not running in multi-user mode, the coverage user ID is not needed
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        String testId = getTestId(context);

        // If multi-user mode, the coverage user ID is retrieved from the Session Manager
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopTest(testId, true, null, getCoverageUserId(context));
            return;
        }
        ParasoftCTPApiClient.stopTest(testId, true, null); // if not running in multi-user mode, the coverage user ID is not needed
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testId = getTestId(context);

        // If multi-user mode, the coverage user ID is retrieved from the Session Manager
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopTest(testId, false, buildFailureMessage(cause), getCoverageUserId(context));
            return;
        }
        ParasoftCTPApiClient.stopTest(testId, false, buildFailureMessage(cause)); // if not running in multi-user mode, the coverage user ID is not needed
    }

    private static String getTestId(ExtensionContext context) {
        return context.getTestClass().get().getName() + '#' + context.getTestMethod().get().getName();
    }

    private static String getCoverageUserId(ExtensionContext context) {
        String testClassName = context.getTestClass().map(Class::getName).orElse(null);
        String coverageUserId = ParasoftSessionManager.getCoverageUserId(testClassName); // retrieve the coverage user ID for this test class from the Session Manager Map, which was set when the WebDriver instance was created

        return coverageUserId;
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