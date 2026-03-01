package org.springframework.samples.petclinic.playwright;

import java.util.HashMap;
import java.util.Map;

import org.springframework.samples.petclinic.testcommon.ParasoftSessionManager;
import org.springframework.samples.petclinic.testcommon.ParasoftSettings;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

@ExtendWith(org.springframework.samples.petclinic.testcommon.junit5.playwright.ParasoftWatcherPlaywright.class)
public class VisitIT {
       static Playwright playwright;
    static Browser browser;

    private static final String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");

    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(ParasoftSettings.isHeadless()).setSlowMo(500));
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        // Using Playwright API for request header injection, required for Parasoft
        // coverage reporting when agents are in multi-user mode
        if (ParasoftSettings.isMultiUserMode()) {
            Map<String, String> headers = new HashMap<>();
            headers.put("baggage", "test-operator-id=" + ParasoftSessionManager.getCoverageUserId());
            context.setExtraHTTPHeaders(headers);
        }
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