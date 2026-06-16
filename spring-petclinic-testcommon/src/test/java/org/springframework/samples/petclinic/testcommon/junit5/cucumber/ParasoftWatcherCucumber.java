package org.springframework.samples.petclinic.testcommon.junit5.cucumber;

import org.springframework.samples.petclinic.testcommon.ParasoftWatcherUtil;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Cucumber lifecycle hook that reports individual scenario start and stop events to Parasoft CTP
 * for per-test coverage tracking.
 * <p>
 * This is the Cucumber equivalent of
 * {@link org.springframework.samples.petclinic.testcommon.junit5.ParasoftWatcher ParasoftWatcher}.
 * Cucumber parallel execution is not yet implemented in this project, but the shared
 * {@link ParasoftWatcherUtil} will branch on {@code isParallelTestExecution()} just like the
 * other watchers if that ever changes.
 */
public class ParasoftWatcherCucumber {
	private static final String WATCHER_TAG = "ParasoftWatcherCucumber";

	@Before
	public void beforeScenario(Scenario scenario) {
		String testId = ParasoftCucumberUtil.getTestId(scenario);
		// For Cucumber the testContextKey is the scenario testId itself (one context per scenario).
		ParasoftWatcherUtil.startTest(testId, testId, WATCHER_TAG);
	}

	@After
	public void afterScenario(Scenario scenario) {
		String testId = ParasoftCucumberUtil.getTestId(scenario);
		boolean passed = !scenario.isFailed();
		String failureMessage = passed ? null : buildFailureMessage(scenario);
		ParasoftWatcherUtil.stopTest(testId, testId, passed, failureMessage, WATCHER_TAG);
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