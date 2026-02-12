package org.springframework.samples.petclinic.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v142.network.Network;
import org.openqa.selenium.devtools.v142.network.model.Headers;
import java.util.HashMap;
import java.util.Optional;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ParasoftWatcherCucumber {
    // System variables for CTP integration
    private static final boolean CTP_DEBUG = Boolean.parseBoolean(System.getProperty("ctpDebug", "true"));
    private static final int CTP_ENV_ID = Integer.parseInt(System.getProperty("ctpEnvId", "4"));
    private static final String CTP_BASE_URL = System.getProperty("ctpBaseUrl", "http://localhost:8070/em");
    private static final String CTP_USERNAME = System.getProperty("ctpUsername", "admin");
    private static final String CTP_PASSWORD = System.getProperty("ctpPassword", "admin");
    private static final String API_BASE_URL = CTP_BASE_URL + "/api/v3/environments/" + CTP_ENV_ID;
    
    private static final Logger LOGGER = Logger.getLogger(ParasoftWatcherCucumber.class.getName());
    private static final String basicAuth = CTP_USERNAME + ":" + CTP_PASSWORD;
    private final TestContext context;

    public ParasoftWatcherCucumber(TestContext context) {
        this.context = context;
    }

    @Before
    public void beforeScenario(Scenario scenario) {
        if (CTP_DEBUG)LOGGER.info("[ParasoftWatcherCucumber] Setting up ChromeDriver for scenario: " + scenario.getName());
        ChromeDriver driver = new ChromeDriver();
        context.setDriver(driver);
        injectBaggageHeader(driver);
        // API call for test start
        try {
            HttpClient client = HttpClient.newBuilder().build();
            String payload = String.format("{\"test\":\"%s\"}", scenario.getName());
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/agents/test/start"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            if (CTP_DEBUG) LOGGER.info("[ParasoftWatcherCucumber] Sending API call: " + request.uri());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (CTP_DEBUG) LOGGER.info("[ParasoftWatcherCucumber] API call response: " + response.statusCode() + " - " + response.body());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftWatcherCucumber] Error during test start API call", e);
        }
    }

    @After
    public void afterScenario(Scenario scenario) {
        // API call for test stop
        try {
            HttpClient client = HttpClient.newBuilder().build();
            String result = scenario.isFailed() ? "FAIL" : "PASS";
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append('{');
            bodyBuilder.append("\"test\":\"" + scenario.getName() + "\"");
            bodyBuilder.append(',');
            bodyBuilder.append("\"result\":\"" + result + "\"");
            if (scenario.isFailed() && scenario.getStatus() != null) {
                bodyBuilder.append(',');
                bodyBuilder.append("\"message\":\"" + scenario.getStatus().toString() + "\"");
            }
            bodyBuilder.append('}');
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/agents/test/stop"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
                .POST(HttpRequest.BodyPublishers.ofString(bodyBuilder.toString()))
                .build();
            if (CTP_DEBUG) LOGGER.info("[ParasoftWatcherCucumber] Sending API call: " + request.uri());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (CTP_DEBUG) LOGGER.info("[ParasoftWatcherCucumber] API call response: " + response.statusCode() + " - " + response.body());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftWatcherCucumber] Error during test stop API call", e);
        }
        ChromeDriver driver = context.getDriver();
        if (driver != null) {
            LOGGER.info("[ParasoftWatcherCucumber] Quitting ChromeDriver for scenario: " + scenario.getName());
            driver.quit();
        }
    }

    public static void injectBaggageHeader(ChromeDriver driver) {
        try {
            DevTools devTools = driver.getDevTools();
            devTools.createSession();
            devTools.send(Network.enable(
                Optional.empty(), // maxTotalBufferSize
                Optional.empty(), // maxResourceBufferSize
                Optional.empty(), // maxPostDataSize
                Optional.empty(), // maxBlockedCookies
                Optional.empty()  // maxBlockedRequests
            ));
            HashMap<String, Object> headers = new HashMap<>();
            headers.put("baggage", "test-operator-id=parasoft-selenium");
            devTools.send(Network.setExtraHTTPHeaders(new Headers(headers)));
            if (CTP_DEBUG) LOGGER.info("[ParasoftWatcherCucumber] Injected baggage header into ChromeDriver");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftWatcherCucumber] Error injecting baggage header", e);
        }
    }
}
