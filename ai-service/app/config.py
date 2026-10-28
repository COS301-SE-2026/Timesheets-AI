"""
This handles the configuration settings for the AI service.
It uses Pydantic's BaseSettings to manage environment variables and application settings.
Please check my draft file for links/resources on Pydantics features and usage.

Author: Zamokuhle Zwane
Date: 12/07/2026
"""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")
    # As suggested i moved the placeholder db url and moved it to .env.example
    database_url: str

    # here i'll place the api key for whatever api we use for the ai service
    anthropic_api_key: str = ""
    gemini_api_key: str = ""  # we went with gemini instead

    backend_url: str

    app_env: str = "development"
    log_level: str = "info"

    # shared secret the Spring backend sends in X-Internal-Api-Key, see app/security.py
    ai_service_api_key: str = ""

    # the browser should never call this service directly, so no origins by default.
    # comma separated, e.g. "http://localhost:4200" if you ever need Swagger from the frontend origin
    cors_allowed_origins: str = ""

    @property
    def is_development(self) -> bool:
        return self.app_env.lower() in {"development", "dev", "local", "test"}

    @property
    def cors_origins(self) -> list[str]:
        return [o.strip() for o in self.cors_allowed_origins.split(",") if o.strip()]


settings = Settings()
