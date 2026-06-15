# ShareNote Backend

ShareNote is a Spring Boot backend for authenticated academic note sharing. It supports user registration, JWT-based stateless authentication, refresh tokens, secure note uploads, local or S3-backed file storage, academic class registration, and note visibility filtering by institution, degree program, subject/class, semester, and year.

## Features

- JWT access-token authentication
- Refresh token rotation
- Stateless Spring Security configuration
- User registration and login
- Authenticated profile-picture setup
- Academic class registration by institution, degree program, year, semester, and subject
- BCrypt password hashing
- Multipart note upload
- File whitelist validation for images, PDF, Word, Excel, and PowerPoint files
- Basic executable-content checks
- 10 MB upload limit by default
- Local and S3 file storage backends
- Note listing filtered by institution, degree program, subject/class, semester, and year
- Academic navigation for degree programs, years, semesters, and subjects
- Note comments and one-level replies
- One-like-per-user note upvotes
- Take-a-look suggestions for sharing a note with selected users
- Persistent notifications for new notes and take-a-look mentions
- Calendar lifecycle support for shared exam reminders and semester class archives
- Quartz-scheduled exam alerts with top upvoted notes from the previous week
- Quartz-driven note retention with uploader warning notices before automatic removal
- Admin-only moderation endpoints
- Audit trail for authentication, content, notifications, and moderation events
- Temporary and permanent user bans with user-facing notices
- DTO-based API responses
- Global exception handling
- Unit tests for auth, user, note, and storage services
- Versioned REST API under `/api/v1` by default
- RFC 9457-style Problem Details error responses
- Local MinIO service for S3-compatible storage testing
- Elasticsearch-backed note search with Hibernate Criteria fallback
- User/admin analytics dashboards for views, downloads, subject traffic, searches, and failed-login spikes
- Resilience4j retry and circuit breaker protection for search operations
- OpenAPI contract tests guarding API version and bearer-auth metadata
- Centralized ECS JSON log shipping to Elasticsearch for Kibana exploration

## Tech Stack

- Java 25
- Spring Boot 4.0.6
- Spring Security
- Spring Data JPA / Hibernate
- Springdoc OpenAPI / Swagger UI
- Resilience4j
- Spring Boot Actuator
- Quartz Scheduler
- Micrometer metrics and tracing
- OpenTelemetry OTLP export
- H2 database for local development
- MySQL Docker Compose profile for production-like local DB testing
- Flyway database migrations
- Testcontainers for MySQL-backed integration checks
- OWASP Dependency-Check vulnerability scanning
- Redis profile for cache/session-adjacent backend workflows
- Elasticsearch and Kibana profile for search workflows
- Filebeat for centralized ECS JSON log shipping
- JJWT
- AWS SDK for Java v2 S3
- MinIO for local S3-compatible testing
- Mailpit for local SMTP testing
- Apache Kafka in KRaft mode for asynchronous verification email delivery
- Prometheus and Grafana for local observability
- OpenTelemetry Collector for OTLP traces and metrics
- Redis for local cache testing
- Elasticsearch and Kibana for local search testing
- Kibana for searching centralized application logs
- Maven

## Project Structure

```text
src/main/java/com/sharenote
  academic/   Academic classes, auto-registration, and navigation
  admin/      Admin moderation endpoints and DTOs
  audit/      Audit-event persistence and query logic
  auth/       Authentication, JWT, refresh-token logic
  common/     Shared error response and exception handling
  analytics/  User/admin note traffic and security analytics
  lifecycle/  Exam reminders, scheduled alerts, and class archives
  note/       Note upload, metadata, and visibility filtering
  resilience/ Retry and circuit-breaker helpers
  search/     Elasticsearch note indexing/search with Criteria fallback
  security/   Spring Security and JWT request filter
  storage/    File validation plus local/S3 storage abstraction
  user/       User registration and user persistence
```

## Configuration

Runtime values are loaded from environment variables. For local development, Spring Boot also imports a local `.env` file:

```properties
spring.config.import=optional:file:.env[.properties]
```

Create your local environment file from the example:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Do not commit `.env`. It is intentionally ignored by Git.

