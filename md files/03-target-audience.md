# 03 - Target Audience

**Related docs:** [01 Product design](01-product-design.md) · [04 Pain points](04-pain-points.md) · [05 Frontend UI style](05-frontend-ui-style.md) · [07 UI/UX flow](07-ui-ux-flow.md)

> **Evidence status.** Facts that come from published surveys are cited in [04 Pain points](04-pain-points.md). The psychology sections below are **hypotheses** based on common behavioural-finance patterns. They are not validated findings. Section 8 lists how to validate them before the design is frozen.

---

## 1. Who we are building for

**Earning people in India aged 18-50 who have at least one recurring financial obligation (rent, EMI, credit card, loan, insurance) and make most payments digitally.**

We deliberately do not target "students" or "young professionals" alone. A 45-year-old with a home loan, school fees and a parent's medical costs has a harder version of the same problem as a 22-year-old with irregular freelance income. The common thread is not age. It is that **money arrives and leaves on different dates, and nobody can see the pinch points in advance.**

### The shared job to be done

> "Tell me whether I'll be okay over the next few months, what is about to hit me, and what I should do about it."

## 2. Segmenting by situation, not age

Age is a weak predictor of what the product must do. Two axes explain nearly all the differences in need:

| Axis | Low | High |
|---|---|---|
| **Income variability** | Fixed monthly salary | Irregular (freelance, gig, commission, allowance) |
| **Obligation load** | Few fixed commitments | Many large fixed commitments (EMIs, rent, fees, insurance) |

```
                      OBLIGATION LOAD
                    Low             High
              +---------------+----------------+
 INCOME   Low |  Comfortable  |  Locked-in     |
 VARIABILITY  |  planner      |  (cohort C)    |
              +---------------+----------------+
          High|  Early earner |  Stretched     |
              |  (cohort A)   |  juggler       |
              +---------------+----------------+
```

Cohort B (mid-career with EMIs) sits across the right column depending on income type. Onboarding places each user using three questions (income type, fixed payments, optional age range) and sets defaults accordingly. One product serves everyone; defaults, tone and emphasis change.

## 3. Life-stage cohorts

These are convenient groupings for design and copy. The two-axis placement above decides actual defaults.

| | **A: Early earner (18-25)** | **B: Building (26-38)** | **C: Committed (39-50)** |
|---|---|---|---|
| Typical situation | Freelance, first job, stipend or allowance; UPI-heavy; small credit, BNPL or a phone EMI; thin credit history | Salary plus EMIs (phone, vehicle, early home loan); credit card with rewards; SIPs; rent; marriage or relocation costs | Home loan, school fees, insurance, parents' medical costs; high fixed outgo, little flexibility |
| Core question | "Will I run out before the month ends?" | "Can I afford this, and what do I pay first?" | "Is my cushion safe, and can I plan big events?" |
| Biggest fear | Running out; being judged | Over-committing; missing a due date | One shock draining the cushion |
| Income pattern | Irregular or small | Mostly fixed, sometimes variable | Fixed, sometimes with bonuses |
| Default horizon | 8 weeks first | 6 months | 6 months with annual events (fees, premiums) |
| What delights them | Instant answer, a first buffer goal | Side-by-side plan comparison, affordability checks | Certainty, visible reserve, clear risk band |

## 4. Behaviour patterns to design for

Common across cohorts (hypotheses):

1. **Avoidance.** People postpone looking at finances when they expect bad news. The first screen must not feel like a report card.
2. **Verdict over data.** People want "am I fine?" before charts.
3. **Salary-day mental accounting.** Money is mentally bucketed around salary day, EMI dates and card due dates.
4. **Low pain of paying with UPI.** Frictionless payments reduce awareness. A UPI study found about 74% of respondents spend more because of UPI (see [04](04-pain-points.md)).
5. **Credit is becoming invisible.** RuPay credit cards linked to UPI let people use credit for small daily payments without consciously "using a card".
6. **Surprises hurt more than known costs.** An unexpected bill causes more anxiety than a scheduled larger one.
7. **Trust is earned slowly.** Bank SMS and financial data are sensitive; users hesitate to hand over access.
8. **Savings on autopilot.** SIPs and other auto-debits are remembered by amount but not by date, so they surprise people when they land next to rent or an EMI (hypothesis).

Cohort differences (hypotheses):

| | A | B | C |
|---|---|---|---|
| Mindset | Short horizon, wants speed and low friction | Optimising, juggling trade-offs | Protective, wants certainty and control |
| Trust signals | Peers, polished app | Numbers, comparisons, reviews | Banks, family, transparent logic |
| Likely drop-off cause | Effort to set up | Notification overload | Jargon and unclear logic |

## 5. Design implications

