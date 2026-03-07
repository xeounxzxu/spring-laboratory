# Coroutine Demo App

A Spring MVC sample that highlights suspend-friendly controllers and outbound HTTP calls via `WebClient`. The module ships with a mock external echo endpoint so you can test the asynchronous flow without additional services.

## Build & Run

```bash
./gradlew :mvc:coroutine-app:bootRun
```

The default port is **8082** (see `src/main/resources/application.properties`). Override the port when running multiple copies:

```bash
./gradlew :mvc:coroutine-app:bootRun --args='--server.port=7100'
```

Or via the packaged JAR / Docker image using `-Dserver.port=7100` or `SERVER_PORT=7100`.

## Configuration

Key properties (all prefixed with `app.external.`) live in `application.properties` and can be overridden per environment:

- `base-url` – target base URL for the outbound echo call (defaults to the built-in mock controller).
- `connect-timeout-ms` – Netty connect timeout.
- `read-timeout-ms` – response timeout.

Example override:

```bash
./gradlew :mvc:coroutine-app:bootRun --args='--app.external.base-url=https://api.example.com/echo --app.external.connect-timeout-ms=1000'
```

## HTTP Endpoints

- `GET /coroutines/echo?text=hello` – coroutine echo service.
- `GET /coroutines/parallel?workers=5` – spawns parallel coroutine workers and returns timing data.
- `GET /coroutines/external-echo?text=ping` – calls the configured external HTTP interface asynchronously.
- `GET /mock/echo?text=test` – internal mock endpoint used as the default external target.
- `GET /perf/blocking?sleepMs=50` – blocking controller (`Thread.sleep`) for baseline.
- `GET /perf/suspend?sleepMs=50` – suspend controller (`delay`) for coroutine path.
- `GET /perf-io/blocking?sleepMs=50` – blocking controller with real outbound HTTP (`/mock/echo`) using `RestClient`.
- `GET /perf-io/suspend?sleepMs=50` – suspend controller with real outbound HTTP (`/mock/echo`) using `WebClient`.

Use `curl` to try the flow:

```bash
curl 'http://localhost:7000/coroutines/external-echo?text=demo'
```

## Performance Comparison (wrk/k6)

The repository includes matrix runners to compare both in-process delay and real HTTP I/O paths.

### 1) Start app

```bash
./gradlew :mvc:coroutine-app:bootRun
```

### 2) Run k6 matrix

```bash
chmod +x scripts/run-k6-perf-matrix.sh
BASE_URL=http://localhost:8082 \
DURATION=30s RATE_MATRIX="200 400" \
ENDPOINT_MATRIX="blocking suspend io-blocking io-suspend" \
SLEEP_MATRIX="10 50 100 300" \
./scripts/run-k6-perf-matrix.sh
```

Outputs are saved in `load/k6/results/*.txt`.

### 3) Run wrk matrix

```bash
chmod +x scripts/run-wrk-perf-matrix.sh
BASE_URL=http://localhost:8082 \
THREADS=8 CONNECTION_MATRIX="200 400" DURATION=30s \
ENDPOINT_MATRIX="blocking suspend io-blocking io-suspend" \
SLEEP_MATRIX="10 50 100 300" \
./scripts/run-wrk-perf-matrix.sh
```

Outputs are saved in `load/wrk/results/*.txt`.

### 4) Generate markdown summary (p95/p99/RPS)

```bash
./scripts/generate-perf-report.sh
```

Report output: `load/reports/perf-summary.md`

### 5) One-shot full experiment (recommended)

This script automates:
- rate step-up (`k6` `RATE_MATRIX`)
- sleep matrix expansion (`SLEEP_MATRIX`)
- Tomcat thread cap matrix (`TOMCAT_MAX_MATRIX`)
- real external HTTP I/O comparison (`io-blocking`, `io-suspend`)

```bash
chmod +x scripts/run-perf-full-matrix.sh
TOMCAT_MAX_MATRIX="50 200" \
K6_RATE_MATRIX="120 300 600" \
WRK_CONNECTION_MATRIX="120 300" \
SLEEP_MATRIX="10 100 300" \
./scripts/run-perf-full-matrix.sh
```

### Tunable environment variables

- `BASE_URL` (default `http://localhost:8082`)
- `SLEEP_MATRIX` (default `"10 50 100 300"`)
- `DURATION` (default `30s`)
- `RATE`, `RATE_MATRIX`, `PRE_ALLOCATED_VUS`, `MAX_VUS` for k6
- `THREADS`, `CONNECTIONS`, `CONNECTION_MATRIX`, `TIMEOUT` for wrk
- `ENDPOINT_MATRIX` (e.g. `"blocking suspend io-blocking io-suspend"`)
- `TOMCAT_MAX_MATRIX`, `K6_RATE_MATRIX`, `WRK_CONNECTION_MATRIX` for `run-perf-full-matrix.sh`
