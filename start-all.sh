#!/bin/bash

if [ "$1" == "stop" ]; then
    docker compose -f kong.yml down
    docker compose down
    echo "All containers stopped!"
else
    echo "Starting all containers..."
    docker compose up -d

    echo "Waiting 30s for all services to initialize..."
    sleep 30

    echo "Starting Kong..."
    docker compose -f kong.yml up -d

    echo "Waiting 10s for Kong..."
    sleep 10

    echo "All containers started!"
    docker ps
fi