| Insight | Design response |
|---|---|
| Avoidance | Lead with a calm one-line verdict, never a score of shame |
| Verdict over data | Cockpit opens with the verdict card; evidence is one tap away |
| Salary-day thinking | 30-day obligations timeline anchored on due dates |
| Low pain of paying | Credit-on-UPI shown as its own stream; micro-spend roll-ups |
| Surprise anxiety | Forward view with ranges; "what could go wrong" shown through the risk band |
| Trust | Privacy promise on the first screen; consent toggles; "what left your phone" log |
| Money sits in many places | A consolidated accounts row (cash, cards, loans, SIP) under the health score; one tap to the detail |
| Different horizons | Default period length by cohort (8 weeks for A, 6 months for B and C) |
| Different risk appetite | Default shortfall-risk threshold by cohort (proposal: A 15%, B 12%, C 8%), always editable |

## 6. Voice and tone by cohort

| Cohort | Tone | Example |
|---|---|---|
| A | Casual, quick, encouraging | "You're good till the 20th. Let's build a small cushion." |
| B | Clear, comparative | "The phone EMI fits, but it narrows your cushion in March." |
| C | Calm, precise, conservative | "Your cushion holds in 9 of 10 cases. One bad month is covered." |

Rules for all cohorts:
- Plain words; explain any finance term in one line.
- Never use blame words (overspent, failed, bad, wasted).
- Every warning ends with a next step.
- Every prediction states its range.
- Sentence case; errors say what happened and how to fix it, without apologising.

## 7. Personas (synthetic, illustrative)

These personas seed the demo and the evaluation data. **All numbers are synthetic assumptions, not real people or survey results.** Generation details are in [06 Backend](06-backend.md).

### Persona A: Aarav, 22, freelance designer
- Income varies between ₹18,000 and ₹45,000 per month (typical ₹30,000), paid late at times.
- Shared-flat rent ₹8,000, phone EMI ₹2,300, one small credit card, heavy UPI use.
- Accounts: one savings account, one card (limit ₹20,000), no SIP yet.
- Cushion goal: ₹10,000. Wants to know: "Can I take a ₹6,000 course this month?"
- Product moments: wide forecast bands, Low or Medium confidence, "first buffer" plan, income-variability explainer.

### Persona B: Neha, 32, salaried
- Income ₹85,000 fixed.
- Rent ₹22,000 (1st), home loan EMI ₹18,500 (10th), car EMI ₹8,200 (15th), utilities ₹2,500 (20th); variable food, shopping and travel.
- Accounts (all five streams): salary and savings account, one credit card with the statement due on the 25th, home and car loans, and a ₹10,000 mutual fund SIP on the 5th. Full seed values are in [06 Backend](06-backend.md), section 12.2a.
- Cushion: ₹40,000. Wants to know: "Can I afford a ₹30,000 trip next month, and should I prepay the loan?"
- Product moments: Plan A (pay debt faster) vs Plan B (build buffer first), interest saved vs reserve trade-off.

### Persona C: Rajesh, 45, salaried
- Income ₹1,40,000; home loan EMI ₹45,000; school fees paid in two large instalments a year; annual insurance premium; ₹5,000 a month towards a parent's medical costs.
- Accounts: salary account, one credit card, home loan, and a ₹15,000 mutual fund SIP on the 5th (assumptions).
- Cushion: ₹1,50,000. Wants to know: "Do the school fees in July break my cushion?"
- Product moments: annual lumpy obligations in the forecast, conservative defaults, calm risk band.

## 8. Validation plan (do before freezing the design)

**Goal:** confirm or correct the hypotheses in sections 4 to 6 in a day or two.

**Who:** 8-10 people, ideally 3 from each cohort (friends, family, classmates, colleagues). They need to have at least one recurring payment.

**Questions (15 minutes each):**
1. Walk me through the last time a payment (EMI, card bill, rent) surprised you. What happened?
2. How do you currently find out what is due and when?
3. When do you look at your bank or UPI history, and what are you looking for?
4. What would "am I okay this month?" look like to you? What would you check?
5. Which would you trust more: a single number, or a range with an explanation? Why?
6. What would make you uncomfortable about an app reading your bank messages?
7. If an app told you "tight on the 25th", what would you want it to say next?
8. Show a rough mock of the Cockpit verdict card. What do you understand from it in five seconds?
9. Do you have a SIP or other auto-debit? Do you remember its date? Has it ever landed when cash was tight?
10. Show the accounts row (cash, card dues, loan EMIs, SIP). What does each number mean to you, and what do you expect to happen when you tap it?

**What to record:** words people use for money stress, the apps they use today, what they check first, what they would never share, and reactions to ranges versus single numbers.

**How the results feed back:**
- Update the tone table (section 6) with their actual words.
- Adjust default horizon and risk thresholds per cohort.
- Update [05 Frontend UI style](05-frontend-ui-style.md) copy rules and [04 Pain points](04-pain-points.md) with first-hand evidence.

## 9. Out of scope (for v1)

- Under-18s and people with no income.
- Business or sole-proprietor accounting.
- Households managing shared budgets.
- Users who do not use digital payments at all.
- Investment advisory for high-net-worth individuals.
- Portfolio valuation or investment recommendations (only SIP debits are observed).
