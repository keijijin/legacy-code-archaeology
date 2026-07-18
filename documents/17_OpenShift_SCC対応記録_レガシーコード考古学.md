# OpenShift SCC 対応記録

- 文書番号：LCA-OCP-002
- 版数：1.0
- 作成日：2026-07-18

---

## 1. 発生した問題

OpenShift 上で Pod が起動せず、以下のエラーが発生した。

```text
pods "lca-postgres-..." is forbidden: unable to validate against any security context constraint:
provider restricted-v2: .spec.securityContext.fsGroup: Invalid value: [26]: 26 is not an allowed group
```

加えて以下も発生した。

1. `bitnami/postgresql:16` の ImagePullBackOff（manifest unknown）
2. API が `localhost:5432` に接続して CrashLoopBackOff
3. Hibernate schema validation 失敗（`confidence_score` 型不一致）

---

## 2. 原因

| 問題 | 原因 |
|---|---|
| SCC 違反 | `fsGroup: 26` を明示指定していた |
| ImagePullBackOff | Bitnami の tag `16` が存在しない |
| localhost 接続 | Secret/Config がサービス名ではなく localhost を指していた |
| schema validation | JPA `Double` と DB `NUMERIC` の型不一致 |

---

## 3. 対応内容

### 3.1 PostgreSQL

- イメージを `quay.io/sclorg/postgresql-15-c9s:latest` に変更
- `fsGroup` / `runAsUser` を明示しない
- `runAsNonRoot: true` + `drop ALL capabilities` を設定

### 3.2 Neo4j

- 既存 PVC を維持
- `runAsNonRoot` / seccomp / capabilities drop を設定
- Secret から `NEO4J_AUTH` を注入

### 3.3 API

- Secret にサービス名接続を設定
  - `jdbc:postgresql://lca-postgresql:5432/lca_db`
  - `bolt://lca-neo4j:7687`
- Spring 明示環境変数を Deployment に追加
- `BusinessRuleEntity.confidenceScore` を `BigDecimal` に修正
- イメージを Podman で再ビルド・push

---

## 4. 結果

```text
lca-api          1/1 Running
lca-neo4j        1/1 Running
lca-postgresql   1/1 Running
```

Health check:

```json
{"service":"legacy-code-archaeology","status":"UP"}
{"status":"UP","groups":["liveness","readiness"]}
```

Route:

```text
https://lca-api-route-legacy-code-archaeology-dev.apps.cluster-9nq5p.dyn.redhatworkshops.io
```

---

## 5. 運用上の注意

- OpenShift では任意 UID を前提にする
- `fsGroup` 固定は避ける
- DB/Graph の接続先は Service DNS 名を使う
- イメージビルド・push は Podman を使う
