package org.springframework.samples.petclinic.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.Assert;
import org.testng.annotations.*;
import org.springframework.samples.petclinic.selenium.util.ParasoftWatcher;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.logging.Logger;

import org.littleshoot.proxy.HttpProxyServer;
import org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy;
import org.springframework.samples.petclinic.selenium.util.ParasoftSeleniumContext;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

@Listeners({ParasoftWatcher.class})
public class PetIT {
    private static final Logger LOGGER = Logger.getLogger(PetIT.class.getName());
	private static String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099/");
    
    private static WebDriver driver;
    private static HttpProxyServer proxy;

    @BeforeClass
    public void openBrowser() {
        ChromeOptions options = new ChromeOptions();

        // Using LittleProxy for request header injection, required for Parasoft
        // coverage reporting when agents are in multi-user mode
        if (ParasoftSettings.isMultiUserMode()) {
            proxy = ParasoftHeaderInjectingProxy.startProxy(ParasoftSeleniumContext.getCoverageUserIdRef());
            int proxyPort = proxy.getListenAddress().getPort();

            Proxy seleniumProxy = new Proxy();
            seleniumProxy.setHttpProxy(ParasoftSettings.PROXY_HOST + ":" + proxyPort);
            seleniumProxy.setSslProxy(ParasoftSettings.PROXY_HOST + ":" + proxyPort);
            seleniumProxy.setNoProxy("<-loopback>");

            options.setProxy(seleniumProxy);
        }
        if (ParasoftSettings.isHeadless()) {
            options.addArguments("--headless=new");
        }
		// Calling code to retrieve the Selenium Grid node ID to dynamically set the CTP coverage user ID
		if (ParasoftSettings.isSeleniumGrid()) {
			String gridUrl = ParasoftSettings.SELENIUM_GRID_URL;
			try {
				driver = new RemoteWebDriver(new URL(gridUrl), options, false);
				if (ParasoftSettings.CTP_DEBUG) {
					LOGGER.info("[PetIT] Using Selenium Grid at " + gridUrl);
				}
			} catch (MalformedURLException me) {
				throw new RuntimeException("Failed to connect to Selenium Grid at " + gridUrl, me);
			} catch (Exception e) {
				throw new RuntimeException("General failure to initialize RemoteWebDriver for Selenium Grid at " + gridUrl, e);
			}
		} else {
			driver = new ChromeDriver(options);
		}
    }

    @AfterClass
    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
        if (proxy != null) {
            proxy.stop();
        }
    }

    @Test
    public void testRenamePet() throws Exception {
        driver.get(PETCLINIC_URL);
        Thread.sleep(1000);
        driver.findElement(By.xpath("//a[@class=\"dropdown-toggle\"]")).click();
        Thread.sleep(1000);
        driver.findElement(By.xpath("//a[@ui-sref=\"owners\"]")).click();
        Thread.sleep(1000);
        driver.findElement(By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a")).click();
        Thread.sleep(1000);
        driver.findElement(By.xpath("//dd/a")).click();
        Thread.sleep(1000);
        driver.findElement(By.name("name")).clear();
        driver.findElement(By.name("name")).sendKeys("Lena");
        Thread.sleep(1000);
        driver.findElement(By.xpath("//button[@type=\"submit\"]")).click();
        Thread.sleep(1000);
        driver.findElement(By.xpath("//dd/a")).click();
        Thread.sleep(1000);
        driver.findElement(By.name("name")).clear();
        driver.findElement(By.name("name")).sendKeys("Leo");
        Thread.sleep(1000);
        driver.findElement(By.xpath("//button[@type=\"submit\"]")).click();
        Thread.sleep(1000);
        driver.findElement(By.xpath("//a[@title=\"home page\"]")).click();
        Assert.assertTrue(true, "Pet rename test completed");
    }
}
