// Render presentation boards from unmodified prototype screenshots, not AI-redrawn UI.
import { chromium } from '../../tidal-tree-prototype/node_modules/playwright/index.mjs';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';

const browser = await chromium.launch({ channel: 'msedge', headless: true, chromiumSandbox: true });
const page = await browser.newPage({ viewport: { width: 2600, height: 2000 }, deviceScaleFactor: 1 });
const errors = [];
page.on('pageerror', error => errors.push(error.message));
const boards = [];
try {
  for (const kind of ['abstract', 'realistic']) {
    await page.goto(new URL(`./${kind}-ui-showcase.html`, import.meta.url).href, { waitUntil: 'networkidle' });
    await page.evaluate(() => document.fonts.ready.then(() => true));
    const check = await page.evaluate(() => {
      const board = document.querySelector('.board');
      const bounds = board.getBoundingClientRect();
      const images = [...board.querySelectorAll('img')];
      const missing = images.filter(img => !img.complete || !img.naturalWidth).map(img => img.src);
      const outOfBounds = [...board.querySelectorAll('h1,h2,h3,p,figcaption,.device,.footnote')].filter(element => {
        const rect = element.getBoundingClientRect();
        return rect.left < bounds.left || rect.top < bounds.top || rect.right > bounds.right + 1 || rect.bottom > bounds.bottom + 1;
      }).map(element => element.textContent.slice(0, 40));
      const footerTop = board.querySelector('.bottom').getBoundingClientRect().top;
      const contentBottom = Math.max(...[...board.querySelectorAll('.main > figure figcaption')].map(element => element.getBoundingClientRect().bottom));
      const miniBottom = Math.max(...[...board.querySelectorAll('.bottom .mini figcaption')].map(element => element.getBoundingClientRect().bottom));
      const footnoteTop = board.querySelector('.footnote').getBoundingClientRect().top;
      return { width: bounds.width, height: bounds.height, images: images.length, missing, outOfBounds, footerTop, contentBottom, miniBottom, footnoteTop, font: document.fonts.check('20px "Unfinished"') };
    });
    assert.deepEqual(check.missing, [], 'Every screenshot must load');
    assert.deepEqual(check.outOfBounds, [], 'Board content must remain inside the export');
    assert(check.font, 'Embedded Chinese font must load');
    assert(check.contentBottom + 10 < check.footerTop, 'Screen captions must not overlap the material section');
    assert(check.miniBottom + 10 < check.footnoteTop, 'Thumbnails must not overlap the provenance note');
    await page.locator('.board').screenshot({ path: fileURLToPath(new URL(`./${kind}-ui-showcase.png`, import.meta.url)), animations: 'disabled' });
    boards.push({ kind, ...check });
  }
  assert.deepEqual(errors, []);
  console.log(JSON.stringify({ status: 'passed', boards, errors }, null, 2));
} finally { await browser.close(); }
