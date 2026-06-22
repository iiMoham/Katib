"""Live smoke test against the real Grok API.

Requires XAI_API_KEY in your environment / .env. Run:
    python smoke_test.py
"""

import asyncio

from app.config import get_settings
from app.grok_client import GrokClient
from app.models import Mode, Task

CASES = [
    ("انشاء الله بكرة نروح", Mode.msa, Task.correct),
    ("الطلاب ذهبت الى المدرسه", Mode.msa, Task.correct),
    ("وش رايك نروح المطعم", Mode.gulf, Task.correct),
    ("الاكل كان زين", Mode.gulf, Task.suggest),
]


async def main() -> None:
    settings = get_settings()
    if not settings.xai_api_key:
        print("XAI_API_KEY not set — put it in proxy/.env first.")
        return

    grok = GrokClient(settings)
    for text, mode, task in CASES:
        print(f"\n=== [{mode.value}/{task.value}] {text}")
        try:
            result = await grok.process(text, mode, task)
        except Exception as exc:  # noqa: BLE001
            print("  ERROR:", exc)
            continue
        for c in result.corrections:
            span = text[c.start : c.start + c.length] if c.start >= 0 else "?"
            print(f"  ✎ {c.original!r} → {c.corrected!r}  [{c.type}] @{c.start} ('{span}')  — {c.reason}")
        for g in result.suggestions:
            opts = "، ".join(f"{o.text}({o.register})" for o in g.options)
            print(f"  ⇢ {g.target!r}: {opts}")
        if not result.corrections and not result.suggestions:
            print("  (no issues / no suggestions)")


if __name__ == "__main__":
    asyncio.run(main())