### Required Local Variables

The default `.env.example` includes development values for:

- `DB_URL`
- `DB_DRIVER`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_EXPIRATION_MINUTES`
- `JWT_REFRESH_TOKEN_EXPIRATION_DAYS`
- `QUARTZ_AUTO_STARTUP`
- `EXAM_REMINDER_CRON`
- `EXAM_REMINDER_DAYS_BEFORE`
- `NOTE_RETENTION_YEARS`
- `NOTE_RETENTION_NOTICE_LEAD_DAYS`
- `NOTE_RETENTION_AUTO_DELETE_ENABLED`
- `NOTE_RETENTION_SCAN_CRON`
- `NOTE_RETENTION_NOTICE_CRON`
- `NOTE_RETENTION_REMOVAL_CRON`
- `NOTE_STORAGE_TYPE`
- `NOTE_STORAGE_DIRECTORY`
- `NOTE_MAX_FILE_SIZE_BYTES`
- `PROFILE_PICTURE_STORAGE_TYPE`
- `PROFILE_PICTURE_STORAGE_DIRECTORY`
- `PROFILE_PICTURE_MAX_FILE_SIZE_BYTES`
- `API_BASE_PATH`
- `ANALYTICS_DEFAULT_WINDOW_DAYS`
- `ANALYTICS_MAX_WINDOW_DAYS`
- `ANALYTICS_MAX_RESULT_LIMIT`
- `ANALYTICS_TRACKING_ENABLED`
- Optional Compose tooling values for MySQL, MinIO, and Mailpit

Use a strong JWT secret in every real environment:

```bash
openssl rand -base64 64
```

## Database Migrations

Flyway is enabled by default and loads SQL migrations from:

```text
src/main/resources/db/migration
```

The current project still keeps `JPA_DDL_AUTO=update` for local development while migrations are introduced incrementally. For production-style environments, prefer:

```properties
JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=true
```

Add new migrations with Flyway naming, for example:

```text
V2__create_calendar_lifecycle_tables.sql
```

## Testcontainers

The migration suite includes a MySQL Testcontainers check that runs Flyway against a real MySQL container. It automatically skips when Docker is unavailable.

```bash
mvn test
```

To run only the migration container test:

```bash
mvn -Dtest=FlywayMySqlMigrationTest test
```

## Dependency Vulnerability Scanning

OWASP Dependency-Check is configured as an opt-in Maven profile so normal builds stay fast:

```bash
mvn -Psecurity-scan verify
```

Reports are generated under:

```text
target/dependency-check
```

The security profile:

- Fails the build for CVSS `8.0` or higher.
- Produces `HTML`, `JSON`, and `SARIF` reports.
- Skips `provided` and `test` scopes to focus on shipped runtime dependencies.
- Uses `dependency-check-suppressions.xml` for reviewed false positives.

The first scan can take a long time because Dependency-Check downloads and processes NVD data. For CI, set an NVD API key if available:

```powershell
$env:NVD_API_KEY="your-nvd-api-key"
mvn -Psecurity-scan verify
```

Reference: OWASP Dependency-Check Maven usage docs note version `12.2.2`, Maven `3.6.3+`, NVD first-run cost, and the `check` goal.

## Continuous Integration

GitHub Actions is configured in `.github/workflows/ci.yml`.

The default CI runs on pushes and pull requests to `main`, `master`, and `develop`:

- Builds and tests the project with Java 25 and Maven cache
- Copies `.env.example` to `.env` for stable local-style test defaults
- Validates default Docker Compose configuration
- Validates the full `tooling` Docker Compose profile
- Checks Dockerfile build rules with `docker build --check`
- Uploads Surefire reports when tests fail

The workflow also includes a manual `workflow_dispatch` OWASP Dependency-Check job. Add a repository secret named `NVD_API_KEY` to speed up NVD vulnerability data access.

## Storage Backends

ShareNote uses the `NoteFileStorage` abstraction.

### Local Storage

```properties
NOTE_STORAGE_TYPE=local
NOTE_STORAGE_DIRECTORY=uploads/notes
```

### S3 Storage

```properties
NOTE_STORAGE_TYPE=s3
NOTE_STORAGE_S3_BUCKET=your-bucket-name
NOTE_STORAGE_S3_PREFIX=notes
NOTE_STORAGE_S3_REGION=us-east-1
```

AWS credentials are not stored in Spring properties. The AWS SDK uses its standard credential provider chain, such as:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN`
- `AWS_PROFILE`
- IAM role credentials in deployed environments

