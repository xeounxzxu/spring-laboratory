# Kafka Request/Reply 테스트 가이드

`kafka/pub-app`(요청 송신 + 응답 대기)과 `kafka/sub-app`(요청 처리 + 응답 송신)을 Kafka pub/sub 위에서 Request/Reply 패턴으로 검증하는 문서입니다.

## 1. 아키텍처 요약

- `pub-app`
  - `ReplyingKafkaTemplate`으로 요청 전송 후 응답을 동기 대기(`suspend`)
  - `app.kafka.reply-group-id=pub-app-${random.uuid}`로 인스턴스별 고유 reply consumer group 사용
- `sub-app`
  - 요청 수신 후 처리
  - 요청 헤더의 `correlation-id`, `reply-topic`을 응답에 복사해 송신

핵심 효과:

- 멀티 `pub-app` 인스턴스 환경에서도 요청-응답 매칭 정확도 개선

## 2. 사전 준비

- Java 21
- Docker, Docker Compose
- 프로젝트 루트에서 명령 실행

선택: 먼저 빌드 확인

```bash
./gradlew :kafka:pub-app:build :kafka:sub-app:build
```

## 3. Kafka 실행

```bash
docker compose -f kafka/docker-compose.yml up -d
```

확인:

```bash
docker compose -f kafka/docker-compose.yml ps
```

## 4. 앱 실행 (기본 단일 인스턴스)

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

## 5. 정상 동작 테스트

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"req-1","message":"hello from pub"}' \
  http://localhost:8081/kafka/publish
```

기대 결과:

- HTTP `200 OK`
- 응답 본문: `processed: hello from pub`
- `sub-app` 로그: `received: hello from pub (request_id=req-1)`

## 6. 멀티 pub-app 인스턴스 테스트

터미널 C (`pub-app` 2번 인스턴스):

```bash
./gradlew :kafka:pub-app:bootRun --args='--server.port=8083'
```

요청 1 (`8081`):

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"multi-1","message":"from-8081"}' \
  http://localhost:8081/kafka/publish
```

요청 2 (`8083`):

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"multi-2","message":"from-8083"}' \
  http://localhost:8083/kafka/publish
```

기대 결과:

- 두 요청 모두 HTTP `200`
- 각각 자기 응답 본문을 정상 수신

## 7. 예외 케이스 테스트

### 7.1 requestId 누락/공백

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"","message":"hello"}' \
  http://localhost:8081/kafka/publish
```

기대 결과:

- HTTP `400 Bad Request`
- 본문: `request_id is required`

### 7.2 타임아웃 (`sub-app` 중지)

`sub-app` 중지 후 요청:

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"timeout-1","message":"no subscriber"}' \
  http://localhost:8081/kafka/publish
```

기대 결과:

- 약 5초 후 HTTP `504 Gateway Timeout`
- 본문: `timeout`

타임아웃 설정:

- `kafka/pub-app/src/main/resources/application.properties`의 `app.kafka.reply-timeout-ms`

## 8. 종료

앱 실행 터미널에서 `Ctrl + C`로 종료 후 Kafka 중지:

```bash
docker compose -f kafka/docker-compose.yml down
```

## 9. 트러블슈팅

- Kafka 연결 실패:
  - `docker compose -f kafka/docker-compose.yml ps`로 컨테이너 상태 확인
  - `localhost:9092` 포트 충돌 확인
- 응답 타임아웃 지속 발생:
  - `sub-app` 실행 여부 확인
  - `app.kafka.request-topic`, `app.kafka.reply-topic` 값 일치 확인
- 다중 인스턴스에서 한쪽만 실패:
  - 각 `pub-app` 인스턴스 로그에서 reply consumer 시작 여부 확인
  - 서로 다른 포트로 실행했는지 확인
