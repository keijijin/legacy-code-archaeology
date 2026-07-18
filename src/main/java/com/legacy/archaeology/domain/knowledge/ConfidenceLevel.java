package com.legacy.archaeology.domain.knowledge;

/** 信頼度状態。すべての知識候補に付与する。 */
public enum ConfidenceLevel {
    CONFIRMED,  // コード・テスト・設計書等の複数根拠が整合し、人間確認済み
    LIKELY,     // 複数根拠が整合するが人間未確認
    INFERRED,   // AI推定または単一根拠中心
    CONFLICTED, // 根拠間に矛盾がある
    UNKNOWN     // 判断材料不足
}
