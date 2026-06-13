CREATE TABLE application_metadata (
    metadata_key VARCHAR(120) NOT NULL PRIMARY KEY,
    metadata_value VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

INSERT INTO application_metadata (metadata_key, metadata_value, created_at)
VALUES ('schema_owner', 'flyway', CURRENT_TIMESTAMP);
