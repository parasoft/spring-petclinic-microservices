package org.springframework.samples.petclinic.testcommon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Manages the single Parasoft CTP session for the entire test run.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Starting and stopping the CTP session (once per suite via the SuiteListener)</li>
 *   <li>Registering per-test-class parallel IDs and Selenium proxy baggage references</li>
 *   <li>Distributing the per-test baggage value from {@code /test/start} responses to Selenium
 *       proxies and Playwright contexts</li>
 *   <li>Publishing coverage data to Parasoft DTP at the end of the test suite</li>
 *   <li>Publishing baseline data to Parasoft CTP at the end of the test suite</li>
 * </ul>
 */
public class ParasoftSessionManager {
    private static final Logger LOGGER = Logger.getLogger(ParasoftSessionManager.class.getName());

    /**
     * Prefix used by all baggage sentinel values. Real API baggage values from {@code /test/start}
     * never start with this prefix, so callers can use {@link #isBaggageSentinel(String)} to
     * distinguish a real value from a placeholder.
     */
    public static final String BAGGAGE_SENTINEL_PREFIX = "__";

    /** Sentinel: a baggage {@link AtomicReference} was created but no test has populated it yet. */
    public static final String BAGGAGE_UNINITIALIZED = "__UNINITIALIZED__";

    /** Sentinel: a test completed and its baggage was cleared until the next test populates it. */
    public static final String BAGGAGE_RESET = "__RESET__";

    /** Returns {@code true} if {@code value} is a baggage sentinel (placeholder, not a real API value). */
    public static boolean isBaggageSentinel(String value) {
        return value != null && value.startsWith(BAGGAGE_SENTINEL_PREFIX);
    }

    // Single-session state — populated once by startSession() at suite start
    private static volatile String userId = null;
    private static volatile String ctpSessionId = null;
    private static volatile String dtpSessionTag = null;

    // Maps the testContextKey to the AtomicReference held inside the Selenium proxy for that context.
    // testContextKey: test class name (Selenium/TestNG/Playwright) or Cucumber scenario ID.
    // baggageRef: updated per-test from the /test/start response via updateBaggage(); pre-set at
    //   registration time with stable sequential baggage when isParallelTestExecution() is false.
    private static final Map<String, AtomicReference<String>> PROXY_BAGGAGE_REFS = new ConcurrentHashMap<>(); // <String testContextKey, AtomicReference<String> baggageRef>

    // Maps the testContextKey to the current test's baggage header value.
    // baggage: full header value (e.g. "test-operator-id=admin+uuid") sourced from /test/start response.
    //
    // CURRENT_BAGGAGE is the canonical state store ("what is the latest baggage for this test context?");
    // PROXY_BAGGAGE_REFS above is a separate push channel that delivers updates into the Netty proxy's
    // lock-free AtomicReference. updateBaggage() writes to both so they stay in sync, but they are not
    // interchangeable — CURRENT_BAGGAGE is required for two cases that PROXY_BAGGAGE_REFS cannot serve:
    //   1. Playwright tests construct no proxy and never call registerProxyBaggageRef(). Their @BeforeEach
    //      reads the baggage via getBaggage(), which reads from this map.
    //   2. In Cucumber, the watcher's @Before fires (and calls updateBaggage()) before the @Given step
    //      creates the WebDriver/proxy. CURRENT_BAGGAGE captures that early write so the later
    //      registerProxyBaggageRef() call can pre-populate the new proxy's AtomicReference with it.
    //
    // Read by getBaggage() (sentinel-filtered); written by updateBaggage(); written-to-sentinel by
    // resetBaggage() via computeIfPresent() (avoids re-introducing a key after unregister()); cleared by
    // unregister().
    private static final Map<String, String> CURRENT_BAGGAGE = new ConcurrentHashMap<>(); // <String testContextKey, String baggage>

    // Maps the testContextKey to the parallelId for that concurrent test execution thread.
    // parallelId: WebDriver session ID (Selenium) or UUID (Playwright) registered in @BeforeAll/@BeforeClass;
    //   uniquely identifies a concurrent thread within the single shared CTP session.
    //   Only populated when isParallelTestExecution() && isMultiUserMode() are both true.
    private static final Map<String, String> PARALLEL_IDS = new ConcurrentHashMap<>(); // <String testContextKey, String parallelId>

