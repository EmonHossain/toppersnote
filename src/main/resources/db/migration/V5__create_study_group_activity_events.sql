CREATE TABLE IF NOT EXISTS study_group_activity_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    study_group_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    type VARCHAR(60) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id BIGINT,
    notebook_id BIGINT,
    note_id BIGINT,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_study_group_activity_group_id
    ON study_group_activity_events (study_group_id, id);

CREATE INDEX idx_study_group_activity_note_created
    ON study_group_activity_events (note_id, created_at);

CREATE INDEX idx_study_group_activity_actor_created
    ON study_group_activity_events (actor_user_id, created_at);
