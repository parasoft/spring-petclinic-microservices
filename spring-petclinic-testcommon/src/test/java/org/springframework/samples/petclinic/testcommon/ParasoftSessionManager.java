package org.springframework.samples.petclinic.testcommon;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Manages Parasoft CTP test sessions for both sequential and parallel test execution.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Starting and stopping CTP sessions</li>
 *   <li>Registering coverage user IDs, CTP test session IDs, and DTP session tags</li>
 *   <li>Publishing coverage data to Parasoft DTP at the end of the test suite</li>
 *   <li>Publishing baseline data to Parasoft CTP at the end of the test suite</li>
 * </ul>
 */
public class ParasoftSessionManager {
    private static final Logger LOGGER = Logger.getLogger(ParasoftSessionManager.class.getName());
    
    // For tracking sessions from parallel test execution
    public static final class SessionInfo {
        public final String coverageUserId;
        public volatile String ctpTestSessionId;
        public volatile String dtpSessionTag;

        private SessionInfo(String coverageUserId) {
            this.coverageUserId = coverageUserId;
        }
    }
    private static final Map<String, AtomicReference<String>> COVERAGE_USER_IDS = new ConcurrentHashMap<>(); // <String testContextKey, AtomicReference<String> coverageUserId>
    private static final Map<String, SessionInfo> SESSIONS = new ConcurrentHashMap<>(); // <String coverageUserId, SessionInfo>

    // For tracking a session from sequential test execution
    private static AtomicReference<String> sequentialCoverageUserIdRef = null;
    private static String sequentialCtpTestSessionId = null;
    private static String sequentialDtpSessionTag = null;

    /** Intended to be used by the SuiteListener for sequential test execution. */
    public static void startSession() {
        startSession(null,null,null);
    }

