package org.springframework.samples.petclinic.testcommon;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralizes configuration for Parasoft CTP and DTP integration.
 * <p>
 * Configuration values are resolved in the following order:
 * <ol>
 *   <li>System property (e.g., {@code -DCTP_BASE_URL})</li>
 *   <li>{@code parasoft-settings.properties} file (classpath {@code src/test/resources},
 *       or path specified by {@code -DPARASOFT_SETTINGS_FILE})</li>
 *   <li>Hardcoded default value</li>
 * </ol>
 */
public class ParasoftSettings {
    private static final Properties fileProperties = new Properties();
    static {
        // Try to load from system property first
        String filePath = System.getProperty("PARASOFT_SETTINGS_FILE");
        boolean loaded = false;
        if (filePath != null && !filePath.isBlank()) {
            try (InputStream in = new FileInputStream(filePath)) {
                fileProperties.load(in);
                loaded = true;
            } catch (IOException ignored) {}
        }
        if (!loaded) {
            // Try to load from classpath (src/test/resources)
            try (InputStream in = ParasoftSettings.class.getClassLoader().getResourceAsStream("parasoft-settings.properties")) {
                if (in != null) {
                    fileProperties.load(in);
                }
            } catch (IOException ignored) {}
        }
    }
    
    // System variables for CTP integration (now support file-based config)
    public static final String CTP_BASE_URL = getSetting("CTP_BASE_URL", "http://localhost:8080");
    public static final String CTP_CONTEXT_PATH = getSetting("CTP_CONTEXT_PATH", "/em"); // Optional setting if CTP is deployed to custom context path
    public static final int CTP_ENV_ID = getIntSetting("CTP_ENV_ID", "1");
    public static final String CTP_USERNAME = getSetting("CTP_USERNAME", "admin");
    public static final String CTP_PASSWORD = getSetting("CTP_PASSWORD", "admin");
    
    // CTP_LOG_LEVEL controls the verbosity of Parasoft logging (ERROR < WARN < INFO < DEBUG < TRACE)
    public static final String CTP_LOG_LEVEL = getSetting("CTP_LOG_LEVEL", "WARN");

    // CTP_MULTI_USER_MODE controls whether tests are executed in an environment where the coverage agents are setup for multi-user
    public static final boolean CTP_MULTI_USER_MODE = getBoolSetting("CTP_MULTI_USER_MODE", "true");

    // CTP_PARALLEL_TEST_EXECUTION controls whether tests are executed in parallel, which requires a different implementation in the test framework
    // CTP_MULTI_USER_MODE must be true for parallel test execution to work
    public static final boolean CTP_PARALLEL_TEST_EXECUTION = getBoolSetting("CTP_PARALLEL_TEST_EXECUTION", "false");

    // publishCoverage controls whether this test run should publish coverage and test result data to DTP.
    public static final boolean CTP_PUBLISH_COVERAGE = getBoolSetting("CTP_PUBLISH_COVERAGE", "false");

    // publishBaseline controls whether this test run should set a baselineBuildId to be used as a reference point for Test Impact Analysis.
    public static final boolean CTP_PUBLISH_BASELINE = getBoolSetting("CTP_PUBLISH_BASELINE", "false");
    public static final String CTP_BASELINE_BUILD_ID = getSetting("CTP_BASELINE_BUILD_ID", "spring-petclinic-baseline");

    // The PROXY variables are used to configure the proxy server for injecting the baggage header into the test requests
    public static final String PROXY_HOST = getSetting("PROXY_HOST", "localhost");
    public static final String PROXY_BIND_HOST = getSetting("PROXY_BIND_HOST", "0.0.0.0");
    
    // Selenium Grid and Headless system variables for configuring the test execution environment
    public static final boolean HEADLESS = getBoolSetting("HEADLESS", "false");
    public static final boolean SELENIUM_GRID = getBoolSetting("SELENIUM_GRID", "false");
    public static final String SELENIUM_GRID_URL = getSetting("SELENIUM_GRID_URL", "http://localhost:4444/wd/hub");

    private static volatile String TESTFRAMEWORK = getSetting("TESTFRAMEWORK", "defaultTestFramework");

    private static String getSetting(String key, String def) {
        return System.getProperty(key, fileProperties.getProperty(key, def));
    }
    private static boolean getBoolSetting(String key, String def) {
        return Boolean.parseBoolean(getSetting(key, def));
    }
    private static int getIntSetting(String key, String def) {
        try {
            return Integer.parseInt(getSetting(key, def));
        } catch (NumberFormatException e) {
            return Integer.parseInt(def);
        }
    }

    public static void setTestFramework(String framework) {
        if (framework != null && !framework.isBlank()) {
            TESTFRAMEWORK = framework;
        }
    }

    public static String getTestFramework() {
        return TESTFRAMEWORK;
    }

    /**
     * Returns true if the requested log level is enabled according to the configured CTP_LOG_LEVEL.
     * Levels: ERROR < WARN < INFO < DEBUG < TRACE
     */
    public static boolean isLogLevelEnabled(String level) {
        String[] levels = {"ERROR", "WARN", "INFO", "DEBUG", "TRACE"};
        int configured = java.util.Arrays.asList(levels).indexOf(CTP_LOG_LEVEL.toUpperCase());
        int requested = java.util.Arrays.asList(levels).indexOf(level.toUpperCase());
        if (configured == -1) configured = 2; // Default to INFO
        if (requested == -1) requested = 2; // Default to INFO
        return requested <= configured;
    }

    public static Boolean isMultiUserMode() {
        return CTP_MULTI_USER_MODE;
    }

    public static Boolean isParallelTestExecution() {
        return CTP_PARALLEL_TEST_EXECUTION;
    }

    public static boolean isHeadless() {
        return HEADLESS;
    }

    public static boolean isSeleniumGrid() {
        return SELENIUM_GRID;
    }
}