### MinIO Local S3 Testing

Docker Compose provides a local MinIO server behind the `s3` profile for testing S3 flows:

```bash
docker compose --profile s3 up -d minio minio-init
```

```text
http://localhost:9000  # S3 API
http://localhost:9001  # MinIO console
```

Default local credentials are:

```properties
MINIO_ROOT_USER=sharenote
MINIO_ROOT_PASSWORD=sharenote-dev-secret
```

To test note and profile-picture uploads through MinIO while running the app on your host machine:

```properties
NOTE_STORAGE_TYPE=s3
NOTE_STORAGE_S3_BUCKET=sharenote-notes
NOTE_STORAGE_S3_ENDPOINT=http://localhost:9000
NOTE_STORAGE_S3_PATH_STYLE_ACCESS=true
NOTE_STORAGE_S3_ACCESS_KEY=sharenote
NOTE_STORAGE_S3_SECRET_KEY=sharenote-dev-secret

PROFILE_PICTURE_STORAGE_TYPE=s3
PROFILE_PICTURE_STORAGE_S3_BUCKET=sharenote-profile-pictures
PROFILE_PICTURE_STORAGE_S3_ENDPOINT=http://localhost:9000
PROFILE_PICTURE_STORAGE_S3_PATH_STYLE_ACCESS=true
PROFILE_PICTURE_STORAGE_S3_ACCESS_KEY=sharenote
PROFILE_PICTURE_STORAGE_S3_SECRET_KEY=sharenote-dev-secret
```

When running the app inside Docker Compose, use `http://minio:9000` for the S3 endpoint instead.

## Running Locally

Install dependencies and start the application:

```bash
mvn spring-boot:run
```

The app uses H2 by default in local development.

## OpenAPI / Swagger UI

OpenAPI documentation is available when the app is running:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs.yaml
```

Swagger UI includes a JWT bearer authentication scheme. After login, copy the access token and use the **Authorize** button with:

```text
Bearer <access-token>
```

Docs are enabled by default for local development. Disable them in deployed environments with:

```properties
OPENAPI_DOCS_ENABLED=false
SWAGGER_UI_ENABLED=false
```

The documented REST server uses the configured API base path:

```properties
API_BASE_PATH=/api/v1
```

The test suite includes OpenAPI contract checks for the versioned server metadata, JWT bearer-auth scheme, and public search route mapping.

## API Versioning

Application REST controllers are prefixed with `/api/v1` by default. Change the prefix with:

```properties
API_BASE_PATH=/api/v2
```

Keep auth callbacks and client URLs aligned with this value because verification links use the same prefix.

## Observability

Spring Boot Actuator, Micrometer, Prometheus, and OpenTelemetry are configured for local and production-style monitoring.

Default public endpoints:

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/info
http://localhost:8080/actuator/prometheus
```

Metrics are available through Actuator and Prometheus by default. OTLP metrics and traces are disabled by default so local development does not fail when no collector is running.

To enable OTLP while running the app on your host machine:

```properties
OTLP_METRICS_ENABLED=true
OTLP_METRICS_URL=http://localhost:4318/v1/metrics
OTEL_TRACING_ENABLED=true
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://localhost:4318/v1/traces
OTEL_TRACES_SAMPLING_PROBABILITY=1.0
```

When running the API container with the observability profile, use the collector service name:

```properties
OTLP_METRICS_ENABLED=true
OTLP_METRICS_URL=http://otel-collector:4318/v1/metrics
OTEL_TRACING_ENABLED=true
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://otel-collector:4318/v1/traces
```

## Centralized Logging

The application writes ECS JSON logs to:

