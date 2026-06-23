# Katib — Monetization & Payouts

How money flows in Katib: users pay through Google Play, Google takes a cut, and
pays you out to your bank. This doc covers the split, the payout flow, how to set
up the subscriptions, and how to test purchases **for free** before launch.

---

## 1. How users pay

1. In the app, the user opens the **Paywall** and taps a plan. The app's
   `SubscriptionManager` (Google Play Billing) launches Google's payment sheet.
2. The user pays with whatever is on their Google account — card, mada, carrier
   billing, or Play gift-card balance. **You never touch their card.**
3. Google charges them, your app receives the purchase token, acknowledges it,
   and flips `isPremium = true` (gating Gulf mode, 3 suggestions, stats, etc.).
4. For subscriptions, Google **auto-renews** and handles the 7-day trial,
   cancellations, retries, and refunds automatically.

Policy note: digital subscriptions **must** use Play Billing — you can't route
them through your own payment system. That's already how the app is built.

## 2. The revenue split

| Revenue | Google keeps | You keep |
|---|---|---|
| **Subscriptions** | **15%** | **85%** |
| One-time digital goods (first $1M/yr) | 15% | 85% |
| One-time digital goods (> $1M/yr) | 30% | 70% |

Auto-renewing subscriptions are 15% from day one.

**Your pricing, after Google's cut (before local tax/VAT):**
| Plan | Price | You receive (~85%) |
|---|---|---|
| Monthly | SAR 14.99 | ~SAR 12.74 |
| Annual | SAR 99.99 | ~SAR 85.00 |

## 3. How you receive the money (one-time setup)

In Play Console → **Setup → Payments profile**:
1. Create a **Google Payments merchant profile** and verify your identity.
2. Add a **bank account** for payouts (a Saudi bank account works — Saudi Arabia
   is a supported merchant/payout country; prices in SAR).
3. Provide **tax information** (note Saudi VAT 15%; Google handles tax collection
   in many regions, you handle reporting).

**Payout schedule:** Google pools your earnings and pays out **monthly**, around
the **15th of the following month**, once you're over the minimum threshold, by
bank transfer. So: user subscribes today → accrues in your console → lands in your
bank next month.

## 4. Create the subscription products

The app references these exact product IDs — they **must** match in Play Console,
or only the debug-unlock works:

| Product ID | Plan | Price |
|---|---|---|
| `katib_monthly` | Monthly, auto-renew | SAR 14.99 |
| `katib_annual` | Yearly, auto-renew | SAR 99.99 |

Steps (Play Console → **Monetize → Products → Subscriptions**):
1. **Create subscription** → Product ID `katib_monthly`.
2. Add a **base plan**: auto-renewing, billing period *monthly*, price SAR 14.99.
3. (Optional) Add an **offer** with a 7-day free trial.
4. **Activate** it. Repeat for `katib_annual` (yearly, SAR 99.99).

> The IDs live in code at `SubscriptionManager.MONTHLY_PRODUCT_ID` /
> `ANNUAL_PRODUCT_ID`. If you change the IDs in Play, change them there too.

## 5. Test purchases — for FREE, before launch

You do **not** spend real money testing. Two ways:

**A. License testers (recommended)**
1. Play Console → **Setup → License testing**.
2. Add the Gmail addresses that will test (e.g. your own).
3. Those accounts see "**Test card, always approves**" in the payment sheet —
   purchases complete with **no charge**, and renewals are fast-forwarded.

**B. Internal testing track**
1. Upload `app-release.aab` to **Testing → Internal testing**.
2. Add testers, share the opt-in link, install from Play on a real device.
3. Combined with license testing, you can run the full
   **subscribe → unlock → restore → cancel** flow for free.

### What to verify in the purchase flow
- Tap a plan → Google sheet shows "Test card" → confirm → app unlocks Premium
- Kill & reopen the app → still Premium (entitlement restored on launch)
- **Restore purchases** button re-grants on a fresh install
- Cancel the sub in Play → after the period, app reverts to Free
- Free limits hold for non-premium: 30 corrections/day, 1 suggestion chip, MSA only

## 6. Going-live checklist
- [ ] Merchant/payments profile verified, bank + tax added
- [ ] `katib_monthly` and `katib_annual` created **and active**
- [ ] Tested the full flow with a license tester (free)
- [ ] Prices set for your target countries (start: Saudi Arabia, SAR)
- [ ] Promote the build from internal testing → production

---

**Quick reference**
- Split: **you keep 85%** of subscription revenue
- Payout: **monthly, ~15th**, to your bank
- Product IDs: **`katib_monthly`**, **`katib_annual`** (must match the app)
- Testing: **License testing** = free test purchases
