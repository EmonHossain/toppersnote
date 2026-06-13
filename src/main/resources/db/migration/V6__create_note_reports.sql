CREATE TABLE IF NOT EXISTS note_reports (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    reported_by_user_id BIGINT NOT NULL,
    reason VARCHAR(40) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    reviewed_by_user_id BIGINT,
    resolution_notes VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    reviewed_at TIMESTAMP(6),
    CONSTRAINT fk_note_reports_note FOREIGN KEY (note_id) REFERENCES notes(id),
    CONSTRAINT fk_note_reports_reported_by FOREIGN KEY (reported_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_note_reports_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_note_reports_note_status ON note_reports(note_id, status);
CREATE INDEX idx_note_reports_reporter_created ON note_reports(reported_by_user_id, created_at);
CREATE INDEX idx_note_reports_status_created ON note_reports(status, created_at);
