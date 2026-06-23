"""Application configuration, loaded from environment / .env file."""

from __future__ import annotations

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict

# Built-in presets for OpenAI-compatible providers: (base_url, default_model).
# All three speak the OpenAI Chat Completions API, so swapping is just config.
PROVIDERS: dict[str, tuple[str, str]] = {
    "xai": ("https://api.x.ai/v1", "grok-2-latest"),
    "groq": ("https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
    "gemini": ("https://generativelanguage.googleapis.com/v1beta/openai/", "gemini-2.0-flash"),
}


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # --- LLM provider (generic). Pick xai | groq | gemini, or set llm_base_url directly. ---
    llm_provider: str = "xai"
    llm_api_key: str = ""
    llm_model: str = ""        # empty -> provider default
    llm_base_url: str = ""     # empty -> provider default
    json_mode: bool = True     # use response_format json_object (turn off if a provider rejects it)

    # --- Back-compat with the original xAI-only vars (still honoured) ---
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

    # ---- Resolved provider settings (what the client actually uses) ----

    @property
    def provider(self) -> str:
        return self.llm_provider.strip().lower() or "xai"

    @property
    def _preset(self) -> tuple[str, str]:
        return PROVIDERS.get(self.provider, PROVIDERS["xai"])

    @property
    def resolved_api_key(self) -> str:
        return self.llm_api_key or self.xai_api_key

    @property
    def resolved_base_url(self) -> str:
        if self.llm_base_url:
            return self.llm_base_url
        if self.provider == "xai":
            return self.xai_base_url
        return self._preset[0]

    @property
    def resolved_model(self) -> str:
        if self.llm_model:
            return self.llm_model
        if self.provider == "xai":
            return self.grok_model
        return self._preset[1]

    @property
    def cors_origin_list(self) -> list[str]:
        raw = self.cors_origins.strip()
        if raw == "*" or raw == "":
            return ["*"]
        return [o.strip() for o in raw.split(",") if o.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
