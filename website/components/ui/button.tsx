"use client";

import { useId, type ComponentProps } from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { LoaderCircle } from "lucide-react";
import { m, useReducedMotion } from "motion/react";
import { cn } from "@/lib/utils";

// shadcn/ui's native Button/CVA pattern, overridden by RupAI CTA v2.
const buttonVariants = cva(
  "rupai-cta inline-flex w-full min-w-0 items-center justify-center gap-[var(--space-xs)] rounded-[var(--radius-button)] px-[var(--space-lg)] font-medium leading-normal disabled:cursor-not-allowed",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground",
        outline: "bg-background text-foreground",
      },
      size: {
        compact: "min-h-[var(--size-button-compact)] py-[var(--space-xs)] text-sm",
        default: "min-h-[var(--size-button-default)] py-[var(--space-sm)] text-base",
        large: "min-h-[var(--size-button-large)] py-[var(--space-md)] text-base",
      },
    },
    defaultVariants: { variant: "default", size: "default" },
  },
);

export type ButtonState = "default" | "pressed" | "focused" | "disabled" | "loading";
type ButtonProps = ComponentProps<"button"> & VariantProps<typeof buttonVariants> & {
  state?: ButtonState;
  disabledReason?: string;
  loadingLabel?: string;
};

export function Button({
  className, variant = "default", size = "default", state = "default", disabled = false,
  disabledReason, loadingLabel = "Saving…", children, type = "button", ...props
}: ButtonProps) {
  const reasonId = useId();
  const reducedMotion = useReducedMotion();
  const unavailable = disabled || state === "disabled";
  const loading = state === "loading";
  if (unavailable && !disabledReason?.trim()) throw new Error("Disabled buttons need a visible reason");

  return (
    <div className="min-w-0 w-full">
      <button
        {...props}
        type={type}
        data-slot="button"
        data-variant={variant}
        data-size={size}
        data-state={unavailable ? "disabled" : state}
        className={cn(buttonVariants({ variant, size, className }))}
        disabled={unavailable || loading}
        aria-busy={loading || undefined}
        aria-describedby={cn(props["aria-describedby"], unavailable && reasonId) || undefined}
      >
        {loading && (
          <m.span
            className="loading-indicator inline-flex shrink-0"
            aria-hidden="true"
            initial={{ rotate: 0 }}
            animate={{ rotate: reducedMotion ? 0 : 360 }}
            transition={reducedMotion ? { duration: 0 } : { repeat: Infinity, duration: 1, ease: "linear" }}
          >
            <LoaderCircle size={20} strokeWidth={2} />
          </m.span>
        )}
        <span className="min-w-0 whitespace-normal [overflow-wrap:anywhere]" aria-live={loading ? "polite" : undefined}>
          {loading ? loadingLabel : children}
        </span>
      </button>
      {unavailable && <p id={reasonId} className="disabled-reason">{disabledReason}</p>}
    </div>
  );
}
