package com.legacy.archaeology.domain.knowledge;

/** レビュー状態。AI候補は必ず PENDING から開始する。 */
public enum ReviewStatus {
    PENDING,   // 未レビュー
    APPROVED,  // 承認済み（人間レビューでのみ付与可能）
    REJECTED,  // 却下済み
    MODIFIED,  // 修正済み
    ON_HOLD    // 保留
}
