import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import copy from "../lib/figma-copy.json";

test("CTA states, keyboard access, font and asset loading, AA accessibility", async ({ page }) => {
  const errors: string[] = [];
  const external: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => { if (message.type() === "error") errors.push(message.text()); });
  page.on("request", (request) => {
    if (!request.url().startsWith("http://127.0.0.1:3100")) external.push(request.url());
  });
  await page.goto("/");
  await page.evaluate(() => document.fonts.ready);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(copy.headline);
  await expect(page.getByRole("button")).toHaveCount(13);

  const first = page.getByRole("group", { name: copy.default, exact: true }).getByRole("button", { name: copy.primary });
  await expect(first).toHaveCSS("background-color", "rgb(82, 161, 69)");
  await expect(first).toHaveCSS("color", "rgb(16, 16, 16)");
  await expect(first).toHaveCSS("min-height", "48px");
  await expect(page.getByRole("button", { name: "Compact · 40" })).toHaveCSS("min-height", "40px");
  await expect(page.getByRole("button", { name: "Large · 56" })).toHaveCSS("min-height", "56px");
  await expect(page.locator(".amount-specimen")).toHaveCSS("font-family", /inter/i);
  await expect(page.locator(".amount-specimen")).toHaveCSS("font-feature-settings", /tnum/);
  await expect(page.locator(".art-specimen")).toHaveCSS("border-radius", "20px");
  expect(await page.locator("h1, .serif-specimen").allTextContents()).not.toEqual(expect.arrayContaining([expect.stringMatching(/[₹\d]/)]));
  const compactTarget = await page.getByRole("button", { name: "Compact · 40" }).evaluate((button) => {
    const bounds = button.getBoundingClientRect();
    const target = getComputedStyle(button, "::after");
    return bounds.height + Math.abs(Number.parseFloat(target.top)) + Math.abs(Number.parseFloat(target.bottom));
  });
  expect(compactTarget).toBeGreaterThanOrEqual(48);
  await page.keyboard.press("Tab");
  await expect(first).toBeFocused();
  await expect(first).toHaveCSS("outline-width", "2px");

  for (const button of await page.getByRole("group", { name: copy.disabled, exact: true }).getByRole("button").all()) {
    await expect(button).toBeDisabled();
    const reasonId = await button.getAttribute("aria-describedby");
    await expect(page.locator(`[id="${reasonId}"]`)).toHaveText(copy.disabledReason);
  }
  for (const button of await page.getByRole("group", { name: copy.loading, exact: true }).getByRole("button").all()) {
    await expect(button).toBeDisabled();
    await expect(button).toHaveAttribute("aria-busy", "true");
    await expect(button).toHaveText(copy.loadingLabel);
  }
  await expect(page.locator(".art-image")).toBeVisible();
  expect(await page.locator(".art-image").evaluate((image) => (image as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
  await expect(page.locator("picture source")).toHaveAttribute("srcset", /@1x\.webp 1x.*@2x\.webp 2x/);
  const audit = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21aa"]).analyze();
  expect(audit.violations).toEqual([]);
  expect(errors).toEqual([]);
  expect(external).toEqual([]);
});

test("reduced motion stops the loading indicators", async ({ page }) => {
  await page.emulateMedia({ reducedMotion: "reduce", colorScheme: "dark" });
  await page.goto("/");
  await expect(page.locator("html")).toHaveCSS("color-scheme", "light");
  for (const indicator of await page.locator(".loading-indicator").all()) {
    await expect(indicator).toHaveCSS("transform", "none");
  }
});

for (const width of [320, 768, 1024, 1440]) {
  for (const scale of [1, 1.3, 2]) {
    test(`reflow ${width}px at ${scale * 100}% text`, async ({ page }, testInfo) => {
      await page.setViewportSize({ width, height: 1000 });
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.goto("/");
      await page.addStyleTag({ content: `html { font-size: ${scale * 100}% !important; }` });
      await page.evaluate(() => document.fonts.ready);
      const layout = await page.evaluate(() => ({
        noHorizontalOverflow: document.documentElement.scrollWidth <= window.innerWidth,
        buttons: Array.from(document.querySelectorAll<HTMLButtonElement>("button")).map((button) => {
          const bounds = button.getBoundingClientRect();
          const text = button.querySelector("span:last-child")!.getBoundingClientRect();
          return {
            touchTarget: bounds.height >= Number.parseFloat(getComputedStyle(button).minHeight),
            textContained: text.left >= bounds.left && text.right <= bounds.right + .5 && text.top >= bounds.top && text.bottom <= bounds.bottom + .5,
          };
        }),
      }));
      expect(layout.noHorizontalOverflow).toBe(true);
      expect(layout.buttons.every((button) => button.touchTarget && button.textContained)).toBe(true);
      if (width === 320 || width === 1440) {
        await page.screenshot({ path: testInfo.outputPath("specimen.png"), fullPage: true });
      }
    });
  }
}
