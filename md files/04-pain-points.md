# 04 - Pain Points

**Related docs:** [01 Product design](01-product-design.md) · [03 Target audience](03-target-audience.md) · [06 Backend](06-backend.md) · [09 Overall plan](09-overall-plan.md)

> **How to read this.** Statements marked **[Sourced]** come from published surveys or reports listed in section 7. Statements marked **[Analysis]** are our own reasoning about how these systems work and have not been measured. Priorities are our judgement.

---

## 1. Context

India's digital money habits changed quickly:

- **[Sourced]** In a CRED-YouGov survey of 5,151 salaried people with at least one credit card, 97% of card holders also had EMIs or loans.
- **[Sourced]** SBI Card reports about 118.6 million credit cards in circulation in FY26, with spends above ₹23.62 trillion. EMI-led spends grew at double digits.
- **[Sourced]** Credit cards are moving into everyday payments through RuPay credit cards linked to UPI, including small-ticket purchases and tier 2 and 3 cities.
- **[Sourced]** A monthly industry summary reports about 122.9 million outstanding credit cards in July 2026 against 111.8 million a year earlier, with lower average ticket sizes partly explained by small RuPay credit-card payments routed through UPI.

More products, more due dates and more payment channels, but no single place that shows the combined picture.

## 2. The pain points

### P1. Fragmented view across accounts, cards, loans and UPI
- **[Sourced]** 39% of surveyed card holders find it hard to track multiple payment dates; 37% struggle with different billing cycles; 36% have difficulty monitoring spend across cards; 30% have trouble reconciling statements.
- **[Analysis]** No app sees bank accounts, cards, loans and UPI together, so users build the picture in their head.

### P2. Hidden upcoming obligations
- **[Analysis]** EMIs, card dues, rent and subscriptions arrive on different dates and through different channels (SMS, email, app notifications). Users cannot answer "what leaves my account in the next 30 days?"
- **[Analysis]** Auto-debits can fail silently when the balance is short, causing bounce charges and late fees. Minimum due versus total due is commonly misunderstood.

### P3. Backward-looking tools, no forward view
- **[Analysis]** Most trackers show what was spent. They do not warn "you will be short on the 18th because rent, EMI and card bill land together."
- **[Analysis]** Irregular income (freelancers, gig workers, early earners) breaks tools that assume a fixed monthly salary.

### P4. Debt pressure is hard to see
- **[Sourced]** 37% of surveyed card holders juggle different interest rates; 30% admit they lack credit-management knowledge.
- **[Analysis]** Users rarely see their combined obligations against income, card utilisation, or the interest cost of paying minimum due.

### P5. Frictionless payments weaken spending awareness
- **[Sourced]** A user study of UPI found about 74-75% of respondents said they spend more because of UPI, and only 7% said they spend less.
- **[Sourced]** Credit is entering UPI through RuPay credit cards, so credit use for small purchases becomes less conscious. Visa and Mastercard credit cards cannot yet be linked to UPI apps, so a person's credit activity splits across two trails.

### P6. Messy, ambiguous transaction data
- **[Analysis]** UPI history is cryptic: payee names are VPAs or personal names, so "Ramesh" could be rent, a friend, or a shop.
- **[Analysis]** Peer-to-peer transfers mix reimbursements, splits and loans with real spending.
- **[Analysis]** Card-bill payments, refunds, reversals and cashback cause double counting when handled naively. A card-bill payment is not new spending.
- **[Analysis]** SMS parsing is brittle because bank formats vary and some transactions generate no SMS.

### P7. Generic advice without impact
- **[Analysis]** Advice such as "reduce food spending" does not say what happens to runway, risk or interest if followed. Users cannot compare options.

### P8. Opaque confidence
- **[Analysis]** Forecasts and scores are shown as single numbers with no range, no method and no way to see what is missing. Users cannot tell whether to trust them.

### P9. Trust and privacy hesitation
- **[Analysis]** Reading bank messages is sensitive. Many trackers upload raw messages to cloud servers. Users hesitate to grant access and drop off during onboarding.

### P10. Engagement drop-off
- **[Analysis]** Manual logging fades within weeks. Notification overload leads users to ignore alerts. Rewards and offers push spending rather than clarity.

