package org.springframework.samples.petclinic.playwright.util;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * JUnit 5 extension to integrate Playwright tests with Parasoft CTP via shared testcommon utilities.
 */
public class ParasoftWatcher implements BeforeEachCallback, TestWatcher {

    @Override
    public void beforeEach(ExtensionContext context) {
        String testId = getTestId(context);
        ParasoftCTPApiClient.startTest(testId);
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        String testId = getTestId(context);
        ParasoftCTPApiClient.stopTest(testId, true, null);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testId = getTestId(context);
        ParasoftCTPApiClient.stopTest(testId, false, cause.getMessage());
    }

    private static String getTestId(ExtensionContext context) {
        return context.getTestClass().get().getName() + '#' + context.getTestMethod().get().getName();
    }
}