```properties
LOG_FILE=logs/sharenote.log
LOGGING_STRUCTURED_FILE_FORMAT=ecs
LOGGING_STRUCTURED_CONSOLE_FORMAT=ecs
```

`ContextLoggingFilter` adds request correlation fields to MDC, including `trace_id`, `tenant_id`, `user_id`, and `client_ip`. Keep logs free of secrets, JWTs, passwords, API keys, and sensitive file contents.

Docker Compose includes a `logging` profile that runs Filebeat, tails the shared `sharenote_logs` volume, and ships ECS JSON log events into Elasticsearch:

```bash
docker compose --profile search --profile logging up -d elasticsearch kibana filebeat
docker compose up --build sharenote-api
```

Filebeat writes daily indices using:

```properties
FILEBEAT_INDEX_NAME=sharenote-ecs
```

In Kibana, create a data view for:

```text
sharenote-ecs-*
```

## Problem Details Errors

Errors are returned as `application/problem+json` using RFC 9457-style fields:

```json
{
  "type": "https://sharenote.local/problems/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/users/register",
  "timestamp": "2026-06-08T12:00:00Z",
  "validationErrors": {
    "email": "must be a well-formed email address"
  }
}
```

## Building the Jar

Build and test the project:

```bash
mvn package
```

The executable Spring Boot jar is created at:

```text
target/sharenote-0.0.1-SNAPSHOT.jar
```

Run it with:

```bash
java -jar target/sharenote-0.0.1-SNAPSHOT.jar
```

## Docker

The project includes a multi-stage Docker build:

- Maven build image for compiling the jar
- Java 25 JRE runtime image with a small runtime package set
- Fixed non-root application user
- Container-aware JVM memory defaults
- Actuator readiness healthcheck
- Named Docker volume for local note uploads

Build the image:

```bash
docker build -t sharenote-api .
```

Run with Docker Compose:

```bash
docker compose up --build
```

The compose file reads environment values from `.env` and maps the API to:

```text
http://localhost:8080
```

To change the host port without changing the application port, set:

```properties
APP_PORT=9090
SERVER_PORT=8080
```

Then run:

```bash
docker compose up --build
```

For local file storage, uploaded notes are persisted in the `sharenote_uploads` Docker volume.

The image healthcheck calls:

```text
http://localhost:${SERVER_PORT:-8080}/actuator/health/readiness
```

Keep `management.endpoint.health.probes.enabled=true` enabled for container health reporting.

### Docker Compose Profiles

The default Compose command starts only the API:

```bash
docker compose up --build
```

Optional professional backend tooling is grouped into profiles:

```bash
docker compose --profile database up -d mysql
docker compose --profile s3 up -d minio minio-init
docker compose --profile mail up -d mailpit
docker compose --profile messaging up -d kafka mailpit
docker compose --profile cache up -d redis
docker compose --profile search up -d elasticsearch kibana
docker compose --profile search --profile logging up -d elasticsearch kibana filebeat
docker compose --profile observability up -d otel-collector prometheus grafana
docker compose --profile tooling up -d
```

Profile summary:

- `database`: starts MySQL for production-like persistence testing.
- `s3`: starts MinIO and creates `sharenote-notes` plus `sharenote-profile-pictures` buckets.
- `mail`: starts Mailpit for local SMTP capture at `http://localhost:8025`.
- `messaging`: starts Kafka in single-node KRaft mode and Mailpit for asynchronous verification email testing.
- `cache`: starts Redis with append-only local persistence.
- `search`: starts Elasticsearch and Kibana for note search indexing and query testing.
- `logging`: starts Filebeat for ECS JSON log shipping into Elasticsearch.
- `observability`: starts OpenTelemetry Collector, Prometheus, and Grafana.
- `tooling`: starts all optional local backend tooling together.

Dependency ordering:

- `sharenote-api` has soft `depends_on` links to MySQL, MinIO, Mailpit, Kafka, Redis, Elasticsearch, and OpenTelemetry Collector. These are marked `required: false` so the default H2/local API startup still works without optional profiles.
- `minio-init` waits for healthy MinIO before creating buckets.
- `kibana` waits for healthy Elasticsearch.
- `filebeat` waits for Elasticsearch and Kibana before shipping ECS logs.
- `prometheus` waits for OpenTelemetry Collector, and `grafana` waits for healthy Prometheus.

