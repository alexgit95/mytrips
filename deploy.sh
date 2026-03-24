#!/usr/bin/env bash
# Deploy script for Raspberry Pi (Debian)
# Usage: ./deploy.sh [--major] <docker-username> <docker-repo>
# Example: ./deploy.sh alexgit95 mytrips
#          ./deploy.sh --major alexgit95 mytrips   (bumps major version)
#
# Version is stored in .docker-version (format: MAJOR.MINOR) next to this script.
# Each run bumps the minor version by 1, unless --major is passed (bumps major, resets minor to 0).
# Both :latest and :<MAJOR>.<MINOR> tags are pushed.

set -euo pipefail

# ---- Parse --major flag ----
BUMP_MAJOR=false
if [[ "${1:-}" == "--major" ]]; then
  BUMP_MAJOR=true
  shift
fi

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 [--major] <docker-username> <docker-repo>"
  echo "Example: $0 alexgit95 mytrips"
  echo "         $0 --major alexgit95 mytrips"
  exit 1
fi

DOCKER_USERNAME="$1"
DOCKER_REPO="$2"  # e.g. mytrips

# ---- Version management ----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION_FILE="${SCRIPT_DIR}/.docker-version"

if [[ -f "${VERSION_FILE}" ]]; then
  VERSION_CURRENT="$(cat "${VERSION_FILE}")"
  MAJOR="${VERSION_CURRENT%%.*}"
  MINOR="${VERSION_CURRENT##*.}"
else
  MAJOR=0
  MINOR=0
fi

if [[ "${BUMP_MAJOR}" == true ]]; then
  MAJOR=$(( MAJOR + 1 ))
  MINOR=0
else
  MINOR=$(( MINOR + 1 ))
fi

VERSION="${MAJOR}.${MINOR}"
echo "${VERSION}" > "${VERSION_FILE}"
echo "Version: ${VERSION}"

# Full image names
IMAGE_LATEST="${DOCKER_USERNAME}/${DOCKER_REPO}:latest"
IMAGE_VERSIONED="${DOCKER_USERNAME}/${DOCKER_REPO}:${VERSION}"

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
echo "Building Docker image ${IMAGE_VERSIONED}..."
docker build -t "${IMAGE_LATEST}" -t "${IMAGE_VERSIONED}" .

# Push the image to Docker Hub
echo "Pushing Docker image ${IMAGE_LATEST}..."
docker push "${IMAGE_LATEST}"
echo "Pushing Docker image ${IMAGE_VERSIONED}..."
docker push "${IMAGE_VERSIONED}"

echo "Done! Images pushed to Docker Hub: ${IMAGE_LATEST} and ${IMAGE_VERSIONED}"
