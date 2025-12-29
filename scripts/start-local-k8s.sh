#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
IMAGE_NAME=${IMAGE_NAME:-spring-laboratory:latest}
MANIFEST_PATH="$REPO_DIR/k8s/local-pod.yaml"

if [[ ! -f "$MANIFEST_PATH" ]]; then
  echo "Manifest not found: $MANIFEST_PATH" >&2
  exit 1
fi

echo "Building Docker image: $IMAGE_NAME"
# 이전에 동일 태그 이미지가 있으면 삭제해 빌드 캐시/레이어를 초기화합니다.
if docker image inspect "$IMAGE_NAME" >/dev/null 2>&1; then
  echo "Removing existing image: $IMAGE_NAME"
  docker rmi -f "$IMAGE_NAME"
fi
docker build -t "$IMAGE_NAME" "$REPO_DIR"

echo "Applying Kubernetes resources from $MANIFEST_PATH"
kubectl apply -f "$MANIFEST_PATH"

WAIT_TIMEOUT=${WAIT_TIMEOUT:-120s}
echo "Waiting for Pod(s) with label app=spring-app to become Ready (timeout: $WAIT_TIMEOUT)"
kubectl wait --for=condition=Ready pod -l app=spring-app --timeout="$WAIT_TIMEOUT"

echo "Starting port-forward to expose service/spring-app on localhost:9000 (HTTP) and 9010 (JMX)"
echo "Press Ctrl+C to stop the port-forward session."
kubectl port-forward service/spring-app 9000:9000 9010:9010
