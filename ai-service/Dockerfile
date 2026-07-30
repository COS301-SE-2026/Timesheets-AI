FROM python:3.11-slim
 
WORKDIR /app

# Install wget so it does not stop nignx container from uilding and for healthchecks
# RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*
# Create non-root user
# RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
# trigger AI to build an image 

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && addgroup --system appgroup \
    && adduser --system --ingroup appgroup appuser \
    && rm -rf /var/lib/apt/lists/*

 
# Install dependencies first (layer caching)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
 
# Copy only necessary files explicitly
COPY main.py .
COPY tests/ ./tests/

RUN chown -R appuser:appgroup /app
USER appuser
 
EXPOSE 8000
 
# reload is not allowed in production
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
