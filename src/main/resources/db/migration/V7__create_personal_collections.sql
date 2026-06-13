CREATE TABLE IF NOT EXISTS personal_collections (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_personal_collections_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT uk_personal_collections_owner_name UNIQUE (owner_user_id, name)
);

CREATE TABLE IF NOT EXISTS personal_collection_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    collection_id BIGINT NOT NULL,
    note_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_personal_collection_items_collection FOREIGN KEY (collection_id) REFERENCES personal_collections(id),
    CONSTRAINT fk_personal_collection_items_note FOREIGN KEY (note_id) REFERENCES notes(id),
    CONSTRAINT uk_personal_collection_items_collection_note UNIQUE (collection_id, note_id)
);

CREATE INDEX idx_personal_collections_owner_updated ON personal_collections(owner_user_id, updated_at);
CREATE INDEX idx_personal_collection_items_collection_order ON personal_collection_items(collection_id, display_order);
CREATE INDEX idx_personal_collection_items_note ON personal_collection_items(note_id);
