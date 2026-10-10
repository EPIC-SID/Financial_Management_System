import type { Metadata } from "next";
import localFont from "next/font/local";
import { MotionProvider } from "@/components/motion-provider";
import "./globals.css";

const inter = localFont({
  src: [
    { path: "../public/fonts/inter-regular.woff2", weight: "400", style: "normal" },
    { path: "../public/fonts/inter-medium.woff2", weight: "500", style: "normal" },
    { path: "../public/fonts/inter-semibold.woff2", weight: "600", style: "normal" },
  ],
  variable: "--font-inter",
  display: "swap",
});

const libre = localFont({
  src: "../public/fonts/libre-baskerville-regular.woff2",
  weight: "400",
  variable: "--font-libre",
  display: "swap",
  adjustFontFallback: false,
  fallback: [],
});

export const metadata: Metadata = {
  title: "RupAI · W1 foundations",
  description: "RupAI light design tokens, local fonts and CTA v2 state specimen.",
  robots: { index: false, follow: false },
  icons: { icon: "data:," },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${inter.variable} ${libre.variable}`}>
      <body><MotionProvider>{children}</MotionProvider></body>
    </html>
  );
}
