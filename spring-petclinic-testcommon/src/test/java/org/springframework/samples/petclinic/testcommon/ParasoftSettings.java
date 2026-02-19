/**
 * ParasoftSettings centralizes configuration and utility methods for Parasoft CTP and DTP integration.
 * <p>
 * This class provides:
 * <ul>
 *   <li>Static fields for system properties controlling CTP environment, authentication, debug mode, and baseline publishing</li>
 *   <li>Conventions for multi-user mode, parallel test execution, and test impact analysis</li>
 * </ul>
 * Used by other classes to access configuration and generate identifiers for REST API calls.
 */
package org.springframework.samples.petclinic.testcommon;

public class ParasoftSettings {
    // System variables for CTP integration
    public static final String CTP_BASE_URL = System.getProperty("CTP_BASE_URL", "http://localhost:8070");
    public static final int CTP_ENV_ID = Integer.parseInt(System.getProperty("CTP_ENV_ID", "4"));
    public static final String CTP_USERNAME = System.getProperty("CTP_USERNAME", "admin");
    public static final String CTP_PASSWORD = System.getProperty("CTP_PASSWORD", "admin");
    
    public static final boolean CTP_MULTI_USER_MODE = Boolean.parseBoolean(System.getProperty("CTP_MULTI_USER_MODE", "true"));
    public static final String PROXY_HOST = System.getProperty("PROXY_HOST", "localhost");
    public static final String PROXY_BIND_HOST = System.getProperty("PROXY_BIND_HOST", "0.0.0.0");
    
    public static final boolean CTP_DEBUG = Boolean.parseBoolean(System.getProperty("CTP_DEBUG", "true"));
    // publishBaseline controls whether this test run should set a baselineBuildId to be used as a reference point for Test Impact Analysis.
    public static final boolean CTP_PUBLISH_BASELINE = Boolean.parseBoolean(System.getProperty("CTP_PUBLISH_BASELINE", "false"));
    public static final String CTP_BASELINE_BUILD_ID = System.getProperty("CTP_BASELINE_BUILD_ID", "spring-petclinic-baseline");

    public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("HEADLESS", "false"));
    public static final boolean SELENIUM_GRID = Boolean.parseBoolean(System.getProperty("SELENIUM_GRID", "false"));
    public static final String SELENIUM_GRID_URL = System.getProperty("SELENIUM_GRID_URL", "http://localhost:4444/wd/hub");

    private static volatile String testFramework = "defaultTestFramework";

    public static void setTestFramework(String framework) {
        if (framework != null && !framework.isBlank()) {
            testFramework = framework;
        }
    }

    public static String getTestFramework() {
        return testFramework;
    }

    public static Boolean isMultiUserMode() {
        return CTP_MULTI_USER_MODE;
    }

    public static boolean isHeadless() {
        return HEADLESS;
    }

    public static boolean isSeleniumGrid() {
        return SELENIUM_GRID;
    }
}
