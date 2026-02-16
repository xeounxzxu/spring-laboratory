# Kafka Request/Reply (API Relay) 가이드

현재 구조는 아래처럼 동작합니다.

- `pub-app`: HTTP 요청을 받아 Kafka `demo-request`로 publish 후 응답 대기
- 외부 브릿지 프로그램: `demo-request`를 consume하고 `sub-app` API 호출
- `sub-app`: API로 받은 메시지를 처리하고 Kafka `demo-reply`로 publish
- `pub-app`: `correlation-id` 매칭 후 HTTP 응답 반환

토픽은 총 2개를 사용합니다.

- `demo-request`
- `demo-reply`

## 1. 사전 준비

- Java 21
- Docker, Docker Compose
- 프로젝트 루트에서 명령 실행

```bash
./gradlew :kafka:pub-app:build :kafka:sub-app:build
```

## 2. Kafka 실행

```bash
docker compose -f kafka/docker-compose.yml up -d
docker compose -f kafka/docker-compose.yml ps
```

## 3. 앱 실행

터미널 A (`sub-app`):

```bash
./gradlew :kafka:sub-app:bootRun
```

터미널 B (`pub-app`):

```bash
./gradlew :kafka:pub-app:bootRun
```

기본 포트:

- `pub-app`: `8081`
- `sub-app`: `8082`

## 4. API 명세

### 4.1 pub-app 요청 API

`POST /kafka/publish`

요청 예시:

```json
{
  "requestId": "req-1",
  "message": "hello"
}
```

성공 응답:

- `200 OK`
- body: `processed: hello`

실패 응답:

- `400 request_id is required`
- `504 timeout`

### 4.2 sub-app 처리 API

`POST /sub/process`

요청 예시:

```json
{
  "message": "hello",
  "correlationIdBase64": "<base64-encoded-correlation-id>",
  "requestId": "req-1",
  "replyTopic": "demo-reply"
}
```

설명:

- `correlationIdBase64`는 브릿지가 `demo-request` 메시지의 `correlation-id` 헤더를 Base64로 인코딩해서 전달해야 합니다.
- `replyTopic` 생략 시 기본값은 `demo-reply`입니다.

성공 응답:

- `200 accepted`

실패 응답:

- `400 message is required`
- `400 correlationIdBase64 is required`
- `400 invalid correlationIdBase64`

## 5. 전체 흐름 테스트

현재 구조에서 정상 E2E를 위해서는 **외부 브릿지 프로그램**이 반드시 필요합니다.

1. 클라이언트가 `pub-app`의 `/kafka/publish` 호출
2. `pub-app`이 `demo-request`에 publish 후 대기
3. 브릿지가 `demo-request`를 consume
4. 브릿지가 메시지와 헤더(`correlation-id`, `reply-topic`, `request-id`)를 `sub-app` `/sub/process`로 전달
5. `sub-app`이 `demo-reply`로 응답 publish
6. `pub-app`이 응답을 받아 HTTP 응답 반환

## 6. 빠른 확인 시나리오

### 6.1 타임아웃 확인 (브릿지 미구동)

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"timeout-1","message":"no-bridge"}' \
  http://localhost:8081/kafka/publish
```

기대:

- 약 5초 후 `504 timeout`

### 6.2 sub-app API 단독 확인

유효하지 않은 `correlationIdBase64`로 검증:

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"message":"hello","correlationIdBase64":"not-base64"}' \
  http://localhost:8082/sub/process
```

기대:

- `400 invalid correlationIdBase64`

## 7. 멀티 인스턴스 참고

- `pub-app`은 `app.kafka.reply-group-id=pub-app-${random.uuid}`를 사용하므로 인스턴스별 고유 reply group이 생성됩니다.
- 멀티 인스턴스에서도 `correlation-id` 기준으로 요청-응답 매칭합니다.

## 8. 종료

앱 종료 후 Kafka 중지:

```bash
docker compose -f kafka/docker-compose.yml down
```

## 9. 트러블슈팅

- `pub-app`이 계속 `504`:
  - 브릿지 프로그램이 `demo-request`를 consume하는지 확인
  - 브릿지가 `correlation-id`를 Base64로 변환해 `sub-app`에 전달하는지 확인
- `sub-app`에서 `invalid correlationIdBase64`:
  - 브릿지 인코딩 로직 확인
- Kafka 연결 실패:
  - `docker compose -f kafka/docker-compose.yml ps`
  - `localhost:9092` 포트 점유 확인
