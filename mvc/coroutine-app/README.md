# Coroutine Demo App

A Spring MVC sample that highlights suspend-friendly controllers and outbound HTTP calls via `WebClient`. The module ships with a mock external echo endpoint so you can test the asynchronous flow without additional services.

## Build & Run

```bash
./gradlew :mvc:coroutine-app:bootRun
```

The default port is **7000** (see `src/main/resources/application.properties`). Override the port when running multiple copies:

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

Use `curl` to try the flow:

```bash
curl 'http://localhost:7000/coroutines/external-echo?text=demo'
```
