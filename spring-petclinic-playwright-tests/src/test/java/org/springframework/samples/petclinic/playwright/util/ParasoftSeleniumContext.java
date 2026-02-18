package org.springframework.samples.petclinic.playwright.util;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

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
        String resolvedCoverageUserId = ParasoftSettings.getTestFramework() + "-" + ParasoftSettings.CTP_USERNAME + "-1";
        coverageUserId = resolvedCoverageUserId;
        dtpSessionTag = resolvedCoverageUserId + "-1";
        COVERAGE_USER_ID_REF.set(resolvedCoverageUserId);
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
