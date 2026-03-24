package org.springframework.samples.petclinic.testcommon;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Centralizes REST API calls to Parasoft CTP for test session and coverage management.
 * Provides methods for session start/stop, test start/stop, coverage publishing, and baseline publishing.
 */
public class ParasoftCTPApiClient {
    private static final Logger LOGGER = Logger.getLogger(ParasoftCTPApiClient.class.getName());
    private static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private static final String basicAuth = ParasoftSettings.CTP_USERNAME + ":" + ParasoftSettings.CTP_PASSWORD;

    /** Starts a CTP coverage session. CTP REST API: {@code /v3/environments/{envId}/agents/session/start} */
    public static String startSession() {
        return startSession(null);
    }

    /** Starts a CTP coverage session with an optional coverage user ID for multi-user mode. */
    public static String startSession(String coverageUserId) {
        String resolvedCoverageUserId = resolveCoverageUserId(coverageUserId);
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
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
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/session/start"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri());
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            int sessionIndex = responseBody.indexOf("\"session\":");
            if (sessionIndex != -1 && response.statusCode() == 200) {
                int start = responseBody.indexOf('"', sessionIndex + 10) + 1;
                int end = responseBody.indexOf('"', start);
                String sessionId =  responseBody.substring(start, end);
                if (ParasoftSettings.isLogLevelEnabled("INFO")) {
                    LOGGER.info("[ParasoftCTPApiClient] CTP session started successfully. Session ID: " + sessionId);
                }
                return sessionId;
            } else {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] Failed to start CTP session. Response: " + response.statusCode() + " - " + responseBody);
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Unexpected error during API call: " + e.getMessage());
            }
        }
        return null;
    }

    /** Starts a CTP test. CTP REST API: {@code /v3/environments/{envId}/agents/test/start} */
    public static void startTest(String testId) {
        startTest(testId, null);
    }

    /** Starts a CTP test with an optional coverage user ID for multi-user mode. */
    public static void startTest(String testId, String coverageUserId) {
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Starting CTP test: " + testId);
        }

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
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/test/start"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri());
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] Failed to start CTP test. Response: " + response.statusCode() + " - " + response.body());
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Unexpected error during API call: " + e.getMessage());
            }
        }
    }

    /** Stops a CTP test. CTP REST API: {@code /v3/environments/{envId}/agents/test/stop} */
    public static void stopTest(String testId, boolean passed, String message) {
        stopTest(testId, passed, message, null);
    }

    /** Stops a CTP test with an optional coverage user ID for multi-user mode. */
    public static void stopTest(String testId, boolean passed, String message, String coverageUserId) {
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Stopping CTP test: " + testId + " - Result: " + (passed ? "PASS" : "FAIL"));
        }

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
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/test/stop"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] Failed to stop CTP test. Response: " + response.statusCode() + " - " + response.body());
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Unexpected error during API call: " + e.getMessage());
            }
        }
    }

    /** Stops a CTP coverage session. CTP REST API: {@code /v3/environments/{envId}/agents/session/stop} */
    public static void stopSession() {
        stopSession(null);
    }

    /** Stops a CTP coverage session with an optional coverage user ID for multi-user mode. */
    public static void stopSession(String coverageUserId) {
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Stopping CTP session for environment: " + ParasoftSettings.CTP_ENV_ID);
            LOGGER.info("[ParasoftCTPApiClient] Coverage User ID: " + resolveCoverageUserId(coverageUserId));
        }

        String resolvedCoverageUserId = resolveCoverageUserId(coverageUserId);
        StringBuilder payload = new StringBuilder();
        // Only include userId if the coverage agents are configured in multi-user mode
        if (ParasoftSettings.isMultiUserMode()) {
            payload.append('{');
            payload.append("\"userId\":\"" + resolvedCoverageUserId + "\"");
            payload.append('}');
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/session/stop"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri());
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] Failed to stop CTP session. Response: " + response.statusCode() + " - " + response.body());
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Unexpected error during API call: " + e.getMessage());
            }
        }
    }

    /** Publishes coverage data to DTP. CTP REST API: {@code /v3/environments/{envId}/coverage/{sessionId}} */
    public static void publishCoverage(String sessionId, String dtpSessionTag) {
        publishCoverage(sessionId, dtpSessionTag, null);
    }

    /** Publishes coverage data to DTP with an optional coverage user ID for multi-user mode. */
    public static void publishCoverage(String sessionId, String dtpSessionTag, String coverageUserId) {
        String resolvedCoverageUserId = resolveCoverageUserId(coverageUserId);
        String resolvedDtpSessionTag = resolveDtpSessionTag(dtpSessionTag);

        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Publishing coverage to CTP for environment: " + ParasoftSettings.CTP_ENV_ID);
            LOGGER.info("[ParasoftCTPApiClient] Coverage User ID: " + resolvedCoverageUserId);
            LOGGER.info("[ParasoftCTPApiClient] DTP Session Tag: " + resolvedDtpSessionTag);
        }

        StringBuilder payload = new StringBuilder();
        payload.append('{');
        payload.append("\"sessionTag\":\"" + resolvedDtpSessionTag + "\"");
        payload.append(',');
        payload.append("\"analysisType\":\"FUNCTIONAL_TEST\"");
        payload.append('}');
        // Only include userId if the coverage agents are configured in multi-user mode
        URI coverageUri;
        if (ParasoftSettings.isMultiUserMode()) {
            coverageUri = URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/coverage/" + sessionId + "?userId=" + resolvedCoverageUserId);
        } else {
            coverageUri = URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/coverage/" + sessionId);
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(coverageUri)
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri());
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] Failed to publish coverage to CTP. Response: " + response.statusCode() + " - " + response.body());
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Unexpected error during API call: " + e.getMessage());
            }
        }
    }

    /** Publishes the baseline build ID to CTP. CTP REST API: {@code /v3/environments/{envId}/coverage/baselines/{baselineId}} */
    public static void publishBaseline() {
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Publishing baseline to CTP for environment: " + ParasoftSettings.CTP_ENV_ID);
            LOGGER.info("[ParasoftCTPApiClient] Baseline Build ID: " + ParasoftSettings.CTP_BASELINE_BUILD_ID);
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID+ "/coverage/baselines/" + ParasoftSettings.CTP_BASELINE_BUILD_ID))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri());
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] Failed to publish baseline to CTP. Response: " + response.statusCode() + " - " + response.body());
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] Unexpected error during API call: " + e.getMessage());
            }
        }
    }

    /** Resolves the coverage user ID, falling back to a placeholder if not provided. */
    private static String resolveCoverageUserId(String coverageUserId) {
        if (coverageUserId == null || coverageUserId.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] No coverage user ID provided. Assuming single-user mode.");
            }
            return "NoCoverageUserIdProvided";
        }
        return coverageUserId;
    }

    /** Resolves the DTP session tag, falling back to a placeholder if not provided. */
    private static String resolveDtpSessionTag(String dtpSessionTag) {
       if (dtpSessionTag == null || dtpSessionTag.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftCTPApiClient] No DTP session tag provided. Assuming default session tag.");
            }
            return "NoDTPSessionTagProvided";
        }
        return dtpSessionTag;
    }
}
