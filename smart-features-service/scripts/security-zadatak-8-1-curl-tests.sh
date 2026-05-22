#!/usr/bin/env bash
set -euo pipefail

AUTH_BASE_URL="${AUTH_BASE_URL:-http://localhost:8081}"
SMART_BASE_URL="${SMART_BASE_URL:-http://localhost:8082}"
USER_EMAIL="${USER_EMAIL:-user@example.com}"
PHARMACIST_EMAIL="${PHARMACIST_EMAIL:-pharmacist@example.com}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"
TEST_PASSWORD="${TEST_PASSWORD:-password123}"

RESPONSE_FILE="/tmp/security-zadatak-8-1-response.json"

PASS_COUNT=0
FAIL_COUNT=0

pass() {
  printf 'PASS %s\n' "$1" >&2
  PASS_COUNT=$((PASS_COUNT + 1))
}

fail() {
  printf 'FAIL %s\n' "$1" >&2
  FAIL_COUNT=$((FAIL_COUNT + 1))
}

json_field() {
  python3 -c 'import json,sys; print((json.load(sys.stdin).get(sys.argv[1]) or ""))' "$1"
}

pretty_json() {
  python3 -m json.tool 2>/dev/null || cat
}

mask_token() {
  local token="${1:-}"

  if [[ -z "$token" ]]; then
    printf ''
  elif [[ "$token" == "invalid-token" ]]; then
    printf 'invalid-token'
  else
    printf '%s...%s' "${token:0:12}" "${token: -8}"
  fi
}

print_http_trace() {
  local name="$1"
  local method="$2"
  local url="$3"
  local token="${4:-}"
  local body="${5:-}"
  local status="$6"

  printf '\n==================== %s ====================\n' "$name" >&2
  printf 'REQUEST:\n' >&2
  printf '%s %s\n' "$method" "$url" >&2
  printf 'Accept: application/json\n' >&2

  if [[ -n "$token" ]]; then
    printf 'Authorization: Bearer %s\n' "$(mask_token "$token")" >&2
  fi

  if [[ -n "$body" ]]; then
    printf 'Content-Type: application/json\n' >&2
    printf 'Body:\n%s\n' "$body" | pretty_json >&2
  else
    printf 'Body: <empty>\n' >&2
  fi

  printf '\nRESPONSE:\n' >&2
  printf 'HTTP %s\n' "$status" >&2

  if [[ -s "$RESPONSE_FILE" ]]; then
    printf 'Body:\n' >&2
    pretty_json < "$RESPONSE_FILE" >&2
  else
    printf 'Body: <empty>\n' >&2
  fi

  printf '============================================================\n' >&2
}

http_status() {
  local name="$1"
  local method="$2"
  local url="$3"
  local token="${4:-}"
  local body="${5:-}"
  local response

  if [[ -n "$token" && -n "$body" ]]; then
    response="$(curl -sS -o "$RESPONSE_FILE" -w '%{http_code}' \
      -X "$method" "$url" \
      -H "Authorization: Bearer $token" \
      -H 'Content-Type: application/json' \
      -H 'Accept: application/json' \
      --data "$body")"
  elif [[ -n "$token" ]]; then
    response="$(curl -sS -o "$RESPONSE_FILE" -w '%{http_code}' \
      -X "$method" "$url" \
      -H "Authorization: Bearer $token" \
      -H 'Accept: application/json')"
  elif [[ -n "$body" ]]; then
    response="$(curl -sS -o "$RESPONSE_FILE" -w '%{http_code}' \
      -X "$method" "$url" \
      -H 'Content-Type: application/json' \
      -H 'Accept: application/json' \
      --data "$body")"
  else
    response="$(curl -sS -o "$RESPONSE_FILE" -w '%{http_code}' \
      -X "$method" "$url" \
      -H 'Accept: application/json')"
  fi

  print_http_trace "$name" "$method" "$url" "$token" "$body" "$response"

  printf '%s' "$response"
}

