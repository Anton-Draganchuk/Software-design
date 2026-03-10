#!/usr/bin/env bash
set -euo pipefail

PACT_FILE="rate-printer/target/pacts/rate-printer-currency-rate-provider.json"
PACT_BROKER_URL="${PACT_BROKER_URL:-http://localhost:9292}"
CONSUMER_VERSION="${CONSUMER_VERSION:-1.0.0}"

if [[ ! -f "$PACT_FILE" ]]; then
  echo "Pact file not found: $PACT_FILE"
  echo "Run consumer tests first: mvn -pl rate-printer test"
  exit 1
fi

curl -fsS \
  -X PUT \
  -H "Content-Type: application/json" \
  --data-binary "@$PACT_FILE" \
  "$PACT_BROKER_URL/pacts/provider/currency-rate-provider/consumer/rate-printer/version/$CONSUMER_VERSION"

echo "Published pact to $PACT_BROKER_URL (version=$CONSUMER_VERSION)"
