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
    # this is temporary database url,I'm not 100% sure about this, I will change this to a proper database url in the future.
    database_url: str = "postgresql://localhost:5432/momently_ai"

    # here i'll place the api key for whatever api we use for the ai service. As it stands we may use anthropic's api.
    anthropic_api_key: str = ""  # Replace with actual API key

    app_env: str = "development"
    log_level: str = "info"


settings = Settings()
