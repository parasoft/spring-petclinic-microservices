/**
 * ParasoftHeaderInjectingSeleniumDevTools injects Parasoft baggage headers via Selenium DevTools.
 */
package org.springframework.samples.petclinic.testcommon;

import java.util.HashMap;
import java.util.Optional;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v142.network.Network;
import org.openqa.selenium.devtools.v142.network.model.Headers;

// *** Leaving this here for reference as an alternative approach to using a proxy server for header injection
public final class ParasoftHeaderInjectingSeleniumDevTools {
    private ParasoftHeaderInjectingSeleniumDevTools() {
    }

    public static void injectBaggageHeader(ChromeDriver driver, String coverageUserId) {
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        devTools.send(Network.enable(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("baggage", "test-operator-id=" + coverageUserId);
        devTools.send(Network.setExtraHTTPHeaders(new Headers(headers)));
    }
}