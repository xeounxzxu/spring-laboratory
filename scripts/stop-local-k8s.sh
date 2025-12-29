#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
MANIFEST_PATH="$REPO_DIR/k8s/local-pod.yaml"

if [[ ! -f "$MANIFEST_PATH" ]]; then
  echo "Manifest not found: $MANIFEST_PATH" >&2
  exit 1
fi

echo "Deleting Kubernetes resources defined in $MANIFEST_PATH"
kubectl delete -f "$MANIFEST_PATH"
