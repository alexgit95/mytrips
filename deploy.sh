#!/usr/bin/env bash
# Deploy script for Raspberry Pi (Debian)
# Usage: ./deploy.sh <docker-username> <docker-repo>
# Example: ./deploy.sh alexgit95 mytrips

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <docker-username> <docker-repo>"
  echo "Example: $0 alexgit95 mytrips"
  exit 1
fi

DOCKER_USERNAME="$1"
DOCKER_REPO="$2"  # e.g. mytrips

# Full image name
IMAGE_NAME="${DOCKER_USERNAME}/${DOCKER_REPO}:latest"

# Clone the repo into a temporary directory
TMP_DIR=$(mktemp -d)
cleanup() {
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

echo "Cloning repository https://github.com/alexgit95/mytrips.git into ${TMP_DIR}..."
git clone --depth 1 https://github.com/alexgit95/mytrips.git "${TMP_DIR}"

# Prompt for Docker Hub password (hidden)
read -rsp "Docker Hub password for ${DOCKER_USERNAME}: " DOCKER_PASSWORD
echo

# Login to Docker Hub
echo "Logging in to Docker Hub as ${DOCKER_USERNAME}..."
# Use --password-stdin to avoid exposing password in process list
printf '%s' "${DOCKER_PASSWORD}" | docker login --username "${DOCKER_USERNAME}" --password-stdin

# Build the Docker image
cd "${TMP_DIR}"
echo "Building Docker image ${IMAGE_NAME}..."
docker build -t "${IMAGE_NAME}" .

# Push the image to Docker Hub
echo "Pushing Docker image ${IMAGE_NAME}..."
docker push "${IMAGE_NAME}"

echo "Done! Image pushed to Docker Hub: ${IMAGE_NAME}"
