#!/bin/bash

wait_for_service() {
    local name=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    echo "Waiting for $name to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -qE "^[2-4]"; then
            echo "$name is ready!"
            return 0
        fi
        echo "$name not ready yet... attempt $attempt/$max_attempts"
        sleep 5
        attempt=$((attempt + 1))
    done
    echo "$name failed to start after $((max_attempts * 5)) seconds"
    return 1
}

if [ "$1" == "stop" ]; then
    docker compose -f kong.yml down
    docker compose down
    echo "All containers stopped!"
else
    echo "Starting core infrastructure..."
    docker compose up -d

    # Wait for MySQL
    wait_for_service "MySQL" "http://localhost:3306" 2>/dev/null || true
    echo "Waiting 20s for MySQL to fully initialize..."
    sleep 20

    # Wait for Kafka
    echo "Waiting 10s for Kafka to initialize..."
    sleep 10

    # Wait for Auth Service
    wait_for_service "Auth Service" "http://localhost:9898/auth/v1/ping"

    # Wait for User Service
    wait_for_service "User Service" "http://localhost:9810/user/v1/getUser"

    # Wait for Expense Service
    wait_for_service "Expense Service" "http://localhost:9820/expense/v1/get?user_id=test"

    # Wait for DS Service
    wait_for_service "DS Service" "http://localhost:8000/"

    echo "Starting Kong..."
    docker compose -f kong.yml up -d

    # Wait for Kong
    wait_for_service "Kong" "http://localhost:8005/"

    echo ""
    echo "All containers started and ready!"
    echo ""
    docker ps -a
fi
