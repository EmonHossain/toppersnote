CREATE TABLE IF NOT EXISTS note_view_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    viewer_user_id BIGINT NOT NULL,
    source VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_note_view_events_note_created
    ON note_view_events (note_id, created_at);

CREATE INDEX idx_note_view_events_viewer_created
    ON note_view_events (viewer_user_id, created_at);

CREATE INDEX idx_note_view_events_created
    ON note_view_events (created_at);

CREATE TABLE IF NOT EXISTS note_download_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    downloader_user_id BIGINT NOT NULL,
    source VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_note_download_events_note_created
    ON note_download_events (note_id, created_at);

CREATE INDEX idx_note_download_events_downloader_created
    ON note_download_events (downloader_user_id, created_at);

CREATE INDEX idx_note_download_events_created
    ON note_download_events (created_at);

CREATE TABLE IF NOT EXISTS search_query_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    searched_by_user_id BIGINT NOT NULL,
    query_text VARCHAR(120) NOT NULL,
    result_count INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_search_query_events_query_created
    ON search_query_events (query_text, created_at);

CREATE INDEX idx_search_query_events_user_created
    ON search_query_events (searched_by_user_id, created_at);

CREATE INDEX idx_search_query_events_created
    ON search_query_events (created_at);
