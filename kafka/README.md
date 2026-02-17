# Kafka Request/Reply Toy Flow

이 구조는 브릿지 애플리케이션 없이, 개발자가 `sub-app` API를 직접 호출하는 흐름입니다.

## 아키텍처

1. 클라이언트 -> `pub-app` 호출
   - `POST /kafka/publish`
2. `pub-app` -> `demo-request` 토픽 publish
3. `pub-app`은 코루틴 Job으로 응답 대기
   - 내부적으로 poll interval(`app.kafka.poll-interval-ms`)로 폴링
   - 60초(`app.kafka.reply-timeout-ms=60000`) 초과 시 `504 timeout`
4. 개발자 -> `sub-app` 호출
   - `POST /sub/process`
   - `requestId`와 `message` 전달
5. `sub-app` -> `demo-reply` 토픽 publish
6. `pub-app`이 `demo-reply`를 consume하고 해당 `requestId` 응답 반환
   - 정상: `200`

토픽:

- `demo-request`
- `demo-reply`

## 실행

### 1) Kafka 기동

```bash
docker compose -f kafka/docker-compose.yml up -d
```

### 2) 앱 실행

터미널 A:

```bash
./gradlew :kafka:sub-app:bootRun
```

터미널 B:

```bash
./gradlew :kafka:pub-app:bootRun
```

## 정상 테스트 (핵심)

### Step 1. pub-app 요청 (대기 상태 진입)

아래 요청은 응답이 바로 오지 않고 대기합니다.

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"req-1","message":"hello"}' \
  http://localhost:8081/kafka/publish
```

### Step 2. sub-app 호출 (개발자가 직접)

다른 터미널에서 같은 `requestId`로 호출:

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"req-1","message":"hello"}' \
  http://localhost:8082/sub/process
```

기대:

- `sub-app` 응답: `200 accepted`
- Step 1의 `pub-app` 요청이 완료되며 `200 processed: hello`

## API 명세

### pub-app

- `POST /kafka/publish`
- request:

```json
{
  "requestId": "req-1",
  "message": "hello"
}
```

- response:
  - `200 processed: hello`
  - `400 request_id is required`
  - `409 duplicate request_id`
  - `504 timeout`

### sub-app

- `POST /sub/process`
- request:

```json
{
  "requestId": "req-1",
  "message": "hello",
  "replyTopic": "demo-reply"
}
```

- response:
  - `200 accepted`
  - `400 requestId is required`
  - `400 message is required`

## 종료

```bash
docker compose -f kafka/docker-compose.yml down
```