    /** Starts the single CTP session for this test run. Called once by the SuiteListener at suite start. */
    public static void startSession() {
        if (ParasoftSettings.isParallelTestExecution() && !ParasoftSettings.isMultiUserMode()) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftSessionManager] startSession(): CTP_PARALLEL_TEST_EXECUTION=true requires CTP_MULTI_USER_MODE=true. Running parallel tests in single-user mode is an invalid configuration because concurrent tests cannot be distinguished by the coverage agents when they are in single-user mode.");
            }
        }
        userId = buildUserId();
        dtpSessionTag = buildDtpSessionTag();
        if (ParasoftSettings.isMultiUserMode()) {
            ctpSessionId = ParasoftCTPApiClient.startSession(userId);
        } else {
            ctpSessionId = ParasoftCTPApiClient.startSession();
        }
    }

    /** Stops the single CTP session for this test run. Called once by the SuiteListener at suite end. */
    public static void stopSession() {
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopSession(userId);
        } else {
            ParasoftCTPApiClient.stopSession();
        }
    }

    /** Returns the stable user ID for this test run (e.g. {@code "seleniumJUnit-admin"}). */
    public static String getUserId() {
        return userId;
    }

    /**
     * Returns the parallelId registered for the given test context key, or {@code null} if none was
     * registered. A {@code null} return value indicates sequential execution for this context.
     */
    public static String getParallelId(String testContextKey) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] getParallelId(): testContextKey must not be null or blank");
            }
            return null;
        }
        return PARALLEL_IDS.get(testContextKey);
    }

    /**
     * Registers the parallel ID for a test context key. Only registers when {@code parallelId} is
     * non-null — {@link ConcurrentHashMap} does not permit null values. A null {@code parallelId}
     * is logged at WARN and ignored; {@link #getParallelId(String)} will return {@code null} from a
     * map miss, and the watcher falls back to the sequential (2-arg) startTest overload.
     * <p>
     * Called from the {@link org.springframework.samples.petclinic.testcommon.selenium.ParasoftWebDriverResource}
     * constructor (Selenium) or a Playwright test class {@code @BeforeAll}.
     */
    public static void registerParallelId(String testContextKey, String parallelId) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] registerParallelId(): testContextKey must not be null or blank; skipping");
            }
            return;
        }
        if (parallelId == null) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] registerParallelId(): parallelId is null for " + testContextKey + "; skipping");
            }
            return;
        }
        PARALLEL_IDS.put(testContextKey, parallelId);
    }

    /**
     * Registers the Selenium proxy's baggage {@link AtomicReference} for a test context key, and
     * immediately pre-sets the best available baggage value on the proxy ref.
     * <p>
     * If a valid baggage string already exists in {@code CURRENT_BAGGAGE} for this key (written by a
     * preceding watcher {@code updateBaggage()} call — possible in Cucumber where the watcher
     * {@code @Before} fires before the {@code @Given} step creates the proxy), that value is applied
     * to the proxy ref. Otherwise, if in sequential multi-user mode, the stable fallback
     * {@code "test-operator-id=" + userId} is pre-set so the proxy has a sensible default from the
     * moment it is created.
     * <p>
     * Called from the {@link org.springframework.samples.petclinic.testcommon.selenium.ParasoftWebDriverResource}
     * constructor.
     */
    public static void registerProxyBaggageRef(String testContextKey, AtomicReference<String> baggageRef) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] registerProxyBaggageRef(): testContextKey must not be null or blank; skipping");
            }
            return;
        }
        if (baggageRef == null) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] registerProxyBaggageRef(): baggageRef is null for " + testContextKey + "; skipping");
            }
            return;
        }
        PROXY_BAGGAGE_REFS.put(testContextKey, baggageRef);
        // Apply the best available baggage to the new proxy ref immediately
        String existingBaggage = CURRENT_BAGGAGE.get(testContextKey);
        if (existingBaggage != null && !existingBaggage.isBlank() && !isBaggageSentinel(existingBaggage)) {
            // Real API baggage already available (e.g. Cucumber watcher @Before fired before proxy creation)
            baggageRef.set(existingBaggage);
        } else if (!ParasoftSettings.isParallelTestExecution() && userId != null) {
            // Sequential multi-user mode: pre-set stable fallback until the watcher fires
            baggageRef.set("test-operator-id=" + userId);
        }
    }

    /**
     * Updates the current baggage value for a test context key and propagates it to the Selenium
     * proxy's {@link AtomicReference} if one is registered. Called by watcher classes immediately
     * after {@link ParasoftCTPApiClient#startTest(String, String, String)} returns.
     */
    public static void updateBaggage(String testContextKey, String baggage) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] updateBaggage(): testContextKey must not be null or blank; skipping");
            }
            return;
        }
        CURRENT_BAGGAGE.put(testContextKey, baggage);
        AtomicReference<String> ref = PROXY_BAGGAGE_REFS.get(testContextKey);
        if (ref != null) {
            ref.set(baggage);
        }
    }

    /**
     * Resets the baggage for a test context key to the {@code "__RESET__"} sentinel after a test
     * completes. Uses {@link Map#compute} with {@code computeIfPresent} semantics to avoid
     * reintroducing the key if {@link #unregister(String)} has already removed it (prevents a race
     * with Cucumber {@code @After} hook ordering). Also updates the Selenium proxy's
     * {@link AtomicReference} to the sentinel if one is registered. Called by watcher classes after
     * each {@code stopTest()} call.
     */
    public static void resetBaggage(String testContextKey) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] resetBaggage(): testContextKey must not be null or blank; skipping");
            }
            return;
        }
        CURRENT_BAGGAGE.computeIfPresent(testContextKey, (k, v) -> BAGGAGE_RESET);
        AtomicReference<String> ref = PROXY_BAGGAGE_REFS.get(testContextKey);
        if (ref != null) {
            ref.set(BAGGAGE_RESET);
        }
    }

    /**
     * Returns the current baggage value for a test context key, or {@code null} if the value is
     * absent or is a sentinel (starts with {@code "__"}). Sentinel filtering is centralized here so
     * callers (e.g. Playwright {@code @BeforeEach}) do not need to know the sentinel convention.
     */
    public static String getBaggage(String testContextKey) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] getBaggage(): testContextKey must not be null or blank");
            }
            return null;
        }
        String baggage = CURRENT_BAGGAGE.get(testContextKey);
        if (baggage == null || isBaggageSentinel(baggage)) {
            return null;
        }
        return baggage;
    }

    /**
     * Removes all registrations for a test context key from the internal maps. Prevents unbounded
     * map growth across many Cucumber scenarios or long-lived JVM sessions.
     * <p>
     * Called from {@link org.springframework.samples.petclinic.testcommon.selenium.ParasoftWebDriverResource#close()}
     * (Selenium) or a Playwright test class {@code @AfterAll}.
     */
    public static void unregister(String testContextKey) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] unregister(): testContextKey must not be null or blank; skipping");
            }
            return;
        }
        PROXY_BAGGAGE_REFS.remove(testContextKey);
        PARALLEL_IDS.remove(testContextKey);
        CURRENT_BAGGAGE.remove(testContextKey);
    }

    /** Publishes coverage for the single session at suite end. */
    public static void publishCoverageAtSuiteEnd() {
        if (ctpSessionId != null && !ctpSessionId.isBlank()
                && dtpSessionTag != null && !dtpSessionTag.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftSessionManager] publishCoverageAtSuiteEnd(): Publishing coverage for session " + ctpSessionId);
            }
            if (ParasoftSettings.isMultiUserMode() && userId != null && !userId.isBlank()) {
                ParasoftCTPApiClient.publishCoverage(ctpSessionId, dtpSessionTag, userId);
            } else {
                ParasoftCTPApiClient.publishCoverage(ctpSessionId, dtpSessionTag);
            }
        } else {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] publishCoverageAtSuiteEnd(): Skipping publishing coverage — missing session information: ctpSessionId=" + ctpSessionId + ", dtpSessionTag=" + dtpSessionTag);
            }
        }
    }

    /** Publishes the baseline build ID to CTP. */
    public static void publishBaselineAtSuiteEnd() {
        ParasoftCTPApiClient.publishBaseline();
    }

    /** Builds the stable user ID for this test run: {@code {testFramework}-{ctpUsername}}. */
    private static String buildUserId() {
        return ParasoftSettings.getTestFramework() + "-" + ParasoftSettings.CTP_USERNAME;
    }

    /**
     * Builds the DTP session tag: {@code {testFramework}-{ctpUsername}-1}.
     * The trailing {@code -1} is a placeholder for a dynamic run count if multiple test execution
     * jobs publish to the same buildId.
     */
    private static String buildDtpSessionTag() {
        return buildUserId() + "-1";
    }
}