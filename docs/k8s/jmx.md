# JMX 연동 가이드

로컬 Kubernetes 환경에서 `spring-app` Pod를 VisualVM 등 JMX 툴과 연동하기 위한 절차를 정리했습니다. `k8s/local-pod.yaml`은 이미 JMX 플래그와 포트를 포함하고 있으므로, 아래 단계를 따르면 바로 접속할 수 있습니다.

## 1. 리소스 및 포트포워딩 준비
1. `./scripts/start-local-k8s.sh` 실행 (기존 이미지 삭제 → 빌드 → 매니페스트 적용 → Pod Ready 대기 → `kubectl port-forward service/spring-app 9000:9000 9010:9010 7091:7091` 자동 수행)
2. 또는 수동으로 다음 명령을 실행:
   ```bash
   docker build -t spring-laboratory:latest .
   kubectl apply -f k8s/local-pod.yaml
   kubectl wait --for=condition=Ready pod -l app=spring-app --timeout=120s
   kubectl port-forward service/spring-app 9000:9000 9010:9010 7091:7091
   ```

`kubectl port-forward` 명령은 포트 9010(JMX)과 7091(jstatd)을 로컬에 노출하므로, VisualVM은 `localhost:9010`으로 JMX 연결을, `localhost:7091`로 Visual GC(jstatd 연결)를 사용할 수 있습니다.

## 2. VisualVM에서 JMX & Visual GC 연결
1. VisualVM 실행 후 좌측 상단에서 `파일(File) > JMX 연결 추가(Add JMX Connection)` 선택
2. 호스트: `localhost`, 포트: `9010` 입력
3. 인증/SSL 옵션은 비활성 상태 그대로 두고 `확인(OK)` 클릭
4. 연결이 성공하면 JVM 트리가 추가되며, CPU/메모리/스레드/샘플링/프로파일링 탭을 사용할 수 있습니다.
5. Visual GC 플러그인을 설치한 상태라면 `도구 > 플러그인 > 사용 설정`에서 `Visual GC`가 켜져 있는지 확인한 뒤, 좌측 JVM 노드를 우클릭 → `Visual GC`를 선택합니다. 이후 `jstatd Connection` 프롬프트에 `localhost:7091`을 입력하면 힙/세대별 그래프가 활성화됩니다. (매니페스트가 main 컨테이너와 `jstatd` sidecar 간 `/tmp` `emptyDir` 볼륨을 공유하고 `shareProcessNamespace: true`가 적용되어 있어 Visual GC가 `hsperfdata_*` 파일을 읽을 수 있습니다.)

> 참고: `k8s/local-pod.yaml`은 `JAVA_OPTS`에 `-Dcom.sun.management.jmxremote.authenticate=false`와 `-Dcom.sun.management.jmxremote.ssl=false`를 포함하고 있으며, `jstatd` sidecar는 내부 policy 파일을 사용해 모든 권한을 허용합니다. 로컬 개발 목적이므로 인증 없이 사용할 수 있지만, 외부 환경에서는 보안 정책을 강화해야 합니다.

## 3. 연결 종료
- 포트포워딩 터미널에서 `Ctrl+C`를 눌러 세션 종료
- 필요 시 `./scripts/stop-local-k8s.sh` 또는 `kubectl delete -f k8s/local-pod.yaml`로 Pod/Service 제거

이후 다시 연결하고 싶다면 1~2 단계를 반복하면 됩니다.
