#!/usr/bin/env bash
set -euo pipefail

SMART_BASE_URL="${SMART_BASE_URL:-http://localhost:8082}"
EUREKA_SERVER_URL="${EUREKA_SERVER_URL:-http://localhost:8761/eureka}"
SERVICE_KEY="${SERVICE_KEY:-product}"
REQUEST_COUNT="${REQUEST_COUNT:-100}"
REPORT_FILE="${REPORT_FILE:-build/reports/eureka-real-loadbalancing-100-report.md}"

case "$SERVICE_KEY" in
  product)
    DEFAULT_SERVICE_ID="product-health-service"
    ;;
  order)
    DEFAULT_SERVICE_ID="order-prescription-service"
    ;;
  user)
    DEFAULT_SERVICE_ID="user-health-service"
    ;;
  *)
    DEFAULT_SERVICE_ID="$SERVICE_KEY"
    ;;
esac

SERVICE_ID="${SERVICE_ID:-$DEFAULT_SERVICE_ID}"

export SMART_BASE_URL EUREKA_SERVER_URL SERVICE_KEY SERVICE_ID REQUEST_COUNT REPORT_FILE

python3 <<'PY'
import json
import os
import socket
import time
import urllib.error
import urllib.request
from collections import Counter
from pathlib import Path

smart_base_url = os.environ["SMART_BASE_URL"].rstrip("/")
eureka_server_url = os.environ["EUREKA_SERVER_URL"].rstrip("/")
service_key = os.environ["SERVICE_KEY"]
service_id = os.environ["SERVICE_ID"]
request_count = int(os.environ["REQUEST_COUNT"])
report_file = Path(os.environ["REPORT_FILE"])


def http_json(url, timeout=10):
    request = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def eureka_instances():
    data = http_json(f"{eureka_server_url}/apps/{service_id.upper()}")
    application = data.get("application") or {}
    instances = application.get("instance") or []
    if isinstance(instances, dict):
        instances = [instances]
    normalized = []
    for instance in instances:
        status = instance.get("status")
        home_url = instance.get("homePageUrl")
        health_url = instance.get("healthCheckUrl")
        instance_id = instance.get("instanceId") or instance.get("instanceID")
        normalized.append(
            {
                "instance_id": instance_id or home_url or health_url or "UNKNOWN",
                "status": status or "UNKNOWN",
                "home_url": home_url or "UNKNOWN",
                "health_url": health_url or "UNKNOWN",
            }
        )
    return normalized


def call_probe(mode):
    url = f"{smart_base_url}/api/discovery/{service_key}/health?mode={mode}"
    started = time.perf_counter()
    try:
        data = http_json(url)
        elapsed_ms = round((time.perf_counter() - started) * 1000)
        instance = data.get("instanceId") or data.get("instanceUri") or "UNKNOWN"
        return {
            "ok": True,
            "instance": instance,
            "service_id": data.get("serviceId", "UNKNOWN"),
            "http_status": data.get("httpStatus"),
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


try:
    instances = eureka_instances()
except urllib.error.HTTPError as exc:
    if exc.code == 404:
        raise SystemExit(
            f"Eureka is reachable at {eureka_server_url}, but service `{service_id}` is not registered.\n"
            f"Start at least one `{service_id}` instance with Eureka client enabled, then rerun this script."
        )
    raise SystemExit(f"Unable to read Eureka instances for {service_id}: HTTP {exc.code} {exc.reason}")
except urllib.error.URLError as exc:
    reason = exc.reason
    if isinstance(reason, ConnectionRefusedError) or getattr(reason, "errno", None) in {61, 111}:
        raise SystemExit(
            f"Eureka is not reachable at {eureka_server_url}.\n"
            "Start the Eureka server first, or pass the correct URL, for example:\n"
            "  EUREKA_SERVER_URL=http://localhost:8761/eureka ./scripts/eureka-real-loadbalancing-100.sh"
        )
    if isinstance(reason, socket.gaierror):
        raise SystemExit(f"Eureka host in `{eureka_server_url}` cannot be resolved: {reason}")
    raise SystemExit(f"Unable to read Eureka instances for {service_id}: {exc}")
except (TimeoutError, json.JSONDecodeError) as exc:
    raise SystemExit(f"Unable to read Eureka instances for {service_id}: {exc}")

up_instances = [instance for instance in instances if instance["status"] == "UP"]
if not up_instances:
    raise SystemExit(f"No UP Eureka instances found for {service_id}.")

direct = run_mode("direct")
load_balanced = run_mode("load-balanced")

report_file.parent.mkdir(parents=True, exist_ok=True)
lines = [
    "# Real Eureka load-balancing benchmark",
    "",
    f"- Eureka server URL: `{eureka_server_url}`",
    f"- Smart service base URL: `{smart_base_url}`",
    f"- Target service key: `{service_key}`",
    f"- Target Eureka service id: `{service_id}`",
    f"- Requests per mode: `{request_count}`",
    f"- Generated at: `{time.strftime('%Y-%m-%d %H:%M:%S %Z')}`",
    "",
    "## Eureka instances",
    "",
]

for instance in up_instances:
    lines.append(
        f"- `{instance['instance_id']}` status `{instance['status']}`, health `{instance['health_url']}`"
    )

lines.extend(
    [
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
)

for instance, count in sorted(direct["counts"].items()):
    lines.append(f"- `{instance}`: {count}")

lines.extend(["", "### Load-balanced", ""])
for instance, count in sorted(load_balanced["counts"].items()):
    lines.append(f"- `{instance}`: {count}")

if len(up_instances) < 2:
    lines.extend(
        [
            "",
            "## Note",
            "",
            "Only one UP Eureka instance was found. The script still runs, but load balancing cannot be demonstrated until at least two instances are registered under the same service id.",
        ]
    )

errors = [response.get("error") for response in direct["responses"] + load_balanced["responses"] if not response["ok"]]
if errors:
    lines.extend(["", "## Errors", ""])
    for error in errors[:10]:
        lines.append(f"- `{error}`")

report_file.write_text("\n".join(lines) + "\n", encoding="utf-8")

print("\n".join(lines))
print(f"\nReport written to {report_file}")
PY
