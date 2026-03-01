package org.springframework.samples.petclinic.testcommon.junit5.cucumber;

import org.springframework.samples.petclinic.testcommon.ParasoftCTPApiClient;
import org.springframework.samples.petclinic.testcommon.ParasoftSessionManager;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Cucumber lifecycle hook that reports individual scenario start and stop events to Parasoft CTP
 * for per-test coverage tracking.
 * <p>
 * This is the Cucumber equivalent of
 * {@link org.springframework.samples.petclinic.testcommon.junit5.ParasoftWatcher ParasoftWatcher}.
 */
public class ParasoftWatcherCucumber {
	@Before
	public void beforeScenario(Scenario scenario) {
		String testId = ParasoftCucumberUtil.getTestId(scenario);

		// If multi-user mode, the coverage user ID is retrieved from the Session Manager
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.startTest(testId, ParasoftSessionManager.getCoverageUserId(testId));
            return;
        }
        ParasoftCTPApiClient.startTest(testId); // if not running in multi-user mode, the coverage user ID is not needed
	}

	@After
	public void afterScenario(Scenario scenario) {
		String testId = ParasoftCucumberUtil.getTestId(scenario);
		
        // If multi-user mode, the coverage user ID is retrieved from the Session Manager
        if (ParasoftSettings.isMultiUserMode()) {
            ParasoftCTPApiClient.stopTest(
					testId,
					!scenario.isFailed(),
					scenario.isFailed() ? buildFailureMessage(scenario) : null,
					ParasoftSessionManager.getCoverageUserId(testId));
			return;
        }
        // If not running in multi-user mode, the coverage user ID is not needed
		ParasoftCTPApiClient.stopTest(
				testId,
				!scenario.isFailed(),
				scenario.isFailed() ? buildFailureMessage(scenario) : null); 
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