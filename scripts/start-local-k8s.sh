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

echo
echo "Next steps:"
echo "  kubectl get pods spring-app"
echo "  kubectl port-forward service/spring-app 9000:9000"
