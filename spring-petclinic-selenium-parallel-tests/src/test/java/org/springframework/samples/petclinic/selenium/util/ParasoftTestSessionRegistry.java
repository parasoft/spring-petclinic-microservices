package org.springframework.samples.petclinic.selenium.util;

import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// This registry class assumes that parallel test execution is being performed at the test class level (i.e., each test
// class is executed in its own thread with its own WebDriver instance)
public final class ParasoftTestSessionRegistry {
	public static final class SessionInfo {
		public final String coverageUserId;
		public volatile String ctpTestSessionId;
		public volatile String dtpSessionTag;

		private SessionInfo(String coverageUserId) {
			this.coverageUserId = coverageUserId;
		}
	}
	
	private static final Map<String, String> COVERAGE_USER_IDS = new ConcurrentHashMap<>();
	private static final Map<String, SessionInfo> SESSIONS = new ConcurrentHashMap<>();

	private ParasoftTestSessionRegistry() {
	}

	// coverageUserId convention: {testFramework}-{ctpUsername}-{sessionId}
	// When coverage agents are deployed in multi-user mode, CTP test sessions are
	// owned by a userId. Test sessions are started/stopped using the userId as an
	// identifier.
	//
	// If multiple sessions are being started in parallel, the userId is used to
	// differentiate between the sessions.
	//
	// - {ctpUsername} is included as a best practice for troubleshooting which CTP
	// user credential was used for calling the REST API.
	// - {sessionId} is a unique identifier for a WebDriver session, to differentiate
	// multiple CTP test sessions that are running in parallel.
	public static String buildRegisterCoverageUserId(String testClassName, String webDriverSessionId) {
		String coverageUserId;
		String resolvedWebDriverSessionId = webDriverSessionId;
		if (resolvedWebDriverSessionId == null || resolvedWebDriverSessionId.isBlank()) {
			resolvedWebDriverSessionId = "defaultSession: " + Thread.currentThread().getName();
		}
		coverageUserId = ParasoftSettings.getTestFramework() + "-" + ParasoftSettings.CTP_USERNAME + "-"
				+ resolvedWebDriverSessionId;
		registerCoverageUserId(testClassName, coverageUserId);
		registerSessionInfo(coverageUserId, null, buildDtpSessionTag(coverageUserId));
		return coverageUserId;
	}	
	
	public static void registerCtpTestSession(String ctpTestSessionId, String coverageUserId) {
		if (ctpTestSessionId == null || ctpTestSessionId.isBlank()) {
			return;
		}
		if (coverageUserId == null || coverageUserId.isBlank()) {
			return;
		}
		registerSessionInfo(coverageUserId, ctpTestSessionId, null);
	}

	public static String getCoverageUserId(String testClassName) {
		if (testClassName == null || testClassName.isBlank()) {
			return null;
		}
		return COVERAGE_USER_IDS.get(testClassName);
	}

	public static Collection<SessionInfo> getSessionInfos() {
		return Collections.unmodifiableCollection(SESSIONS.values());
	}
	
	private static void registerCoverageUserId(String testClassName, String coverageUserId) {
		if (testClassName == null || testClassName.isBlank()) {
			return;
		}
		if (coverageUserId == null || coverageUserId.isBlank()) {
			return;
		}
		COVERAGE_USER_IDS.put(testClassName, coverageUserId);
	}	

	private static void registerSessionInfo(String coverageUserId, String ctpTestSessionId, String dtpSessionTag) {
		if (coverageUserId == null || coverageUserId.isBlank()) {
			return;
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

	// sessionTag convention: {testFramework}-{ctpUsername}-{sessionId}-{runCount}
	// - {ctpUsername} is included as a best practice for troubleshooting which CTP
	// user credential was used for calling the REST API.
	// - {sessionId} is a unique identifier for the WebDriver session, to
	// differentiate multiple CTP test sessions that are running in parallel.
	// - {runCount} is used to differentiate multiple test runs that publish reports
	// to the same buildId. If test executions are batched and publish to the same
	// buildId, incrementing runCount for each test execution job will ensure that test
	// results and coverage data from each test run are not overwritten in DTP when published.
	private static String buildDtpSessionTag(String coverageUserId) {
		// placeholder for dynamic runCount if multiple test execution jobs are run
		// against the same buildId
		String dtpSessionTag = coverageUserId + "-1";
		return dtpSessionTag;
	}
}
