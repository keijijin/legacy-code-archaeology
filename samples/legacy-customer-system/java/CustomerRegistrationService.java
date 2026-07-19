package com.legacy.customer;

/**
 * 顧客登録サービス（レガシーシステムのサンプル）。
 * 業務ルールはここに埋め込まれている。
 */
public class CustomerRegistrationService {

    /**
     * 口座開設可否判定。
     * 法人かつ KYC 完了かつ AML クリアの場合に true を返す。
     */
    public boolean canOpenAccount(String customerType, String kycStatus, String amlStatus) {
        if ("CORPORATE".equals(customerType)
                && "COMPLETED".equals(kycStatus)
                && "CLEAR".equals(amlStatus)) {
            return true;
        }
        return false;
    }

    /**
     * 顧客登録処理。
     */
    public String registerCustomer(String name, String customerType) {
        validateName(name);
        // 実際には DB 登録などを行う
        return "CUS-" + name.hashCode();
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }
}
