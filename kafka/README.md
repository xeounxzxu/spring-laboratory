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
   - `requestId`와 `type/version/payload`(또는 `message`) 전달
5. `sub-app` -> `demo-reply` 토픽 publish
6. `pub-app`이 `demo-reply`를 consume하고 해당 `requestId` 응답 반환
   - 정상: `200` + JSON Envelope

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
  --data '{"requestId":"req-1","type":"demo.text","version":1,"payload":{"message":"hello"}}' \
  http://localhost:8081/kafka/publish
```

### Step 2. sub-app 호출 (개발자가 직접)

다른 터미널에서 같은 `requestId`로 호출:

```bash
curl -i -X POST \
  -H 'Content-Type: application/json' \
  --data '{"requestId":"req-1","type":"demo.text.response","version":1,"payload":{"message":"hello","status":"ok"}}' \
  http://localhost:8082/sub/process
```

기대:

- `sub-app` 응답: `200 accepted`
- Step 1의 `pub-app` 요청이 완료되며 `200` + Envelope JSON 응답

예시 응답:

```json
{
  "requestId": "req-1",
  "type": "demo.text.response",
  "version": 1,
  "payload": {
    "processed": true,
    "echo": {
      "message": "hello",
      "status": "ok"
    }
  }
}
```

## API 명세

### pub-app

- `POST /kafka/publish`
- request:

```json
{
  "requestId": "req-1",
  "type": "demo.text",
  "version": 1,
  "payload": {
    "message": "hello"
  }
}
```

`payload` 대신 기존 `message` 필드도 호환 입력 가능합니다.

- response:
  - `200` + JSON Envelope
  - `400 request_id is required`
  - `400 payload or message is required`
  - `409 duplicate request_id`
  - `504 timeout`

### sub-app

- `POST /sub/process`
- request:

```json
{
  "requestId": "req-1",
  "type": "demo.text.response",
  "version": 1,
  "payload": {
    "message": "hello",
    "status": "ok"
  },
  "replyTopic": "demo-reply"
}
```

`payload` 대신 기존 `message` 필드도 호환 입력 가능합니다.

- response:
  - `200 accepted`
  - `400 requestId is required`
  - `400 payload or message is required`

## 종료

```bash
docker compose -f kafka/docker-compose.yml down
```

## 운영 시 고려사항 (권장)

### 1) 멱등성/중복 처리

- `requestId`는 전역 유니크(UUID) 사용 권장
- `sub-app` API가 중복 호출될 수 있으므로, 운영에서는 `requestId` 기반 멱등 저장소(예: Redis/DB) 도입 권장

### 2) 타임아웃/재시도 정책

- 현재 `pub-app`은 `app.kafka.reply-timeout-ms=60000`
- 운영에서는 API SLA에 맞춰 timeout을 줄이거나, 요청 타입별 timeout 정책 분리 권장
- `sub-app` 수동/외부 호출 실패 시 재시도 횟수와 백오프 정책 정의 필요

### 3) 장애 복구

- `ReplyStore`는 인메모리이므로 `pub-app` 인스턴스 재시작 시 대기 요청은 유실됨
- 운영에서는 상태 저장소 외부화(예: Redis) 검토 권장

### 4) 관측성(Observability)

- 필수 로그 키: `requestId`, `type`, `version`, `topic`, `status`, `latency`
- 메트릭 권장:
  - 요청 수 / 성공 수 / 타임아웃 수 / 중복 수
  - 평균/최대 응답 시간
  - 토픽 lag
- 분산 추적(traceId) 헤더 도입 권장

### 5) 토픽/파티션 운영

- 현재는 `demo-request`, `demo-reply` 2개 토픽
- 처리량 증가 시 파티션 수 + 컨슈머 병렬도를 함께 조정해야 효과가 큼
- 순서 보장은 파티션 단위임을 전제로 설계 필요

### 6) 데이터 스키마 관리

- Envelope(`type`, `version`, `payload`)를 표준 계약으로 유지
- breaking change는 `version` 증가로 처리
- 호환성 테스트(구버전/신버전 혼용) 자동화 권장

## 송수신 이력 추가 아이디어

요구사항: 요청/응답의 전체 이력을 추적 가능해야 함.

### 1) 저장 모델 제안

- 테이블명 예: `message_history`
- 컬럼 예:
  - `id` (PK)
  - `request_id` (인덱스, 필수)
  - `event_type` (`PUB_RECEIVED`, `PUB_PUBLISHED`, `SUB_RECEIVED`, `SUB_PUBLISHED`, `PUB_REPLIED`, `PUB_TIMEOUT`, `FAILED`)
  - `message_type`, `message_version`
  - `topic`, `partition`, `offset` (가능 시)
  - `payload_json` (원본 또는 마스킹본)
  - `created_at`
  - `source_app` (`pub-app`/`sub-app`)

### 2) 기록 시점 제안

- `pub-app`
  - `/kafka/publish` 수신 직후
  - `demo-request` publish 직후
  - 최종 reply 반환 직후
  - timeout/예외 발생 시
- `sub-app`
  - `/sub/process` 수신 직후
  - `demo-reply` publish 직후

### 3) 운영 시 고려

- `requestId` 기반 빠른 조회를 위해 인덱스 필수
- 개인정보/민감정보는 payload 마스킹 후 저장
- 보관기간(TTL)과 파기 정책 정의
- 기록 실패가 본 처리에 영향 주지 않도록 비동기 기록 또는 재시도 큐 고려
- 대시보드 지표:
  - 요청 수 / 성공 수 / 타임아웃 수 / 실패 수
  - 평균 응답시간, p95/p99
