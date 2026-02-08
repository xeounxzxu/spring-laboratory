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

## Publish a message
```bash
curl -X POST \
  -H 'Content-Type: text/plain' \
  --data 'hello from pub' \
  http://localhost:8081/kafka/publish
```

## Verify
- Check the `sub-app` logs for `received: hello from pub`.

## Stop Kafka
```bash
docker compose -f kafka/docker-compose.yml down
```
