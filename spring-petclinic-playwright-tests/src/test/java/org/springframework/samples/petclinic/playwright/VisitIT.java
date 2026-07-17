package org.springframework.samples.petclinic.playwright;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.parasoft.coverage.integration.playwright.PlaywrightCoverageIntegration;

public class VisitIT {
    static Playwright playwright;
    static Browser browser;

    private static final String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("org.springframework.samples.petclinic.headless", "false"));

    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setSlowMo(500).setHeadless(HEADLESS));
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void createContextAndPage() {
        // Using Playwright API for request header injection, required for Parasoft
        // coverage reporting when agents are in multi-user mode
        Browser.NewContextOptions browserContextOptions = PlaywrightCoverageIntegration.createBrowserContextOptions();
        context = browser.newContext(browserContextOptions);
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void testVisitTest() {
        page.navigate(PETCLINIC_URL);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Owners")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("All")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("George Franklin")).click();
        page.locator("tr:has-text(\"Leo\")").getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Add Visit")).click();
        page.locator("input[type=\"date\"]").fill("2023-11-15");
        page.locator("textarea").fill("Yearly checkup");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add New Visit")).click();
    }
}