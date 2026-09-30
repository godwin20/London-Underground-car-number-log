#!/usr/bin/env node
// Generates every Waymark app-icon asset from one geometry definition.
//
//   npm run generate            (from app-icon/)
//
// Geometry and colours are taken verbatim from the Claude Design handoff
// (project/icons/app-icon-{dark,light}.svg): a 100×100 artboard with an
// underground car, head-on, coming out of a tunnel mouth in gold line.
// SVGs are written directly; PNGs are rasterised with Playwright's Chromium,
// so they match what a browser shows for the SVG.

import { mkdir, writeFile, rm } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));
const OUT = join(ROOT, 'dist');

// ─── Palettes ───────────────────────────────────────────────────────────────
// Where a colour matches a Classical design-system token it is noted.
const PALETTES = {
  dark: {
    bg: '#1c1a17',
    edge: null,
    tunnel: '#7a5d35',
    gold: '#c9974a',
    ink: '#f3f2f2', //         --color-bg
  },
  light: {
    bg: '#f3f2f2', //          --color-bg
    edge: '#d6d3d0', //        thin edge so it holds on pale wallpapers
    tunnel: '#c9a46a',
    gold: '#b68235', //        --color-accent
    ink: '#201f1d', //         --color-text
  },
};

// ─── Artwork ────────────────────────────────────────────────────────────────
// The glyph, in the design's 100-unit space. `mono` renders a single-colour
// version (Android 13+ themed icons) where the car body can't be filled with
// the background colour, so the tunnel lines are masked out behind it instead.
const CAR_BODY = 'M31 78 V50 Q31 29 50 27 Q69 29 69 50 V78 Z';

function glyph(p, { mono = false, id = 'g' } = {}) {
  const c = mono
    ? { tunnel: '#fff', gold: '#fff', ink: '#fff', carFill: 'none' }
    : { tunnel: p.tunnel, gold: p.gold, ink: p.ink, carFill: p.bg };
  const maskDef = mono
    ? `<mask id="${id}-m" maskUnits="userSpaceOnUse" x="0" y="0" width="100" height="100"><rect width="100" height="100" fill="#fff"/><path d="${CAR_BODY}" fill="#000"/></mask>`
    : '';
  const tunnelAttrs = mono ? ` mask="url(#${id}-m)"` : '';
  return `${maskDef}<g fill="none" stroke-linecap="round" stroke-linejoin="round">
<g${tunnelAttrs}>
<path d="M14 84 V52 A36 36 0 0 1 86 52 V84" stroke="${c.tunnel}" stroke-width="1.8"/>
<path d="M20 84 V53 A30 30 0 0 1 80 53 V84" stroke="${c.tunnel}" stroke-width="1.1" opacity=".7"/>
</g>
<path d="${CAR_BODY}" fill="${c.carFill}" stroke="${c.gold}" stroke-width="3"/>
<rect x="43" y="32" width="14" height="3.5" rx="1" stroke="${c.gold}" stroke-width="1.6"/>
<rect x="35" y="42" width="7.5" height="11" rx="1.6" stroke="${c.ink}" stroke-width="2"/>
<rect x="57.5" y="42" width="7.5" height="11" rx="1.6" stroke="${c.ink}" stroke-width="2"/>
<rect x="45" y="40" width="10" height="33" rx="1.6" stroke="${c.ink}" stroke-width="2"/>
<rect x="47.2" y="43" width="5.6" height="10" rx="1" stroke="${c.ink}" stroke-width="1.4"/>
<path d="M31 72 H45 M55 72 H69" stroke="${c.gold}" stroke-width="1.6"/>
<path d="M36 78 L34 84 M64 78 L66 84" stroke="${c.gold}" stroke-width="2"/>
<path d="M12 84 H88" stroke="${c.gold}" stroke-width="2.6"/>
</g>
<circle cx="37.5" cy="64" r="2.4" fill="${c.gold}"/><circle cx="62.5" cy="64" r="2.4" fill="${c.gold}"/>`;
}

// Scale the glyph about the artboard centre (the glyph's bounding box is
// centred on 50,50, so this keeps it optically centred).
const scaled = (inner, s, size = 100) =>
  `<g transform="translate(${size / 2} ${size / 2}) scale(${s}) translate(-50 -50)">${inner}</g>`;