All Compose services join the explicit `sharenote_backend` bridge network. Inside Docker, services should talk to each other by Compose service name, for example `mysql:3306`, `redis:6379`, `elasticsearch:9200`, `kibana:5601`, `minio:9000`, `mailpit:1025`, `kafka:29092`, `filebeat`, and `otel-collector:4318`.

To use MySQL while running the app on your host machine:

```properties
DB_URL=jdbc:mysql://localhost:3306/sharenote?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_DRIVER=com.mysql.cj.jdbc.Driver
DB_USERNAME=sharenote
DB_PASSWORD=sharenote-dev-secret
```

To use Mailpit while running the app on your host machine:

```properties
EMAIL_VERIFICATION_DELIVERY=kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_AUTH=false
SMTP_STARTTLS_ENABLE=false
```

Start both services with:

```bash
docker compose --profile messaging up -d kafka mailpit
```

Verification requests are published only after the user and hashed token transaction commits. SMTP failures are retried by the Kafka consumer and then routed to `sharenote.email-verification.dlt`. If Kafka publication itself fails, the application attempts direct SMTP delivery without rolling back registration.

To use Redis while running the app on your host machine:

```properties
CACHE_TYPE=redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_HEALTH_ENABLED=true
```

To use Elasticsearch while running the app on your host machine:

```properties
ELASTICSEARCH_URIS=http://localhost:9200
ELASTICSEARCH_HEALTH_ENABLED=true
NOTE_SEARCH_INDEX_NAME=sharenote-notes
```

Search visible notes through the versioned API:

```http
GET /api/v1/notes/search?q=calculus&limit=20
Authorization: Bearer <access-token>
```

Search writes are indexed after note upload and new-version approval. If Elasticsearch is down or the circuit breaker is open, the search service falls back to the Hibernate Criteria query path.

Analytics events are persisted for note previews, download preparation, and search queries. Tune the dashboard window and result caps with:

```properties
ANALYTICS_DEFAULT_WINDOW_DAYS=7
ANALYTICS_MAX_WINDOW_DAYS=90
ANALYTICS_MAX_RESULT_LIMIT=50
ANALYTICS_TRACKING_ENABLED=true
```

Current users can inspect traffic on their uploaded notes:

```http
GET /api/v1/analytics/me/notes?days=7
Authorization: Bearer <access-token>
```

Admins can inspect global traffic, subject trends, failed-login spike alerts, and search trends:

```http
GET /api/v1/admin/analytics/overview?days=7
GET /api/v1/admin/analytics/subjects?days=7&limit=10
GET /api/v1/admin/analytics/security/failed-logins/spike?days=7
GET /api/v1/admin/analytics/searches?days=7&limit=10
Authorization: Bearer <admin-access-token>
```

Tune the search circuit breaker and retry policy with:

```properties
SEARCH_RESILIENCE_FAILURE_RATE_THRESHOLD=50
SEARCH_RESILIENCE_SLIDING_WINDOW_SIZE=10
SEARCH_RESILIENCE_MINIMUM_CALLS=5
SEARCH_RESILIENCE_OPEN_STATE_DURATION=30s
SEARCH_RESILIENCE_RETRY_MAX_ATTEMPTS=2
SEARCH_RESILIENCE_RETRY_WAIT_DURATION=200ms
```

Observability tooling URLs:

```text
http://localhost:9090  # Prometheus
http://localhost:3000  # Grafana
http://localhost:4318  # OpenTelemetry Collector OTLP HTTP
http://localhost:5601  # Kibana
```

Grafana is pre-provisioned with:

- A `Prometheus` datasource pointing to `http://prometheus:9090`.
- A `ShareNote Overview` dashboard with HTTP throughput, latency, 5xx errors, JVM heap, and CPU panels.

