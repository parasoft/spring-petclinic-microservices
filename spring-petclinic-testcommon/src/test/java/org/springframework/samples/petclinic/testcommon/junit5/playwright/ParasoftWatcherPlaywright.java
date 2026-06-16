package org.springframework.samples.petclinic.testcommon.junit5.playwright;

import org.springframework.samples.petclinic.testcommon.ParasoftWatcherUtil;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * JUnit 5 extension that reports individual test start and stop events to Parasoft CTP
 * for per-test coverage tracking in Playwright-based tests.
 */
public class ParasoftWatcherPlaywright implements BeforeEachCallback, TestWatcher {
    private static final String WATCHER_TAG = "ParasoftWatcherPlaywright";

    @Override
    public void beforeEach(ExtensionContext context) {
        ParasoftWatcherUtil.startTest(getTestId(context), getTestContextKey(context), WATCHER_TAG);
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        ParasoftWatcherUtil.stopTest(getTestId(context), getTestContextKey(context), true, null, WATCHER_TAG);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        ParasoftWatcherUtil.stopTest(getTestId(context), getTestContextKey(context), false, buildFailureMessage(cause), WATCHER_TAG);
    }

    private static String getTestId(ExtensionContext context) {
        return context.getTestClass().get().getName() + '#' + context.getTestMethod().get().getName();
    }

    private static String getTestContextKey(ExtensionContext context) {
        return context.getTestClass().map(Class::getName).orElse(null);
    }

    private static String buildFailureMessage(Throwable cause) {
        if (cause == null) {
            return null;
        }
        String message = cause.getMessage();
        if (message != null) {
            message = normalizeWhitespace(message);
            int stackIndex = message.indexOf(" stack=");
            if (stackIndex >= 0) {
                message = message.substring(0, stackIndex);
            }
            int atIndex = message.indexOf(" at ");
            if (atIndex >= 0) {
                message = message.substring(0, atIndex);
            }
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

    private static String normalizeWhitespace(String value) {
        String normalized = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        while (normalized.contains("  ")) {
            normalized = normalized.replace("  ", " ");
        }
        return normalized.trim();
    }
}