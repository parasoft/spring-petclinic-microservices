/**
 * ParasoftCTPApiClient centralizes REST API calls to Parasoft CTP for test session and coverage management.
 * Provides methods for session start/stop, test start/stop, coverage publishing, and baseline publishing.
 */

package org.springframework.samples.petclinic.testcommon;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.net.SocketException;

public class ParasoftCTPApiClient {
    private static final Logger LOGGER = Logger.getLogger(ParasoftCTPApiClient.class.getName());
    private static final HttpClient client = HttpClient.newBuilder().build();
    private static final String basicAuth = ParasoftSettings.CTP_USERNAME + ":" + ParasoftSettings.CTP_PASSWORD;

    // CTP REST API: /v3/environments/{envId}/agents/session/start
    public static String startSession() {
        return startSession(null); // if coverageUserId is not provided, the API client will assume single user mode
    }

    public static String startSession(String coverageUserId) {
        String resolvedCoverageUserId = resolveCoverageUserId(coverageUserId);
        if (ParasoftSettings.CTP_DEBUG) {
            LOGGER.info("[ParasoftCTPApiClient] Starting CTP session for environment: " + ParasoftSettings.CTP_ENV_ID);
            LOGGER.info("[ParasoftCTPApiClient] Coverage User ID: " + resolvedCoverageUserId);
        }

        StringBuilder payload = new StringBuilder();
        // Only include userId if the coverage agents are configured in multi-user mode
        if (ParasoftSettings.isMultiUserMode() && coverageUserId != null && !coverageUserId.isBlank()) {
            payload.append('{');
            payload.append("\"userId\":\"" + resolvedCoverageUserId + "\"");
            payload.append('}');
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/session/start"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] API call response: " + response.statusCode() + " - " + response.body());
            String responseBody = response.body();
            int sessionIndex = responseBody.indexOf("\"session\":");
            if (sessionIndex != -1) {
                int start = responseBody.indexOf('"', sessionIndex + 10) + 1;
                int end = responseBody.indexOf('"', start);
                return responseBody.substring(start, end);
            }
        } catch (SocketException ce) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage(), ce);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Unexpected error during API call", e);
        }
        return null;
    }

    // CTP REST API: /v3/environments/{envId}/agents/test/start
    public static void startTest(String testId) {
        startTest(testId, null); // if coverageUserId is not provided, the API client will assume single user mode
    }

    public static void startTest(String testId, String coverageUserId) {
        String resolvedCoverageUserId = resolveCoverageUserId(coverageUserId);
        StringBuilder payload = new StringBuilder();
        payload.append('{');
        payload.append("\"test\":\"" + testId + "\"");
        // Only include userId if the coverage agents are configured in multi-user mode
        if (ParasoftSettings.isMultiUserMode()) {
            payload.append(',');
            payload.append("\"userId\":\"" + resolvedCoverageUserId + "\"");
        }
        payload.append('}');
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/test/start"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] API call response: " + response.statusCode() + " - " + response.body());
        } catch (SocketException ce) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage(), ce);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Unexpected error during API call", e);
        }
    }

    // CTP REST API: /v3/environments/{envId}/agents/test/stop
    public static void stopTest(String testId, boolean passed, String message) {
        stopTest(testId, passed, message, null); // if coverageUserId is not provided, the API client will assume single user mode
    }

    public static void stopTest(String testId, boolean passed, String message, String coverageUserId) {
        String resolvedCoverageUserId = resolveCoverageUserId(coverageUserId);
        StringBuilder payload = new StringBuilder();
        payload.append('{');
        payload.append("\"test\":\"" + testId + "\"");
        payload.append(',');
        // Only include userId if the coverage agents are configured in multi-user mode
        if (ParasoftSettings.isMultiUserMode()) {
            payload.append("\"userId\":\"" + resolvedCoverageUserId + "\"");
            payload.append(',');
        }
        payload.append("\"result\":\"" + (passed ? "PASS" : "FAIL") + "\"");
        if (!passed && message != null) {
            payload.append(',');
            payload.append("\"message\":\"" + message + "\"");
        }
        payload.append('}');
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/test/stop"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] API call response: " + response.statusCode() + " - " + response.body());
        } catch (SocketException ce) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage(), ce);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Unexpected error during API call", e);
        }
    }

    // CTP REST API: /v3/environments/{envId}/agents/session/stop
    public static void stopSession() {
        stopSession(null); // if coverageUserId is not provided, the API client will assume single user mode
    }

    public static void stopSession(String coverageUserId) {
        String resolvedCoverageUserId = resolveCoverageUserId(coverageUserId);
        StringBuilder payload = new StringBuilder();
        // Only include userId if the coverage agents are configured in multi-user mode
        if (ParasoftSettings.isMultiUserMode()) {
            payload.append('{');
            payload.append("\"userId\":\"" + resolvedCoverageUserId + "\"");
            payload.append('}');
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/session/stop"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] API call response: " + response.statusCode() + " - " + response.body());
        } catch (SocketException ce) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage(), ce);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Unexpected error during API call", e);
        }
    }

    // CTP REST API: /v3/environments/{envId}/coverage/{sessionId}
    public static void publishCoverage(String sessionId, String dtpSessionTag) {
        publishCoverage(sessionId, dtpSessionTag, null); // if coverageUserId is not provided, the API client will assume single user mode  
    }

    public static void publishCoverage(String sessionId, String dtpSessionTag, String coverageUserId) {
        String resolvedCoverageUserId = resolveCoverageUserId(coverageUserId);
        String resolvedDtpSessionTag = resolveDtpSessionTag(dtpSessionTag);
        StringBuilder payload = new StringBuilder();
        payload.append('{');
        payload.append("\"sessionTag\":\"" + resolvedDtpSessionTag + "\"");
        payload.append(',');
        payload.append("\"analysisType\":\"FUNCTIONAL_TEST\"");
        payload.append('}');
        // Only include userId if the coverage agents are configured in multi-user mode
        URI coverageUri;
        if (ParasoftSettings.isMultiUserMode()) {
            coverageUri = URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/coverage/" + sessionId + "?userId=" + resolvedCoverageUserId);
        } else {
            coverageUri = URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/coverage/" + sessionId);
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(coverageUri)
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] API call response: " + response.statusCode() + " - " + response.body());
        } catch (SocketException ce) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage(), ce);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Unexpected error during API call", e);
        }

        // System property 'PUBLISH_BASELINE' defines whether a BASELINE_BUILD_ID (also a System property) gets published to CTP for this test execution.
        if (ParasoftSettings.CTP_PUBLISH_BASELINE) {
            publishBaseline();
        }
    }

    // CTP REST API: /v3/environments/{envId}/coverage/baselines/{baselineId}
    private static void publishBaseline() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID+ "/coverage/baselines/" + ParasoftSettings.CTP_BASELINE_BUILD_ID))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        try {
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftCTPApiClient] API call response: " + response.statusCode() + " - " + response.body());
        } catch (SocketException ce) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage(), ce);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ParasoftCTPApiClient] Unexpected error during API call", e);
        }
    }

    private static String resolveCoverageUserId(String coverageUserId) {
        if (coverageUserId == null || coverageUserId.isBlank()) {
            return "NoCoverageUserIdProvided";
        }
        return coverageUserId;
    }

    private static String resolveDtpSessionTag(String dtpSessionTag) {
       if (dtpSessionTag == null || dtpSessionTag.isBlank()) {
            return "NoDTPSessionTagProvided";
        }
        return dtpSessionTag;
    }
}
