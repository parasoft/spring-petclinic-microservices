/**
 * ParasoftWatcher integrates JUnit test execution with Parasoft CTP for test-level coverage tracking.
 * <p>
 * Implements BeforeEachCallback and TestWatcher to:
 * <ul>
 *   <li>Tell the CTP coverage agents when a test is starting</li>
 *   <li>Tell the CTP coverage agents when a test has passed or failed</li>
 * </ul>
 * Handles authentication, error logging, and multi-user mode support.
 */
package org.springframework.samples.petclinic.selenium;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
// import org.openqa.selenium.chrome.ChromeDriver;
// import org.openqa.selenium.devtools.DevTools;
// import org.openqa.selenium.devtools.v142.network.Network;
// import org.openqa.selenium.devtools.v142.network.model.Headers;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Base64;
// import java.util.HashMap;
// import java.util.Optional;
import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ParasoftWatcher implements BeforeEachCallback, TestWatcher  {
	private static final Logger LOGGER = Logger.getLogger(ParasoftWatcher.class.getName());
	private static final String basicAuth = ParasoftSettings.CTP_USERNAME + ":" + ParasoftSettings.CTP_PASSWORD;

	// Selenium DevTools header injection for coverage agent baggage header
	// public static void injectBaggageHeader(ChromeDriver driver) {
	// 	DevTools devTools = driver.getDevTools();
	// 	devTools.createSession();
	// 	devTools.send(Network.enable(
	// 		Optional.empty(), // maxTotalBufferSize
	// 		Optional.empty(), // maxResourceBufferSize
	// 		Optional.empty(), // maxPostDataSize
	// 		Optional.empty(), // maxBlockedCookies
	// 		Optional.empty()  // maxBlockedRequests
	// 	));
	// 	HashMap<String, Object> headers = new HashMap<>();
	// 	headers.put("baggage", "test-operator-id=" + ParasoftSettings.getCoverageUserId());
	// 	devTools.send(Network.setExtraHTTPHeaders(new Headers(headers)));
	// }

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		// CTP REST API: /v3/environments/{envId}/agents/test/start
		String testId = getTestId(context);
		try {
			StringBuilder testStartPayload = new StringBuilder();
			testStartPayload.append('{');
			testStartPayload.append("\"test\":\"" + testId + "\"");
			if (ParasoftSettings.CTP_MULTI_USER_MODE.equalsIgnoreCase("true")) {
				testStartPayload.append(',');
				testStartPayload.append("\"userId\":\"" + ParasoftSettings.getCoverageUserId() + "\"");
			}
			testStartPayload.append('}');
			HttpClient client = HttpClient.newBuilder().build();
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/test/start"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
				.POST(HttpRequest.BodyPublishers.ofString(testStartPayload.toString()))
				.build();
			if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftWatcher] Sending API call: " + request.uri());
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftWatcher] API call response: " + response.statusCode() + " - " + response.body());
		} catch (SocketException ce) {
			LOGGER.log(Level.SEVERE, "[ParasoftWatcher] Connection error during API call: " + ce.getMessage(), ce);
		} catch (IOException ioe) {
			LOGGER.log(Level.SEVERE, "[ParasoftWatcher] IO error during API call: " + ioe.getMessage(), ioe);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ParasoftWatcher] Unexpected error during API call", e);
		}
		// Object testInstance = context.getTestInstance().orElse(null);
		// if (testInstance != null) {
		// 	try {
		// 		java.lang.reflect.Field driverField = testInstance.getClass().getDeclaredField("driver");
		// 		driverField.setAccessible(true);
		// 		Object driverObj = driverField.get(testInstance);
		// 		if (driverObj instanceof ChromeDriver) {
		// 			injectBaggageHeader((ChromeDriver) driverObj);
		// 		}
		// 	} catch (Exception e) {
		// 		e.printStackTrace();
		// 	}
		// }
	}

	@Override
	public void testSuccessful(ExtensionContext context) {
		// CTP REST API: /v3/environments/{envId}/agents/test/stop
		try {
			String testId = getTestId(context);
			StringBuilder testSuccessPayload = new StringBuilder();
			testSuccessPayload.append('{');
			testSuccessPayload.append("\"test\":\"" + testId + "\"");
			testSuccessPayload.append(',');
			if (ParasoftSettings.CTP_MULTI_USER_MODE.equalsIgnoreCase("true")) {
				testSuccessPayload.append("\"userId\":\"" + ParasoftSettings.getCoverageUserId() + "\"");
				testSuccessPayload.append(',');
			}
			testSuccessPayload.append("\"result\":\"PASS\"");
			testSuccessPayload.append('}');
			HttpClient client = HttpClient.newBuilder().build();
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/test/stop"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
				.POST(HttpRequest.BodyPublishers.ofString(testSuccessPayload.toString()))
				.build();
			if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftWatcher] Sending API call: " + request.uri());
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftWatcher] API call response: " + response.statusCode() + " - " + response.body());
		} catch (SocketException ce) {
			LOGGER.log(Level.SEVERE, "[ParasoftWatcher] Connection error during API call: " + ce.getMessage(), ce);
		} catch (IOException ioe) {
			LOGGER.log(Level.SEVERE, "[ParasoftWatcher] IO error during API call: " + ioe.getMessage(), ioe);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ParasoftWatcher] Unexpected error during API call", e);
		}
	}

	@Override
	public void testFailed(ExtensionContext context, Throwable cause) {
		// CTP REST API: /v3/environments/{envId}/agents/test/stop
		try {
			String testId = getTestId(context);
			StringBuilder testFailedPayload = new StringBuilder();
			testFailedPayload.append('{');
			testFailedPayload.append("\"test\":\"" + testId + "\"");
			testFailedPayload.append(',');
			if (ParasoftSettings.CTP_MULTI_USER_MODE.equalsIgnoreCase("true")) {
				testFailedPayload.append("\"userId\":\"" + ParasoftSettings.getCoverageUserId() + "\"");
				testFailedPayload.append(',');
			}
			testFailedPayload.append("\"result\":\"FAIL\"");
			testFailedPayload.append(',');
			testFailedPayload.append("\"message\":\"" + cause.getMessage() + "\"");
			testFailedPayload.append('}');
			HttpClient client = HttpClient.newBuilder().build();
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(ParasoftSettings.CTP_BASE_URL + "/api/v3/environments/" + ParasoftSettings.CTP_ENV_ID + "/agents/test/stop"))
				.header("Content-Type", "application/json")
				.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basicAuth.getBytes()))
				.POST(HttpRequest.BodyPublishers.ofString(testFailedPayload.toString()))
				.build();
			if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftWatcher] Sending API call: " + request.uri());
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (ParasoftSettings.CTP_DEBUG) LOGGER.info("[ParasoftWatcher] API call response: " + response.statusCode() + " - " + response.body());
		} catch (SocketException ce) {
			LOGGER.log(Level.SEVERE, "[ParasoftWatcher] Connection error during API call: " + ce.getMessage(), ce);
		} catch (IOException ioe) {
			LOGGER.log(Level.SEVERE, "[ParasoftWatcher] IO error during API call: " + ioe.getMessage(), ioe);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "[ParasoftWatcher] Unexpected error during API call", e);
		}
	}
	
	private static String getTestId(ExtensionContext context) {
		return context.getTestClass().get().getName() + '#' + context.getTestMethod().get().getName();
	}
}