const svg = (viewBox, body) =>
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${viewBox}">\n${body}\n</svg>\n`;

// Layouts ───────────────────────────────────────────────────────────────────
const layouts = {
  // Exactly the handoff design: pre-rounded square (rx 22), light gets an edge.
  rounded: (p) =>
    svg(
      '0 0 100 100',
      `<rect x=".5" y=".5" width="99" height="99" rx="22" fill="${p.bg}"${
        p.edge ? ` stroke="${p.edge}" stroke-width="1"` : ''
      }/>\n${glyph(p)}`,
    ),
  // Full-bleed square for stores / iOS, which apply their own corner mask.
  square: (p) => svg('0 0 100 100', `<rect width="100" height="100" fill="${p.bg}"/>\n${glyph(p)}`),
  // Web "maskable": the key artwork sits inside the 80%-diameter safe circle.
  maskable: (p) =>
    svg('0 0 100 100', `<rect width="100" height="100" fill="${p.bg}"/>\n${scaled(glyph(p), 0.8)}`),
  // Legacy Android round launcher icon.
  circle: (p) =>
    svg('0 0 100 100', `<circle cx="50" cy="50" r="50" fill="${p.bg}"/>\n${scaled(glyph(p), 0.8)}`),
  // Android adaptive layers: 108dp canvas, 72dp visible, 66dp safe circle.
  // 0.66 keeps the tunnel and car inside the safe circle.
  adaptiveFg: (p) => svg('0 0 108 108', scaled(glyph(p), 0.66, 108)),
  adaptiveBg: (p) => svg('0 0 108 108', `<rect width="108" height="108" fill="${p.bg}"/>`),
  adaptiveMono: (p) => svg('0 0 108 108', scaled(glyph(p, { mono: true }), 0.66, 108)),
};

// ─── Asset manifest ─────────────────────────────────────────────────────────
// [output path, layout, palette, pixel size (omit for SVG), opaque?]
const ANDROID_DENSITIES = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };

const assets = [
  // Vector masters
  ['svg/waymark-dark.svg', 'rounded', 'dark'],
  ['svg/waymark-light.svg', 'rounded', 'light'],
  ['svg/waymark-dark-square.svg', 'square', 'dark'],
  ['svg/waymark-light-square.svg', 'square', 'light'],
  ['svg/waymark-dark-maskable.svg', 'maskable', 'dark'],
  ['svg/android-foreground.svg', 'adaptiveFg', 'dark'],
  ['svg/android-background.svg', 'adaptiveBg', 'dark'],
  ['svg/android-monochrome.svg', 'adaptiveMono', 'dark'],

  // iOS — single-size 1024 app icons (Xcode 14+), opaque as App Store requires
  ['ios/AppIcon.appiconset/AppIcon-1024.png', 'square', 'dark', 1024, true],
  ['ios/AppIcon-Light.appiconset/AppIcon-Light-1024.png', 'square', 'light', 1024, true],

  // Android
  ...Object.entries(ANDROID_DENSITIES).flatMap(([d, k]) => [
    [`android/res/mipmap-${d}/ic_launcher.png`, 'rounded', 'dark', 48 * k],
    [`android/res/mipmap-${d}/ic_launcher_round.png`, 'circle', 'dark', 48 * k],
    [`android/res/mipmap-${d}/ic_launcher_foreground.png`, 'adaptiveFg', 'dark', 108 * k],
    [`android/res/mipmap-${d}/ic_launcher_monochrome.png`, 'adaptiveMono', 'dark', 108 * k],
  ]),
  ['android/play-store-512.png', 'square', 'dark', 512, true],

  // Web / PWA
  ['web/favicon.svg', 'rounded', 'dark'],
  ['web/apple-touch-icon.png', 'square', 'dark', 180, true],
  ['web/icon-192.png', 'rounded', 'dark', 192],
  ['web/icon-512.png', 'rounded', 'dark', 512],
  ['web/icon-maskable-512.png', 'maskable', 'dark', 512, true],
];
const FAVICON_SIZES = [16, 32, 48];

// ─── Static text files ──────────────────────────────────────────────────────
const iosContents = (file) =>
  JSON.stringify(
    {
      images: [{ filename: file, idiom: 'universal', platform: 'ios', size: '1024x1024' }],
      info: { author: 'xcode', version: 1 },
    },
    null,
    2,
  ) + '\n';

const adaptiveXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
`;

const textFiles = {
  'ios/AppIcon.appiconset/Contents.json': iosContents('AppIcon-1024.png'),
  'ios/AppIcon-Light.appiconset/Contents.json': iosContents('AppIcon-Light-1024.png'),
  'android/res/mipmap-anydpi-v26/ic_launcher.xml': adaptiveXml,
  'android/res/mipmap-anydpi-v26/ic_launcher_round.xml': adaptiveXml,
  'android/res/values/ic_launcher_background.xml': `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">${PALETTES.dark.bg.toUpperCase()}</color>
</resources>
`,
  'web/manifest.webmanifest':
    JSON.stringify(
      {
        name: 'Waymark',
        short_name: 'Waymark',
        background_color: PALETTES.dark.bg,
        theme_color: PALETTES.dark.bg,
        icons: [
          { src: 'icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
          { src: 'icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
          { src: 'icon-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
          { src: 'favicon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' },
        ],
      },
      null,
      2,
    ) + '\n',
  'web/head-snippet.html': `<link rel="icon" href="/favicon.ico" sizes="48x48">
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="/apple-touch-icon.png">
<link rel="manifest" href="/manifest.webmanifest">
<meta name="theme-color" content="${PALETTES.dark.bg}">
`,
};

// ─── PNG helpers ────────────────────────────────────────────────────────────
// The App Store rejects icons with an alpha channel. Chromium writes fully
// opaque screenshots as 8-bit RGB, so opaque assets are checked, not converted.
function assertNoAlpha(png, rel) {
  const colourType = png[25]; // IHDR is always the first chunk
  if (colourType !== 2) throw new Error(`${rel}: expected an RGB PNG without alpha, got colour type ${colourType}`);
  return png;
}

// ICO container holding PNG-encoded images (supported by every current browser).
function ico(pngs) {
  const header = Buffer.alloc(6 + 16 * pngs.length);
  header.writeUInt16LE(1, 2);
  header.writeUInt16LE(pngs.length, 4);
  let offset = header.length;
  pngs.forEach(({ size, data }, i) => {
    const e = 6 + 16 * i;
    header[e] = size % 256;
    header[e + 1] = size % 256;
    header.writeUInt16LE(1, e + 4);
    header.writeUInt16LE(32, e + 6);
    header.writeUInt32LE(data.length, e + 8);
    header.writeUInt32LE(offset, e + 12);
    offset += data.length;
  });
  return Buffer.concat([header, ...pngs.map((p) => p.data)]);
}

// ─── Main ───────────────────────────────────────────────────────────────────
async function put(rel, data) {
  const file = join(OUT, rel);
  await mkdir(dirname(file), { recursive: true });
  await writeFile(file, data);
}

const { chromium } = await import('playwright');
// CHROMIUM_PATH lets you use an existing Chromium instead of Playwright's own.
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined });
const page = await browser.newPage();

async function rasterise(markup, size) {
  await page.setViewportSize({ width: size, height: size });
  const src = `data:image/svg+xml;base64,${Buffer.from(markup).toString('base64')}`;
  await page.setContent(
    `<style>html,body{margin:0;background:transparent}img{display:block;width:${size}px;height:${size}px}</style><img src="${src}">`,
  );
  await page.locator('img').evaluate((img) => img.decode());
  const png = await page.screenshot({ omitBackground: true, clip: { x: 0, y: 0, width: size, height: size } });
  return png;
}

await rm(OUT, { recursive: true, force: true });

for (const [rel, layout, palette, size, opaque] of assets) {
  const markup = layouts[layout](PALETTES[palette]);
  if (!size) await put(rel, markup);
  else {
    const png = await rasterise(markup, size);
    await put(rel, opaque ? assertNoAlpha(png, rel) : png);
  }
}

const favicons = [];
for (const size of FAVICON_SIZES) {
  favicons.push({ size, data: await rasterise(layouts.rounded(PALETTES.dark), size) });
}
await put('web/favicon.ico', ico(favicons));

for (const [rel, text] of Object.entries(textFiles)) await put(rel, text);

await browser.close();
console.log(`Wrote ${assets.length + 1 + Object.keys(textFiles).length} files to ${OUT}`);
