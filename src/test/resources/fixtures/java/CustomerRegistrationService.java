package com.example.customer;

/**
 * 顧客登録サービス（回帰テスト用フィクスチャ）。
 */
public class CustomerRegistrationService {

    public boolean canOpenAccount(String customerType, String kycStatus, String amlStatus) {
        if ("CORPORATE".equals(customerType)
                && "COMPLETED".equals(kycStatus)
                && "CLEAR".equals(amlStatus)) {
            return true;
        }
        return false;
    }

    public String registerCustomer(String name) {
        validateName(name);
        return "CUS-" + name.hashCode();
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }
}
