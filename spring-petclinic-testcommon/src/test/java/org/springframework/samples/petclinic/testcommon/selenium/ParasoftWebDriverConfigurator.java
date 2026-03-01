package org.springframework.samples.petclinic.testcommon.selenium;

import org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Configures WebDriver capabilities for Parasoft CTP integration, including
 * proxy setup for header injection and headless mode.
 */
public class ParasoftWebDriverConfigurator implements WebDriverConfigurator {
    private ParasoftHeaderInjectingProxy parasoftProxyHandle;
    private String testContextKey;

    public ParasoftWebDriverConfigurator(String testContextKey) {
        this.testContextKey = testContextKey;
    }

    public ParasoftHeaderInjectingProxy getParasoftProxyHandle() {
        return parasoftProxyHandle;
    }

    public String getTestContextKey() {
        return testContextKey; 
    }

    @Override
    public void configure(MutableCapabilities options) {
        // Start the Parasoft header-injecting proxy if in multi-user mode
        int proxyPort = -1;
        if (ParasoftSettings.isMultiUserMode()) {
            parasoftProxyHandle = new ParasoftHeaderInjectingProxy();
            proxyPort = parasoftProxyHandle.getProxy().getListenAddress().getPort();
        }
        
        // Configure ChromeOptions if the provided options are of that type
        if (options instanceof ChromeOptions) {
            ChromeOptions chromeOptions = (ChromeOptions) options;
            if (ParasoftSettings.isMultiUserMode() && proxyPort != -1) {
                Proxy seleniumProxy = new Proxy();
                seleniumProxy.setHttpProxy(ParasoftSettings.PROXY_HOST + ":" + proxyPort);
                seleniumProxy.setSslProxy(ParasoftSettings.PROXY_HOST + ":" + proxyPort);
                seleniumProxy.setNoProxy("<-loopback>");

                chromeOptions.setProxy(seleniumProxy);
            }
            if (ParasoftSettings.isHeadless()) {
                chromeOptions.addArguments("--headless=new");
            }
        }
        
        // Add support for other browsers as needed
    }
}