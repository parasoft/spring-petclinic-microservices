package org.springframework.samples.petclinic.cucumber;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.net.SocketException;

public class ParasoftSuiteListenerCucumber implements TestExecutionListener {
    // System variables for CTP integration
    private static final boolean CTP_DEBUG = Boolean.parseBoolean(System.getProperty("ctpDebug", "true"));
    private static final int CTP_ENV_ID = Integer.parseInt(System.getProperty("ctpEnvId", "4"));
    private static final String CTP_BASE_URL = System.getProperty("ctpBaseUrl", "http://localhost:8070/em");
    private static final String CTP_USERNAME = System.getProperty("ctpUsername", "admin");
    private static final String CTP_PASSWORD = System.getProperty("ctpPassword", "admin");
    private static final boolean publishBaseline = Boolean.parseBoolean(System.getProperty("publishBaseline", "false"));
    private static final String baseLineBuildId = System.getProperty("baseLineBuildId", "defaultBaseline");

    private static final Logger LOGGER = Logger.getLogger(ParasoftSuiteListenerCucumber.class.getName());
    private static final String basicAuth = CTP_USERNAME + ":" + CTP_PASSWORD;
    private static String sessionId;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        try {
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CTP_BASE_URL + "/api/v3/environments/" + CTP_ENV_ID + "/agents/session/start"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            if (CTP_DEBUG) LOGGER.info("[ParasoftSuiteListenerCucumber] Sending API call: " + request.uri());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (CTP_DEBUG) LOGGER.info("[ParasoftSuiteListenerCucumber] API call response: " + response.statusCode() + " - " + response.body());
            String responseBody = response.body();
            int sessionIndex = responseBody.indexOf("\"session\":");
            if (sessionIndex != -1) {
                int start = responseBody.indexOf('"', sessionIndex + 10) + 1;
                int end = responseBody.indexOf('"', start);
                sessionId = responseBody.substring(start, end);
            }
        } catch (SocketException ce) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListenerCucumber] Connection error during API call: " + ce.getMessage(), ce);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListenerCucumber] IO error during API call: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListenerCucumber] Unexpected error during API call", e);
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        try {
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest stopSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(CTP_BASE_URL + "/api/v3/environments/" + CTP_ENV_ID + "/agents/session/stop"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            if (CTP_DEBUG) LOGGER.info("[ParasoftSuiteListenerCucumber] Sending API call: " + stopSessionRequest.uri());
            HttpResponse<String> stopSessionResponse = client.send(stopSessionRequest, HttpResponse.BodyHandlers.ofString());
            if (CTP_DEBUG) LOGGER.info("[ParasoftSuiteListenerCucumber] API call response: " + stopSessionResponse.statusCode() + " - " + stopSessionResponse.body());

            // Publish coverage to DTP
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append('{');
            // sessionTag convention: {testFramework}-{username}-{runCount}
            // {username} is included to differentiate multiple CTP test sessions that are running in parallel, if running on a Grid consider username as the grid node identified.
            // {runCount} is used to differentiate multiple test runs that publish reports to the same buildId.  If test executions are batched and publish to the same buildId, 
            //      incrementing runCount for each test run will ensure that coverage data from each test run is published and not overwritten in DTP.
            bodyBuilder.append("\"sessionTag\":\"selenium-" + CTP_USERNAME + "-" + "1" + "\"");
            bodyBuilder.append(',');
            bodyBuilder.append("\"analysisType\":\"FUNCTIONAL_TEST\"");
            bodyBuilder.append('}');
            HttpRequest coverageRequest = HttpRequest.newBuilder()
                .uri(URI.create(CTP_BASE_URL + "/api/v3/environments/" + CTP_ENV_ID + "/coverage/" + sessionId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
                .POST(HttpRequest.BodyPublishers.ofString(bodyBuilder.toString()))
                .build();
            if (CTP_DEBUG) LOGGER.info("[ParasoftSuiteListenerCucumber] Sending API call: " + coverageRequest.uri());
            HttpResponse<String> response = client.send(coverageRequest, HttpResponse.BodyHandlers.ofString());
            if (CTP_DEBUG) LOGGER.info("[ParasoftSuiteListenerCucumber] API call response: " + response.statusCode() + " - " + response.body());

            // publish "baselineBuildId" if "publishBaseline" System property is set to true
            if (publishBaseline) {
                HttpRequest baselineRequest = HttpRequest.newBuilder()
                    .uri(URI.create(CTP_BASE_URL + "/api/v3/environments/" + CTP_ENV_ID+ "/coverage/baselines/" + baseLineBuildId))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
                if (CTP_DEBUG) LOGGER.info("[ParasoftSuiteListenerCucumber] Sending API call: " + baselineRequest.uri());
                HttpResponse<String> baselineResponse = client.send(baselineRequest, HttpResponse.BodyHandlers.ofString());
                if (CTP_DEBUG) LOGGER.info("[ParasoftSuiteListenerCucumber] API call response: " + baselineResponse.statusCode() + " - " + baselineResponse.body());
            }
        } catch (SocketException ce) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListenerCucumber] Connection error during API call: " + ce.getMessage(), ce);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListenerCucumber] IO error during API call: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListenerCucumber] Unexpected error during API call", e);
        }
    }
}
