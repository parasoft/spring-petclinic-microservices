package org.springframework.samples.petclinic.playwright;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

@ExtendWith(org.springframework.samples.petclinic.testcommon.junit5.playwright.ParasoftWatcherPlaywright.class)
public class VetsIT {
    static Playwright playwright;
    static Browser browser;

    private static final String PETCLINIC_URL = System.getProperty("PETCLINIC_URL", "http://localhost:8099");

    private static String playwrightSessionId;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwrightSessionId = UUID.randomUUID().toString();
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(ParasoftSettings.isHeadless()).setSlowMo(500));
        if (ParasoftSettings.isParallelTestExecution() && ParasoftSettings.isMultiUserMode()) {
            ParasoftSessionManager.registerParallelId(VetsIT.class.getName(), playwrightSessionId);
        }
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
        ParasoftSessionManager.unregister(VetsIT.class.getName());
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        // Using Playwright API for request header injection, required for Parasoft
        // coverage reporting when agents are in multi-user mode
        if (ParasoftSettings.isMultiUserMode()) {
            String baggage = ParasoftSessionManager.getBaggage(VetsIT.class.getName());
            if (baggage != null && !baggage.isBlank()) {
                Map<String, String> headers = new HashMap<>();
                headers.put("baggage", baggage);
                context.setExtraHTTPHeaders(headers);
            }
        }
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void testVetsTest() {
        page.navigate(PETCLINIC_URL);
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Veterinarians")).click();
    }
}