When running the API container together with profile services, use Compose service names such as `mysql`, `redis`, `elasticsearch`, `kibana`, `minio`, `mailpit`, `kafka`, `filebeat`, and `otel-collector` instead of `localhost` inside app connection URLs.

## Running Tests

```bash
mvn test
```

## API Overview

### Register User

```http
POST /api/v1/users/register
Content-Type: application/json
```

```json
{
  "firstName": "Amina",
  "middleName": null,
  "lastName": "Rahman",
  "email": "amina@example.com",
  "password": "StrongPass123",
  "institution": "university",
  "degreeProgram": "Computer Science",
  "currentSemesterOrYear": "3",
  "currentYear": "2026",
  "currentSemester": "3",
  "phoneNumber": "+491234567890",
  "country": "Germany"
}
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "amina@example.com",
  "password": "StrongPass123"
}
```

Returns an access token and refresh token.

### Setup Profile Picture

```http
POST /api/v1/users/me/profile-picture
Authorization: Bearer <access-token>
Content-Type: multipart/form-data
```

Multipart fields:

- `file` image only: jpg, jpeg, png, gif, or webp

Returns the updated user DTO with profile-picture metadata.

### Refresh Token

```http
POST /api/v1/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "refresh-token-value"
}
```

Refresh tokens are rotated on use.

### Upload Note

```http
POST /api/v1/notes/upload
Authorization: Bearer <access-token>
Content-Type: multipart/form-data
```

Multipart fields:

- `file`
- `subjectClass`
- `semester`
- `year`

### List Visible Notes

```http
GET /api/v1/notes?subjectClass=Mathematics&semester=3&year=2026
Authorization: Bearer <access-token>
```

Returns notes matching the current user's institution and degree program plus the requested subject/class, semester, and year.

### Academic Navigation

```http
GET /api/v1/academic/degree-programs
Authorization: Bearer <access-token>
```

```http
GET /api/v1/academic/years?degreeProgram=Computer%20Science
Authorization: Bearer <access-token>
```

```http
GET /api/v1/academic/semesters?degreeProgram=Computer%20Science&year=2026
Authorization: Bearer <access-token>
```

```http
GET /api/v1/academic/subjects?degreeProgram=Computer%20Science&year=2026&semester=3
Authorization: Bearer <access-token>
```

```http
GET /api/v1/academic/classes/me
Authorization: Bearer <access-token>
```

Users are automatically registered to a class when their institution, degree program, current year, and current semester match a note's class key.

### Add Comment

```http
POST /api/v1/notes/{noteId}/comments
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "content": "This explanation helped me understand the topic."
}
```

### Reply To Comment

```http
POST /api/v1/notes/{noteId}/comments/{commentId}/replies
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "content": "Same here, especially section 2."
}
```

### List Comments

```http
GET /api/v1/notes/{noteId}/comments
Authorization: Bearer <access-token>
```

Returns top-level comments with their replies.

### Upvote Note

```http
POST /api/v1/notes/{noteId}/upvotes
Authorization: Bearer <access-token>
```

Creates a like for the current user if one does not already exist, then returns the note's upvote count.

### Suggest Take A Look

```http
POST /api/v1/notes/{noteId}/take-a-look
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "recipientUserIds": [2, 3],
  "message": "This may help with our assignment."
}
```

### My Take A Look Suggestions

```http
GET /api/v1/notes/take-a-look
Authorization: Bearer <access-token>
```

Returns notes other users suggested for the current user to review.

### List Notifications

```http
GET /api/v1/notifications?unreadOnly=true
Authorization: Bearer <access-token>
```

Returns notifications for the current user. Omit `unreadOnly` to include read notifications.

### Notification Summary

```http
GET /api/v1/notifications/summary
Authorization: Bearer <access-token>
```

Returns the current user's unread notification count.

### Mark Notification Read

```http
PATCH /api/v1/notifications/{notificationId}/read
Authorization: Bearer <access-token>
```

### Mark All Notifications Read

```http
PATCH /api/v1/notifications/read-all
Authorization: Bearer <access-token>
```

### Create Exam Reminder

