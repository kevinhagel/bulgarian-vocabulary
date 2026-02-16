#!/bin/bash
cd ~/bulgarian-vocabulary

echo '====================================='
echo 'Bulgarian Vocabulary - Dev Environment'
echo '====================================='
echo ''

# Kill existing session if it exists
if tmux has-session -t bgvocab 2>/dev/null; then
    echo 'Stopping existing session...'
    tmux kill-session -t bgvocab
    sleep 1
fi

echo 'Starting services...'
echo ''
echo 'URLs (from MacBook M2):'
echo '  Frontend:        http://192.168.1.10:5173'
echo '  Backend API:     http://192.168.1.10:8080'
echo '  pgAdmin:         http://192.168.1.10:5050'
echo '  Redis Commander: http://192.168.1.10:8082'
echo ''

# Create new tmux session
tmux new-session -d -s bgvocab -n backend
tmux send-keys -t bgvocab:backend 'cd ~/bulgarian-vocabulary/backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev' C-m
tmux new-window -t bgvocab -n frontend
tmux send-keys -t bgvocab:frontend 'cd ~/bulgarian-vocabulary/frontend && npm run dev -- --host 0.0.0.0' C-m

echo ''
echo 'Services starting in tmux session: bgvocab'
echo ''
echo 'To view logs:'
echo '  tmux attach -t bgvocab'
echo ''
echo 'To detach (keep services running):'
echo '  Ctrl+B, then D'
echo ''
echo 'To stop everything:'
echo '  ./stop-dev.sh'
echo ''
