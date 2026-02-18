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
package org.springframework.samples.petclinic.selenium.util;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

public class ParasoftWatcher implements BeforeEachCallback, TestWatcher  {

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		String testId = getTestId(context);
		
		// When in multi-user mode, the coverage user ID is required to associate the test with the correct CTP test session for coverage reporting
		if (ParasoftSettings.isMultiUserMode()) {
			ParasoftCTPApiClient.startTest(testId, ParasoftSeleniumContext.getCoverageUserId());
			return;
		}
		ParasoftCTPApiClient.startTest(testId); // if not running in multi-user mode, the coverage user ID is not needed 

		// *** Reference only. Proxy-based header injection is the recommended approach.
		// Object testInstance = context.getTestInstance().orElse(null);
		// if (testInstance != null) {
		// 	try {
		// 		java.lang.reflect.Field driverField = testInstance.getClass().getDeclaredField("driver");
		// 		driverField.setAccessible(true);
		// 		Object driverObj = driverField.get(testInstance);
		// 		if (driverObj instanceof ChromeDriver) {
		// 			ParasoftHeaderInjectingSeleniumDevTools.injectBaggageHeader((ChromeDriver) driverObj);
		// 		}
		// 	} catch (Exception e) {
		// 		e.printStackTrace();
		// 	}
		// }
	}

	@Override
	public void testSuccessful(ExtensionContext context) {
		String testId = getTestId(context);
		
		// When in multi-user mode, the coverage user ID is required to associate the test with the correct CTP test session for coverage reporting
		if (ParasoftSettings.isMultiUserMode()) {
			ParasoftCTPApiClient.stopTest(testId, true, null, ParasoftSeleniumContext.getCoverageUserId());
			return;
		}
		ParasoftCTPApiClient.stopTest(testId, true, null); // if not running in multi-user mode, the coverage user ID is not needed 
	}

	@Override
	public void testFailed(ExtensionContext context, Throwable cause) {
		String testId = getTestId(context);
		
		// When in multi-user mode, the coverage user ID is required to associate the test with the correct CTP test session for coverage reporting
		if (ParasoftSettings.isMultiUserMode()) {
			ParasoftCTPApiClient.stopTest(testId, false, buildFailureMessage(cause), ParasoftSeleniumContext.getCoverageUserId());
			return;
		}
		ParasoftCTPApiClient.stopTest(testId, false, buildFailureMessage(cause)); // if not running in multi-user mode, the coverage user ID is not needed 
	}

	private static String getTestId(ExtensionContext context) {
		return context.getTestClass().get().getName() + '#' + context.getTestMethod().get().getName();
	}

	private static String buildFailureMessage(Throwable cause) {
		if (cause == null) {
			return null;
		}
		String message = cause.getMessage();
		if (message != null) {
			int newlineIndex = message.indexOf('\n');
			if (newlineIndex >= 0) {
				message = message.substring(0, newlineIndex);
			}
			message = message.replace('\r', ' ');
		}
		StringBuilder summary = new StringBuilder();
		summary.append(cause.getClass().getSimpleName());
		if (message != null && !message.isBlank()) {
			summary.append(": ").append(message);
		}
		String sanitized = summary.toString()
				.replace('"', '\'')
				.replace("\n", " ")
				.replace("\t", " ");
		int maxLength = 500;
		return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
	}
}