# legacy-customer-system サンプル

本サンプルは「レガシーコード考古学」MVP のエンドツーエンドデモに使用するレガシー資産一式です。

## 1. 目的

- Java / Camel / SQL / YAML の解析精度検証
- 業務ルール候補抽出の確認
- 影響分析・OpenShift移行課題抽出の確認
- 人間レビュー UI の動作確認

## 2. 構成

```text
legacy-customer-system/
├── java/CustomerRegistrationService.java
├── camel/customer-registration-route.xml
├── sql/customer_schema.sql
├── config/application.yml
└── expected/
    ├── java_customer_registration.json
    ├── camel_customer_route.json
    ├── sql_customer_schema.json
    └── expected_rules.json
```

## 3. 主要な業務ルール

- 法人かつ KYC 完了かつ AML クリアの場合に口座開設可能
- 顧客登録時に通知サービス呼び出し
- 口座開設時に口座テーブルへ書き込み

## 4. 外部接続

- `jms:queue:customer.in`
- `jdbc:customerDS`
- `http://notification-service/api/send`

## 5. 期待される抽出結果

`expected/` 配下の JSON を正解として解析結果を比較可能にしています。

## 6. 利用方法

詳細は `documents/23_利用ガイド_レガシーコード考古学.md` を参照してください。