    /** Intended to be (directly) used by the ParasoftWebDriverResource for parallel test execution. */
    public static void startSession(String testContextKey, String webDriverSessionId, AtomicReference<String> coverageUserIdRef) {
        String dtpSessionTag = buildDtpSessionTag(webDriverSessionId);

        if (ParasoftSettings.isMultiUserMode()) {
            if (ParasoftSettings.isParallelTestExecution()) {
                // For parallel test execution, coverageUserIdRef is passed to this method from the ParasoftWebDriverResource
                // Note: the coverageUserIdRef and accompanying session info will be registered for this testContextKey to publish coverage at suite end
                // Start a CTP session for this coverageUserIdRef
                coverageUserIdRef = setExistingCoverageUserIdRef(coverageUserIdRef, webDriverSessionId);
                registerCoverageUserId(testContextKey, coverageUserIdRef, dtpSessionTag);
                String ctpTestSessionId = ParasoftCTPApiClient.startSession(coverageUserIdRef.get());
                registerCtpTestSession(ctpTestSessionId, coverageUserIdRef.get());
            } else {
                // For sequential test execution in multi-user mode, build a new coverageUserIdRef on session start
                // Note: the shared sequentialCoverageUserIdRef will be bound to each WebDriver's proxy for sequential test execution
                // Start a CTP session for this coverageUserIdRef
                sequentialCoverageUserIdRef = buildNewCoverageUserIdRef(webDriverSessionId);
                sequentialDtpSessionTag = dtpSessionTag;
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftSessionManager] startSession(): Setting sequentialCoverageUserIdRef for multi-user sequential test execution to:" + sequentialCoverageUserIdRef.get());
                    LOGGER.info("[ParasoftSessionManager] startSession(): Setting sequentialDtpSessionTag for multi-user sequential test execution to:" + sequentialDtpSessionTag);
                }
                sequentialCtpTestSessionId = ParasoftCTPApiClient.startSession(sequentialCoverageUserIdRef.get());
            }
        } else {
            // For sequential test execution in single-user mode, start a CTP session without a coverageUserIdRef
            // Note: a proxy is not needed for single-user sequential test execution
            sequentialDtpSessionTag = dtpSessionTag;
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftSessionManager] startSession(): Setting sequentialDtpSessionTag for single-user sequential test execution to:" + sequentialDtpSessionTag);
            }
            sequentialCtpTestSessionId = ParasoftCTPApiClient.startSession();
        }
    }

    public static void stopSession(String testContextKey) {
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopSession(getCoverageUserId(testContextKey));
        } else {
            ParasoftCTPApiClient.stopSession();
        }
    }

    /**
     * Retrieves the coverage user ID for the given test context key in parallel test execution,
     * or the sequential coverage user ID for sequential test execution.
     */
    public static String getCoverageUserId(String testContextKey) {
        return getCoverageUserIdRef(testContextKey).get();
    }

    /**
     * Retrieves the {@code AtomicReference<String>} coverage user ID ref for the given test context key
     * in parallel test execution, or the sequential coverage user ID ref for sequential test execution.
     */
    public static AtomicReference<String> getCoverageUserIdRef(String testContextKey) {
        if (ParasoftSettings.isParallelTestExecution() && ParasoftSettings.isMultiUserMode()) {
            if (testContextKey == null || testContextKey.isBlank()) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("getCoverageUserIdRef: Bad testContextKey");
                }
                return null;
            }
            return COVERAGE_USER_IDS.get(testContextKey);
        } else {
            return sequentialCoverageUserIdRef;
        }
    }

    /** Publishes coverage for all sessions (parallel) or single session (sequential). */
    public static void publishCoverageAtSuiteEnd() {
        if (ParasoftSettings.isMultiUserMode()) {
            if (ParasoftSettings.isParallelTestExecution()) {
                // multi-user parallel: publishing coverage for all sessions that are started with WebDriver lifecycle at suite end
                Collection<SessionInfo> sessions = getSessionInfos();
                for (SessionInfo session : sessions) {
                    if (session.ctpTestSessionId != null && !session.ctpTestSessionId.isBlank()) {
                        ParasoftCTPApiClient.publishCoverage(session.ctpTestSessionId, session.dtpSessionTag,
                                session.coverageUserId);
                    }
                }
            } else {
                // multi-user sequential: coverageUserId is needed for publishing coverage
                ParasoftCTPApiClient.publishCoverage(sequentialCtpTestSessionId, sequentialDtpSessionTag, sequentialCoverageUserIdRef.get());
            }
        } else {
            // single-user: coverageUserId is not needed
            if (sequentialCtpTestSessionId != null && !sequentialCtpTestSessionId.isBlank()
                    && sequentialDtpSessionTag != null && !sequentialDtpSessionTag.isBlank()) {
                ParasoftCTPApiClient.publishCoverage(sequentialCtpTestSessionId, sequentialDtpSessionTag);
            }
        }
    }

    /** Publishes the baseline build ID to CTP. */
    public static void publishBaselineAtSuiteEnd() {
        ParasoftCTPApiClient.publishBaseline();
    }

    /** Retrieves all {@link SessionInfo} objects from parallel test execution sessions. */
    private static Collection<SessionInfo> getSessionInfos() {
        return Collections.unmodifiableCollection(SESSIONS.values());
    }

    /** Registers a coverage user ID for parallel test execution sessions. */
    private static void registerCoverageUserId(String testContextKey, AtomicReference<String> coverageUserIdRef, String dtpSessionTag) {
        if (testContextKey == null || testContextKey.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftSessionManager] registerCoverageUserId(): Bad testContextKey");
            }
            return;
        }
        if (coverageUserIdRef == null || coverageUserIdRef.get() == null || coverageUserIdRef.get().isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftSessionManager] registerCoverageUserId(): Bad coverageUserIdRef");
            }
            return;
        }
        if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
            LOGGER.info("[ParasoftSessionManager] registerCoverageUserId(): For parallel test execution, registering coverageUserId to: " + coverageUserIdRef.get());
        }
        registerSessionInfo(coverageUserIdRef.get(), null, dtpSessionTag);
        COVERAGE_USER_IDS.put(testContextKey, coverageUserIdRef);
    }

    /** Registers a CTP test session ID for parallel test execution sessions. */
    private static void registerCtpTestSession(String ctpTestSessionId, String coverageUserId) {
        if (ctpTestSessionId == null || ctpTestSessionId.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftSessionManager] registerCtpTestSession(): Bad ctpTestSessionId");
            }
            return;
        }   
        if (coverageUserId == null || coverageUserId.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftSessionManager] registerCtpTestSession(): Bad coverageUserId");
            }
            return;
        }
        if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
            LOGGER.info("[ParasoftSessionManager] registerCtpTestSession(): For parallel test execution, registering CTP test session: " + ctpTestSessionId);
        }
        registerSessionInfo(coverageUserId, ctpTestSessionId, null);
    }

    /** Registers session info for parallel test execution sessions. */
    private static void registerSessionInfo(String coverageUserId, String ctpTestSessionId, String dtpSessionTag) {
        if (coverageUserId == null || coverageUserId.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftSessionManager] registerSessionInfo(): Bad coverageUserId");
            }
            return;
        }
        if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
            LOGGER.info("[ParasoftSessionManager] registerSessionInfo(): For parallel test execution, registering session info: coverageUserId=" + coverageUserId + ", ctpTestSessionId=" + ctpTestSessionId + ", dtpSessionTag=" + dtpSessionTag);
        }
        SESSIONS.compute(coverageUserId, (key, existing) -> {
            SessionInfo info = existing == null ? new SessionInfo(key) : existing;
            if (ctpTestSessionId != null && !ctpTestSessionId.isBlank()) {
                info.ctpTestSessionId = ctpTestSessionId;
            }
            if (dtpSessionTag != null && !dtpSessionTag.isBlank()) {
                info.dtpSessionTag = dtpSessionTag;
            }
            return info;
        });
    }

    private static AtomicReference<String> buildNewCoverageUserIdRef(String webDriverSessionId) {
        AtomicReference<String> coverageUserIdRef = new AtomicReference<>("__UNINITIALIZED__");
        coverageUserIdRef = setExistingCoverageUserIdRef(coverageUserIdRef, webDriverSessionId);
        return coverageUserIdRef;
    }

    private static AtomicReference<String> setExistingCoverageUserIdRef(AtomicReference<String> coverageUserIdRef, String webDriverSessionId) {
        coverageUserIdRef.set(buildCoverageUserIdString(webDriverSessionId));
        return coverageUserIdRef;
    }

    /**
     * Builds a coverage user ID string following the convention:
     * {@code {testFramework}-{ctpUsername}-{webDriverSessionId}}
     * <p>
     * When coverage agents are deployed in multi-user mode, CTP test sessions are
     * owned by a userId. Test sessions are started/stopped using the userId as an identifier.
     * <ul>
     *   <li>{@code ctpUsername} — included for troubleshooting which CTP user credential was used</li>
     *   <li>{@code webDriverSessionId} — unique identifier for a WebDriver session, to
     *       differentiate multiple CTP test sessions running in parallel</li>
     * </ul>
     */
    private static String buildCoverageUserIdString(String webDriverSessionId) {
        String resolvedWebDriverSessionId = webDriverSessionId;
        if (resolvedWebDriverSessionId == null || resolvedWebDriverSessionId.isBlank()) {
            resolvedWebDriverSessionId = "defaultSession";
        }
        return ParasoftSettings.getTestFramework() + "-" + ParasoftSettings.CTP_USERNAME + "-"
                + resolvedWebDriverSessionId;
    }

    /**
     * Builds a DTP session tag following the convention:
     * {@code {testFramework}-{ctpUsername}-{webDriverSessionId}-{runCount}}
     * <ul>
     *   <li>{@code ctpUsername} — included for troubleshooting which CTP user credential was used</li>
     *   <li>{@code webDriverSessionId} — unique identifier for a WebDriver session, to
     *       differentiate multiple CTP test sessions running in parallel</li>
     *   <li>{@code runCount} — differentiates multiple test runs publishing to the same buildId,
     *       ensuring test results and coverage data are not overwritten in DTP</li>
     * </ul>
     */
    private static String buildDtpSessionTag(String webDriverSessionId) {
        String dtpSessionTag = buildCoverageUserIdString(webDriverSessionId) + "-1"; // placeholder for dynamic runCount if multiple test execution jobs are run against the same buildId
        return dtpSessionTag;
    }
}
