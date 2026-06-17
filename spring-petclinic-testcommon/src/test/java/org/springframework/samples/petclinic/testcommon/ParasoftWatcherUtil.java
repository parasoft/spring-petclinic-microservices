package org.springframework.samples.petclinic.testcommon;

import java.util.logging.Logger;

/**
 * Shared logic for the per-test watchers (JUnit 5, JUnit 5 + Playwright, TestNG, Cucumber).
 * Centralizes the multi-user / parallel branching so individual watcher classes can stay thin
 * adapters that only translate framework-specific events into {@code testId},
 * {@code testContextKey}, {@code passed}, and {@code failureMessage}.
 */
public final class ParasoftWatcherUtil {
    private static final Logger LOGGER = Logger.getLogger(ParasoftWatcherUtil.class.getName());

    private ParasoftWatcherUtil() { }

    /**
     * Reports a test-started event to CTP using the appropriate API variant for the current
     * single-user / multi-user / parallel mode configuration. Updates the session manager with
     * the resulting baggage value when applicable.
     *
     * @param testId          fully-qualified test identifier (e.g. {@code ClassName#methodName})
     * @param testContextKey  key used to look up the per-context parallelId / proxy baggage ref
     *                        (typically the test class name; for Cucumber this is the scenario ID)
     * @param watcherTag      short label included in diagnostic log messages to identify the
     *                        calling watcher (e.g. {@code "ParasoftWatcher"})
     */
    public static void startTest(String testId, String testContextKey, String watcherTag) {
        if (ParasoftSettings.isMultiUserMode()) {
            String baggage;
            if (ParasoftSettings.isParallelTestExecution()) {
                // Tests running in parallel must include a baggage header containing userId+parallelId
                String parallelId = ParasoftSessionManager.getParallelId(testContextKey);
                if (parallelId == null && ParasoftSettings.isLogLevelEnabled("WARN")) {
                    LOGGER.warning("[ParasoftWatcherUtil] [" + watcherTag + "] [" + testContextKey + "] startTest: parallelId is null in parallel mode");
                }
                baggage = ParasoftCTPApiClient.startTest(testId, ParasoftSessionManager.getUserId(), parallelId);
            } else {
                // Coverage agents in multi-user mode require passing a baggage header containing userId
                baggage = ParasoftCTPApiClient.startTest(testId, ParasoftSessionManager.getUserId());
            }
            ParasoftSessionManager.updateBaggage(testContextKey, baggage);
        } else {
            // Coverage agents in single-user mode do not require passing baggage headers with the test
            ParasoftCTPApiClient.startTest(testId);
        }
    }

    /**
     * Reports a test-stopped event to CTP using the appropriate API variant for the current
     * single-user / multi-user / parallel mode configuration, then resets the per-context
     * baggage to the sentinel value.
     *
     * @param testId           fully-qualified test identifier (e.g. {@code ClassName#methodName})
     * @param testContextKey   key used to look up the per-context parallelId / proxy baggage ref
     * @param passed           {@code true} if the test passed; {@code false} if it failed
     * @param failureMessage   sanitized failure summary to attach when {@code passed} is false;
     *                         ignored when {@code passed} is true (callers should pass {@code null})
     * @param watcherTag       short label included in diagnostic log messages to identify the
     *                         calling watcher (currently unused but reserved for parity with
     *                         {@link #startTest(String, String, String)})
     */
    public static void stopTest(String testId, String testContextKey, boolean passed, String failureMessage, String watcherTag) {
        if (ParasoftSettings.isMultiUserMode()) {
            if (ParasoftSettings.isParallelTestExecution()) {
                // userId+parallelId is required to stop a test when running in multi-user mode with parallel execution
                ParasoftCTPApiClient.stopTest(testId, passed, failureMessage,
                        ParasoftSessionManager.getUserId(), ParasoftSessionManager.getParallelId(testContextKey));
            } else {
                // userId is required to stop a test when running sequentially in multi-user mode
                ParasoftCTPApiClient.stopTest(testId, passed, failureMessage, ParasoftSessionManager.getUserId());
            }
        } else {
            // No userId or parallelId is required to stop a test when running in single-user mode
            ParasoftCTPApiClient.stopTest(testId, passed, failureMessage);
        }
        ParasoftSessionManager.resetBaggage(testContextKey);
    }
}
