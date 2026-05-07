#!/usr/bin/env bash
set -euo pipefail

SMART_BASE_URL="${SMART_BASE_URL:-http://localhost:8082}"
SERVICE_KEY="${SERVICE_KEY:-product}"
REQUEST_COUNT="${REQUEST_COUNT:-100}"
REPORT_FILE="${REPORT_FILE:-build/reports/eureka-loadbalancing-report.md}"

export SMART_BASE_URL SERVICE_KEY REQUEST_COUNT REPORT_FILE

python3 <<'PY'
import json
import os
import time
import urllib.error
import urllib.request
from collections import Counter
from pathlib import Path

smart_base_url = os.environ["SMART_BASE_URL"].rstrip("/")
service_key = os.environ["SERVICE_KEY"]
request_count = int(os.environ["REQUEST_COUNT"])
report_file = Path(os.environ["REPORT_FILE"])


def call_probe(mode):
    url = f"{smart_base_url}/api/discovery/{service_key}/health?mode={mode}"
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(url, timeout=10) as response:
            body = response.read().decode("utf-8")
            elapsed_ms = round((time.perf_counter() - started) * 1000)
            data = json.loads(body)
            instance = data.get("instanceId") or data.get("instanceUri") or "UNKNOWN"
            return {
                "ok": True,
                "instance": instance,
                "service_id": data.get("serviceId", "UNKNOWN"),
                "http_status": data.get("httpStatus", response.status),
                "probe_duration_ms": data.get("durationMillis", elapsed_ms),
                "wall_duration_ms": elapsed_ms,
            }
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, json.JSONDecodeError) as exc:
        elapsed_ms = round((time.perf_counter() - started) * 1000)
        return {
            "ok": False,
            "instance": "ERROR",
            "service_id": "UNKNOWN",
            "http_status": "ERROR",
            "probe_duration_ms": elapsed_ms,
            "wall_duration_ms": elapsed_ms,
            "error": str(exc),
        }


def run_mode(mode):
    started = time.perf_counter()
    responses = [call_probe(mode) for _ in range(request_count)]
    total_ms = round((time.perf_counter() - started) * 1000)
    counts = Counter(response["instance"] for response in responses)
    success_count = sum(1 for response in responses if response["ok"])
    return {
        "mode": mode,
        "responses": responses,
        "counts": counts,
        "success_count": success_count,
        "total_ms": total_ms,
        "avg_wall_ms": round(sum(response["wall_duration_ms"] for response in responses) / request_count, 2),
        "avg_probe_ms": round(sum(response["probe_duration_ms"] for response in responses) / request_count, 2),
    }


direct = run_mode("direct")
load_balanced = run_mode("load-balanced")
service_id = next(
    (
        response["service_id"]
        for response in load_balanced["responses"] + direct["responses"]
        if response["service_id"] != "UNKNOWN"
    ),
    "UNKNOWN",
)

report_file.parent.mkdir(parents=True, exist_ok=True)
lines = [
    "# Eureka load-balancing benchmark",
    "",
    f"- Smart service base URL: `{smart_base_url}`",
    f"- Target service key: `{service_key}`",
    f"- Resolved service id: `{service_id}`",
    f"- Requests per mode: `{request_count}`",
    f"- Generated at: `{time.strftime('%Y-%m-%d %H:%M:%S %Z')}`",
    "",
    "## Summary",
    "",
    "| Mode | Successful requests | Total wall time (ms) | Avg wall time (ms) | Avg downstream health time (ms) |",
    "| --- | ---: | ---: | ---: | ---: |",
    f"| Direct | {direct['success_count']}/{request_count} | {direct['total_ms']} | {direct['avg_wall_ms']} | {direct['avg_probe_ms']} |",
    f"| Load-balanced | {load_balanced['success_count']}/{request_count} | {load_balanced['total_ms']} | {load_balanced['avg_wall_ms']} | {load_balanced['avg_probe_ms']} |",
    "",
    "## Requests per instance",
    "",
    "### Direct",
    "",
]

for instance, count in sorted(direct["counts"].items()):
    lines.append(f"- `{instance}`: {count}")

lines.extend(["", "### Load-balanced", ""])
for instance, count in sorted(load_balanced["counts"].items()):
    lines.append(f"- `{instance}`: {count}")

errors = [response.get("error") for response in direct["responses"] + load_balanced["responses"] if not response["ok"]]
if errors:
    lines.extend(["", "## Errors", ""])
    for error in errors[:10]:
        lines.append(f"- `{error}`")

report_file.write_text("\n".join(lines) + "\n", encoding="utf-8")

print("\n".join(lines))
print(f"\nReport written to {report_file}")
PY
