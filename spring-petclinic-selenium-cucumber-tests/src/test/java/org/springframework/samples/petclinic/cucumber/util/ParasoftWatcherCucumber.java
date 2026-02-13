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

import org.springframework.samples.petclinic.testcommon.*;

import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;

// import org.openqa.selenium.devtools.DevTools;
// import org.openqa.selenium.devtools.v142.network.Network;
// import org.openqa.selenium.devtools.v142.network.model.Headers;
// import java.util.HashMap;
// import java.util.Optional;

public class ParasoftWatcherCucumber {
    @Before
    public void beforeScenario(Scenario scenario) {
        ParasoftCTPApiClient.startTest(scenario.getName());
    }

    @After
    public void afterScenario(Scenario scenario) {
        ParasoftCTPApiClient.stopTest(
            scenario.getName(),
            !scenario.isFailed(),
            scenario.isFailed() && scenario.getStatus() != null ? scenario.getStatus().toString() : null
        );
    }

    //*** Leaving this here for reference as an alternative approach to using a proxy server for header injection
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
}
