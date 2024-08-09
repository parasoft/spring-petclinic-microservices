package org.example;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;

/**
 * JUnit 5 extensions class to handle coverage agent sessions and tests via the CTP REST API.
 * See https://stackoverflow.com/a/51556718 for an explanation of how this class wraps on an all-annotated test class basis
 * Make sure to invoke tests via maven with command line args -DenvId, -DctpBaseUrl, -DuserId set
 * @author smathog
 */
public class ParasoftWatcher 
    implements 
        BeforeAllCallback, 
        BeforeTestExecutionCallback,
        ExecutionCondition,
        ExtensionContext.Store.CloseableResource,
        ParameterResolver,
        TestWatcher {
    private static Playwright playwright;
    private static APIRequest request;
    private static APIRequestContext apiContext;

    private static int envId;
    private static String ctpUrl;
    private static String userId;
    private static boolean impacted;
    private static String baselineBuildId;
    private static ImpactedTestCase[] impactedTestCases;

    static { 
        impacted = Boolean.valueOf(System.getProperty("impacted"));
        envId = Integer.valueOf(System.getProperty("envId"));
        ctpUrl = System.getProperty("ctpBaseUrl");
        userId = System.getProperty("userId");
        if (impacted) {
            baselineBuildId = System.getProperty("baselineBuildId");
            if (baselineBuildId == null) {
                throw new RuntimeException("baselineBuildId must be set if impacted flag is set to true");
            }
        } 
        if (envId == 0 || ctpUrl == null || userId == null) {
            throw new RuntimeException("envId, ctpBaseUrl, userId must be set");
        }
    }

    private static String sessionId;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (playwright == null) {
            globalSetup();

            if (impacted) {
                impactedTestCases = getImpactedTestCases();
                System.out.println("Impacted tests: ");
                for (ImpactedTestCase impactedTestCase : impactedTestCases) {
                    System.out.println(impactedTestCase.testSuiteName + " - " + impactedTestCase.testName);
                }
            }

            String url = String.format("/em/api/v3/environments/%d/agents/session/start", envId);
            System.out.println("Starting session");
            APIResponse response = apiContext.get(url, RequestOptions.create().setQueryParam("userId", userId));
            System.out.println("CTP API Response: " + response.text());
            ObjectMapper mapper = new ObjectMapper();
            sessionId = mapper.readValue(response.text(), CoverageStatus.class).session();
            
            // Allows for a single cleanup after all test classes have finished
            context.getRoot().getStore(GLOBAL).put("playwright", this);
        }
    }

    public static record CoverageStatus(String session, String test, String testCase) {}

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        String testClass = context.getTestClass().orElseThrow().getSimpleName();
        String testName = context.getTestMethod().orElseThrow().getName();
        System.out.println("Executing test class: " + testClass + " test: " + testName);
        String url = String.format("/em/api/v3/environments/%d/agents/test/start", envId);
        Map<String, Object> data = new HashMap<>();
        data.put("test", testName);
        data.put("userId", userId);
        APIResponse response = apiContext.post(url, RequestOptions.create().setData(data));
        System.out.println("CTP API Response: " + response.text());
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        String testClass = context.getTestClass().orElseThrow().getSimpleName();
        String testName = context.getTestMethod().orElseThrow().getName();
        System.out.println("Finished executing test class: " + testClass + " test: " + testName);
        System.out.println("Test PASSED");
        String url = String.format("/em/api/v3/environments/%d/agents/test/stop", envId);
        Map<String, Object> data = new HashMap<>();
        data.put("test", testName);
        data.put("userId", userId);
        data.put("result", "PASS");
        APIResponse response = apiContext.post(url, RequestOptions.create().setData(data));
        System.out.println("CTP API Response: " + response.text());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testClass = context.getTestClass().orElseThrow().getSimpleName();
        String testName = context.getTestMethod().orElseThrow().getName();
        System.out.println("Finished executing test class: " + testClass + " test: " + testName);
        System.out.println("Test FAILED: " + cause.getMessage());
        String url = String.format("/em/api/v3/environments/%d/agents/test/stop", envId);
        Map<String, Object> data = new HashMap<>();
        data.put("test", testName);
        data.put("userId", userId);
        data.put("result", "FAIL");
        APIResponse response = apiContext.post(url, RequestOptions.create().setData(data));
        System.out.println("CTP API Response: " + response.text());
    }

    @Override
    public void close() throws Exception {
        // Stop the session
        String url = String.format("/em/api/v3/environments/%d/agents/session/stop", envId);
        System.out.println("Stopping session");
        APIResponse response = apiContext.get(url, RequestOptions.create().setQueryParam("userId", userId));
        System.out.println("CTP API Response: " + response.text());

        // Publish to DTP
        url = String.format("em/api/v3/environments/%d/coverage/%s", envId, sessionId);
        Map<String, Object> data = new HashMap<>();
        data.put("sessionTag", "playwright");
        data.put("analysisType", "FUNCTIONAL_TEST");
        System.out.println("Publishing to DTP");
        response = apiContext.post(url, RequestOptions.create().setQueryParam("userId", userId).setData(data).setTimeout(180000));
        System.out.println("CTP API Response: " + response.text());

        System.out.println("Cleaning up playwright");
        playwright.close();
        playwright = null;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == String.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return userId;
    }
    
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!impacted) {
            return ConditionEvaluationResult.enabled("Not in impacted tests mode");
        }
        if (context.getTestMethod().isEmpty()) {
            return ConditionEvaluationResult.enabled("Not a test method");
        }
        String testClass = context.getTestClass().map(clazz -> clazz.getSimpleName()).orElseThrow();
        String testName = context.getTestMethod().map(method -> method.getName()).orElseThrow();
        System.out.println("TestName: " + testName);
        for (ImpactedTestCase testCase : impactedTestCases) {
            System.out.println("TestCase: " + testCase);
            if (testCase.testName().equals(testName)) {
                System.out.println("Test impacted: " + testClass + " - " + testName);
                return ConditionEvaluationResult.enabled("Impacted test");
            }
        }
        System.out.println("Test not impacted: " + testClass + " - " + testName);
        return ConditionEvaluationResult.disabled("Not impacted test");
    }

    private void globalSetup() {
        System.out.println("Setting up playwright");
        playwright = Playwright.create();
        request = playwright.request();
        APIRequest.NewContextOptions options = new APIRequest.NewContextOptions();
        options.setBaseURL(ctpUrl);
        options.setHttpCredentials("admin", "admin");
        apiContext = request.newContext(options);
    }

    public static record ImpactedTestCase(String id, String testName, String analysisType, 
    String testSuiteName, String toolName, String resourcePath, String relativePath) {}

    private ImpactedTestCase[] getImpactedTestCases() throws Exception {
        String url = String.format("/em/api/v3/environments/%d/coverage/impactedTests", envId);
        APIResponse response = apiContext.get(url, RequestOptions.create().setQueryParam("baselineBuildId", baselineBuildId));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.text(), ImpactedTestCase[].class);
    }
}