```http
POST /api/v1/lifecycle/exam-reminders
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "subjectClass": "Mathematics",
  "semester": "3",
  "year": "2026",
  "examDate": "2026-06-30",
  "details": "Mid-term exam"
}
```

Only one active reminder is allowed for the same institution, degree program, year, semester, and subject/class. If one classmate already activated it, another eligible classmate must reschedule the existing reminder instead of creating a duplicate.

### Reschedule Exam Reminder

```http
PATCH /api/v1/lifecycle/exam-reminders/{reminderId}/reschedule
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "examDate": "2026-07-02",
  "details": "Moved to lecture hall B"
}
```

Quartz scans daily by default using `EXAM_REMINDER_CRON` and sends alerts `EXAM_REMINDER_DAYS_BEFORE` days before the exam. The alert includes the top three upvoted notes uploaded in the previous seven days when available.

### Archive Class

```http
POST /api/v1/lifecycle/archived-classes
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "subjectClass": "Mathematics",
  "semester": "3",
  "year": "2026"
}
```

Use `GET /api/v1/lifecycle/archived-classes` to list archived dashboard classes and `DELETE /api/v1/lifecycle/archived-classes/{archivedClassId}` to restore one.

### Admin Note Retention Jobs

```http
GET /api/v1/admin/note-retention/jobs
Authorization: Bearer <admin-access-token>
```

Returns the Quartz jobs that scan old notes, send one-month warnings, and automatically remove due notes.

### Admin Note Retention Candidates

```http
GET /api/v1/admin/note-retention/candidates
Authorization: Bearer <admin-access-token>
```

Returns upcoming automatic removal candidates for observation. Notes are scheduled after `NOTE_RETENTION_YEARS`, warned `NOTE_RETENTION_NOTICE_LEAD_DAYS` before removal, and removed automatically when `NOTE_RETENTION_AUTO_DELETE_ENABLED=true`.

```http
PATCH /api/v1/admin/note-retention/candidates/{candidateId}/cancel
Authorization: Bearer <admin-access-token>
Content-Type: application/json
```

```json
{
  "reason": "Course material is still required for accreditation review."
}
```

Cancellation is kept for future emergency intervention. There is intentionally no manual delete or extension endpoint.

### Admin List Users

```http
GET /api/v1/admin/users
Authorization: Bearer <admin-access-token>
```

### Temporary Ban User

```http
PATCH /api/v1/admin/users/{userId}/ban-temporary
Authorization: Bearer <admin-access-token>
Content-Type: application/json
```

```json
{
  "durationDays": 7,
  "reason": "Uploaded policy-violating content",
  "notice": "Your account is paused for review."
}
```

Repeated temporary bans escalate to a permanent ban after repeated policy violations.

### Permanent Ban User

```http
PATCH /api/v1/admin/users/{userId}/ban-permanent
Authorization: Bearer <admin-access-token>
Content-Type: application/json
```

```json
{
  "reason": "Repeated policy violations",
  "notice": "Your account has been permanently banned."
}
```

### Unban User

```http
PATCH /api/v1/admin/users/{userId}/unban
Authorization: Bearer <admin-access-token>
Content-Type: application/json
```

```json
{
  "notice": "Your appeal was accepted."
}
```

### Audit Events

```http
GET /api/v1/admin/audit-events
Authorization: Bearer <admin-access-token>
```

Optional filters:

- `action`
- `actorUserId`
- `targetType`
- `targetId`

## Security Notes

- Passwords are hashed with BCrypt.
- Access tokens include user ID and roles.
- Endpoints are stateless; HTTP sessions are not used.
- All endpoints are protected except `/api/v1/auth/**` and `POST /api/v1/users/register`.
- Admin endpoints under `/api/v1/admin/**` require `ROLE_ADMIN`.
- Temporarily or permanently banned users cannot log in, refresh tokens, or use JWT-protected endpoints.
- Uploaded files are validated by extension, content type, size, and basic file signature checks.
- Internal storage paths and stored filenames are not exposed in note listing responses.

## Development Notes

- Use DTOs for API requests and responses.
- Keep secrets in environment variables or `.env`, never in committed configuration.
- Use `mvn test` before packaging or deploying changes.