### P11. Credit-score opacity
- **[Sourced]** In the same survey, 47% of borrowers reportedly face higher borrowing costs due to poor credit scores.
- **[Sourced]** Thin credit history is a known barrier for young adults applying for a first card.

### P12. Segment-specific gaps
- **[Analysis]** Early earners and freelancers have lumpy income and delayed client payments. Mid-career users juggle EMIs and card cycles. Users in their 40s carry large lumpy annual obligations (fees, premiums). Tools built for one pattern miss the others. See [03 Target audience](03-target-audience.md).

### P13. Automatic debits for savings hide in the cash picture
- **[Analysis]** SIPs and other auto-debits leave the account on fixed dates. People remember the amount but rarely the date, so a SIP can land beside rent and an EMI and cause a short week. A planner that ignores SIPs overstates free cash; one that treats them as spending hides that they are voluntary and pausable.
- **[Analysis]** Messages show the debit, not the portfolio. A tool that claims to know someone's wealth from SMS alone is guessing.

## 3. How the pain points map to the problem statement

| Problem statement requirement | Pain points addressed |
|---|---|
| Consolidated financial-health view across bank accounts, cards, loans, investments and payment history | P1, P4, P6, P13 |
| Identify spending patterns, recurring obligations, debt pressure, future cash-flow gaps | P2, P3, P4, P5 |
| Conversational answers with personalised recommendations | P7 |
| Expected impact of each recommended action | P7 |
| Separate observed facts, model predictions and recommendations, with confidence where information is incomplete | P8 |
| Recommendations change as new data is added | P3, P7 |
| Constrained action planning (Addition 1) | P3, P4, P7 |
| Probabilistic cash-flow forecasting (Addition 2) | P3, P8, P12 |

## 4. Gap analysis: pain point, what RupAI has, what to add

| # | Pain point | What RupAI provides today | What needs to be added | Priority |
|---|---|---|---|---|
| 1 | Fragmented view across accounts, cards, loans, UPI | SMS ingestion from bank shortcodes, one local ledger, dedupe by SMS ID | Account and liability model (limit, outstanding, due date, loan balance, EMI schedule); card, loan and SIP SMS parsers; `Account`, `Investment` and `ConsolidatedView` (five streams); transfer-aware ledger; CSV import | P0 |
| 2 | Hidden upcoming obligations | Nothing (recurring detection is a todo item) | Recurring-payment detector; due-date extraction; 30-day obligations timeline | P0 |
| 3 | No forward view or cash-flow gaps | "Remaining balance = income minus expenses" | Probabilistic forecast (low, expected, high); shortfall-date detection; assumptions shown | P0 |
| 4 | Debt pressure unclear | Nothing | Debt-to-income ratio, card utilisation, minimum vs total due, interest cost, payoff scenarios | P0 |
| 5 | Generic advice, no impact shown | Gemini advisor card; rule-based emergency-reserve fallback | Constrained planner and what-if simulator with before and after impact | P0 |
| 6 | No conversational answers (affordability, why-questions) | Nothing | Natural-language question to engine calls to narrated answer; numbers never from the language model | P0 |
| 7 | Facts, predictions and advice blurred | Nothing | Observed / Predicted / Recommended labels, interval-based confidence, data-completeness meter | P0 |
| 8 | Does not adapt to new information | Re-categorises merchants only | Incremental recompute when income, debt or expenses change; "what changed" summary | P1 |
| 9 | Messy data (cryptic VPAs, double counting) | Four-layer categorisation, VPA resolver, user re-categorisation, AI review screen | Dedupe card-bill payments, refunds, reversals; P2P tagging; salted hashed counterparty IDs instead of masking | P1 |
| 10 | Spending awareness | Category donut, recent feed, monthly budget bar | Category budgets, pattern and anomaly detection, micro-spend roll-ups, credit-on-UPI stream | P1 |
| 11 | Irregular income | Single static income field | Income detection from credits; variability modelling; forecast that uses ranges | P1 |
| 12 | Privacy and trust | Local-first storage, sanitisation, backend key proxy, PKCE auth | Keep all of it; add disclosure that only aggregates go to the model; consent-scoped data; privacy center | P1 |
| 13 | Savings and investment behaviour | Savings-rate indicator | SIP detection treated as committed savings (excluded from debt load, pausable in what-if); emergency-fund progress. No portfolio value | P1 |
| 14 | Cash and unrecorded spend | Todo item only | Quick manual entry | P2 |
| 15 | Alert fatigue and drop-off | Automatic SMS sync | A few high-signal alerts; weekly digest; biometric lock | P2 |

