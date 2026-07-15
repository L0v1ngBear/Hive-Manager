#!/bin/bash
set -euo pipefail

ENV_FILE="${1:-.env}"

if [ ! -f "${ENV_FILE}" ]; then
  exit 0
fi

# Keep deployment stable when files are edited or uploaded from Windows.
# - Strip UTF-8 BOM so the first key can be matched by grep/source.
# - Strip CRLF so bash and docker compose read values consistently.
sed -i '1s/^\xEF\xBB\xBF//' "${ENV_FILE}"
sed -i 's/\r$//' "${ENV_FILE}"
