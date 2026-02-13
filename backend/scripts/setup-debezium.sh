#!/usr/bin/env bash
set -euo pipefail

CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"
CONNECTOR_NAME="todo-postgres-connector"
MAX_ATTEMPTS=30
SLEEP_SEC=2

echo "Waiting for Kafka Connect at ${CONNECT_URL}..."
for i in $(seq 1 "$MAX_ATTEMPTS"); do
  if curl -sf -o /dev/null "${CONNECT_URL}/connectors" 2>/dev/null; then
    echo "Kafka Connect is ready."
    break
  fi
  if [[ $i -eq $MAX_ATTEMPTS ]]; then
    echo "Kafka Connect did not become ready in time. Is 'docker compose up -d' running?" >&2
    exit 1
  fi
  echo "  attempt $i/$MAX_ATTEMPTS..."
  sleep "$SLEEP_SEC"
done

echo "Registering Debezium connector: ${CONNECTOR_NAME}"
resp=$(curl -s -w "\n%{http_code}" --location "${CONNECT_URL}/connectors" \
  --header 'Content-Type: application/json' \
  --data '{
  "name": "todo-postgres-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",
    "topic.prefix": "debezium",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "postgres",
    "database.dbname": "todo",

    "slot.name": "debezium_slot",
    "plugin.name": "pgoutput",
    "publication.autocreate.mode": "filtered",

    "schema.include.list": "public",
    "table.include.list": "public.outbox",

    "decimal.handling.mode": "string",
    "time.precision.mode": "adaptive_time_microseconds",
    "tombstones.on.delete": "false"
  }
}')

http_code=$(echo "$resp" | tail -n1)
body=$(echo "$resp" | sed '$d')

if [[ "$http_code" -ge 200 && "$http_code" -lt 300 ]]; then
  echo "Connector registered successfully."
  echo "$body" | head -c 500
  echo ""
elif [[ "$http_code" -eq 409 ]] || echo "$body" | grep -q "already exists"; then
  echo "Connector '${CONNECTOR_NAME}' already exists. Skipping."
else
  echo "Failed to register connector (HTTP $http_code):" >&2
  echo "$body" >&2
  exit 1
fi