### 4.1 Coverage by stream, and what messages cannot tell us

| Stream | Pain points | What RupAI reads | What it cannot know (shown as missing, never guessed) |
|---|---|---|---|
| Bank accounts | P1, P2, P3 | Credit and debit alerts, balance after each | Accounts that send no SMS, cash held, other banks not on the allow-list |
| Credit cards | P1, P2, P4, P5 | Spend alerts, statement-generated and due reminders | Limit and minimum due if the statement message omits them |
| Loans and EMIs | P2, P4 | EMI debits | Outstanding principal and rate unless declared |
| Investments | P13 | SIP debits | Portfolio value, fund choice, returns |
| Payment histories | P3, P5, P6, P12 | The full debit and credit stream | Cash spending, unrecorded transfers |

**Summary.** Reusable core: ingestion, categorisation, privacy and the backend proxy. Net-new work is rows 2-8, the engine and trust layers, which is exactly what the problem statement scores.

## 5. Severity and frequency (analysis)

| Pain point | Severity | Frequency |
|---|---|---|
| No forward-looking cash-flow view | High | Constant |
| Scattered due dates, EMIs and missed payments | High | Monthly |
| Fragmented accounts, cards and UPI | High | Constant |
| Poor categorisation and double counting | Medium | Daily |
| Credit-on-UPI invisibility | Medium, rising | Daily |
| Irregular-income handling | High for some users | Monthly |
| Invisible auto-debits (SIPs, EMIs) landing together | Medium | Monthly |
| Trust and engagement drop-off | Medium | Ongoing |

## 6. What this means for the build

1. **Lead with the forecast.** A "next 30 days obligations vs balance" view addresses P1-P3 together and carries the probabilistic requirement.
2. **Handle irregular income with ranges,** not single numbers.
3. **Deduplicate aggressively:** card-bill payments, refunds, reversals, P2P reimbursements.
4. **Show credit-on-UPI as its own stream** so small card spends are visible.
5. **Output actions with impact,** not charts: "move ₹X before the 15th to avoid a bounce".
6. **Be transparent about what is unknown,** with a completeness meter and interval-based confidence.

## 7. Sources

| Topic | Source |
|---|---|
| CRED-YouGov Credit Survey (5,151 salaried respondents; 97% of card holders have EMIs or loans; tracking, billing-cycle and interest-rate difficulties) | Business Standard report on the CRED-YouGov survey: https://www.business-standard.com/finance/personal-finance/97-per-cent-of-credit-card-holders-have-emis-or-loans-cred-survey-125030700617_1.html |
| SBI Card FY26 data (118.6 million cards, ₹23.62 trillion spends, double-digit EMI growth, UPI-linked credit cards) | Moneycontrol, 20 May 2026 (SBI Card news archive): https://www.sbicard.com/sbi-card-en/assets/docs/pdf/who-we-are/news/2026/Moneycontrol-20260520.pdf · Outlook Money: https://www.outlookmoney.com/personal-finance/from-emis-to-upi-major-trends-driving-credit-card-usage-in-india |
| UPI and spending behaviour (235 valid responses; about 74% report spending more) | "From Cash to Cashless: UPI's Impact on Spending Behavior among Indian Users", arXiv 2401.09937: https://arxiv.org/html/2401.09937v2 |
| Credit card counts and average ticket size, July 2026 | Loans Jagat summary of a monthly credit-card insights report: https://www.loansjagat.com/news/credit-card-transactions-july-2026 |
| UPI apps support only RuPay credit cards; UPI and personal loans competing with cards | Angel One summary of a CIBIL report: https://www.angelone.in/news/hindi/personal-finance/upi-and-personal-loans-are-replacing-credit-cards-in-indians-wallets-cibil-report |

Survey sample sizes are modest and some figures come through secondary reporting. Re-check the primary reports before quoting figures in a pitch, and replace or supplement with first-hand findings from the validation plan in [03 Target audience](03-target-audience.md).
