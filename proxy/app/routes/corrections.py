"""/correct endpoint — the single entry point used by the keyboard."""

from __future__ import annotations

from fastapi import APIRouter, Depends, Header, HTTPException, Request

from ..config import Settings, get_settings
from ..grok_client import GrokError
from ..models import CorrectRequest, CorrectResponse

router = APIRouter(tags=["corrections"])


def _check_auth(settings: Settings, provided: str | None) -> None:
    """Optional shared-secret gate between the app and this proxy."""
    if settings.proxy_api_key and provided != settings.proxy_api_key:
        raise HTTPException(status_code=401, detail="Invalid or missing X-Katib-Key")


@router.post("/correct", response_model=CorrectResponse)
async def correct(
    payload: CorrectRequest,
    request: Request,
    settings: Settings = Depends(get_settings),
    x_katib_key: str | None = Header(default=None, alias="X-Katib-Key"),
) -> CorrectResponse:
    _check_auth(settings, x_katib_key)

    text = payload.text.strip()
    if not text:
        return CorrectResponse(mode=payload.mode, task=payload.task)

    if len(text) > settings.max_text_length:
        raise HTTPException(
            status_code=413,
            detail=f"Text exceeds max length of {settings.max_text_length} characters.",
        )

    cache = request.app.state.cache
    key = (text, payload.mode.value, payload.task.value)

    cached = cache.get(key)
    if cached is not None:
        return cached.model_copy(update={"cached": True})

    grok = request.app.state.grok
    try:
        result = await grok.process(text, payload.mode, payload.task)
    except GrokError as exc:
        raise HTTPException(status_code=502, detail=f"Upstream model error: {exc}") from exc

    cache.put(key, result)
    return result
