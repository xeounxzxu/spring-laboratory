# Kafka Pub/Sub Test

## Prerequisites
- Java 21
- Docker (for Kafka)

## Start Kafka
```bash
docker compose -f kafka/docker-compose.yml up -d
```

## Run subscriber
```bash
./gradlew :kafka:sub-app:bootRun
```

## Run publisher
```bash
./gradlew :kafka:pub-app:bootRun
```

## Publish a message and wait for reply
```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"req-1","message":"hello from pub"}' \
  http://localhost:8081/kafka/publish
```

## Verify
- The HTTP response should be `processed: hello from pub`.
- Check the `sub-app` logs for `received: hello from pub` and `pub-app` logs for reply acknowledgements.

## Notes
- `pub-app` attaches a coroutine job per `requestId`, sends the message to `demo-request`, and waits for `sub-app` to publish the reply on `demo-reply`.
- Default timeout is 5 seconds (`app.kafka.reply-timeout-ms`).

## Stop Kafka
```bash
docker compose -f kafka/docker-compose.yml down
```
