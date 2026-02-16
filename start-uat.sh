#!/bin/bash
# Complete UAT Startup Script
# Starts infrastructure, backend, and frontend for testing

set -e

PROJECT_ROOT="/Users/kevin/bulgarian-vocabulary"
cd "$PROJECT_ROOT"

echo "🧪 === Bulgarian Vocabulary UAT Startup ==="
echo ""

# Export environment variables
echo "📝 Setting environment variables..."
export POSTGRES_PASSWORD='BG_Vocab_2026_PostgreSQL!'
export OLLAMA_BASE_URL='http://localhost:11434'
export AUDIO_STORAGE_PATH='/Volumes/T9-NorthStar/bulgarian-vocab/audio'
echo "   ✓ Environment variables set"
echo ""

# Check Docker infrastructure
echo "🐳 Checking Docker infrastructure..."
if docker-compose ps | grep -q "Up"; then
  echo "   ✓ Docker services already running"
else
  echo "   ⚠️  Starting Docker services..."
  docker-compose up -d
  echo "   ⏳ Waiting for services to be healthy..."
  sleep 10
fi
echo ""

# Verify PostgreSQL
echo "🗄️  Verifying PostgreSQL..."
if PGPASSWORD=$POSTGRES_PASSWORD psql -h localhost -U vocab_user -d bulgarian_vocab -c "SELECT 1;" > /dev/null 2>&1; then
  echo "   ✓ PostgreSQL is accessible"
  LEMMA_COUNT=$(PGPASSWORD=$POSTGRES_PASSWORD psql -h localhost -U vocab_user -d bulgarian_vocab -t -c "SELECT COUNT(*) FROM lemmas;")
  echo "   📊 Lemmas in database: $LEMMA_COUNT"
else
  echo "   ✗ PostgreSQL connection failed!"
  exit 1
fi
echo ""

# Verify Ollama
echo "🤖 Verifying Ollama..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
  echo "   ✓ Ollama is accessible"
  BGGPT_CHECK=$(curl -s http://localhost:11434/api/tags | grep -c "todorov/bggpt:9b" || echo "0")
  if [ "$BGGPT_CHECK" -gt 0 ]; then
    echo "   ✓ BgGPT model available"
  else
    echo "   ⚠️  BgGPT model not found (LLM features will fail)"
  fi
else
  echo "   ⚠️  Ollama not accessible (LLM features will fail)"
fi
echo ""

# Check audio storage
echo "🔊 Checking audio storage..."
if [ -d "$AUDIO_STORAGE_PATH" ]; then
  echo "   ✓ Audio storage exists: $AUDIO_STORAGE_PATH"
else
  echo "   ⚠️  Creating audio storage: $AUDIO_STORAGE_PATH"
  mkdir -p "$AUDIO_STORAGE_PATH"
fi
echo ""

# Start backend
echo "🚀 Starting Spring Boot backend..."
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../backend-uat.log 2>&1 &
BACKEND_PID=$!
echo "   Backend PID: $BACKEND_PID"
echo "   Logs: $PROJECT_ROOT/backend-uat.log"
cd ..
echo ""

# Wait for backend
echo "⏳ Waiting for backend to start (max 90s)..."
BACKEND_READY=0
for i in {1..90}; do
  if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "   ✓ Backend is UP at http://localhost:8080"
    BACKEND_READY=1
    break
  fi
  echo -n "."
  sleep 1
done
echo ""

if [ $BACKEND_READY -eq 0 ]; then
  echo "   ✗ Backend failed to start! Check backend-uat.log"
  tail -50 backend-uat.log
  exit 1
fi
echo ""

# Test backend API
echo "🧪 Testing backend API..."
echo "   - Health check:"
curl -s http://localhost:8080/actuator/health | jq -r '.status'
echo "   - Vocabulary count:"
VOCAB_COUNT=$(curl -s http://localhost:8080/api/vocabulary | jq -r '.content | length')
echo "     $VOCAB_COUNT entries"
echo ""

# Start frontend
echo "🎨 Starting React frontend..."
cd frontend
npm run dev > ../frontend-uat.log 2>&1 &
FRONTEND_PID=$!
echo "   Frontend PID: $FRONTEND_PID"
echo "   Logs: $PROJECT_ROOT/frontend-uat.log"
cd ..
echo ""

# Wait for frontend
echo "⏳ Waiting for frontend to start (max 30s)..."
FRONTEND_READY=0
for i in {1..30}; do
  if curl -s http://localhost:5173 > /dev/null 2>&1; then
    echo "   ✓ Frontend is UP at http://localhost:5173"
    FRONTEND_READY=1
    break
  fi
  echo -n "."
  sleep 1
done
echo ""

if [ $FRONTEND_READY -eq 0 ]; then
  echo "   ⚠️  Frontend may still be starting... check frontend-uat.log"
fi
echo ""

# Summary
echo "✅ === UAT Environment Ready ==="
echo ""
echo "📍 Access Points:"
echo "   - Frontend:        http://localhost:5173"
echo "   - Backend API:     http://localhost:8080"
echo "   - pgAdmin:         http://localhost:5050"
echo "   - Redis Commander: http://localhost:8082"
echo ""
echo "📝 Process IDs:"
echo "   - Backend:  $BACKEND_PID"
echo "   - Frontend: $FRONTEND_PID"
echo ""
echo "📋 UAT Checklist: $PROJECT_ROOT/UAT-CHECKLIST.md"
echo ""
echo "🛑 To stop services:"
echo "   kill $BACKEND_PID $FRONTEND_PID"
echo "   docker-compose stop"
echo ""
echo "📊 Monitor logs:"
echo "   tail -f backend-uat.log"
echo "   tail -f frontend-uat.log"
echo ""
