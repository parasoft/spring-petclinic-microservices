package org.springframework.samples.petclinic.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.*;
import org.springframework.samples.petclinic.selenium.util.ParasoftWatcher;
import org.littleshoot.proxy.HttpProxyServer;
import org.springframework.samples.petclinic.testcommon.ParasoftHeaderInjectingProxy;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

@Listeners({ParasoftWatcher.class})
public class PetIT {
    private static WebDriver driver;
    private static HttpProxyServer proxy;

    @BeforeClass
    public void openBrowser() {
        ChromeOptions options = new ChromeOptions();

        // Using LittleProxy for request header injection, required for Parasoft
        // coverage reporting when agents are in multi-user mode
        if (ParasoftSettings.CTP_MULTI_USER_MODE.equalsIgnoreCase("true")) {
            proxy = ParasoftHeaderInjectingProxy.startProxy();
            int proxyPort = proxy.getListenAddress().getPort();

            Proxy seleniumProxy = new Proxy();
            seleniumProxy.setHttpProxy("localhost:" + proxyPort);
            seleniumProxy.setSslProxy("localhost:" + proxyPort);
            seleniumProxy.setNoProxy("<-loopback>");

            options.setProxy(seleniumProxy);
        }
        if (ParasoftSettings.isHeadless()) {
            options.addArguments("--headless=new");
        }
        
        driver = new ChromeDriver(options);
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
        driver.get("http://localhost:8099/");
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
