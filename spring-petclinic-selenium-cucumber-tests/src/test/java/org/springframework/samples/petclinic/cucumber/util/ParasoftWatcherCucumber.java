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
package org.springframework.samples.petclinic.cucumber.util;

import java.net.URI;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;

public class ParasoftWatcherCucumber {
	private static final ThreadLocal<String> CURRENT_TEST_ID = new ThreadLocal<>();

	@Before
	public void beforeScenario(Scenario scenario) {
		String testId = getTestId(scenario);
		CURRENT_TEST_ID.set(testId);
		if (ParasoftSettings.isMultiUserMode()) {
			ParasoftCTPApiClient.startTest(testId, ParasoftSeleniumContext.getCoverageUserId());
			return;
		}
		ParasoftCTPApiClient.startTest(testId);
	}

	@After
	public void afterScenario(Scenario scenario) {
		String testId = CURRENT_TEST_ID.get();
		if (testId == null) {
			testId = getTestId(scenario);
		}
		if (ParasoftSettings.isMultiUserMode()) {
			ParasoftCTPApiClient.stopTest(
					testId,
					!scenario.isFailed(),
					scenario.isFailed() ? buildFailureMessage(scenario) : null,
					ParasoftSeleniumContext.getCoverageUserId());
			CURRENT_TEST_ID.remove();
			return;
		}
		ParasoftCTPApiClient.stopTest(
				testId,
				!scenario.isFailed(),
				scenario.isFailed() ? buildFailureMessage(scenario) : null);
		CURRENT_TEST_ID.remove();
	}

	// Results in a test name like name.feature#Scenario Name, where
	// "filename.feature" is the test file reported in DTP, and
	// "filename.feature#Scenario Name" is the test name reported in DTP.
	private static String getTestId(Scenario scenario) {
		String scenarioName = scenario.getName();
		String featureFileName = extractFeatureFileName(scenario.getUri());
		if (featureFileName == null || featureFileName.isBlank()) {
			return scenarioName;
		}
		return featureFileName + '#' + scenarioName;
	}

	private static String extractFeatureFileName(URI uri) {
		if (uri == null) {
			return null;
		}
		String path = uri.getPath();
		if (path == null || path.isBlank()) {
			path = uri.toString();
		}
		if (path == null || path.isBlank()) {
			return null;
		}
		if (path.startsWith("classpath:")) {
			path = path.substring("classpath:".length());
		} else if (path.startsWith("file:")) {
			path = path.substring("file:".length());
		}
		path = path.replace('\\', '/');
		String fileName = path.substring(path.lastIndexOf('/') + 1);
		return fileName.isBlank() ? null : fileName;
	}

	private static String buildFailureMessage(Scenario scenario) {
		if (scenario == null) {
			return null;
		}
		String status = scenario.getStatus() != null ? scenario.getStatus().toString() : "FAILED";
		String name = scenario.getName();
		String message = status;
		if (name != null && !name.isBlank()) {
			message = status + ": " + name;
		}
		int newlineIndex = message.indexOf('\n');
		if (newlineIndex >= 0) {
			message = message.substring(0, newlineIndex);
		}
		message = message.replace('\r', ' ');
		String sanitized = message
				.replace('"', '\'')
				.replace("\n", " ")
				.replace("\t", " ");
		int maxLength = 500;
		return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
	}
}
