#!/usr/bin/env bash
# ローカル開発環境の停止スクリプト（Podman使用）
# ADR-2026-004: ローカルコンテナ実行には Podman を使用する

set -euo pipefail
source ~/.bash_profile

echo "=== レガシーコード考古学 ローカル環境停止 ==="

podman compose -f "$(dirname "$0")/../../podman-compose.yml" down

echo "停止完了"
