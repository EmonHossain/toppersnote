CREATE TABLE IF NOT EXISTS note_retention_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT,
    note_id_snapshot BIGINT NOT NULL,
    uploaded_by_user_id BIGINT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    subject_class VARCHAR(120) NOT NULL,
    institution VARCHAR(120) NOT NULL,
    degree_program VARCHAR(120) NOT NULL,
    semester VARCHAR(50) NOT NULL,
    year VARCHAR(20) NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL,
    notice_due_at TIMESTAMP(6) NOT NULL,
    removal_due_at TIMESTAMP(6) NOT NULL,
    notice_sent_at TIMESTAMP(6),
    removed_at TIMESTAMP(6),
    cancelled_at TIMESTAMP(6),
    cancel_reason VARCHAR(500),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_note_retention_status_notice_due
    ON note_retention_candidates (status, notice_due_at);

CREATE INDEX idx_note_retention_status_removal_due
    ON note_retention_candidates (status, removal_due_at);

CREATE INDEX idx_note_retention_note_snapshot
    ON note_retention_candidates (note_id_snapshot);
