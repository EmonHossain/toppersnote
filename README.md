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
- Admin-only moderation endpoints
- Audit trail for authentication, content, notifications, and moderation events
- Temporary and permanent user bans with user-facing notices
- DTO-based API responses
- Global exception handling
- Unit tests for auth, user, note, and storage services

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- Spring Security
- Spring Data JPA / Hibernate
- H2 database for local development
- JJWT
- AWS SDK for Java v2 S3
- Maven

## Project Structure

```text
src/main/java/com/sharenote
  academic/   Academic classes, auto-registration, and navigation
  admin/      Admin moderation endpoints and DTOs
  audit/      Audit-event persistence and query logic
  auth/       Authentication, JWT, refresh-token logic
  common/     Shared error response and exception handling
  note/       Note upload, metadata, and visibility filtering
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
- `NOTE_STORAGE_TYPE`
- `NOTE_STORAGE_DIRECTORY`
- `NOTE_MAX_FILE_SIZE_BYTES`
- `PROFILE_PICTURE_STORAGE_TYPE`
- `PROFILE_PICTURE_STORAGE_DIRECTORY`
- `PROFILE_PICTURE_MAX_FILE_SIZE_BYTES`

Use a strong JWT secret in every real environment:

```bash
openssl rand -base64 64
```

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

## Running Locally

Install dependencies and start the application:

```bash
mvn spring-boot:run
```

The app uses H2 by default in local development.

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
- Java 17 JRE runtime image
- Non-root application user
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

## Running Tests

```bash
mvn test
```

## API Overview

### Register User

```http
POST /users/register
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
POST /auth/login
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
POST /users/me/profile-picture
Authorization: Bearer <access-token>
Content-Type: multipart/form-data
```

Multipart fields:

- `file` image only: jpg, jpeg, png, gif, or webp

Returns the updated user DTO with profile-picture metadata.

### Refresh Token

```http
POST /auth/refresh
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
POST /notes/upload
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
GET /notes?subjectClass=Mathematics&semester=3&year=2026
Authorization: Bearer <access-token>
```

Returns notes matching the current user's institution and degree program plus the requested subject/class, semester, and year.

### Academic Navigation

```http
GET /academic/degree-programs
Authorization: Bearer <access-token>
```

```http
GET /academic/years?degreeProgram=Computer%20Science
Authorization: Bearer <access-token>
```

```http
GET /academic/semesters?degreeProgram=Computer%20Science&year=2026
Authorization: Bearer <access-token>
```

```http
GET /academic/subjects?degreeProgram=Computer%20Science&year=2026&semester=3
Authorization: Bearer <access-token>
```

```http
GET /academic/classes/me
Authorization: Bearer <access-token>
```

Users are automatically registered to a class when their institution, degree program, current year, and current semester match a note's class key.

### Add Comment

```http
POST /notes/{noteId}/comments
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
POST /notes/{noteId}/comments/{commentId}/replies
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
GET /notes/{noteId}/comments
Authorization: Bearer <access-token>
```

Returns top-level comments with their replies.

### Upvote Note

```http
POST /notes/{noteId}/upvotes
Authorization: Bearer <access-token>
```

Creates a like for the current user if one does not already exist, then returns the note's upvote count.

### Suggest Take A Look

```http
POST /notes/{noteId}/take-a-look
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
GET /notes/take-a-look
Authorization: Bearer <access-token>
```

Returns notes other users suggested for the current user to review.

### List Notifications

```http
GET /notifications?unreadOnly=true
Authorization: Bearer <access-token>
```

Returns notifications for the current user. Omit `unreadOnly` to include read notifications.

### Notification Summary

```http
GET /notifications/summary
Authorization: Bearer <access-token>
```

Returns the current user's unread notification count.

### Mark Notification Read

```http
PATCH /notifications/{notificationId}/read
Authorization: Bearer <access-token>
```

### Mark All Notifications Read

```http
PATCH /notifications/read-all
Authorization: Bearer <access-token>
```

### Admin List Users

```http
GET /admin/users
Authorization: Bearer <admin-access-token>
```

### Temporary Ban User

```http
PATCH /admin/users/{userId}/ban-temporary
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
PATCH /admin/users/{userId}/ban-permanent
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
PATCH /admin/users/{userId}/unban
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
GET /admin/audit-events
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
- All endpoints are protected except `/auth/**` and `POST /users/register`.
- Admin endpoints under `/admin/**` require `ROLE_ADMIN`.
- Temporarily or permanently banned users cannot log in, refresh tokens, or use JWT-protected endpoints.
- Uploaded files are validated by extension, content type, size, and basic file signature checks.
- Internal storage paths and stored filenames are not exposed in note listing responses.

## Development Notes

- Use DTOs for API requests and responses.
- Keep secrets in environment variables or `.env`, never in committed configuration.
- Use `mvn test` before packaging or deploying changes.
