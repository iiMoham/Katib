# Katib Proxy (كاتب)

FastAPI backend for the **Katib** Arabic writing assistant. It keeps the xAI
Grok key server-side, corrects/suggests Arabic text, and returns JSON the
Android keyboard can render directly.

## Contract

`POST /correct`

```jsonc
// request
{ "text": "انشاء الله بكرة", "mode": "msa", "task": "correct" }
//   mode: "msa" | "gulf"        task: "correct" | "suggest"
```

```jsonc
// response (task = correct)
{
  "corrections": [
    {
      "original": "انشاء",
      "corrected": "إن شاء",
      "start": 0, "length": 5,        // offsets located server-side, for underlining
      "type": "hamza",
      "reason": "الصواب فصل (إن شاء) لا (انشاء)."
    }
  ],
  "suggestions": [],
  "mode": "msa", "task": "correct", "cached": false
}
```

```jsonc
// response (task = suggest)
{
  "corrections": [],
  "suggestions": [
    {
      "target": "زين", "start": 5, "length": 3,
      "options": [
        { "text": "جيد",   "register": "فصحى",  "rank": 1 },
        { "text": "ممتاز", "register": "فصحى",  "rank": 2 },
        { "text": "زين",   "register": "خليجي", "rank": 3 }
      ]
    }
  ],
  "mode": "gulf", "task": "suggest", "cached": false
}
```

Other endpoints: `GET /health`, `GET /` , interactive docs at `GET /docs`.

## Run locally (Windows)

```powershell
cd proxy
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env       # then put your XAI_API_KEY in .env
uvicorn main:app --reload --port 8000
```

Open http://localhost:8000/docs to try it.

## Test

```powershell
.\.venv\Scripts\python.exe -m pytest -q
```

Tests stub the Grok client, so they run **without** an API key or network.

## Live smoke test (needs a real key in .env)

```powershell
.\.venv\Scripts\python.exe smoke_test.py
```

## AI provider (pick one — two are free)

The proxy talks to any OpenAI-compatible LLM, so you can swap providers with config only:

| `LLM_PROVIDER` | Cost | Get a key | Default model |
|---|---|---|---|
| `xai` | paid | https://console.x.ai | `grok-2-latest` |
| `groq` | **free tier** | https://console.groq.com/keys | `llama-3.3-70b-versatile` |
| `gemini` | **free tier** | https://aistudio.google.com/apikey | `gemini-2.0-flash` |

To run **free**, set just two vars: `LLM_PROVIDER=gemini` (or `groq`) and `LLM_API_KEY=...`.
For better Arabic on Groq, try `LLM_MODEL=qwen-2.5-32b`.

## Configuration (.env)

| Key | Default | Notes |
|---|---|---|
| `LLM_PROVIDER` | `xai` | `xai` \| `groq` \| `gemini` |
| `LLM_API_KEY` | — | key for the chosen provider |
| `LLM_MODEL` | _(provider default)_ | override the model |
| `LLM_BASE_URL` | _(provider default)_ | override the endpoint |
| `JSON_MODE` | `true` | uses `response_format=json_object` |
| `MAX_TEXT_LENGTH` | `600` | rejects longer text (privacy + cost) |
| `CACHE_SIZE` | `50` | LRU results cached in memory |
| `REQUEST_TIMEOUT` | `12` | seconds, per LLM call |
| `TEMPERATURE` | `0.2` | lower = more deterministic |
| `PROXY_API_KEY` | _(empty)_ | if set, clients must send `X-Katib-Key` |
| `CORS_ORIGINS` | `*` | comma-separated; lock down in prod |

> Legacy `XAI_API_KEY` / `GROK_MODEL` still work if `LLM_*` are unset.

## Privacy

The proxy **does not persist user text**. Requests are processed in memory and
only an LRU of recent results is cached transiently. See
[`../docs/privacy-policy.html`](../docs/privacy-policy.html).

## Deploy

- **Docker / Fly.io:** a `Dockerfile` is included. `fly launch` then `fly deploy`.
- **Railway:** point it at this folder; it auto-detects Python and uses the
  `Procfile`. Set env vars in the Railway dashboard.

Either way, set `XAI_API_KEY` (and ideally `PROXY_API_KEY`) as secrets.
