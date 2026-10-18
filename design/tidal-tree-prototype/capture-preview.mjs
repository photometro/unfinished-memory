// Isolated prototype smoke check. No user profile, real memories or native UI are accessed.
import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { buildTerrain } from './src/channel-geometry.js';

const screenshotDirectory = fileURLToPath(new URL('./screenshots/drainage-model/', import.meta.url));
await mkdir(screenshotDirectory, { recursive: true });
const browser = await chromium.launch({ channel: 'msedge', headless: true, chromiumSandbox: true });
const context = await browser.newContext({ viewport: { width: 1280, height: 1100 }, deviceScaleFactor: 1, reducedMotion: 'no-preference' });
const page = await context.newPage();
const errors = [];
page.on('pageerror', error => errors.push(error.message));
page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
page.on('response', response => { if (response.status() >= 400) errors.push(`${response.status()}: ${response.url()}`); });
const wait = milliseconds => page.waitForTimeout(milliseconds);
const shot = async (name, full = false) => (full ? page : page.locator('.app-window')).screenshot({ path: `${screenshotDirectory}${name}.png`, animations: 'allow' });
const arrive = async () => page.locator('.app-window[data-stage="shore"]').waitFor({ timeout: 15000 });
const home = async () => page.locator('.app-window[data-route="home"]').waitFor();
const button = name => page.getByRole('button', { name, exact: true });
const widths = [];
const morphology = [];
for (const variant of ['A', 'B', 'C']) {
  const model = buildTerrain(variant);
  assert.equal(model.stats.disconnected, 0, `${variant}: all land must drain to the sea outlet`);
  assert.equal(model.stats.reverseFlux, 0, `${variant}: flux cannot decrease downstream`);
  assert.equal(model.stats.uphillPotential, 0, `${variant}: routing must be acyclic`);
  assert(model.stats.junctions > 20 && model.stats.heads > 20, `${variant}: multiple contributing areas must merge`);
  const starts = new Map(model.segments.map(segment => [segment.ids[0], segment.points[0]]));
  for (const segment of model.segments) {
    if (segment.ids.length === 1) continue; // The outlet extends into the sea polygon.
    const end = segment.points.at(-1);
    const following = starts.get(segment.ids.at(-1));
    assert(following, `${variant}: a reach must have a downstream continuation`);
    assert.equal(end.x, following.x); assert.equal(end.y, following.y);
    assert.equal(end.width, following.width);
    for (let index = 1; index < segment.points.length; index++) {
      assert(segment.points[index].width >= segment.points[index - 1].width - 1e-8, `${variant}: a reach must widen downstream`);
    }
  }
  assert.equal(buildTerrain(variant).layers.water, model.layers.water, 'Replay must not regenerate terrain');
  for (let y = 0; y <= 1440; y += 40) for (let x = 0; x <= 720; x += 40) assert(Number.isFinite(model.heightAt(x, y)));
  morphology.push({ variant, ...model.stats });
}

