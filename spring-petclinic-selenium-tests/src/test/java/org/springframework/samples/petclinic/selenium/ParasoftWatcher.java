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

import org.springframework.samples.petclinic.testcommon.*;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

public class ParasoftWatcher implements BeforeEachCallback, TestWatcher  {
	
	// *** Leaving this here for reference as an alternative approach to using a proxy server for header injection
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
		String testId = getTestId(context);
		ParasoftCTPApiClient.startTest(testId);

		// *** Leaving this here for reference as an alternative approach to using a proxy server for header injection
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
		String testId = getTestId(context);
		ParasoftCTPApiClient.stopTest(testId, true, null);
	}

	@Override
	public void testFailed(ExtensionContext context, Throwable cause) {
		String testId = getTestId(context);
		ParasoftCTPApiClient.stopTest(testId, false, cause.getMessage());
	}

	private static String getTestId(ExtensionContext context) {
		return context.getTestClass().get().getName() + '#' + context.getTestMethod().get().getName();
	}
}