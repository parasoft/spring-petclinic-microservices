package org.springframework.samples.petclinic.selenium;

public class ParasoftSettings {
    // System variables for CTP integration
    public static final String CTP_BASE_URL = System.getProperty("ctpBaseUrl", "http://localhost:8070/em");
    public static final int CTP_ENV_ID = Integer.parseInt(System.getProperty("ctpEnvId", "4"));
    public static final String CTP_USERNAME = System.getProperty("ctpUsername", "admin");
    public static final String CTP_PASSWORD = System.getProperty("ctpPassword", "admin");
    public static final String CTP_MULTI_USER_MODE = System.getProperty("ctpMultiUserMode", "true");
    public static final boolean CTP_DEBUG = Boolean.parseBoolean(System.getProperty("ctpDebug", "true"));

    // publishBaseline controls whether this test run should set a baselineBuildId to be used as a reference point for Test Impact Analysis.
    public static final boolean publishBaseline = Boolean.parseBoolean(System.getProperty("publishBaseline", "false"));
    public static final String baseLineBuildId = System.getProperty("baseLineBuildId", "defaultBaseline");

    public static final String testFramework = "selenium";

    // ctpUserId convention: {testFramework}-{username}-{nodeId}
    // When coverage agents are deployed in multi-user mode, CTP test sessions are owned by a userId. Starting and stopping sessions use the userId to identify which user's session to start/stop.
    // {username} is included to differentiate multiple CTP test sessions that are running in parallel, if running on a Grid consider username as the grid node identifier.
    // {nodeId} is a unique identifier for the grid node or thread, provided by the grid or fallback to thread name.
    public static String getCoverageUserId() {
        // placeholder for dynamic username retrieval from parallel test execution
        return testFramework + "-" + CTP_USERNAME + "-" + "defaultNode";
    }

    // sessionTag convention: {testFramework}-{username}-{nodeId}-{runCount}
    // {username} is included to differentiate multiple CTP test sessions that are running in parallel, if running on a Grid consider username as the grid node identifier.
    // {nodeId} is a unique identifier for the grid node or thread, provided by the grid or fallback to thread name.
    // {runCount} is used to differentiate multiple test runs that publish reports to the same buildId.  If test executions are batched and publish to the same buildId, 
    //    incrementing runCount for each test run will ensure that coverage data from each test run is published and not overwritten in DTP.
    public static String getDtpSessionTag() {
        return getCoverageUserId() + "-2";
    }
}
