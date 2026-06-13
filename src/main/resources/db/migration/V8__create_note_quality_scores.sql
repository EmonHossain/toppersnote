CREATE TABLE IF NOT EXISTS note_quality_scores (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    score BIGINT NOT NULL,
    upvote_count BIGINT NOT NULL,
    download_count BIGINT NOT NULL,
    view_count BIGINT NOT NULL,
    version_count INT NOT NULL,
    preview_available BOOLEAN NOT NULL,
    active_report_count BIGINT NOT NULL,
    upvote_score BIGINT NOT NULL,
    download_score BIGINT NOT NULL,
    view_score BIGINT NOT NULL,
    freshness_score BIGINT NOT NULL,
    version_score BIGINT NOT NULL,
    preview_score BIGINT NOT NULL,
    moderation_penalty BIGINT NOT NULL,
    calculated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_note_quality_scores_note FOREIGN KEY (note_id) REFERENCES notes(id),
    CONSTRAINT uk_note_quality_scores_note UNIQUE (note_id)
);

CREATE INDEX idx_note_quality_scores_score ON note_quality_scores(score, calculated_at);
CREATE INDEX idx_note_quality_scores_calculated ON note_quality_scores(calculated_at);
