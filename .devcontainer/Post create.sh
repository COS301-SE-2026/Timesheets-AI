#!/bin/bash
set -e

echo "Setting up Timesheets-AI dev environment..."

# Install Angular CLI globally
echo "Installing Angular CLI..."
npm install -g @angular/cli@latest

# Install Python dependencies for AI services
if [ -f "apps/ai-services/requirements.txt" ]; then
  echo "Installing Python dependencies..."
  pip install -r apps/ai-services/requirements.txt
else
  echo "Installing base FastAPI dependencies..."
  pip install fastapi uvicorn sqlalchemy psycopg2-binary redis python-dotenv
fi

# Install frontend dependencies
if [ -f "apps/frontend/package.json" ]; then
  echo "Installing frontend dependencies..."
  cd apps/frontend && npm install && cd ../..
fi

# Build backend (download Maven dependencies)
if [ -f "apps/backend/pom.xml" ]; then
  echo "Downloading Maven dependencies..."
  cd apps/backend && mvn dependency:resolve -q && cd ../..
fi

echo "Dev environment ready!"
echo ""
echo "Quick start commands:"
echo "  Frontend:   cd apps/frontend && ng serve"
echo "  Backend:    cd apps/backend && mvn spring-boot:run"
echo "  AI Service: cd apps/ai-services && uvicorn main:app --reload"