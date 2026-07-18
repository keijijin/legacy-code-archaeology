#!/usr/bin/env bash
# OpenShift デプロイスクリプト
# 実行前に oc login 済みであること
# ADR-2026-004: ローカルコンテナは Podman / 本番は oc コマンドを使用

set -euo pipefail
source ~/.bash_profile

NAMESPACE="legacy-code-archaeology-dev"

echo "[1/6] Namespace を作成します"
oc apply -f deploy/openshift/base/namespace.yaml

echo "[2/6] ConfigMap を適用します"
oc apply -f deploy/openshift/base/configmap.yaml

echo "[3/6] Secret を確認してください（apply は手動で行うこと）"
echo "  以下を実行してください:"
echo "  oc create secret generic lca-secret -n ${NAMESPACE} \\"
echo "    --from-literal=DB_PASSWORD=<your_password> \\"
echo "    --from-literal=OPENAI_API_KEY= \\"
echo "    --dry-run=client -o yaml | oc apply -f -"

echo "[4/6] PostgreSQL を適用します（OpenShift SCC準拠版）"
oc apply -f deploy/openshift/base/postgresql.yaml

echo "[5/6] Neo4j を適用します（OpenShift SCC準拠版）"
oc apply -f deploy/openshift/base/neo4j.yaml

echo "[6/6] API Deployment / Service / Route を適用します"
oc apply -f deploy/openshift/base/api-deployment.yaml
oc apply -f deploy/openshift/base/api-service.yaml
oc apply -f deploy/openshift/base/api-route.yaml

echo ""
echo "デプロイ完了"
oc get pods -n "${NAMESPACE}"
oc get routes -n "${NAMESPACE}"
