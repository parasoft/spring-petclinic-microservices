package org.springframework.samples.petclinic.selenium.util;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

// Holds suite-scoped Parasoft metadata for the selenium-tests module.
// This module uses a fixed coverage user ID per suite: {testFramework}-{ctpUsername}-1.
public final class ParasoftSeleniumContext {
    private static final AtomicReference<String> COVERAGE_USER_ID_REF = new AtomicReference<>("");
    private static volatile String coverageUserId = "";
    private static volatile String dtpSessionTag = "";
    private static volatile String ctpSessionId = "";

    private ParasoftSeleniumContext() {
    }

    public static void initForSuite() {
        if (!ParasoftSettings.isMultiUserMode()) {
            return;
        }
        
        // coverageUserId convention: {testFramework}-{ctpUsername}-{sessionId}
        // When coverage agents are deployed in multi-user mode, CTP test sessions are
        // owned by a userId. Test sessions are started/stopped using the userId as an
        // identifier.
        //
        // - {ctpUsername} is included as a best practice for troubleshooting which CTP
        // user credential was used for calling the REST API.
        // - {sessionId} is a unique identifier for a WebDriver session, to differentiate
        // multiple CTP test sessions that are running in parallel.
        String resolvedCoverageUserId = ParasoftSettings.getTestFramework() + "-" + ParasoftSettings.CTP_USERNAME + "-1";
        coverageUserId = resolvedCoverageUserId;
        COVERAGE_USER_ID_REF.set(resolvedCoverageUserId);

        // sessionTag convention: {testFramework}-{ctpUsername}-{sessionId}-{runCount}
        // - {ctpUsername} is included as a best practice for troubleshooting which CTP
        // user credential was used for calling the REST API.
        // - {sessionId} is a unique identifier for the WebDriver session, to
        // differentiate multiple CTP test sessions that are running in parallel.
        // - {runCount} is used to differentiate multiple test runs that publish reports
        // to the same buildId. If test executions are batched and publish to the same
        // buildId, incrementing runCount for each test execution job will ensure that test
        // results and coverage data from each test run are not overwritten in DTP when published.
        dtpSessionTag = resolvedCoverageUserId + "-1";  // placeholder for dynamic runCount if multiple test execution jobs are run against the same buildId
    }

    public static String getCoverageUserId() {
        return coverageUserId;
    }

    public static String getDtpSessionTag() {
        return dtpSessionTag;
    }

    public static AtomicReference<String> getCoverageUserIdRef() {
        return COVERAGE_USER_ID_REF;
    }

    public static String getCtpSessionId() {
        return ctpSessionId;
    }

    public static void setCtpSessionId(String sessionId) {
        ctpSessionId = sessionId;
    }
}
