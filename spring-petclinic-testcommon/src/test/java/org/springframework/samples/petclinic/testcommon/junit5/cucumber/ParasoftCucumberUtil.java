package org.springframework.samples.petclinic.testcommon.junit5.cucumber;

import java.net.URI;

import io.cucumber.java.Scenario;

/**
 * Utility methods for extracting test identifiers from Cucumber scenarios for Parasoft DTP reporting.
 */
public class ParasoftCucumberUtil {
	// Results in a test name like name.feature#Scenario Name, where
	// "filename.feature" is the test file reported in DTP, and
	// "filename.feature#Scenario Name" is the test name reported in DTP.
	public static String getTestId(Scenario scenario) {
		String scenarioName = scenario.getName();
		String featureFileName = extractFeatureFileName(scenario.getUri());
		if (featureFileName == null || featureFileName.isBlank()) {
			return scenarioName;
		}
		return featureFileName + '#' + scenarioName;
	}

	public static String extractFeatureFileName(URI uri) {
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
}
