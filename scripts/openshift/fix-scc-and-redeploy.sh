#!/usr/bin/env bash
# OpenShift SCC 準拠リソースへ整理して再デプロイする
set -euo pipefail
source ~/.bash_profile

NS="legacy-code-archaeology-dev"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "=== [1/6] 壊れた旧リソースを削除 ==="
oc -n "${NS}" delete deployment lca-postgres lca-postgresql lca-neo4j lca-api --ignore-not-found
oc -n "${NS}" delete svc lca-postgres-service lca-postgresql lca-neo4j lca-neo4j-service --ignore-not-found
# PVC は残す（データ保持）。権限問題が続く場合のみ手動削除する

echo "=== [2/6] Secret をサービス名接続に更新 ==="
oc -n "${NS}" create secret generic lca-secret \
  --from-literal=DB_URL='jdbc:postgresql://lca-postgresql:5432/lca_db' \
  --from-literal=DB_USERNAME='lca_user' \
  --from-literal=DB_PASSWORD='lca_password' \
  --from-literal=NEO4J_URI='bolt://lca-neo4j:7687' \
  --from-literal=NEO4J_USERNAME='neo4j' \
  --from-literal=NEO4J_PASSWORD='neo4j_password' \
  --from-literal=NEO4J_AUTH='neo4j/neo4j_password' \
  --from-literal=OPENAI_API_KEY='' \
  --dry-run=client -o yaml | oc apply -f -

echo "=== [3/6] ConfigMap 適用 ==="
oc apply -f "${ROOT}/deploy/openshift/base/configmap.yaml"

echo "=== [4/6] PostgreSQL / Neo4j 適用 ==="
oc apply -f "${ROOT}/deploy/openshift/base/postgresql.yaml"
oc apply -f "${ROOT}/deploy/openshift/base/neo4j.yaml"

echo "=== [5/6] API / Service / Route 適用 ==="
oc apply -f "${ROOT}/deploy/openshift/base/api-deployment.yaml"
oc apply -f "${ROOT}/deploy/openshift/base/api-service.yaml"
oc apply -f "${ROOT}/deploy/openshift/base/api-route.yaml"

echo "=== [6/6] 状態確認 ==="
sleep 5
oc -n "${NS}" get pods,svc,route,pvc
echo ""
echo "完了。pod が Ready になるまで数分待ってください:"
echo "  oc -n ${NS} get pods -w"
