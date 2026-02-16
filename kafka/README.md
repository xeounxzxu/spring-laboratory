# Kafka Pub/Sub 테스트 가이드

`kafka/pub-app` 과 `kafka/sub-app` 을 사용해 Kafka Request/Reply(pub/sub 기반) 흐름을 검증하는 문서입니다.

## 1. 사전 준비

- Java 21
- Docker, Docker Compose
- 프로젝트 루트에서 명령 실행

선택: 먼저 빌드 확인

```bash
./gradlew :kafka:pub-app:build :kafka:sub-app:build
```

## 2. Kafka 실행

```bash
docker compose -f kafka/docker-compose.yml up -d
```

확인:

```bash
docker compose -f kafka/docker-compose.yml ps
```

## 3. 앱 실행 (각각 별도 터미널)

터미널 A (`sub-app`, 수신/처리):

```bash
./gradlew :kafka:sub-app:bootRun
```

터미널 B (`pub-app`, 송신/API):

```bash
./gradlew :kafka:pub-app:bootRun
```

기본 포트:

- `pub-app`: `8081`
- `sub-app`: `8082`

## 4. 정상 동작 테스트

요청:

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"req-1","message":"hello from pub"}' \
  http://localhost:8081/kafka/publish
```

기대 결과:

- HTTP `200 OK`
- 응답 본문: `processed: hello from pub`

로그 확인:

- `sub-app` 로그에 `received: hello from pub (request_id=req-1)`

## 5. 예외 케이스 테스트

### 5.1 requestId 누락/공백

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"","message":"hello"}' \
  http://localhost:8081/kafka/publish
```

기대 결과:

- HTTP `400 Bad Request`
- 본문: `request_id is required`

### 5.2 중복 requestId

첫 요청을 보낸 뒤, 매우 짧은 시간 안에 같은 `requestId`로 다시 요청:

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"dup-1","message":"first"}' \
  http://localhost:8081/kafka/publish
```

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"dup-1","message":"second"}' \
  http://localhost:8081/kafka/publish
```

가능한 결과:

- 첫 번째 요청 처리 중이면 두 번째는 HTTP `409 Conflict` + `duplicate request_id`
- 첫 번째 요청이 이미 완료됐으면 두 번째도 정상 처리될 수 있음

### 5.3 타임아웃 (sub-app 중지)

`sub-app` 을 중지한 뒤 요청:

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"timeout-1","message":"no subscriber"}' \
  http://localhost:8081/kafka/publish
```

기대 결과:

- 약 5초 후 HTTP `504 Gateway Timeout`
- 본문: `timeout`

참고: 타임아웃은 `app.kafka.reply-timeout-ms` 로 조정 가능 (`kafka/pub-app/src/main/resources/application.properties`).

## 6. 종료

앱 실행 터미널에서 `Ctrl + C`로 종료 후 Kafka 중지:

```bash
docker compose -f kafka/docker-compose.yml down
```

## 7. 트러블슈팅

- `500 Internal Server Error` 발생 시:
  - 최신 코드 기준으로 `pub-app` 의존성(`kotlinx-coroutines-reactor`)이 포함되어야 함
  - `./gradlew :kafka:pub-app:clean :kafka:pub-app:build` 후 재실행
- Kafka 연결 실패 시:
  - `docker compose -f kafka/docker-compose.yml ps` 로 컨테이너 상태 확인
  - `localhost:9092` 포트 충돌 여부 확인
- 메시지 수신이 안 될 때:
  - `sub-app` 로그에서 `partitions assigned` 확인
  - 두 앱의 `app.kafka.request-topic`, `app.kafka.reply-topic` 값 일치 여부 확인
