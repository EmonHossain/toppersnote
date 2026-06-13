CREATE TABLE IF NOT EXISTS exam_reminders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    institution VARCHAR(120) NOT NULL,
    degree_program VARCHAR(120) NOT NULL,
    year VARCHAR(20) NOT NULL,
    semester VARCHAR(50) NOT NULL,
    subject_class VARCHAR(120) NOT NULL,
    exam_date DATE NOT NULL,
    details VARCHAR(500),
    created_by_user_id BIGINT NOT NULL,
    last_rescheduled_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    last_reminder_sent_at TIMESTAMP(6),
    active_lock_token VARCHAR(20),
    status VARCHAR(30) NOT NULL,
    CONSTRAINT uk_exam_reminder_subject_context UNIQUE (institution, degree_program, year, semester, subject_class, active_lock_token)
);

CREATE TABLE IF NOT EXISTS archived_classes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    institution VARCHAR(120) NOT NULL,
    degree_program VARCHAR(120) NOT NULL,
    year VARCHAR(20) NOT NULL,
    semester VARCHAR(50) NOT NULL,
    subject_class VARCHAR(120) NOT NULL,
    archived_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_archived_class_user_context UNIQUE (user_id, institution, degree_program, year, semester, subject_class)
);
