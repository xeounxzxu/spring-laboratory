#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME="spring-k8s-repo"
CONTAINER_NAME="spring-k8s-repo"
PORT="8080"

# Build the Docker image using the existing Dockerfile.
docker build -t "${IMAGE_NAME}" .

# Stop and remove any existing container with the same name to avoid conflicts.
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  docker rm -f "${CONTAINER_NAME}"
fi

# Run the freshly built image and forward the default application port.
docker run -it --name "${CONTAINER_NAME}" -p "${PORT}:${PORT}" -e JAVA_OPTS="${JAVA_OPTS:-}" "${IMAGE_NAME}"
