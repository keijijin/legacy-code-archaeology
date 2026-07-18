#!/usr/bin/env bash
# アプリコンテナイメージビルドスクリプト（Podman使用）
# ADR-2026-004: ローカルコンテナ実行には Podman を使用する

set -euo pipefail
source ~/.bash_profile

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
IMAGE_NAME="lca-api"
IMAGE_TAG="${1:-latest}"

echo "=== コンテナイメージビルド (Podman) ==="
echo "[イメージ] ${IMAGE_NAME}:${IMAGE_TAG}"
echo "[使用ランタイム: Podman $(podman --version)]"

echo "[1/2] Gradleビルド"
cd "${PROJECT_ROOT}"
./gradlew bootJar -x test

echo "[2/2] podman build"
podman build \
  -t "${IMAGE_NAME}:${IMAGE_TAG}" \
  -f "${PROJECT_ROOT}/Dockerfile" \
  "${PROJECT_ROOT}"

echo ""
echo "ビルド完了: ${IMAGE_NAME}:${IMAGE_TAG}"
echo "実行確認: podman run --rm -p 8080:8080 ${IMAGE_NAME}:${IMAGE_TAG}"
