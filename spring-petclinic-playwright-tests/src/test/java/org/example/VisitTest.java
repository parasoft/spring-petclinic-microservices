package org.example;

import java.util.HashMap;
import java.util.Map;

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

@ExtendWith(ParasoftWatcher.class)
public class VisitTest {
       static Playwright playwright;
    static Browser browser;

    BrowserContext context;
    Page page;
    String userId;
    String petclinicUrl;

    public VisitTest(String userId) {
        this.userId = userId;
        petclinicUrl = System.getProperty("petclinicUrl");
        if (petclinicUrl == null) {
            petclinicUrl = "http://localhost:8080";
        }
    }

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        boolean headless = Boolean.valueOf(System.getProperty("headless"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless).setSlowMo(500));
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        Map<String, String> headers = new HashMap<>();
        headers.put("baggage", "test-operator-id=" + userId);
        context.setExtraHTTPHeaders(headers);
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void testVisitTest() {
        page.navigate(petclinicUrl);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Owners")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("All")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("George Franklin")).click();
        page.locator("tr:has-text(\"Leo\")").getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Add Visit")).click();
        page.locator("input[type=\"date\"]").fill("2023-11-15");
        page.locator("textarea").fill("Yearly checkup");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add New Visit")).click();
    }
}
