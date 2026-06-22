# كاتب — Katib

An intelligent **Arabic writing assistant** (Grammarly-style) that works
system-wide as a custom keyboard. Real-time grammar & spelling correction plus
word/phrase suggestions, in **Modern Standard Arabic (الفصحى)** and **Gulf
dialect (الخليجي)**.

> **Platform note:** the original spec targeted iOS. Since development is on
> Windows (where iOS/Xcode isn't available), this is built for **Android**,
> which supports a true system-wide keyboard via `InputMethodService` and builds
> natively on Windows. The iOS → Android mapping is in the table below.

## Repository layout

```
ArabicCorrector/
├── proxy/      FastAPI backend — Arabic correction/suggestion engine (xAI Grok)
├── android/    Android app (Kotlin/Compose) + system-wide keyboard (IME)
└── docs/       privacy-policy.html (required for store submission)
```

## Status

| Component | State |
|---|---|
| **Proxy** (`proxy/`) | ✅ Builds & runs on Windows. 12 unit tests pass. Boots clean. |
| **Android app** (`android/`) | ✅ `assembleDebug` succeeds → `app-debug.apk`. Main app + IME implemented. |
| Live AI corrections | ⏳ Needs an `XAI_API_KEY` in `proxy/.env`, then run `smoke_test.py` / the app. |
| Google Play Billing | ⚙️ Wired with a dev fallback; real products need Play Console setup. |
| Store assets / screenshots | ⬜ Not yet produced. |

## Architecture

```
 Android keyboard (IME)  ──HTTPS──>  FastAPI proxy  ──>  xAI Grok
   debounced 400ms                    (holds API key,
   shows chips,                        caches results,
   applies fixes                       no text stored)
```

The proxy contract is a single `POST /correct`:
`{ text, mode: msa|gulf, task: correct|suggest }` →
`{ corrections: [...], suggestions: [...] }`. Full details in
[`proxy/README.md`](proxy/README.md).

## iOS spec → Android implementation

| iOS (spec) | Android (this repo) |
|---|---|
| Custom Keyboard Extension (`UIInputViewController`) | `InputMethodService` (`KatibInputMethodService`) |
| App Groups shared storage | Shared DataStore (`Prefs`) |
| StoreKit 2 | Google Play Billing (`SubscriptionManager`) |
| Keychain | (premium flag in DataStore; Keystore-ready) |
| CoreData / SwiftData | DataStore (stats); Room-ready for history |
| SF Arabic font | System Arabic (Noto Naskh) |
| Inline error underline in any app | Chip bar above keys (Android can't style other apps' text — same as Grammarly) |

## Quick start

### 1. Backend
```powershell
cd proxy
python -m venv .venv; .\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env          # add your XAI_API_KEY
uvicorn main:app --reload --port 8000
```
Test without a key: `pytest -q`. Test with a key: `python smoke_test.py`.

### 2. Android
Open `android/` in Android Studio (or build from CLI):
```powershell
cd android
.\gradlew assembleDebug
```
On the **emulator**, the app reaches the proxy at `http://10.0.2.2:8000`
(already set as the debug `PROXY_BASE_URL`). On a **physical device**, change it
to your machine's LAN IP in `android/app/build.gradle.kts`. For production, set
the release `PROXY_BASE_URL` to your deployed proxy URL.

Then in the app: complete onboarding → enable the **كاتب** keyboard in Android
keyboard settings → switch to it → start typing Arabic.

## Freemium model

| | Free | Premium |
|---|---|---|
| Corrections/day | 30 | unlimited |
| Suggestion chips | 1 | 3 + thesaurus |
| Gulf dialect mode | — | ✓ |
| Stats dashboard | basic | full |

Pricing (configurable in Play Console): Monthly SAR 14.99 · Annual SAR 99.99
(7-day trial).

## Privacy

User text is never persisted server-side; only a transient LRU of recent
results is cached. See [`docs/privacy-policy.html`](docs/privacy-policy.html).
