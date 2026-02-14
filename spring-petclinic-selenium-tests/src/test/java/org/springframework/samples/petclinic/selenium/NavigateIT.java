package org.springframework.samples.petclinic.selenium;

import org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy;
import org.littleshoot.proxy.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

@ExtendWith(org.springframework.samples.petclinic.selenium.util.ParasoftWatcher.class)
public class NavigateIT {
	private static WebDriver driver;
	private static HttpProxyServer proxy;

	@BeforeAll
	static void openBrowser() {
		ChromeOptions options = new ChromeOptions();

		// Using LittleProxy for request header injection, required for Parasoft
		// coverage reporting when agents are in multi-user mode
		if (ParasoftSettings.CTP_MULTI_USER_MODE.equalsIgnoreCase("true")) {
			proxy = ParasoftHeaderInjectingProxy.startProxy();
			int proxyPort = proxy.getListenAddress().getPort();

			Proxy seleniumProxy = new Proxy();
			seleniumProxy.setHttpProxy("localhost:" + proxyPort); // need to replace localhost if running on Grid
			seleniumProxy.setSslProxy("localhost:" + proxyPort); // need to replace localhost if running on Grid
			seleniumProxy.setNoProxy("<-loopback>");
			
			options.setProxy(seleniumProxy);
		}
		if (ParasoftSettings.isHeadless()) {
			options.addArguments("--headless=new");
		}
		
		driver = new ChromeDriver(options);
	}

	@AfterAll
	static void closeBrowser() {
		if (driver != null) {
			driver.quit();
		}
		if (proxy != null) {
			proxy.stop();
		}
	}

	@Test
	public void testPetClinicNavigation() throws Exception {
		driver.get("http://localhost:8099/");
		Thread.sleep(1000);
		driver.findElement(By.xpath("//a[@title=\"veterinarians\"]")).click();
		Thread.sleep(1000);
		driver.findElement(By.xpath("//a[@class=\"dropdown-toggle\"]")).click();
		Thread.sleep(1000);
		driver.findElement(By.xpath("//a[@ui-sref=\"owners\"]")).click();
		Thread.sleep(1000);
		driver.findElement(By.xpath("//owner-list/table/tbody/tr[1]/td[1]/a")).click();
		Thread.sleep(1000);
		driver.findElement(By.xpath("//dd/a")).click();
		Thread.sleep(1000);
		driver.findElement(By.xpath("//a[@title=\"home page\"]")).click();
	}
}
