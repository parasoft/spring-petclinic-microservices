package org.springframework.samples.petclinic.selenium;

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

public class ParasoftSuiteListener implements TestExecutionListener {
    private static final Logger LOGGER = Logger.getLogger(ParasoftSuiteListener.class.getName());
    private static final String basicAuth = ParasoftSettings.CTP_USERNAME + ":" + ParasoftSettings.CTP_PASSWORD;
    private static String sessionId;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        // CTP REST API: /v3/environments/{envId}/agents/session/start
        try {
            StringBuilder sessionStartPayload = new StringBuilder();
            if (ParasoftSettings.CTP_MULTI_USER_MODE.equalsIgnoreCase("true")) {
                sessionStartPayload.append('{');
                sessionStartPayload.append("\"userId\":\"" + ParasoftSettings.getCoverageUserId() + "\"");
                sessionStartPayload.append('}');
            }
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/session/start"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
                .POST(HttpRequest.BodyPublishers.ofString(sessionStartPayload.toString()))
                .build();
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftSuiteListener] Sending API call: " + request.uri());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftSuiteListener] API call response: " + response.statusCode() + " - " + response.body());
            String responseBody = response.body();
            int sessionIndex = responseBody.indexOf("\"session\":");
            if (sessionIndex != -1) {
                int start = responseBody.indexOf('"', sessionIndex + 10) + 1;
                int end = responseBody.indexOf('"', start);
                sessionId = responseBody.substring(start, end);
            }
        } catch (SocketException ce) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListener] Connection error during API call: " + ce.getMessage(), ce);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListener] IO error during API call: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListener] Unexpected error during API call", e);
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        // CTP REST API: /v3/environments/{envId}/agents/session/stop
        // CTP REST API: /v3/environments/{envId}/coverage/{sessionId}
        // (Conditional) CTP REST API: /v3/environments/{envId}/coverage/baselines/{baselineBuildId}
        try {
            // Stop session
            StringBuilder sessionStopPayload = new StringBuilder();
            if (ParasoftSettings.CTP_MULTI_USER_MODE.equalsIgnoreCase("true")) {
                sessionStopPayload.append('{');
                sessionStopPayload.append("\"userId\":\"" + ParasoftSettings.getCoverageUserId() + "\"");
                sessionStopPayload.append('}');
            }
            
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest stopSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/session/stop"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
                .POST(HttpRequest.BodyPublishers.ofString(sessionStopPayload.toString()))
                .build();
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftSuiteListener] Sending API call: " + stopSessionRequest.uri());
            HttpResponse<String> stopSessionResponse = client.send(stopSessionRequest, HttpResponse.BodyHandlers.ofString());
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftSuiteListener] API call response: " + stopSessionResponse.statusCode() + " - " + stopSessionResponse.body());

            // Publish coverage to DTP
            StringBuilder coveragePayload = new StringBuilder();
            coveragePayload.append('{');
            coveragePayload.append("\"sessionTag\":\"" + ParasoftSettings.getDtpSessionTag() + "\"");
            coveragePayload.append(',');
            coveragePayload.append("\"analysisType\":\"FUNCTIONAL_TEST\"");
            coveragePayload.append('}');
            
            URI coverageUri;
            if (ParasoftSettings.CTP_MULTI_USER_MODE.equalsIgnoreCase("true")) {
                coverageUri = URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/coverage/" + sessionId + "?userId=" + ParasoftSettings.getCoverageUserId());
            } else {
                coverageUri = URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/coverage/" + sessionId);
            }
            HttpRequest coverageRequest = HttpRequest.newBuilder()
                .uri(coverageUri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
                .POST(HttpRequest.BodyPublishers.ofString(coveragePayload.toString()))
                .build();
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftSuiteListener] Sending API call: " + coverageRequest.uri());
            HttpResponse<String> response = client.send(coverageRequest, HttpResponse.BodyHandlers.ofString());
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftSuiteListener] API call response: " + response.statusCode() + " - " + response.body());

            // publish "baselineBuildId" if "publishBaseline" System property is set to true
            if (ParasoftSettings.publishBaseline) {
                HttpRequest baselineRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID+ "/coverage/baselines/" + ParasoftSettings.baseLineBuildId))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
                if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftSuiteListener] Sending API call: " + baselineRequest.uri());
                HttpResponse<String> baselineResponse = client.send(baselineRequest, HttpResponse.BodyHandlers.ofString());
                if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftSuiteListener] API call response: " + baselineResponse.statusCode() + " - " + baselineResponse.body());
            }
        } catch (SocketException ce) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListener] Connection error during API call: " + ce.getMessage(), ce);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListener] IO error during API call: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftSuiteListener] Unexpected error during API call", e);
        }
    }
}
