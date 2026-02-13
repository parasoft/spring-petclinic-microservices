/**
 * ParasoftSettings centralizes configuration and utility methods for Parasoft CTP and DTP integration.
 * <p>
 * This class provides:
 * <ul>
 *   <li>Static fields for system properties controlling CTP environment, authentication, debug mode, and baseline publishing</li>
 *   <li>Utility methods for generating user and session tags used in coverage tracking and reporting</li>
 *   <li>Conventions for multi-user mode, parallel test execution, and test impact analysis</li>
 * </ul>
 * Used by other classes to access configuration and generate identifiers for REST API calls.
 */
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

    // coverageUserId convention: {testFramework}-{ctpUsername}-{nodeId}
    // When coverage agents are deployed in multi-user mode, CTP test sessions are owned by a userId. Test sessions are started/stopped using the userId as an identifier.
    //    If multiple sessions are being started in parallel, the userId is used to differentiate between the sessions.
    // {ctpUsername} is included as a best practice for troubleshooting which CTP user credential was used for calling the REST API.
    // {nodeId} is a unique identifier for a grid node or thread, to differentiate multiple CTP test sessions that are running in parallel.
    public static String getCoverageUserId() {
        // placeholder for dynamic nodeName retrieval from parallel test execution
        return testFramework + "-" + CTP_USERNAME + "-" + "defaultNode";
    }

    // sessionTag convention: {testFramework}-{ctpUsername}-{nodeId}-{runCount}
    // {ctpUsername} is included as a best practice for troubleshooting which CTP user credential was used for calling the REST API.
    // {nodeId} is a unique identifier for the grid node or thread, to differentiate multiple CTP test sessions that are running in parallel.
    // {runCount} is used to differentiate multiple test runs that publish reports to the same buildId.  If test executions are batched and publish to the same buildId, 
    //    incrementing runCount for each test execution job will ensure that test results and coverage data from each test run are not overwritten in DTP when published.
    public static String getDtpSessionTag() {
        // placeholder for dynamic runCount if multiple test execution jobs are run against the same buildId
        return getCoverageUserId() + "-1";
    }
}
