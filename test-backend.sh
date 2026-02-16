#!/bin/bash
# UAT Backend Testing Script

set -e

echo "=== Bulgarian Vocabulary Backend UAT ==="
echo ""

# Check environment
echo "1. Checking environment variables..."
if [ -z "$POSTGRES_PASSWORD" ]; then
  echo "   ⚠️  POSTGRES_PASSWORD not set. Exporting from .env..."
  export POSTGRES_PASSWORD='BG_Vocab_2026_PostgreSQL!'
fi
echo "   ✓ POSTGRES_PASSWORD is set"
echo ""

# Check Docker services
echo "2. Checking Docker infrastructure..."
docker-compose ps --format "table {{.Name}}\t{{.Status}}" | grep -E "postgres|valkey"
echo ""

# Check PostgreSQL connectivity
echo "3. Testing PostgreSQL connection..."
PGPASSWORD=$POSTGRES_PASSWORD psql -h localhost -U vocab_user -d bulgarian_vocab -c "SELECT 1;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "   ✓ PostgreSQL connection successful"
else
  echo "   ✗ PostgreSQL connection failed"
  exit 1
fi
echo ""

# Check Valkey/Redis
echo "4. Testing Valkey connection..."
redis-cli -h localhost -p 6379 ping > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "   ✓ Valkey connection successful"
else
  echo "   ✗ Valkey connection failed (non-critical)"
fi
echo ""

# Check Ollama
echo "5. Testing Ollama connection..."
curl -s http://localhost:11434/api/tags > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "   ✓ Ollama is accessible"
  echo "   Available models:"
  curl -s http://localhost:11434/api/tags | grep -o '"name":"[^"]*"' | head -3
else
  echo "   ✗ Ollama not accessible (will fail LLM operations)"
fi
echo ""

# Start backend
echo "6. Starting Spring Boot backend..."
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev &
BACKEND_PID=$!
echo "   Backend PID: $BACKEND_PID"
echo ""

# Wait for backend to start
echo "7. Waiting for backend to start (max 60s)..."
for i in {1..60}; do
  if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "   ✓ Backend is UP!"
    break
  fi
  echo -n "."
  sleep 1
done
echo ""

# Test API endpoints
echo "8. Testing API endpoints..."

echo "   - GET /actuator/health"
curl -s http://localhost:8080/actuator/health | jq .

echo ""
echo "   - GET /api/vocabulary (list vocabulary)"
curl -s http://localhost:8080/api/vocabulary | jq '.content | length' || echo "Failed"

echo ""
echo "=== Backend UAT Complete ==="
echo ""
echo "Backend is running on: http://localhost:8080"
echo "To stop: kill $BACKEND_PID"
echo ""
echo "Next: Start frontend with 'cd frontend && npm run dev'"
