#!/bin/bash
set -e
APP_DIR="$(cd "$(dirname "$0")" && pwd)"
URL_FILE="$APP_DIR/server-url.txt"
APP_EXE="$APP_DIR/MedicalTriageClient.app/Contents/MacOS/MedicalTriageClient"
if [ ! -f "$URL_FILE" ]; then
  echo "Missing server-url.txt next to this script."
  read -r -p "Press Enter to exit..."
  exit 1
fi
SERVER_URL="$(grep -v "^[[:space:]]*#" "$URL_FILE" | sed "/^[[:space:]]*$/d" | head -n 1)"
if [ -z "$SERVER_URL" ]; then
  echo "No server URL found in server-url.txt."
  read -r -p "Press Enter to exit..."
  exit 1
fi
if [ ! -x "$APP_EXE" ]; then
  echo "Missing app executable: $APP_EXE"
  read -r -p "Press Enter to exit..."
  exit 1
fi
echo "Using server: $SERVER_URL"
export TRIAGE_SERVER_BASE_URL="$SERVER_URL"
cd "$APP_DIR"
exec "$APP_EXE"
