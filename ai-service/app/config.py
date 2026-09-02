"""
This handles the configuration settings for the AI service.
It uses Pydantic's BaseSettings to manage environment variables and application settings.
Please check my draft file for links/resources on Pydantics features and usage.

Author: Zamokuhle Zwane
Date: 12/07/2026
"""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")
    # As suggested i moved the placeholder db url and moved it to .env.example
    database_url: str = "postgresql://postgres:postgres@localhost:5432/momently_dev"

    # here i'll place the api key for whatever api we use for the ai service
    anthropic_api_key: str = ""
    gemini_api_key: str = ""  # we went with gemini instead
    app_env: str = "development"
    log_level: str = "info"


settings = Settings()