try {
  await page.goto('http://127.0.0.1:5188/?variant=A', { waitUntil: 'networkidle' });
  await arrive(); await wait(500);
  await page.locator('.terrain[data-morphology="tidal-creeks"]').waitFor();
  assert.equal(await page.locator('.terrain').getAttribute('data-model'), 'weighted-drainage-incision');
  assert.equal(await page.locator('.terrain').getAttribute('data-disconnected'), '0');
  assert(await page.locator('.terrain path').count() < 20, 'Static geometry should be combined into a few material layers');
  await shot('A-desktop', true); await shot('A-quiet');
  await button('重播退潮').click(); await wait(220); await shot('A-sea');
  assert.equal(await page.locator('.tide-layer').getAttribute('data-reveal-model'), 'incised-height-field');
  assert.equal(await page.locator('.tide-layer').getAttribute('data-retreat-direction'), 'bottom');
  await wait(1800); await shot('A-ebb');
  await button('抵达岸边').click(); await arrive();
  await button('有记忆靠岸').click(); await wait(300); await shot('A-encounter');
  await button('再给一点').click();
  await page.getByText('一点线索：一处老车站。', { exact: true }).waitFor();
  await button('直接看看').click();
  await page.locator('.app-window[data-route="read"]').waitFor();
  await page.getByRole('heading', { name: '雨停以后，老车站', exact: true }).waitFor();
  await wait(300); await shot('reading');
  await button('返回岸边').click(); await home(); await wait(350);
  await button('让它再待一会').click();
  await page.getByRole('heading', { name: /今天海面.*很安静/ }).waitFor();
  await button('下一种构图').click(); await page.waitForURL(/variant=B/); await wait(350); await shot('B-quiet');
  await button('重播退潮').click(); await wait(220);
  assert.equal(await page.locator('.tide-layer').getAttribute('data-retreat-direction'), 'right');
  await wait(1800); await shot('B-ebb');
  await button('抵达岸边').click(); await arrive();
  await button('下一种构图').click(); await page.waitForURL(/variant=C/); await wait(350); await shot('C-quiet');
  await button('重播退潮').click(); await wait(220);
  assert.equal(await page.locator('.tide-layer').getAttribute('data-retreat-direction'), 'top');
  await wait(1800); await shot('C-ebb');
  await button('抵达岸边').click(); await arrive();
  await button('有记忆靠岸').click(); await wait(350); await shot('C-encounter');
  await button('深海模式').click(); await wait(350); await shot('C-night');
  for (const width of [320, 375, 768, 1440]) {
    await page.setViewportSize({ width, height: 1000 }); await wait(160);
    if (width === 320) {
      await button('再给一点').click();
      await button('再给一点').click(); await wait(200);
    }
    const geometry = await page.evaluate(() => ({ width: innerWidth, scroll: document.documentElement.scrollWidth, font: document.fonts.check('16px "Unfinished Noto"') }));
    assert(geometry.scroll <= geometry.width + 1, `Horizontal overflow at ${width}: ${geometry.scroll}`);
    assert(geometry.font, `Bundled font has not loaded at ${width}`);
    const navigationGap = await page.locator('.message-c').evaluate(element => element.parentElement.querySelector('nav').getBoundingClientRect().top - element.getBoundingClientRect().bottom);
    assert(navigationGap >= 12, `Clue content overlaps navigation at ${width}: ${navigationGap}`);
    widths.push(geometry);
    if (width === 375) await shot('C-mobile');
  }
  await page.setViewportSize({ width: 1280, height: 1100 });
  await page.goto('http://127.0.0.1:5188/?variant=A', { waitUntil: 'networkidle' });
  const skip = button('抵达岸边');
  if (await skip.isVisible()) await skip.click();
  await arrive(); await wait(450);
  await button('留下一段').click(); await button('封存 · 演示').waitFor();
  await button('封存 · 演示').click();
  await page.getByRole('alert').filter({ hasText: '先写下一点内容，再试一次。' }).waitFor();
  await page.getByLabel('想留下什么？', { exact: true }).fill('这是界面检查的虚构文字。');
  await button('暂存草稿').click(); await home(); await wait(350);
  await button('留下一段').click();
  const writing = page.getByLabel('想留下什么？', { exact: true });
  await writing.waitFor(); assert.equal(await writing.inputValue(), '这是界面检查的虚构文字。');
  await wait(350); await shot('writing');
  await button('封存 · 演示').click(); await page.locator('.app-window[data-route="seal"]').waitFor();
  await home(); await wait(350);
  await button('过去').click();
  await page.getByLabel('寻找一段记忆', { exact: true }).fill('完全不存在的虚构词');
  await page.getByText('这里还没有这段痕迹。', { exact: true }).waitFor();
  await button('清空搜索').click();
  await page.getByText('这是界面检查的虚构文字。', { exact: true }).waitFor();
  await button('返回岸边').click(); await home(); await wait(350);
  await button('打开设置').click();
  const reducedSwitch = page.getByRole('switch', { name: '减少动画', exact: true });
  await reducedSwitch.click(); assert.equal(await reducedSwitch.getAttribute('aria-checked'), 'true');
  await button('返回岸边').click(); await home();
  assert(await button('重播退潮').isDisabled());
  await context.close();
  const reducedContext = await browser.newContext({ viewport: { width: 390, height: 1000 }, reducedMotion: 'reduce' });
  const reducedPage = await reducedContext.newPage();
  await reducedPage.goto('http://127.0.0.1:5188/?variant=B', { waitUntil: 'networkidle' });
  await reducedPage.locator('.app-window[data-stage="shore"]').waitFor({ timeout: 3000 });
  assert(await reducedPage.getByRole('button', { name: '重播退潮', exact: true }).isDisabled());
  await reducedContext.close();
  assert.deepEqual(errors, []);
  console.log(JSON.stringify({ status: 'passed', morphology, widths, checks: ['sea-connected/acyclic/flux-width/junction-continuity', 'combined-SVG', 'height-field/replay/skip', 'A/B/C', 'clues/direct-view/defer/narrow-layout', 'writing-validation/draft/seal', 'search/empty', 'manual/system-reduced-motion', 'embedded-font'], screenshots: screenshotDirectory }, null, 2));
} finally {
  await browser.close();
}
