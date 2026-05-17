#!/bin/bash
# .devcontainer/setup.sh
# Runs automatically when a Codespace is created.
# Safe to re-run manually if something fails.

set -e

REPO_ROOT="/workspaces/Timesheets-AI"
cd "$REPO_ROOT"

echo "Momently Codespace Setup"

# ── PostgreSQL ─────────────────────────────────────────────────────────────
echo "Starting PostgreSQL..."
sudo service postgresql start 2>/dev/null || true
sleep 2

sudo -u postgres psql -c "
  DO \$\$
  BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'momently') THEN
      CREATE USER momently WITH PASSWORD 'momently_dev_password';
    END IF;
  END
  \$\$;
" 2>/dev/null || true

sudo -u postgres psql -c "
  SELECT 'CREATE DATABASE momently_dev OWNER momently'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'momently_dev')
" -t 2>/dev/null | sudo -u postgres psql 2>/dev/null || true

sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE momently_dev TO momently;" 2>/dev/null || true
echo "  PostgreSQL ready → localhost:5432"

# ── Redis ──────────────────────────────────────────────────────────────────
echo "Starting Redis..."
sudo service redis-server start 2>/dev/null || redis-server --daemonize yes 2>/dev/null || true
echo "  Redis ready → localhost:6379"

# ── Root workspace (Turbo) ─────────────────────────────────────────────────
echo "Installing root workspace dependencies (Turborepo)..."
npm install -g yarn --silent 2>/dev/null || true
yarn install --silent
echo "  Turborepo ready"

# ── Backend ────────────────────────────────────────────────────────────────
echo "Resolving backend Maven dependencies..."
cd "$REPO_ROOT/apps/backend"
mvn dependency:resolve -q 2>/dev/null && echo "  Maven dependencies resolved" || echo "  Maven resolve had warnings (non-fatal)"

# ── Frontend ───────────────────────────────────────────────────────────────
echo "Installing frontend dependencies..."
# Angular app lives at apps/frontend/client/ (matches frontend-ci.yml)
FRONTEND_DIR="$REPO_ROOT/apps/frontend/client"
if [ -d "$FRONTEND_DIR" ]; then
  npm install -g @angular/cli --silent 2>/dev/null || true
  cd "$FRONTEND_DIR"
  yarn install --silent 2>/dev/null && echo "  Frontend dependencies installed" || echo "  yarn install had warnings"
else
  echo "  apps/frontend/client/ not found yet — skipping (scaffold it with: ng new client)"
fi

# ── AI Service ─────────────────────────────────────────────────────────────
echo "Installing AI service dependencies..."
cd "$REPO_ROOT/ai-service"
if [ -f requirements.txt ]; then
  pip install -r requirements.txt -q
  echo "  AI service dependencies installed"
else
  echo "  No requirements.txt yet (skipping)"
fi

# ── Done ───────────────────────────────────────────────────────────────────
cd "$REPO_ROOT"

echo "Setup complete!"

echo ""
echo "Run individual services:"
echo "  Backend    →  cd apps/backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "  Frontend   →  cd apps/frontend && yarn start"
echo "  AI service →  cd ai-service && uvicorn main:app --reload --port 8000"
echo ""
echo "Or run all at once from repo root:"
echo "  yarn dev"
echo ""
echo "Health checks:"
echo "  Backend  → http://localhost:8080/actuator/health"
echo "  Swagger  → http://localhost:8080/swagger-ui.html"
echo "  AI docs  → http://localhost:8000/docs"
echo ""