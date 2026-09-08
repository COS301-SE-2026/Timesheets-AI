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

# upgrade pip tooks
RUN python -m pip install --upgrade \
    pip==25.0.1 \
    setuptools==75.3.4 \
    wheel==0.45.1 \
    && pip install --no-cache-dir -r requirements.txt
 
# Copy only necessary files explicitly
COPY main.py .
COPY app/ ./app/
COPY tests/ ./tests/

RUN chown -R appuser:appgroup /app
USER appuser
 
EXPOSE 8000
 
# reload is not allowed in production
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

