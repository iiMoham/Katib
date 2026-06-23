"""Thin async wrapper around an OpenAI-compatible LLM (xAI Grok, Groq, or Gemini).

The proxy never exposes the API key to the client; all calls go through here.
We request a JSON object response and parse it into our schema, computing
character offsets locally via text_utils. Provider is chosen in config.
"""

from __future__ import annotations

import json
import logging
import re

from openai import AsyncOpenAI

from .config import Settings
from .models import (
    Correction,
    CorrectResponse,
    Mode,
    SuggestionGroup,
    SuggestionOption,
    Task,
)
from .prompts import build_system_prompt, build_user_prompt
from .text_utils import locate

logger = logging.getLogger("katib.llm")


class GrokError(RuntimeError):
    """Raised when the upstream model call fails or returns unusable data."""


def _extract_json(content: str) -> dict:
    """Parse a JSON object from a model response, tolerating markdown fences.

    Some providers wrap JSON in ```json ... ``` or add stray prose; we strip
    fences and fall back to the outermost {...} block.
    """
    text = content.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?\s*", "", text)
        text = re.sub(r"\s*```$", "", text).strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        start, end = text.find("{"), text.rfind("}")
        if start != -1 and end != -1 and end > start:
            return json.loads(text[start : end + 1])
        raise


class GrokClient:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._client = AsyncOpenAI(
            api_key=settings.resolved_api_key,
            base_url=settings.resolved_base_url,
            timeout=settings.request_timeout,
        )

    async def process(self, text: str, mode: Mode, task: Task) -> CorrectResponse:
        system = build_system_prompt(mode, task)
        user = build_user_prompt(text, task)

        kwargs: dict = dict(
            model=self._settings.resolved_model,
            temperature=self._settings.temperature,
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
        )
        if self._settings.json_mode:
            kwargs["response_format"] = {"type": "json_object"}

        try:
            completion = await self._client.chat.completions.create(**kwargs)
        except Exception as exc:  # noqa: BLE001 — surface a clean error to the route
            logger.exception("LLM API call failed (provider=%s)", self._settings.provider)
            raise GrokError(str(exc)) from exc

        content = completion.choices[0].message.content or "{}"
        try:
            data = _extract_json(content)
        except json.JSONDecodeError as exc:
            logger.error("Model returned non-JSON content: %r", content[:500])
            raise GrokError("Model returned malformed JSON") from exc

        if task is Task.correct:
            return self._parse_corrections(text, data, mode)
        return self._parse_suggestions(text, data, mode)

    # ---------- parsing ----------

    def _parse_corrections(self, text: str, data: dict, mode: Mode) -> CorrectResponse:
        out: list[Correction] = []
        for item in data.get("corrections", []) or []:
            original = str(item.get("original", "")).strip()
            corrected = str(item.get("corrected", "")).strip()
            if not original or original == corrected:
                continue
            start, length = locate(text, original)
            out.append(
                Correction(
                    original=original,
                    corrected=corrected,
                    start=start,
                    length=length,
                    type=_safe_type(item.get("type")),
                    reason=str(item.get("reason", "")).strip(),
                )
            )
        return CorrectResponse(corrections=out, mode=mode, task=Task.correct)

    def _parse_suggestions(self, text: str, data: dict, mode: Mode) -> CorrectResponse:
        groups: list[SuggestionGroup] = []
        for grp in data.get("suggestions", []) or []:
            target = str(grp.get("target", "")).strip()
            start, length = locate(text, target) if target else (-1, 0)
            options: list[SuggestionOption] = []
            for opt in (grp.get("options", []) or [])[:3]:
                opt_text = str(opt.get("text", "")).strip()
                if not opt_text:
                    continue
                options.append(
                    SuggestionOption(
                        text=opt_text,
                        register=str(opt.get("register", "فصحى")).strip() or "فصحى",
                        rank=int(opt.get("rank", len(options) + 1) or len(options) + 1),
                    )
                )
            if options:
                groups.append(
                    SuggestionGroup(target=target, start=start, length=length, options=options)
                )
        return CorrectResponse(suggestions=groups, mode=mode, task=Task.suggest)


def _safe_type(value) -> str:
    from .models import CorrectionType

    try:
        return CorrectionType(str(value)).value
    except (ValueError, TypeError):
        return CorrectionType.other.value
