-- V1: 初期テーブル作成

CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    project_id  VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE assets (
    id            BIGSERIAL PRIMARY KEY,
    asset_id      VARCHAR(20)  NOT NULL UNIQUE,
    project_id    VARCHAR(20)  NOT NULL,
    asset_type    VARCHAR(50)  NOT NULL,
    source_path   TEXT,
    version_hash  VARCHAR(64),
    imported_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_assets_project FOREIGN KEY (project_id) REFERENCES projects(project_id)
);

CREATE TABLE analysis_jobs (
    id            BIGSERIAL PRIMARY KEY,
    job_id        VARCHAR(20)  NOT NULL UNIQUE,
    project_id    VARCHAR(20)  NOT NULL,
    job_type      VARCHAR(50)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
    requested_by  VARCHAR(100),
    started_at    TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    error_code    VARCHAR(100),
    error_message TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_jobs_project FOREIGN KEY (project_id) REFERENCES projects(project_id)
);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    event_type  VARCHAR(100) NOT NULL,
    project_id  VARCHAR(20),
    job_id      VARCHAR(20),
    user_id     VARCHAR(100),
    target_type VARCHAR(100),
    target_id   VARCHAR(50),
    details     TEXT,
    timestamp   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_assets_project_id    ON assets(project_id);
CREATE INDEX idx_jobs_project_id      ON analysis_jobs(project_id);
CREATE INDEX idx_jobs_status          ON analysis_jobs(status);
CREATE INDEX idx_audit_project_id     ON audit_logs(project_id);
CREATE INDEX idx_audit_job_id         ON audit_logs(job_id);
