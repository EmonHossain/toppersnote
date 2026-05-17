# Project Task List

## 🔐 AUTH MODULE

* [x] AUTH-001 JWT Authentication

  * Implement login-based JWT issuance
  * Use stateless authentication (no session)
  * Include userId and roles in token
  * Token expiration: short-lived (e.g., 15 min)

* [x] AUTH-002 Authorization Filter

  * Create Spring Security filter to:

    * Extract JWT from Authorization header
    * Validate token signature and expiration
    * Populate SecurityContext
  * Protect all endpoints except `/auth/**`

* [x] AUTH-003 Refresh Token

  * Implement refresh token mechanism
  * Store refresh tokens securely (DB or Redis optional)
  * Endpoint: `/auth/refresh`
  * Rotate refresh tokens on use

---

## 👤 USER MODULE

* [x] USER-004 User Registration

  * Fields:

    * firstName (required)
    * middleName (optional)
    * lastName (required)
    * email (unique, required)
    * password (hashed)
    * institution (school/college/university)
    * currentSemesterOrYear
    * phoneNumber
    * country
  * Use Hibernate (JPA) for persistence
  * Validate input (email format, password strength)
  * Encrypt password (BCrypt)

* [x] USER-005 User Login

  * Validate credentials against database
  * On success:

    * generate JWT (access token)
    * generate refresh token
  * Return tokens in response

---

## 📝 NOTE MODULE

* [x] NOTE-006 Upload Note

  * Allow file upload:

    * image, pdf, word, excel, ppt
  * Associate note with:

    * subject/class
    * semester
    * year
    * uploadedBy (userId)

* [x] NOTE-007 File Validation & Storage

  * Validate:

    * file type (whitelist)
    * file size limit (10 MB)
    * basic security checks (no executable content)
  * Save:

    * metadata in database
    * file in storage (local or cloud)
  * Ensure atomicity:

    * both DB + file storage must succeed
    * otherwise rollback

* [x] NOTE-008 Note Visibility

  * Notes visible only to users with:

    * same subject/class
    * same semester
    * same year
  * Implement query filtering at service layer

* [x] NOTE-009 Note Comments & Replies

  * Allow authenticated users to add comments to shared notes
  * Allow authenticated users to reply to top-level comments
  * Return comments with nested replies through DTOs

* [x] NOTE-010 Note Upvotes

  * Allow authenticated users to like helpful notes
  * Prevent duplicate likes from the same user
  * Do not implement dislikes

* [x] NOTE-011 Take A Look Suggestions

  * Allow authenticated users to suggest note review to selected users
  * Keep suggestions separate from comments, replies, and likes
  * Return received suggestions for the current user

* [x] NOTE-012 Notifications

  * Notify users when a new note is added
  * Notify mentioned users when someone suggests they take a look at a note
  * Allow users to list notifications and mark them as read

---

## ✅ GENERAL RULES

* Use RESTful API design
* Use DTOs for request/response
* Apply proper exception handling (global handler)
* Use layered architecture:

  * Controller → Service → Repository
* Write clean, modular, testable code
* Add unit tests for services
* Add integration tests for auth flow
* Add file storage abstraction (local vs S3)
