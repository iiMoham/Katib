"""Application configuration, loaded from environment / .env file."""

from __future__ import annotations

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # xAI Grok
    xai_api_key: str = ""
    xai_base_url: str = "https://api.x.ai/v1"
    grok_model: str = "grok-2-latest"

    # Server
    host: str = "0.0.0.0"
    port: int = 8000
    cors_origins: str = "*"

    # Behaviour
    max_text_length: int = 600
    cache_size: int = 50
    request_timeout: int = 12
    temperature: float = 0.2

    # Optional shared-secret auth
    proxy_api_key: str = ""

    @property
    def cors_origin_list(self) -> list[str]:
        raw = self.cors_origins.strip()
        if raw == "*" or raw == "":
            return ["*"]
        return [o.strip() for o in raw.split(",") if o.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
