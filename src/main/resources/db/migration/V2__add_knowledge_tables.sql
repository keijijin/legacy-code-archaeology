-- V2: 知識・証拠・業務ルールテーブル追加

CREATE TABLE evidences (
    id              BIGSERIAL PRIMARY KEY,
    evidence_id     VARCHAR(20)  NOT NULL UNIQUE,
    project_id      VARCHAR(20)  NOT NULL,
    source_asset_id VARCHAR(20),
    source_path     TEXT,
    start_line      INTEGER,
    end_line        INTEGER,
    evidence_type   VARCHAR(50),
    snippet         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_evidences_project FOREIGN KEY (project_id) REFERENCES projects(project_id)
);

CREATE TABLE business_rules (
    id               BIGSERIAL PRIMARY KEY,
    business_rule_id VARCHAR(20)  NOT NULL UNIQUE,
    project_id       VARCHAR(20)  NOT NULL,
    rule_text        TEXT         NOT NULL,
    confidence_level VARCHAR(20)  NOT NULL DEFAULT 'INFERRED',
    confidence_score NUMERIC(5,4),
    review_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reason           TEXT,
    model_name       VARCHAR(100),
    prompt_version   VARCHAR(50),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rules_project FOREIGN KEY (project_id) REFERENCES projects(project_id)
);

CREATE TABLE business_rule_evidence_links (
    business_rule_id VARCHAR(20) NOT NULL,
    evidence_id      VARCHAR(20) NOT NULL,
    PRIMARY KEY (business_rule_id, evidence_id)
);

CREATE TABLE reviews (
    id          BIGSERIAL PRIMARY KEY,
    review_id   VARCHAR(20)  NOT NULL UNIQUE,
    target_type VARCHAR(100) NOT NULL,
    target_id   VARCHAR(50)  NOT NULL,
    action      VARCHAR(20)  NOT NULL,
    comment     TEXT,
    reviewer_id VARCHAR(100),
    reviewed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evidences_project_id    ON evidences(project_id);
CREATE INDEX idx_evidences_asset_id      ON evidences(source_asset_id);
CREATE INDEX idx_rules_project_id        ON business_rules(project_id);
CREATE INDEX idx_rules_review_status     ON business_rules(review_status);
CREATE INDEX idx_reviews_target          ON reviews(target_type, target_id);
