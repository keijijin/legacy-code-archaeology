#!/usr/bin/env bash
# OpenShift デプロイスクリプト
# 実行前に oc login 済みであること

set -euo pipefail

source ~/.bash_profile

NAMESPACE="legacy-code-archaeology-dev"

echo "[1/4] Namespace を作成します"
oc apply -f deploy/openshift/base/namespace.yaml

echo "[2/4] ConfigMap を適用します"
oc apply -f deploy/openshift/base/configmap.yaml

echo "[3/4] Secret テンプレートを確認してください（apply は手動で行うこと）"
echo "  secret.template.yaml を参考に以下を実行:"
echo "  oc create secret generic lca-secret -n ${NAMESPACE} \\"
echo "    --from-literal=DB_URL=... \\"
echo "    --from-literal=DB_USERNAME=... \\"
echo "    --from-literal=DB_PASSWORD=..."

echo "[4/4] Deployment / Service / Route を適用します"
oc apply -f deploy/openshift/base/api-deployment.yaml
oc apply -f deploy/openshift/base/api-service.yaml
oc apply -f deploy/openshift/base/api-route.yaml

echo "デプロイ完了"
oc get pods -n "${NAMESPACE}"
oc get routes -n "${NAMESPACE}"
