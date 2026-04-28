#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:-env/prod.env}"

docker compose --env-file "${ENV_FILE}" build service1 service2 client
