# Vegeta 부하 테스트 가이드

Vegeta를 사용해 로컬 Kubernetes에서 동작하는 `spring-app` Pod에 부하를 전송하는 방법을 정리했습니다. `load/vegeta/targets.txt`는 `curl`과 유사한 형식으로 HTTP 요청을 정의하며, `scripts/run-vegeta.sh` 스크립트가 해당 파일을 읽어 `vegeta attack`을 실행합니다.

## 사전 준비
1. `./scripts/start-local-k8s.sh` 혹은 수동 포트포워딩(`kubectl port-forward service/spring-app 9000:9000 ...`)으로 애플리케이션을 `localhost:9000`에서 접근 가능하도록 만듭니다.
2. Vegeta CLI 설치
   - macOS: `brew install vegeta`
   - 기타 OS: [Tsenart/Vegeta Releases](https://github.com/tsenart/vegeta/releases)에서 바이너리를 다운로드해 PATH에 추가

## 타겟 파일 편집
- 기본 파일: `load/vegeta/targets.txt`
- 예시:
  ```
  GET http://localhost:9000/hello
  GET http://localhost:9000/latency/probe?iterations=60000&rounds=5&loadClasses=
  ```
- `curl` 명령과 유사하게 헤더, 본문 등을 추가하고 싶다면 Vegeta 문법에 맞춰 `@` 파일 포함 등을 사용할 수 있습니다.

## 실행
```bash
./scripts/run-vegeta.sh
```
- 기본 값: `RATE=20`, `DURATION=30s`
- 환경 변수로 조정:
  ```bash
  RATE=100 DURATION=60s ./scripts/run-vegeta.sh
  TARGETS_FILE=load/vegeta/custom.txt ./scripts/run-vegeta.sh
  ```
- 실행 후 산출물:
  - `load/vegeta/results.bin`: raw 결과 (추가 분석 용)
  - `load/vegeta/report.txt`: 텍스트 요약 (RPS, 지연 분포 등)
  - `load/vegeta/report.html`: HTML 지연 그래프 (`open load/vegeta/report.html`)

## 수동 명령 예시
파일 없이 간단히 테스트할 경우 다음과 같이 직접 실행할 수도 있습니다.
```bash
vegeta attack -duration=30s -rate=50 -targets=load/vegeta/targets.txt | vegeta report
```

## 마무리
부하 테스트가 끝나면 `Ctrl+C`로 포트포워딩을 중지하고 필요 시 `./scripts/stop-local-k8s.sh`로 리소스를 정리하세요.
