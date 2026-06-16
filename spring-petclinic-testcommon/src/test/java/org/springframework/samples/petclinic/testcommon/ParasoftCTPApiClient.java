package org.springframework.samples.petclinic.testcommon;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Centralizes REST API calls to Parasoft CTP for test session and coverage management.
 * Provides methods for session start/stop, test start/stop, coverage publishing, and baseline publishing.
 */
public class ParasoftCTPApiClient {
    private static final Logger LOGGER = Logger.getLogger(ParasoftCTPApiClient.class.getName());
    private static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private static final String AUTH_HEADER = "Basic " + Base64.getEncoder().encodeToString(
            (ParasoftSettings.CTP_USERNAME + ":" + ParasoftSettings.CTP_PASSWORD).getBytes(StandardCharsets.UTF_8));
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Starts a CTP coverage session. Intended for single-user mode.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/agents/session/start}
     *
     * @return the CTP session ID, or {@code null} on failure
     */
    public static String startSession() {
        return startSession(null);
    }

    /**
     * Starts a CTP coverage session, attributed to {@code userId}. Intended for multi-user mode.
     * <p>
     * The {@code userId} payload field is included only when {@link ParasoftSettings#isMultiUserMode()};
     * in single-user mode the value is ignored and an empty body is sent.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/agents/session/start}
     *
     * @param  userId user ID to attribute the session to
     * @return the CTP session ID, or {@code null} on failure
     */
    public static String startSession(String userId) {
        String resolvedUserId = resolveUserId(userId);
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Starting CTP session for environment: " + ParasoftSettings.CTP_ENV_ID);
            LOGGER.info("[ParasoftCTPApiClient] User ID: " + resolvedUserId);
        }

        StringBuilder payload = new StringBuilder();
        // Only include userId if the coverage agents are configured in multi-user mode
        if (ParasoftSettings.isMultiUserMode()) {
            payload.append('{');
            payload.append("\"userId\":\"" + resolvedUserId + "\"");
            payload.append('}');
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/session/start"))
            .header("Content-Type", "application/json")
            .header("Authorization", AUTH_HEADER)
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri() + " with payload: " + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String sessionId = parseStringField(response.body(), "session");
                if (sessionId != null && !sessionId.isBlank()) {
                    if (ParasoftSettings.isLogLevelEnabled("INFO")) {
                        LOGGER.info("[ParasoftCTPApiClient] CTP session started successfully. Session ID: " + sessionId);
                    }
                    return sessionId;
                }
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] /session/start response missing 'session' field. Body: " + response.body());
                }
            } else {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] Failed to start CTP session. Response: " + response.statusCode() + " - " + response.body());
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

    /**
     * Starts a CTP test. Intended for single-user mode.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/agents/test/start}
     *
     * @param testId unique test identifier
     */
    public static void startTest(String testId) {
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Starting CTP test: " + testId);
        }
        StringBuilder payload = new StringBuilder();
        payload.append('{');
        payload.append("\"test\":\"" + testId + "\"");
        payload.append('}');
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/test/start"))
            .header("Content-Type", "application/json")
            .header("Authorization", AUTH_HEADER)
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri() + " with payload: " + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] Received API response: " + response.statusCode() + " - " + response.body());
                }
            } else {
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

    /**
     * Starts a CTP test, attributed to {@code userId}. Intended for multi-user sequential mode.
     * <p>
     * Returns the {@code baggage} value from the {@code /test/start} response (e.g.
     * {@code "test-operator-id=admin"}). Returns a locally-constructed fallback if the response
     * is unavailable or malformed; never returns {@code null}.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/agents/test/start}
     *
     * @param  testId unique test identifier
     * @param  userId user ID to attribute the test to
     * @return the {@code baggage} header value to inject into AUT requests
     */
    public static String startTest(String testId, String userId) {
        return startTest(testId, userId, null);
    }

    /**
     * Starts a CTP test, attributed to {@code userId} and disambiguated by {@code parallelId}.
     * Intended for multi-user parallel mode.
     * <p>
     * Returns the {@code baggage} value from the {@code /test/start} response (e.g.
     * {@code "test-operator-id=admin+uuid"}). Returns a locally-constructed fallback if the
     * response is unavailable or malformed; never returns {@code null}.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/agents/test/start}
     *
     * @param  testId     unique test identifier
     * @param  userId     user ID to attribute the test to
     * @param  parallelId per-class identifier disambiguating concurrent test classes
     * @return the {@code baggage} header value to inject into AUT requests
     */
    public static String startTest(String testId, String userId, String parallelId) {
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Starting CTP test: " + testId);
        }
        String resolvedUserId = resolveUserId(userId);
        // Build fallback baggage before attempting the API call so it is always available
        String fallbackBaggage = "test-operator-id=" + resolvedUserId;
        if (parallelId != null && !parallelId.isBlank()) {
            fallbackBaggage += "+" + parallelId;
        }
        StringBuilder payload = new StringBuilder();
        payload.append('{');
        payload.append("\"test\":\"" + testId + "\"");
        payload.append(',');
        payload.append("\"userId\":\"" + resolvedUserId + "\"");
        if (parallelId != null && !parallelId.isBlank()) {
            payload.append(',');
            payload.append("\"parallelId\":\"" + parallelId + "\"");
        }
        payload.append('}');
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/test/start"))
            .header("Content-Type", "application/json")
            .header("Authorization", AUTH_HEADER)
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri() + " with payload: " + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] Received API response: " + response.statusCode() + " - " + response.body());
                }
                String baggage = parseStringField(response.body(), "baggage");
                if (baggage != null && !baggage.isBlank()) {
                    return baggage;
                }
            } else {
                if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                    LOGGER.warning("[ParasoftCTPApiClient] Failed to start CTP test. Response: " + response.statusCode() + " - " + response.body() + ". Using fallback baggage.");
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftCTPApiClient] Connection error during API call: " + ce.getMessage() + ". Using fallback baggage.");
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftCTPApiClient] IO error during API call: " + ioe.getMessage() + ". Using fallback baggage.");
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftCTPApiClient] Unexpected error during API call: " + e.getMessage() + ". Using fallback baggage.");
            }
        }
        return fallbackBaggage;
    }

    /**
     * Stops a CTP test and records its result. Intended for single-user mode.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/agents/test/stop}
     *
     * @param testId  unique test identifier (must match the {@code startTest} call)
     * @param passed  {@code true} if the test passed, {@code false} if it failed
     * @param message optional failure message; ignored when {@code passed} is {@code true}
     */
    public static void stopTest(String testId, boolean passed, String message) {
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Stopping CTP test: " + testId + " - Result: " + (passed ? "PASS" : "FAIL"));
        }
        StringBuilder payload = new StringBuilder();
        payload.append('{');
        payload.append("\"test\":\"" + testId + "\"");
        payload.append(',');
        payload.append("\"result\":\"" + (passed ? "PASS" : "FAIL") + "\"");
        if (!passed && message != null) {
            payload.append(',');
            payload.append("\"message\":\"" + message + "\"");
        }
        payload.append('}');
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/test/stop"))
            .header("Content-Type", "application/json")
            .header("Authorization", AUTH_HEADER)
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri() + " with payload: " + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] Received API response: " + response.statusCode() + " - " + response.body());
                }
            } else {
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

    /**
     * Stops a CTP test and records its result, attributed to {@code userId}. Intended for
     * multi-user sequential mode.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/agents/test/stop}
     *
     * @param testId  unique test identifier (must match the {@code startTest} call)
     * @param passed  {@code true} if the test passed, {@code false} if it failed
     * @param message optional failure message; ignored when {@code passed} is {@code true}
     * @param userId  user ID the test was attributed to
     */
    public static void stopTest(String testId, boolean passed, String message, String userId) {
        stopTest(testId, passed, message, userId, null);
    }

    /**
     * Stops a CTP test and records its result, attributed to {@code userId} and disambiguated by
     * {@code parallelId}. Intended for multi-user parallel mode.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/agents/test/stop}
     *
     * @param testId     unique test identifier (must match the {@code startTest} call)
     * @param passed     {@code true} if the test passed, {@code false} if it failed
     * @param message    optional failure message; ignored when {@code passed} is {@code true}
     * @param userId     user ID the test was attributed to
     * @param parallelId per-class identifier disambiguating concurrent test classes
     */
    public static void stopTest(String testId, boolean passed, String message, String userId, String parallelId) {
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Stopping CTP test: " + testId + " - Result: " + (passed ? "PASS" : "FAIL"));
        }
        String resolvedUserId = resolveUserId(userId);
        StringBuilder payload = new StringBuilder();
        payload.append('{');
        payload.append("\"test\":\"" + testId + "\"");
        payload.append(',');
        payload.append("\"userId\":\"" + resolvedUserId + "\"");
        payload.append(',');
        if (parallelId != null && !parallelId.isBlank()) {
            payload.append("\"parallelId\":\"" + parallelId + "\"");
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
            .header("Authorization", AUTH_HEADER)
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri() + " with payload: " + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] Received API response: " + response.statusCode() + " - " + response.body());
                }
            } else {
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

    /**
     * Stops a CTP coverage session. Intended for single-user mode.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/agents/session/stop}
     */
    public static void stopSession() {
        stopSession(null);
    }

    /**
     * Stops a CTP coverage session, attributed to {@code userId}. Intended for multi-user mode.
     * <p>
     * The {@code userId} payload field is included only when {@link ParasoftSettings#isMultiUserMode()};
     * in single-user mode the value is ignored and an empty body is sent.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/agents/session/stop}
     *
     * @param userId user ID the session was attributed to
     */
    public static void stopSession(String userId) {
        String resolvedUserId = resolveUserId(userId);
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Stopping CTP session for environment: " + ParasoftSettings.CTP_ENV_ID);
            LOGGER.info("[ParasoftCTPApiClient] User ID: " + resolvedUserId);
        }

        StringBuilder payload = new StringBuilder();
        // Only include userId if the coverage agents are configured in multi-user mode
        if (ParasoftSettings.isMultiUserMode()) {
            payload.append('{');
            payload.append("\"userId\":\"" + resolvedUserId + "\"");
            payload.append('}');
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/session/stop"))
            .header("Content-Type", "application/json")
            .header("Authorization", AUTH_HEADER)
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri() + " with payload: " + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] Received API response: " + response.statusCode() + " - " + response.body());
                }
            } else {
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

    /**
     * Publishes coverage data to DTP. Intended for single-user mode.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/coverage/{sessionId}}
     *
     * @param sessionId     CTP session ID returned by {@link #startSession()}
     * @param dtpSessionTag DTP session tag identifying this coverage run
     */
    public static void publishCoverage(String sessionId, String dtpSessionTag) {
        publishCoverage(sessionId, dtpSessionTag, null);
    }

    /**
     * Publishes coverage data to DTP, scoped to {@code userId}. Intended for multi-user mode.
     * <p>
     * The {@code userId} is appended as a query parameter only when
     * {@link ParasoftSettings#isMultiUserMode()}; in single-user mode it is omitted from the URL.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/coverage/{sessionId}}
     *
     * @param sessionId     CTP session ID returned by {@link #startSession(String)}
     * @param dtpSessionTag DTP session tag identifying this coverage run
     * @param userId        user ID the coverage data was attributed to
     */
    public static void publishCoverage(String sessionId, String dtpSessionTag, String userId) {
        String resolvedUserId = resolveUserId(userId);
        String resolvedDtpSessionTag = resolveDtpSessionTag(dtpSessionTag);

        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Publishing coverage to CTP for environment: " + ParasoftSettings.CTP_ENV_ID);
            LOGGER.info("[ParasoftCTPApiClient] User ID: " + resolvedUserId);
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
            coverageUri = URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/coverage/" + sessionId + "?userId=" + resolvedUserId);
        } else {
            coverageUri = URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/coverage/" + sessionId);
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(coverageUri)
            .header("Content-Type", "application/json")
            .header("Authorization", AUTH_HEADER)
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri() + " with payload: " + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] Received API response: " + response.statusCode() + " - " + response.body());
                }
            } else {
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

    /**
     * Publishes the configured baseline build ID to CTP. Mode-independent.
     * <p>
     * CTP REST API: {@code POST /api/v3/environments/{envId}/coverage/baselines/{baselineId}}
     */
    public static void publishBaseline() {
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            LOGGER.info("[ParasoftCTPApiClient] Publishing baseline to CTP for environment: " + ParasoftSettings.CTP_ENV_ID);
            LOGGER.info("[ParasoftCTPApiClient] Baseline Build ID: " + ParasoftSettings.CTP_BASELINE_BUILD_ID);
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID+ "/coverage/baselines/" + ParasoftSettings.CTP_BASELINE_BUILD_ID))
            .header("Content-Type", "application/json")
            .header("Authorization", AUTH_HEADER)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] Sending API call: " + request.uri() + " with payload: <none>");
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] Received API response: " + response.statusCode() + " - " + response.body());
                }
            } else {
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

    /**
     * Resolves the user ID, falling back to a placeholder if not provided. Logs at WARN when a
     * user ID is missing in multi-user mode (a likely misconfiguration); logs at DEBUG when
     * missing in single-user mode (the expected case where the lifecycle does not need a user ID).
     */
    private static String resolveUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            if (ParasoftSettings.isMultiUserMode()) {
                if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                    LOGGER.warning("[ParasoftCTPApiClient] No user ID provided in multi-user mode; using fallback 'NoUserIdProvided'. This indicates a configuration or lifecycle error and the resulting coverage data may be unattributable.");
                }
            } else if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] No user ID provided. Assuming single-user mode.");
            }
            return "NoUserIdProvided";
        }
        return userId;
    }

    /**
     * Extracts a top-level string field from a JSON response body. Returns {@code null} if the
     * field is absent, not textual, or if the body cannot be parsed.
     */
    private static String parseStringField(String responseBody, String fieldName) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode root = JSON.readTree(responseBody);
            JsonNode node = root.get(fieldName);
            if (node != null && node.isTextual()) {
                return node.asText();
            }
            return null;
        } catch (Exception parseEx) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftCTPApiClient] Failed to parse JSON response for field '" + fieldName + "': " + parseEx.getMessage());
            }
            return null;
        }
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
