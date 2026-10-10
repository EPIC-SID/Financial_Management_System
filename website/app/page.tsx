import Image from "next/image";
import { Button, type ButtonState } from "@/components/ui/button";
import copy from "@/lib/figma-copy.json";

const states: ButtonState[] = ["default", "pressed", "focused", "disabled", "loading"];
const colors = [
  ["Canvas", "--background"], ["Surface", "--muted"],
  ["Green / summary", "--accent"], ["Pink / error", "--error-container"],
  ["Primary action", "--primary"], ["Guidance", "--guidance"],
];

export default function Foundations() {
  return (
    <main className="specimen">
      <header className="specimen-header">
        <p className="eyebrow">{copy.eyebrow}</p>
        <h1>{copy.headline}</h1>
        <p className="supporting-copy">{copy.intro}</p>
      </header>

      <section aria-labelledby="cta-heading" className="specimen-section">
        <h2 id="cta-heading">CTA v2 / Light</h2>
        <div className="button-states">
          {states.map((state) => (
            <div key={state} className="state-specimen" role="group" aria-label={copy[state]}>
              <p className="state-label">{copy[state]}</p>
              <Button state={state} disabledReason={copy.disabledReason} loadingLabel={copy.loadingLabel}>{copy.primary}</Button>
              <Button variant="outline" state={state} disabledReason={copy.disabledReason} loadingLabel={copy.loadingLabel}>{copy.outline}</Button>
            </div>
          ))}
        </div>
        <p className="supporting-copy scale-note">{copy.scaleNote}</p>
      </section>

      <div className="foundations-grid">
        <section aria-labelledby="palette-heading" className="specimen-section">
          <h2 id="palette-heading">Palette / Light</h2>
          <dl className="palette">
            {colors.map(([label, token]) => (
              <div key={token}>
                <span className="swatch" aria-hidden="true" style={{ background: `var(${token})` }} />
                <dt>{label}</dt>
                <dd><code>{token}</code></dd>
              </div>
            ))}
          </dl>
        </section>

        <section aria-labelledby="type-heading" className="specimen-section type-specimen">
          <h2 id="type-heading">Typography / Latin</h2>
          <p className="serif-specimen">{copy.foundationHeadline}</p>
          <p className="supporting-copy">Body / Inter Regular 16; compact body 14 minimum</p>
          <p className="amount-specimen">₹18K–₹26K</p>
          <p className="caption">Caption / 12 minimum; legend glyph 16</p>
          <p className="token-note">Radius / small 8, input 12, button 16, card 20, sheet 24 dp.</p>
        </section>
      </div>

      <section aria-label="Illustration asset specimen" className="art-specimen">
        <picture>
          <source srcSet="/art/home-cushion@1x.webp 1x, /art/home-cushion@2x.webp 2x" />
          <Image
            src="/art/home-cushion@1x.webp"
            alt=""
            aria-hidden="true"
            width={724}
            height={543}
            unoptimized
            loading="eager"
            className="art-image"
            sizes="(max-width: 640px) 100vw, 360px"
          />
        </picture>
      </section>
    </main>
  );
}
