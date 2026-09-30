# Waymark app icon

Production icon set for Waymark, built from the Claude Design handoff (`project/Waymark App Icon.dc.html`, `project/icons/app-icon-{dark,light}.svg`). The icon shows an underground car, head-on, coming out of a tunnel mouth in gold line.

- **Dark** (gold on near-black `#1c1a17`) is the main icon.
- **Light** (gold on off-white `#f3f2f2`, thin grey edge) is the alternate icon.

`generate.mjs` holds the artwork geometry and both palettes, and writes everything in `dist/`. The rounded SVG masters match the handoff SVGs pixel for pixel. The only difference is that the C2PA metadata block has been removed.

## Regenerate

```sh
cd app-icon
npm install
npm run generate
# To use a Chromium you already have instead of Playwright's download:
CHROMIUM_PATH=/path/to/chromium npm run generate
```

## What's in `dist/`

| Path | Use |
| --- | --- |
| `svg/waymark-{dark,light}.svg` | Masters in the design's pre-rounded shape (rx 22). Use them for marketing, docs and in-app use. |
| `svg/waymark-{dark,light}-square.svg` | Full-bleed square versions, for platforms that apply their own mask. |
| `svg/waymark-dark-maskable.svg` | Artwork scaled to 80% so it fits the web maskable safe zone. |
| `svg/android-{foreground,background,monochrome}.svg` | Android adaptive-icon layers on the 108dp canvas. |
| `ios/AppIcon.appiconset/` | Primary iOS icon: a single 1024×1024 RGB PNG with no alpha. Drop it into `Assets.xcassets`. |
| `ios/AppIcon-Light.appiconset/` | Light alternate icon. Enable it with `setAlternateIconName("AppIcon-Light")` and `CFBundleAlternateIcons` / Xcode's "Alternate App Icon Sets". |
| `android/res/` | Copy it over `app/src/main/res/`. It includes the adaptive icon (background colour, foreground, and the Android 13+ monochrome/themed layer) and legacy `ic_launcher` / `ic_launcher_round` PNGs for mdpi–xxxhdpi. |
| `android/play-store-512.png` | Google Play listing icon: full-bleed, no alpha. |
| `web/` | `favicon.ico` (16/32/48), `favicon.svg`, `apple-touch-icon.png` (180), `icon-192/512.png`, `icon-maskable-512.png`, `manifest.webmanifest`, and `head-snippet.html` with the `<link>` tags. |

## Notes

- The full-bleed and platform-masked versions leave out the light icon's grey edge, because the platform mask would cut it unevenly.
- Android has no simple way to offer an alternate icon, so only the dark icon is shipped there. Adding one needs an `activity-alias` per icon.
- Colours are fixed hex values, the same as the handoff. The ones that match Classical design-system tokens (`--color-bg`, `--color-accent`, `--color-text`) are marked in `generate.mjs`.
