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
 *   <li>Registering per-test-class parallel IDs and vending baggage references for Selenium proxies</li>
 *   <li>Holding the per-test baggage value from {@code /test/start} responses in a single
 *       {@link AtomicReference} per test context, read on the Netty proxy hot path and by Playwright
 *       {@code @BeforeEach}</li>
 *   <li>Publishing coverage data to Parasoft DTP at the end of the test suite</li>
 *   <li>Publishing baseline data to Parasoft CTP at the end of the test suite</li>
 * </ul>
 */
public class ParasoftSessionManager {
    private static final Logger LOGGER = Logger.getLogger(ParasoftSessionManager.class.getName());

    /** Sentinel: a baggage {@link AtomicReference} was created but no test has populated it yet. */
    public static final String BAGGAGE_UNINITIALIZED = "__UNINITIALIZED__";

    /** Sentinel: a test completed and its baggage was cleared until the next test populates it. */
    public static final String BAGGAGE_RESET = "__RESET__";

    /**
     * Returns {@code true} if {@code value} is one of the baggage sentinels
     * ({@link #BAGGAGE_UNINITIALIZED} or {@link #BAGGAGE_RESET}) rather than a real
     * value from the {@code /test/start} API response. Null-safe.
     */
    public static boolean isBaggageSentinel(String value) {
        return BAGGAGE_UNINITIALIZED.equals(value) || BAGGAGE_RESET.equals(value);
    }

    // Single-session state — populated once by startSession() at suite start
    private static volatile String userId = null;
    private static volatile String ctpSessionId = null;
    private static volatile String dtpSessionTag = null;

    // Single canonical baggage store: maps the testContextKey to an AtomicReference holding the
    // current test's baggage header value. The same reference is handed to the Netty proxy at
    // construction time via obtainProxyBaggageRef(), so the proxy's hot path is a single lock-free
    // ref.get() and updateBaggage() / resetBaggage() / getBaggage() all operate on the same ref.
    // Handles three consumer patterns with one map:
    //   - Selenium: the proxy is constructed with the ref returned by obtainProxyBaggageRef() and
    //     reads it directly on every proxied request.
    //   - Playwright (no proxy): @BeforeEach reads the latest value via getBaggage(testContextKey).
    //   - Cucumber early-write: the watcher's @Before may fire before the @Given step constructs
    //     the proxy. updateBaggage() lazily creates the ref so the value is preserved; the proxy
    //     then adopts the existing ref when it later calls obtainProxyBaggageRef().
    //
    // testContextKey: test class name (Selenium/TestNG/Playwright) or Cucumber scenario ID.
    // baggage value: full header value (e.g. "test-operator-id=admin+uuid") sourced from the
    //   /test/start response, or a sentinel ("__UNINITIALIZED__" / "__RESET__").
    private static final Map<String, AtomicReference<String>> BAGGAGE_REFS = new ConcurrentHashMap<>(); // <String testContextKey, AtomicReference<String> baggageRef>

    // Maps the testContextKey to the parallelId for that concurrent test execution thread.
    // parallelId: WebDriver session ID (Selenium) or UUID (Playwright) registered in @BeforeAll/@BeforeClass;
    //   uniquely identifies a concurrent thread within the single shared CTP session.
    //   Only populated when isParallelTestExecution() && isMultiUserMode() are both true.
    private static final Map<String, String> PARALLEL_IDS = new ConcurrentHashMap<>(); // <String testContextKey, String parallelId>

