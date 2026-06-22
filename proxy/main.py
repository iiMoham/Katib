"""Katib proxy — FastAPI application entrypoint.

Run locally:
    uvicorn main:app --reload --port 8000

The proxy exposes a single POST /correct endpoint that the Android keyboard
calls. It keeps the xAI key server-side, caches recent results, and never
persists user text.
"""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app import __version__
from app.cache import LRUCache
from app.config import get_settings
from app.grok_client import GrokClient
from app.routes.corrections import router as corrections_router

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger("katib")


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    if not settings.xai_api_key:
        logger.warning("XAI_API_KEY is not set — /correct will return 502 until configured.")
    app.state.settings = settings
    app.state.cache = LRUCache(capacity=settings.cache_size)
    app.state.grok = GrokClient(settings)
    logger.info("Katib proxy v%s ready (model=%s)", __version__, settings.grok_model)
    yield


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(
        title="Katib Proxy",
        description="Arabic writing-assistant backend (grammar correction + suggestions) powered by Grok.",
        version=__version__,
        lifespan=lifespan,
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origin_list,
        allow_credentials=False,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(corrections_router)

    @app.get("/health", tags=["meta"])
    async def health() -> dict:
        return {
            "status": "ok",
            "version": __version__,
            "model": settings.grok_model,
            "key_configured": bool(settings.xai_api_key),
            "cache_size": len(app.state.cache) if hasattr(app.state, "cache") else 0,
        }

    @app.get("/", tags=["meta"])
    async def root() -> dict:
        return {"service": "katib-proxy", "version": __version__, "docs": "/docs"}

    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn

    s = get_settings()
    uvicorn.run("main:app", host=s.host, port=s.port, reload=True)
