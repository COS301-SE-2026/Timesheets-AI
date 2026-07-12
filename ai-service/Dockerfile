FROM python:3.11-slim
 
WORKDIR /app

# Create non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
 
# Install dependencies first (layer caching)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
 
# Copy only necessary files explicitly
COPY main.py .
COPY tests/ ./tests/

RUN chown -R appuser:appgroup /app
USER appuser
 
EXPOSE 8000

#I just changed main:app to app.main:app because the main.py file is in the app directory.
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]
