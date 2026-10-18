// Presentation capture only. Inputs are fictional and RAM-only; prototype sources stay untouched.
import { chromium } from '../../tidal-tree-prototype/node_modules/playwright/index.mjs';
import { fileURLToPath } from 'node:url';
import { mkdir } from 'node:fs/promises';

const assets = fileURLToPath(new URL('./assets/', import.meta.url));
await mkdir(assets, { recursive: true });
const browser = await chromium.launch({ channel: 'msedge', headless: true, chromiumSandbox: true });
const errors = [];
const results = [];
try {
  for (const [kind, port] of [['abstract', 5188], ['realistic', 5189]]) {
    const context = await browser.newContext({ viewport: { width: 1280, height: 1100 }, deviceScaleFactor: 2, reducedMotion: 'reduce' });
    const page = await context.newPage();
    page.on('pageerror', error => errors.push(`${kind}: ${error.message}`));
    const button = name => page.getByRole('button', { name, exact: true });
    const home = () => page.locator('.app-window[data-route="home"]').waitFor();
    const shot = async name => {
      await page.waitForTimeout(350);
      await page.locator('.app-window').screenshot({ path: `${assets}${kind}-${name}.png`, animations: 'disabled' });
      results.push(`${kind}-${name}.png`);
    };
    await page.goto(`http://127.0.0.1:${port}/?variant=A&period=noon`, { waitUntil: 'networkidle' });
    await page.locator('.app-window[data-stage="shore"]').waitFor();
    await page.evaluate(() => document.fonts.ready.then(() => true));
    await shot('home');
    await button('有记忆靠岸').click();
    await shot('encounter');
    await button('直接看看').click();
    await page.locator('.app-window[data-route="read"]').waitFor();
    await shot('reading');
    await button('返回岸边').click(); await home(); await page.waitForTimeout(300);
    await button('留下一段').click();
    await page.locator('.app-window[data-route="leave"]').waitFor();
    await page.getByLabel('想留下什么？', { exact: true }).fill('今天绕了一小段路。\n\n雨停以后，路边的树还在滴水。我站了一会儿，没有急着回去。');
    await page.getByText('不必整理得很完整', { exact: true }).click();
    await shot('writing');
    await button('返回岸边').click(); await home(); await page.waitForTimeout(300);
    await button('海面安静').click();
    if (kind === 'realistic') {
      for (const [period, label] of [['morning', '早上'], ['noon', '中午'], ['afternoon', '下午'], ['night', '晚上']]) {
        await page.locator('.study-panel').getByRole('button', { name: label, exact: true }).click();
        await page.waitForTimeout(850);
        await shot(period);
      }
    } else {
      await button('深海模式').click(); await shot('night');
      for (const variant of ['B', 'C']) {
        await page.goto(`http://127.0.0.1:${port}/?variant=${variant}`, { waitUntil: 'networkidle' });
        await page.locator('.app-window[data-stage="shore"]').waitFor();
        await shot(variant.toLowerCase());
      }
    }
    await context.close();
  }
  if (errors.length) throw new Error(errors.join('\n'));
  console.log(JSON.stringify({ status: 'passed', deviceScaleFactor: 2, captures: results, errors }, null, 2));
} finally {
  await browser.close();
}
