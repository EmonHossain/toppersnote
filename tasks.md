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

* [] AUTH-004  Deep Engineering & Security Upgrades

  * Data De-duplication Engine: If 10 students upload the exact same syllabus PDF or professor's slide deck, storing it 10 times wastes disk space/S3 costs. Implement a hashing check (e.g., SHA-256) on the file content before saving. If the hash exists, point the metadata to the existing file rather than duplicating storage.
  * Pre-Signed URLs for Cloud Storage: Instead of streaming files through your Spring Boot server (which eats up server memory and bandwidth), have your backend generate a temporary, secure AWS S3 Pre-Signed URL. The user's browser downloads the 10MB file directly from the cloud bucket safely.
  * Rate Limiting & DDoS Protection: Protect your /auth/ and AI endpoints from abuse. Implement Bucket4j rate limiter to ensure a malicious user can't spam your AI hook or attempt credential stuffing attacks on your login endpoint.

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
    * degreeProgram
    * currentSemesterOrYear
    * currentYear
    * currentSemester
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

* [] USER-006 User Dark Mode Preference: 

  * Save UI theme preferences in the USER schema.

* [] USER-007 User Email Verification

  * A step between registration and active login where a user must verify their university email (.edu or institution domain) to unlock upload/download privileges.

---

## 📝 NOTE MODULE

* [x] NOTE-001 Upload Note

  * Allow file upload:

    * image, pdf, word, excel, ppt
  * Associate note with:

    * subject/class
    * semester
    * year
    * uploadedBy (userId)

* [x] NOTE-002 File Validation & Storage

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

* [x] NOTE-03 Note Visibility

  * Notes visible only to users with:

    * same institution
    * same degree program
    * same subject/class
    * same semester
    * same year
  * Implement query filtering at service layer

* [x] NOTE-04 Note Comments & Replies

  * Allow authenticated users to add comments to shared notes
  * Allow authenticated users to reply to top-level comments
  * Return comments with nested replies through DTOs

* [x] NOTE-05 Note Upvotes

  * Allow authenticated users to like helpful notes
  * Prevent duplicate likes from the same user
  * Do not implement dislikes

* [x] NOTE-06 Take A Look Suggestions

  * Allow authenticated users to suggest note review to selected users
  * Keep suggestions separate from comments, replies, and likes
  * Return received suggestions for the current user

* [x] NOTE-07 Notifications

  * Notify users when a new note is added
  * Notify mentioned users when someone suggests they take a look at a note
  * Allow users to list notifications and mark them as read

* [x] NOTE-08 Academic Class Registration & Navigation

  * Register users automatically for matching classes by institution, degree program, year, semester, and subject
  * Store note academic context using the uploader's institution and degree program
  * Allow users to navigate degree programs, years, semesters, subjects, and their registered classes

* [] NOTE-09 Download note

  * Register users will be able to download note
  * User will be able to download all notes or selected notes

* [] NOTE-010 Recently uploaded note

  * User only able to see list that are related to their course and subject/class.
    * file name
    * upload date and time
    * who uploaded it
    * course subject name

* [] NOTE-011 AI capability

  * user will be able to hook any AI model to play with the note

* [] NOTE-012 Collaborative Study Groups

  * Allow users within the same institution/degree program to form virtual study groups. They can pool specific notes into a shared "Notebook" or "Deck."
  * Note Version Control & Co-Authoring: If a note has a typo or missing info, allow other verified classmates to propose edits or upload a "V2" of the note, creating a wiki-like environment for classroom topics.

* [] NOTE-013 Anonymous Upload Toggle

  * Students are sometimes shy about sharing their notes if they fear they aren't perfect. Allow an "Upload Anonymously" checkbox while maintaining the backend userId link for moderation/rewards.

* [] NOTE-014 Note Preview Generation: 

  * Instead of downloading a whole 10MB PDF blindly, use a library (like PDFBox or a frontend viewer) to generate a watermarked or limited 2-page preview of the note before downloading.

* [] NOTE-015  Calendar & Academic Lifecycles

  * Exam Date Synchronization: Allow users to post exam dates for a specific subject/class. The system can aggregate these dates to send an automated system alert 7 days before the exam: "Your Calculus mid-term is in 7 days! Here are the top 3 upvoted notes from this week to help you prepare."
  * Semester Archiving: Give users an elegant way to archive their old classes at the end of a semester so their dashboard stays clean, while keeping the data accessible for the next batch of incoming students.

---

## ADMIN MODULE

* [x] ADMIN-001 Admin Moderation

  * Protect admin endpoints with `ROLE_ADMIN`
  * Allow admins to list users
  * Allow admins to temporarily ban users with a notice
  * Escalate repeated policy violations to permanent bans
  * Allow admins to permanently ban and unban users
  * Enforce bans during login, token refresh, and JWT-protected requests

* [x] ADMIN-002 Audit Trail

  * Persist audit events for authentication, user, note, notification, and moderation actions
  * Allow admins to query audit events
  * Include actor, target, action, message, metadata, and timestamp

* [] ADMIN-003 Notes auto removal

  * Notes related to obsolite course subject/class will automatically get removed after 5 years
  * issue a notice to the user who uploaded the note one month earlier before auto remove with proper link to the exact note so that user can click on the link and go directly to the note and take nessecery actions.
  * implement a robust scheduling framework like Quartz Scheduler. Add an admin capability to monitor these upcoming cron jobs and email queues.

* [] ADMIN-004 System analytics

    * Global Search with Elasticsearch
      * If the database grows large, standard JPA queries for navigating degree programs and subjects might slow down. Introducing Elasticsearch to make text search incredibly fast.
    * Analytics Dashboard for Admins/Users: 
      * For Users: Show them how many people downloaded or viewed their notes this week.
      * For Admins: Show trends on which subjects are currently generating the most traffic, or track spike alerts for failed login attempts (security analytics).

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