login() {
  local email="$1"
  local role="$2"
  local login_body
  local register_body
  local status
  local token

  login_body="$(printf '{"email":"%s","password":"%s"}' "$email" "$TEST_PASSWORD")"

  status="$(http_status "login $role" POST "$AUTH_BASE_URL/api/auth/login" "" "$login_body")"
  token="$(json_field accessToken < "$RESPONSE_FILE" || true)"

  if [[ "$status" == "200" && -n "$token" ]]; then
    pass "login $role"
    printf '%s' "$token"
    return 0
  fi

  register_body="$(printf '{"firstName":"Security","lastName":"%s","email":"%s","password":"%s","role":"%s"}' "$role" "$email" "$TEST_PASSWORD" "$role")"

  status="$(http_status "register fallback $role" POST "$AUTH_BASE_URL/api/auth/register" "" "$register_body")"
  token="$(json_field accessToken < "$RESPONSE_FILE" || true)"

  if [[ "$status" == "201" && -n "$token" ]]; then
    pass "register fallback $role"
    printf '%s' "$token"
    return 0
  fi

  fail "login/register $role returned HTTP $status"
  printf ''
  return 0
}

expect_status() {
  local name="$1"
  local actual="$2"
  shift 2
  local expected

  for expected in "$@"; do
    if [[ "$actual" == "$expected" ]]; then
      pass "$name returned $actual"
      return
    fi
  done

  fail "$name returned $actual, expected one of: $*"
}

expect_not_auth_failure() {
  local name="$1"
  local actual="$2"

  if [[ "$actual" != "401" && "$actual" != "403" ]]; then
    pass "$name reached controller/service with HTTP $actual"
    return
  fi

  fail "$name returned security failure HTTP $actual"
}

USER_TOKEN="$(login "$USER_EMAIL" ROLE_USER)"
PHARMACIST_TOKEN="$(login "$PHARMACIST_EMAIL" ROLE_PHARMACIST)"
ADMIN_TOKEN="$(login "$ADMIN_EMAIL" ROLE_ADMIN)"

if [[ -z "$USER_TOKEN" || -z "$PHARMACIST_TOKEN" || -z "$ADMIN_TOKEN" ]]; then
  printf '\nSecurity curl tests finished: %s passed, %s failed\n' "$PASS_COUNT" "$FAIL_COUNT" >&2
  exit 1
fi

expect_status "valid user token GET /api/symptoms" \
  "$(http_status "valid user token GET /api/symptoms" GET "$SMART_BASE_URL/api/symptoms" "$USER_TOKEN")" 200

expect_status "missing token GET /api/symptoms" \
  "$(http_status "missing token GET /api/symptoms" GET "$SMART_BASE_URL/api/symptoms")" 401

expect_status "invalid token GET /api/symptoms" \
  "$(http_status "invalid token GET /api/symptoms" GET "$SMART_BASE_URL/api/symptoms" invalid-token)" 401

FRAUD_RULE_BODY='{"ruleName":"Zadatak 8.1 curl security rule","ruleCode":"ZADATAK_8_1_CURL_SECURITY","category":"ORDER","description":"Created by curl security script","weight":10.0,"isActive":true}'

expect_status "ROLE_USER POST /api/fraud-rules" \
  "$(http_status "ROLE_USER POST /api/fraud-rules" POST "$SMART_BASE_URL/api/fraud-rules" "$USER_TOKEN" "$FRAUD_RULE_BODY")" 403

expect_status "ROLE_ADMIN POST /api/fraud-rules" \
  "$(http_status "ROLE_ADMIN POST /api/fraud-rules" POST "$SMART_BASE_URL/api/fraud-rules" "$ADMIN_TOKEN" "$FRAUD_RULE_BODY")" 201 409

expect_not_auth_failure "ROLE_PHARMACIST POST /api/fraud-checks" \
  "$(http_status "ROLE_PHARMACIST POST /api/fraud-checks" POST "$SMART_BASE_URL/api/fraud-checks" "$PHARMACIST_TOKEN" '{"orderId":100}')"

expect_status "public GET /actuator/health" \
  "$(http_status "public GET /actuator/health" GET "$SMART_BASE_URL/actuator/health")" 200

printf '\nSecurity curl tests finished: %s passed, %s failed\n' "$PASS_COUNT" "$FAIL_COUNT" >&2

if [[ "$FAIL_COUNT" -eq 0 ]]; then
  exit 0
fi

exit 1