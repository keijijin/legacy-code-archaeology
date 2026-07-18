#!/usr/bin/env bash
# ローカル開発環境の起動スクリプト（Podman使用）
# ADR-2026-004: ローカルコンテナ実行には Podman を使用する

set -euo pipefail
source ~/.bash_profile

echo "=== レガシーコード考古学 ローカル環境起動 ==="
echo "[使用ランタイム: Podman $(podman --version)]"

echo "[1/2] コンテナ起動 (podman compose up)"
podman compose -f "$(dirname "$0")/../../podman-compose.yml" up -d

echo "[2/2] 起動状態確認"
podman compose -f "$(dirname "$0")/../../podman-compose.yml" ps

echo ""
echo "起動完了"
echo "  PostgreSQL : localhost:5432 (lca_db / lca_user / lca_password)"
echo "  Neo4j      : http://localhost:7474 (neo4j / neo4j_password)"
echo ""
echo "停止するには: scripts/local/stop.sh"
