# ADR-2026-004 ローカルコンテナ実行に Podman を使用する

- ステータス：Accepted
- 決定日：2026-07-18

---

## 1. 背景

本プロジェクトでは、ローカル開発環境での PostgreSQL・Neo4j などのコンテナ起動が必要である。  
当初 `docker-compose.yml` を作成していたが、ローカルコンテナ実行基盤として **Podman を正式採用する** ことを明示的にルール化する。

---

## 2. 決定事項

- ローカル開発環境のコンテナ実行には **Podman** を使用する
- `docker-compose.yml` を廃止し、`podman-compose.yml` を正式設定とする
- ビルド・実行コマンドは `podman build` / `podman compose` に統一する
- Docker CLI の直接使用を禁止する

---

## 3. 代替案

- **Docker Desktop**：有償ライセンス制約あり。採用しない。
- **Docker CLI（CE）**：Podman と機能的に同等だが、rootful daemon が必要。採用しない。
- **Podman**：ルートレス実行可能。OpenShift との親和性が高い。**採用する。**

---

## 4. 採用理由

- OpenShift は内部的に CRI-O を使用しており、Podman は同系統のコンテナランタイム
- ルートレスコンテナによりセキュリティリスクを低減できる
- `podman compose` は `docker-compose` 互換コマンドとして使用可能
- CI/CD や OpenShift デプロイとの一貫性を保てる

---

## 5. 影響

- `docker-compose.yml` → `podman-compose.yml` へリネーム・更新
- `scripts/` に Podman 用起動スクリプトを追加
- `README.md` の手順を Podman ベースに更新
- `documents/16_OpenShiftデプロイ方針` にローカル起動手順を追記
- `.codex/rules/01_実装ルール規定.md` に 3.5 コンテナ実行環境原則を追加

---

## 6. リスク

- `podman compose` は一部の `docker-compose` 機能と挙動が異なる場合がある

### 回避策

- `podman-compose` パッケージを `pip install podman-compose` でインストールして利用する
- または `podman compose`（Podman 4.x 以降の組み込みコマンド）を使用する
- ヘルスチェック等の差異は都度確認する

---

## 7. 関連文書

- `.codex/rules/01_実装ルール規定.md`（3.5 コンテナ実行環境原則）
- `.codex/rules/02_コーディング規約.md`（7.2 コンテナ実行規約）
- `documents/16_OpenShiftデプロイ方針_レガシーコード考古学.md`
