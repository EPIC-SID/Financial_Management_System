# RupAI W1 foundations

W1 is a component specimen only. There is no install landing page, dashboard, navigation, authentication or data integration.

## Run and verify

```powershell
cd website
npm ci
npm run dev
```

```powershell
npm run verify
```

The build statically exports to `out/`. Browser checks serve that export using Python's standard HTTP server and installed Chrome, check keyboard and disabled/loading behavior, run WCAG AA checks, and test 320/768/1024/1440px widths at 100/130/200% text size. Node.js, Python and Chrome must be installed.

## Source and boundaries

Tokens and CTA copy come from `../docs/figma-source.json`, with the user's green/ink, yellow, orange Watch, light-only and zero-shadow overrides. See `../docs/figma-diff.md` for authored geometry and source gaps. The specimen's outer padding is authored; no verified screen-margin token is exposed.

The shadcn CSS variable scheme and native Button/CVA pattern are configured in `components.json`, `app/globals.css` and `components/ui/button.tsx`. The Button override exposes primary/outline and default/pressed/focused/disabled/loading states. Every disabled button requires a visible reason; loading is non-interactive and announced. Focus/press emphasis uses a flat inset outline.

Inter regular/medium/semibold and Libre Baskerville regular are locally hosted, subset WOFF2s. Both source licenses are included. Inter subsets retain `₹` and `tnum`. Libre Baskerville is used only for word-only headlines; every monetary string uses Inter, so currency never depends on a mid-line serif fallback. Font subset metadata uses distinct family names; the source outlines are preserved.

All supplied illustrations are converted to lossless WebP with alpha. Native resolution is used for 2x and half-size rounded up for 1x; no new detail is fabricated. `public/asset-manifest.json` records dimensions, hashes and font coverage. The one art panel on the specimen demonstrates the asset pipeline, not a screen assignment.

To regenerate assets, install `requirements-assets.txt` in your Python environment, then run:

```powershell
python scripts/prepare-assets.py
```

Motion features load through `LazyMotion`. `MotionConfig` follows the user's reduced-motion preference; the spinner also stops explicitly under reduced motion. No fade, gradient, blur, shadow or dark theme is used.

References: [Next.js static export](https://nextjs.org/docs/app/guides/static-exports), [local fonts](https://nextjs.org/docs/app/api-reference/components/font), [shadcn manual installation](https://ui.shadcn.com/docs/installation/manual), [Button](https://ui.shadcn.com/docs/components/radix/button), [LazyMotion](https://motion.dev/docs/react-lazy-motion), [reduced motion](https://motion.dev/docs/react-use-reduced-motion).
