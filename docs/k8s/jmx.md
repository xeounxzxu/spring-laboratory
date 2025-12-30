# JMC/JFR 연동 가이드

로컬 Kubernetes 환경에서 `spring-app` Pod를 Java Mission Control(JMC)과 JFR(Flight Recorder)로 분석하는 절차입니다. `k8s/local-pod.yaml`이 이미 필요한 JVM 플래그와 볼륨을 포함하고 있으므로 아래 순서로 연결하면 됩니다.

## 1. 리소스 및 포트포워딩 준비
1. `./scripts/start-local-k8s.sh` 실행 (이미지 빌드 → 매니페스트 적용 → Pod Ready 대기 → `kubectl port-forward service/spring-app 9000:9000 9010:9010` 자동 수행)
2. 또는 수동으로 다음 명령을 실행:
   ```bash
   docker build -t spring-laboratory:latest .
   kubectl apply -f k8s/local-pod.yaml
   kubectl wait --for=condition=Ready pod -l app=spring-app --timeout=120s
   kubectl port-forward service/spring-app 9000:9000 9010:9010
   ```

9010 포트는 JMX/JMC 접속용이며, `-Djava.rmi.server.hostname=0.0.0.0`으로 설정되어 있어 필요 시 로드밸런서 IP로 직접 접근해도 됩니다(보안 환경에서는 방화벽과 인증을 별도로 구성하세요).

## 2. Mission Control 연결 및 JFR 제어
1. JMC 실행 후 `File > Connect`를 선택하고 `localhost:9010`을 입력합니다.
2. 연결이 성공하면 JVM이 왼쪽 패널에 나타납니다. `Start Flight Recording`을 눌러 새 녹화를 생성하거나, 이미 실행 중인 Recording을 더블클릭해 실시간으로 모니터링할 수 있습니다.
3. 매니페스트가 기본으로 `-XX:StartFlightRecording=delay=5s,...`를 지정해 duration 없이 즉시(5초 뒤) 롤링 녹화를 수행하므로, `/app/records/startup.jfr` 파일이 Pod에 생성됩니다. 필요 시 `kubectl cp spring-app:/app/records/startup.jfr ./startup.jfr`로 다운로드해 JMC에서 열면 됩니다.
4. 더 긴 프로파일링이나 맞춤 설정이 필요하면 JMC에서 Recording Template, max size/age, disk path 등을 조절하거나 매니페스트의 `JAVA_OPTS`를 수정하세요.

## 3. 연결 종료 및 정리
- 포트포워딩 터미널에서 `Ctrl+C`로 세션 종료
- `./scripts/stop-local-k8s.sh` 또는 `kubectl delete -f k8s/local-pod.yaml`로 리소스 삭제

필요할 때마다 동일한 순서를 반복해 새로운 JFR 세션을 수집하면 됩니다.

## 4. Pod에서 JFR 파일 꺼내오기
- `kubectl cp spring-app:/app/records/startup.jfr ./startup.jfr` 처럼 `kubectl cp <pod>:/원격경로 <로컬경로>`를 사용하면 가장 빠르게 파일을 복사할 수 있습니다. 다른 네임스페이스라면 `kubectl cp my-ns/spring-app:/app/records/startup.jfr ./startup.jfr`처럼 `namespace/pod` 형태로 작성하세요.
- tar 경고를 피하고 싶다면 `kubectl exec spring-app -- cat /app/records/startup.jfr > startup.jfr`처럼 표준 출력으로 흘려보낸 뒤 로컬에서 리디렉션해 저장할 수 있습니다.
- 장기 보관이 필요하면 `/app/records`에 `PersistentVolumeClaim`이나 `hostPath`를 마운트하도록 매니페스트를 수정하세요. 이렇게 하면 Pod가 재시작해도 JFR 파일이 남고, PVC 또는 호스트 파일시스템을 통해 직접 접근할 수 있어 반복 복사가 줄어듭니다.
