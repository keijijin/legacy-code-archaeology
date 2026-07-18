package com.legacy.archaeology.domain.knowledge;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BusinessRuleEntityTest {

    @Test
    void デフォルトのreviewStatusはPENDINGであること() {
        BusinessRuleEntity rule = new BusinessRuleEntity();
        rule.setBusinessRuleId("BR-001");
        rule.setProjectId("PRJ-001");
        rule.setRuleText("テストルール");
        rule.setConfidenceLevel(ConfidenceLevel.INFERRED);
        rule.onCreate();

        assertThat(rule.getReviewStatus()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    void 承認するとAPPROVEDとCONFIRMEDになること() {
        BusinessRuleEntity rule = new BusinessRuleEntity();
        rule.setReviewStatus(ReviewStatus.PENDING);
        rule.setConfidenceLevel(ConfidenceLevel.LIKELY);
        rule.approve("テスト承認");

        assertThat(rule.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(rule.getConfidenceLevel()).isEqualTo(ConfidenceLevel.CONFIRMED);
    }

    @Test
    void 却下するとREJECTEDになること() {
        BusinessRuleEntity rule = new BusinessRuleEntity();
        rule.setReviewStatus(ReviewStatus.PENDING);
        rule.setConfidenceLevel(ConfidenceLevel.INFERRED);
        rule.reject();

        assertThat(rule.getReviewStatus()).isEqualTo(ReviewStatus.REJECTED);
    }

    @Test
    void 却下済みのルールを直接承認しようとすると例外になること() {
        BusinessRuleEntity rule = new BusinessRuleEntity();
        rule.setReviewStatus(ReviewStatus.REJECTED);
        rule.setConfidenceLevel(ConfidenceLevel.INFERRED);

        assertThatThrownBy(() -> rule.approve("強引に承認"))
                .isInstanceOf(IllegalStateException.class);
    }
}
