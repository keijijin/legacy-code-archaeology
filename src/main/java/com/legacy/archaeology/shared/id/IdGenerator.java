package com.legacy.archaeology.shared.id;

import java.util.UUID;
import org.springframework.stereotype.Component;

/** ドメイン識別子の生成。外部表示用IDは種別プレフィックスを持つ。 */
@Component
public class IdGenerator {

    public String generateProjectId() {
        return "PRJ-" + shortUuid();
    }

    public String generateAssetId() {
        return "AST-" + shortUuid();
    }

    public String generateJobId() {
        return "JOB-" + shortUuid();
    }

    public String generateBusinessRuleId() {
        return "BR-" + shortUuid();
    }

    public String generateEvidenceId() {
        return "EV-" + shortUuid();
    }

    public String generateReviewId() {
        return "REV-" + shortUuid();
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
