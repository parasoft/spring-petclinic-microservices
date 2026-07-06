package org.springframework.samples.petclinic.playwright;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.parasoft.coverage.integration.playwright.PlaywrightCoverageIntegration;

    public class NavigateIT {
    static Playwright playwright;
    static Browser browser;

    private static final String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");

    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(500));
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
        NewContextOptions browserContextOptions = PlaywrightCoverageIntegration.createBrowserContextOptions();
        context = browser.newContext(browserContextOptions);
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void testPetClinicNavigation() {
        page.navigate(PETCLINIC_URL);
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Veterinarians")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Owners")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("All")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Betty Davis")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Basil")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Home")).click();
    }
}
