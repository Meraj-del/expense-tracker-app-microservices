#!/bin/bash

if [ "$1" == "stop" ]; then
  docker compose -f kong.yml down
  docker compose down
  echo "All containers stopped!"
else
  docker compose up -d
  sleep 20
  docker compose -f kong.yml up -d
  echo "All containers started!"
  docker ps -a
fi
