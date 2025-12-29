# 로컬 Kubernetes Pod 가이드

`k8s/local-pod.yaml` 매니페스트를 사용해 Docker Desktop, Minikube, Kind 같은 로컬 Kubernetes 클러스터에서 애플리케이션을 Pod + Service 조합으로 실행하는 방법을 정리했습니다.

## 사전 준비물
- Docker CLI: 이미지를 `spring-laboratory:latest` 태그로 빌드합니다.
- `kubectl`: 현재 로컬 클러스터 컨텍스트와 통신되도록 설정되어 있어야 합니다.
- (선택) Docker Desktop 이외 환경: Minikube/Kind 등에 로컬 이미지를 로드할 수 있어야 합니다.

## 런타임 이미지 빌드
```bash
# 프로젝트 루트에서 실행
docker build -t spring-laboratory:latest .
```

Docker Desktop 외의 환경에서는 클러스터가 로컬 이미지를 인식하도록 추가 로드가 필요합니다.
- **Minikube**: `minikube image load spring-laboratory:latest`
- **Kind**: `kind load docker-image spring-laboratory:latest`

### 스크립트로 한 번에 실행하기
반복 테스트 시 `scripts/start-local-k8s.sh` 스크립트가 위 과정을 자동화합니다.

```bash
./scripts/start-local-k8s.sh
```

기본적으로 `spring-laboratory:latest` 이미지를 빌드하고 매니페스트를 적용한 뒤, `app=spring-app` Pod가 Ready 상태가 될 때까지 기다린 다음 `kubectl port-forward service/spring-app 9000:9000 9010:9010`을 실행합니다. (9000은 HTTP, 9010은 VisualVM/JMX 용입니다.) 이 과정에서 포트포워딩이 유지되는 동안 스크립트가 포그라운드에 머무르므로, 다른 작업은 별도 터미널에서 진행하거나 `Ctrl+C`로 포트포워딩을 중지한 뒤 진행하세요.

환경 변수로 동작을 조정할 수 있습니다.
- `IMAGE_NAME`: 빌드 및 배포에 사용할 이미지 태그 (기본값 `spring-laboratory:latest`)
- `WAIT_TIMEOUT`: Pod Ready를 기다릴 최대 시간 (기본값 `120s`)

예시) `IMAGE_NAME=myrepo/spring-laboratory:dev WAIT_TIMEOUT=180s ./scripts/start-local-k8s.sh`

## Pod 및 Service 적용
```bash
kubectl apply -f k8s/local-pod.yaml
```

해당 매니페스트는 다음을 수행합니다.
- `spring-laboratory:latest` 이미지를 사용하는 `spring-app` Pod 1개를 생성합니다.
- 기존 JFR 설정을 유지하기 위해 `JAVA_OPTS` 환경 변수를 주입하고 `/app/dir` 경로에 `emptyDir` 볼륨을 마운트합니다.
- 컨테이너 포트 8080을 노출하고, 이를 대상으로 하는 `Service/spring-app`을 포트 9000으로 생성합니다.

## 상태 확인
```bash
kubectl get pods spring-app
kubectl describe pod spring-app    # 문제 시 상세 원인 확인
```

Pod가 `Running`(READY 1/1) 상태가 될 때까지 기다리세요. CrashLoop가 발생하면 로그를 확인합니다.
```bash
kubectl logs spring-app
```

## 애플리케이션 접속
가장 간단한 방법은 Service를 포트포워딩하는 것입니다.
```bash
kubectl port-forward service/spring-app 9000:9000 9010:9010
```

포트포워딩이 열려 있는 동안 다른 터미널에서 요청을 보낼 수 있습니다.
```bash
curl http://localhost:9000/hello
curl http://localhost:9000/latency/probe
```

### VisualVM 연동
`k8s/local-pod.yaml`은 `JAVA_OPTS`에 JMX 관련 플래그를 포함하고 9010 포트를 개방합니다. 위의 포트포워딩이 열린 상태에서 VisualVM → `File > Add JMX Connection` → `localhost:9010`을 지정하면 즉시 프로파일링/모니터링을 시작할 수 있습니다. 인증/SSL은 비활성화되어 있으므로 추가 자격증명 입력이 필요 없습니다.

`Ctrl+C`로 포트포워딩을 종료합니다. 로컬 클러스터가 LoadBalancer를 제공한다면(`kubectl port-forward deployment/spring-app 9000:8080` 또는 `kubectl expose` 등) 환경에 맞는 방식을 사용해도 됩니다.

## 종료 및 정리
포트포워딩 세션은 열린 터미널에서 `Ctrl+C`로 즉시 종료할 수 있습니다.

Pod와 Service를 완전히 중지하려면 아래 명령으로 리소스를 삭제하세요.
```bash
kubectl delete -f k8s/local-pod.yaml
```

또는 편의 스크립트를 사용할 수 있습니다.
```bash
./scripts/stop-local-k8s.sh
```

테스트가 끝난 뒤 리소스를 제거해두면 로컬 클러스터 자원을 깔끔하게 회수할 수 있습니다.
