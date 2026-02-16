#!/bin/bash

echo '====================================='
echo 'Stopping Bulgarian Vocabulary Dev'
echo '====================================='
echo ''

# Check if tmux session exists
if tmux has-session -t bgvocab 2>/dev/null; then
    echo 'Killing tmux session: bgvocab'
    tmux kill-session -t bgvocab
    echo '✓ Tmux session stopped'
else
    echo 'No tmux session found. Checking for running processes...'
    
    # Kill backend if running
    if lsof -ti:8080 >/dev/null 2>&1; then
        echo 'Stopping backend (port 8080)...'
        lsof -ti:8080 | xargs kill -9
        echo '✓ Backend stopped'
    fi
    
    # Kill frontend if running
    if lsof -ti:5173 >/dev/null 2>&1; then
        echo 'Stopping frontend (port 5173)...'
        lsof -ti:5173 | xargs kill -9
        echo '✓ Frontend stopped'
    fi
fi

echo ''
echo 'All services stopped!'
