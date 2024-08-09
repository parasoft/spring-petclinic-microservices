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
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

@ExtendWith(ParasoftWatcher.class)
public class RegisterOwnerAndPetTest {
        static Playwright playwright;
    static Browser browser;

    BrowserContext context;
    Page page;
    String userId;
    String petclinicUrl;

    public RegisterOwnerAndPetTest(String userId) {
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
    void testRegisterOwnerAndPetTest() {
        page.navigate(petclinicUrl);
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Home")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Owners")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Register")).click();
        page.locator("input[name=\"firstName\"]").fill("Mark");
        page.locator("input[name=\"lastName\"]").fill("Verdugo");
        page.locator("input[name=\"address\"]").fill("101 E. Huntington Dr.");
        page.locator("input[name=\"city\"]").fill("Monrovia");
        page.locator("input[name=\"telephone\"]").fill("016267391734");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Mark Verdugo")).last().click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Add New Pet")).click();
        page.locator("input[name=\"name\"]").fill("Arty");
        page.locator("input[type=\"date\"]").fill("2016-11-11");
        page.getByRole(AriaRole.COMBOBOX).selectOption("2");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();   
    }
}
