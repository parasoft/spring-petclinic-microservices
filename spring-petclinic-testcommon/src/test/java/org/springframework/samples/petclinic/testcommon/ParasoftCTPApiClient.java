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
            LOGGER.info("[ParasoftCTPApiClient] startSession: envId=" + ParasoftSettings.CTP_ENV_ID + ", userId=" + resolvedUserId);
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
                LOGGER.info("[ParasoftCTPApiClient] -> POST " + apiPath(request.uri()) + " payload=" + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] <- " + response.statusCode() + " body=" + response.body());
                }
                String sessionId = parseStringField(response.body(), "session");
                if (sessionId != null && !sessionId.isBlank()) {
                    if (ParasoftSettings.isLogLevelEnabled("INFO")) {
                        LOGGER.info("[ParasoftCTPApiClient] startSession completed: sessionId=" + sessionId);
                    }
                    return sessionId;
                }
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] startSession failed: response missing 'session' field, body=" + summarizeBody(response.body()));
                }
            } else {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] startSession failed: " + response.statusCode() + " Response - " + summarizeBody(response.body()));
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] startSession failed: connection error - " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] startSession failed: IO error - " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] startSession failed: unexpected error - " + e.getMessage());
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
            LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] startTest");
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
                LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] -> POST " + apiPath(request.uri()) + " payload=" + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] <- " + response.statusCode() + " body=" + response.body());
                }
            } else {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] startTest failed: " + response.statusCode() + " Response - " + summarizeBody(response.body()));
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] startTest failed: connection error - " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] startTest failed: IO error - " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] startTest failed: unexpected error - " + e.getMessage());
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
        String resolvedUserId = resolveUserId(userId);
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            String parallelSuffix = (parallelId != null && !parallelId.isBlank()) ? ", parallelId=" + parallelId : "";
            LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] startTest: userId=" + resolvedUserId + parallelSuffix);
        }
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
                LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] -> POST " + apiPath(request.uri()) + " payload=" + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] <- " + response.statusCode() + " body=" + response.body());
                }
                String baggage = parseStringField(response.body(), "baggage");
                if (baggage != null && !baggage.isBlank()) {
                    return baggage;
                }
            } else {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] startTest failed: " + response.statusCode() + " Response - " + summarizeBody(response.body()) + "; using fallback baggage");
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] startTest failed: connection error - " + ce.getMessage() + "; using fallback baggage");
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] startTest failed: IO error - " + ioe.getMessage() + "; using fallback baggage");
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] startTest failed: unexpected error - " + e.getMessage() + "; using fallback baggage");
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
            LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] stopTest: result=" + (passed ? "PASS" : "FAIL"));
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
                LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] -> POST " + apiPath(request.uri()) + " payload=" + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] <- " + response.statusCode() + " body=" + response.body());
                }
            } else {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] stopTest failed: " + response.statusCode() + " Response - " + summarizeBody(response.body()));
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] stopTest failed: connection error - " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] stopTest failed: IO error - " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] stopTest failed: unexpected error - " + e.getMessage());
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
        String resolvedUserId = resolveUserId(userId);
        if (ParasoftSettings.isLogLevelEnabled("INFO")) {
            String parallelSuffix = (parallelId != null && !parallelId.isBlank()) ? ", parallelId=" + parallelId : "";
            LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] stopTest: userId=" + resolvedUserId + parallelSuffix + ", result=" + (passed ? "PASS" : "FAIL"));
        }
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
                LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] -> POST " + apiPath(request.uri()) + " payload=" + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] [" + testId + "] <- " + response.statusCode() + " body=" + response.body());
                }
            } else {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] stopTest failed: " + response.statusCode() + " Response - " + summarizeBody(response.body()));
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] stopTest failed: connection error - " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] stopTest failed: IO error - " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + testId + "] stopTest failed: unexpected error - " + e.getMessage());
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
            LOGGER.info("[ParasoftCTPApiClient] stopSession: envId=" + ParasoftSettings.CTP_ENV_ID + ", userId=" + resolvedUserId);
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
                LOGGER.info("[ParasoftCTPApiClient] -> POST " + apiPath(request.uri()) + " payload=" + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] <- " + response.statusCode() + " body=" + response.body());
                }
            } else {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] stopSession failed: " + response.statusCode() + " Response - " + summarizeBody(response.body()));
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] stopSession failed: connection error - " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] stopSession failed: IO error - " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] stopSession failed: unexpected error - " + e.getMessage());
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
            LOGGER.info("[ParasoftCTPApiClient] [" + sessionId + "] publishCoverage: envId=" + ParasoftSettings.CTP_ENV_ID + ", userId=" + resolvedUserId + ", dtpSessionTag=" + resolvedDtpSessionTag);
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
                LOGGER.info("[ParasoftCTPApiClient] [" + sessionId + "] -> POST " + apiPath(request.uri()) + " payload=" + payload);
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] [" + sessionId + "] <- " + response.statusCode() + " body=" + response.body());
                }
            } else {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] [" + sessionId + "] publishCoverage failed: " + response.statusCode() + " Response - " + summarizeBody(response.body()));
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + sessionId + "] publishCoverage failed: connection error - " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + sessionId + "] publishCoverage failed: IO error - " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + sessionId + "] publishCoverage failed: unexpected error - " + e.getMessage());
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
            LOGGER.info("[ParasoftCTPApiClient] [" + ParasoftSettings.CTP_BASELINE_BUILD_ID + "] publishBaseline: envId=" + ParasoftSettings.CTP_ENV_ID);
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID+ "/coverage/baselines/" + ParasoftSettings.CTP_BASELINE_BUILD_ID))
            .header("Content-Type", "application/json")
            .header("Authorization", AUTH_HEADER)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        try {
            if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] [" + ParasoftSettings.CTP_BASELINE_BUILD_ID + "] -> POST " + apiPath(request.uri()) + " payload=<none>");
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                    LOGGER.info("[ParasoftCTPApiClient] [" + ParasoftSettings.CTP_BASELINE_BUILD_ID + "] <- " + response.statusCode() + " body=" + response.body());
                }
            } else {
                if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                    LOGGER.severe("[ParasoftCTPApiClient] [" + ParasoftSettings.CTP_BASELINE_BUILD_ID + "] publishBaseline failed: " + response.statusCode() + " Response - " + summarizeBody(response.body()));
                }
            }
        } catch (SocketException ce) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + ParasoftSettings.CTP_BASELINE_BUILD_ID + "] publishBaseline failed: connection error - " + ce.getMessage());
            }
        } catch (IOException ioe) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + ParasoftSettings.CTP_BASELINE_BUILD_ID + "] publishBaseline failed: IO error - " + ioe.getMessage());
            }
        } catch (Exception e) {
            if (ParasoftSettings.isLogLevelEnabled("ERROR")) {
                LOGGER.severe("[ParasoftCTPApiClient] [" + ParasoftSettings.CTP_BASELINE_BUILD_ID + "] publishBaseline failed: unexpected error - " + e.getMessage());
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
                    LOGGER.warning("[ParasoftCTPApiClient] resolveUserId: no user ID provided in multi-user mode; using fallback 'NoUserIdProvided' (configuration or lifecycle error - coverage may be unattributable)");
                }
            } else if (ParasoftSettings.isLogLevelEnabled("DEBUG")) {
                LOGGER.info("[ParasoftCTPApiClient] resolveUserId: no user ID provided, assuming single-user mode");
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
                LOGGER.warning("[ParasoftCTPApiClient] parseStringField failed: field='" + fieldName + "' - " + parseEx.getMessage());
            }
            return null;
        }
    }

    /** Resolves the DTP session tag, falling back to a placeholder if not provided. */
    private static String resolveDtpSessionTag(String dtpSessionTag) {
        if (dtpSessionTag == null || dtpSessionTag.isBlank()) {
            if (ParasoftSettings.isLogLevelEnabled("WARN")) {
                LOGGER.warning("[ParasoftCTPApiClient] resolveDtpSessionTag: no tag provided, using fallback 'NoDTPSessionTagProvided'");
            }
            return "NoDTPSessionTagProvided";
        }
        return dtpSessionTag;
    }

    /** Strips the configured base URL + context path from the URI to leave just the API path. */
    private static String apiPath(URI uri) {
        String prefix = ParasoftSettings.CTP_BASE_URL + ParasoftSettings.CTP_CONTEXT_PATH;
        String s = uri.toString();
        return s.startsWith(prefix) ? s.substring(prefix.length()) : s;
    }

    /**
     * Produces a one-line summary of an HTTP response body for inclusion in WARN/SEVERE logs.
     * For HTML bodies, extracts the {@code <title>} text if present (e.g. an Apache Tomcat error
     * page collapses to {@code "HTTP Status 500 - Internal Server Error"}); falls back to a
     * length tag when no title is found. For non-HTML bodies, returns the first line, truncated
     * at 200 characters.
     */
    private static String summarizeBody(String body) {
        if (body == null || body.isEmpty()) {
            return "<empty>";
        }
        if (body.trim().startsWith("<")) {
            int titleStart = body.indexOf("<title>");
            if (titleStart >= 0) {
                int titleEnd = body.indexOf("</title>", titleStart + 7);
                if (titleEnd > titleStart + 7) {
                    return body.substring(titleStart + 7, titleEnd).trim();
                }
            }
            return "<HTML response, " + body.length() + " chars>";
        }
        int newline = body.indexOf('\n');
        String firstLine = (newline >= 0) ? body.substring(0, newline) : body;
        if (firstLine.length() > 200) {
            return firstLine.substring(0, 200) + "...";
        }
        return firstLine;
    }
}