    /** Starts the single CTP session for this test run. Called once by the SuiteListener at suite start. */
    public static void startSession() {
        if (ParasoftSettings.isParallelTestExecution() && !ParasoftSettings.isMultiUserMode()) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftSessionManager] startSession: invalid configuration - CTP_PARALLEL_TEST_EXECUTION=true requires CTP_MULTI_USER_MODE=true");
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
                LOGGER.warning("[ParasoftSessionManager] getParallelId: testContextKey must not be null or blank");
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
                LOGGER.warning("[ParasoftSessionManager] registerParallelId: testContextKey must not be null or blank; skipping");
            }
            return;
        }
        if (parallelId == null) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] [" + testContextKey + "] registerParallelId: parallelId is null; skipping");
            }
            return;
        }
        PARALLEL_IDS.put(testContextKey, parallelId);
    }

    /**
     * Returns the baggage {@link AtomicReference} for a test context key, creating it lazily if
     * none exists yet. The {@link org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy}
     * constructor calls this to obtain the ref it will read on its hot path; subsequent
     * {@link #updateBaggage(String, String)} and {@link #resetBaggage(String)} calls operate on
     * the same ref so updates are visible to the proxy without any further wiring.
     * <p>
     * If a preceding {@link #updateBaggage(String, String)} already created the ref with a real
     * value (Cucumber: watcher {@code @Before} fires before the {@code @Given} step creates the
     * proxy), the existing ref is returned unchanged. Otherwise a new ref is created and
     * pre-populated with a sensible default: in sequential multi-user mode that is the stable
     * {@code "test-operator-id=" + userId} fallback; in parallel mode the ref starts at the
     * {@code __UNINITIALIZED__} sentinel and the proxy serves no header until the watcher fires.
     * <p>
     * Returns {@code null} if {@code testContextKey} is null or blank.
     */
    public static AtomicReference<String> obtainProxyBaggageRef(String testContextKey) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] obtainProxyBaggageRef: testContextKey must not be null or blank; returning null");
            }
            return null;
        }
        return BAGGAGE_REFS.compute(testContextKey, (k, existing) -> {
            if (existing != null) {
                // Watcher already created the ref (e.g. Cucumber early-write); proxy adopts it.
                return existing;
            }
            AtomicReference<String> ref = new AtomicReference<>(BAGGAGE_UNINITIALIZED);
            if (!ParasoftSettings.isParallelTestExecution() && userId != null) {
                // Sequential multi-user mode: pre-set stable fallback until the watcher fires.
                ref.set("test-operator-id=" + userId);
            }
            return ref;
        });
    }

    /**
     * Updates the baggage value for a test context key. The update is visible atomically to all
     * readers — the Netty proxy's hot-path read, Playwright {@code @BeforeEach}'s
     * {@link #getBaggage(String)} call, and any subsequent {@link #obtainProxyBaggageRef(String)}
     * call. Called by watcher classes immediately after
     * {@link ParasoftCTPApiClient#startTest(String, String, String)} returns.
     * <p>
     * If no ref exists yet for this key, one is created lazily so an early-write (Cucumber
     * watcher {@code @Before} firing before the {@code @Given} step constructs the proxy) is not
     * lost.
     */
    public static void updateBaggage(String testContextKey, String baggage) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] updateBaggage: testContextKey must not be null or blank; skipping");
            }
            return;
        }
        BAGGAGE_REFS.computeIfAbsent(testContextKey, k -> new AtomicReference<>(BAGGAGE_UNINITIALIZED)).set(baggage);
    }

    /**
     * Resets the baggage for a test context key to the {@code "__RESET__"} sentinel after a test
     * completes. Uses {@link Map#computeIfPresent} so {@link #unregister(String)} can safely run
     * before this call (Cucumber {@code @After} hook ordering) without reintroducing the key.
     * Called by watcher classes after each {@code stopTest()} call.
     */
    public static void resetBaggage(String testContextKey) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] resetBaggage: testContextKey must not be null or blank; skipping");
            }
            return;
        }
        BAGGAGE_REFS.computeIfPresent(testContextKey, (k, ref) -> {
            ref.set(BAGGAGE_RESET);
            return ref;
        });
    }

    /**
     * Returns the current baggage value for a test context key, or {@code null} if the value is
     * absent or is a sentinel ({@link #BAGGAGE_UNINITIALIZED} or {@link #BAGGAGE_RESET}). Sentinel
     * filtering is centralized here so callers (e.g. Playwright {@code @BeforeEach}) do not need
     * to know which placeholder values exist.
     */
    public static String getBaggage(String testContextKey) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] getBaggage: testContextKey must not be null or blank");
            }
            return null;
        }
        AtomicReference<String> ref = BAGGAGE_REFS.get(testContextKey);
        if (ref == null) {
            return null;
        }
        String baggage = ref.get();
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
                LOGGER.warning("[ParasoftSessionManager] unregister: testContextKey must not be null or blank; skipping");
            }
            return;
        }
        BAGGAGE_REFS.remove(testContextKey);
        PARALLEL_IDS.remove(testContextKey);
    }

    /** Publishes coverage for the single session at suite end. */
    public static void publishCoverageAtSuiteEnd() {
        if (ctpSessionId != null && !ctpSessionId.isBlank()
                && dtpSessionTag != null && !dtpSessionTag.isBlank()) {
            if (ParasoftSettings.isMultiUserMode() && userId != null && !userId.isBlank()) {
                ParasoftCTPApiClient.publishCoverage(ctpSessionId, dtpSessionTag, userId);
            } else {
                ParasoftCTPApiClient.publishCoverage(ctpSessionId, dtpSessionTag);
            }
        } else {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftSessionManager] publishCoverageAtSuiteEnd: skipping publishing coverage - missing session information: ctpSessionId=" + ctpSessionId + ", dtpSessionTag=" + dtpSessionTag);
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