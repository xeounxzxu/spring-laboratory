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
docker build -t "$IMAGE_NAME" "$REPO_DIR"

echo "Applying Kubernetes resources from $MANIFEST_PATH"
kubectl apply -f "$MANIFEST_PATH"

WAIT_TIMEOUT=${WAIT_TIMEOUT:-120s}
echo "Waiting for Pod(s) with label app=spring-app to become Ready (timeout: $WAIT_TIMEOUT)"
kubectl wait --for=condition=Ready pod -l app=spring-app --timeout="$WAIT_TIMEOUT"

echo "Starting port-forward to expose service/spring-app on localhost:9000"
echo "Press Ctrl+C to stop the port-forward session."
kubectl port-forward service/spring-app 9000:9000
