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

* [x] AUTH-004  Deep Engineering & Security Upgrades

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

* [x] USER-006 User Dark Mode Preference: 

  * Save UI theme preferences in the USER schema.

* [x] USER-007 User Email Verification

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

* [x] NOTE-09 Download note

  * Register users will be able to download note
  * User will be able to download all notes or selected notes

* [x] NOTE-010 Recently uploaded note

  * User only able to see list that are related to their course and subject/class.
    * file name
    * upload date and time
    * who uploaded it
    * course subject name

* [x] NOTE-011 AI capability

  * user will be able to hook any AI model to play with the note

* [x] NOTE-012 Collaborative Study Groups

  * Allow users within the same institution/degree program to form virtual study groups. They can pool specific notes into a shared "Notebook" or "Deck."
  * Note Version Control & Co-Authoring: If a note has a typo or missing info, allow other verified classmates to propose edits or upload a "V2" of the note, creating a wiki-like environment for classroom topics.

* [x] NOTE-013 Anonymous Upload Toggle

  * Students are sometimes shy about sharing their notes if they fear they aren't perfect. Allow an "Upload Anonymously" checkbox while maintaining the backend userId link for moderation/rewards.

* [x] NOTE-014 Note Preview Generation: 

  * Instead of downloading a whole 10MB PDF blindly, use a library (like PDFBox or a frontend viewer) to generate a watermarked or limited 2-page preview of the note before downloading.

* [x] NOTE-015  Calendar & Academic Lifecycles

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

* [x] ADMIN-003 Notes auto removal

  * Notes related to obsolite course subject/class will automatically get removed after 5 years
  * issue a notice to the user who uploaded the note one month earlier before auto remove with proper link to the exact note so that user can click on the link and go directly to the note and take nessecery actions.
  * implement a robust scheduling framework like Quartz Scheduler. Add an admin capability to monitor these upcoming cron jobs and email queues.

* [x] ADMIN-004 System analytics

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

---

## 🚀 FEATURE EXTENSION BACKLOG

* [x] EXT-010 Study Group Activity Feed

  * Track study group events such as note added, edit proposed, comment added, exam date posted, and notebook updated
  * Show activity only to authorized study group members
  * Keep feed pagination efficient for large groups
  * Consider Redis for hot recent-feed caching if it improves performance
  * Persist audit-relevant group actions where appropriate

* [x] EXT-004 Personal Study Dashboard

  * Show upcoming exams, recommended notes, unread suggestions, recent notifications, and active study groups
  * Personalize dashboard content using the user's registered classes and academic lifecycle data
  * Include top notes for the user's current classes using downloads, views, freshness, and upvotes
  * Keep all dashboard endpoints behind the configured `API_BASE_PATH`
  * Add DTOs and OpenAPI documentation without exposing entities directly

* [x] EXT-006 Report And Flag System

  * Allow authenticated users to report duplicate, unsafe, copyrighted, irrelevant, or low-quality notes
  * Prevent duplicate active reports from the same user for the same note and reason
  * Notify admins or expose admin moderation queues for unresolved reports
  * Persist audit events for report creation and moderation decisions
  * Add validation, OpenAPI documentation, and service tests
  
* [x] EXT-009 Personal Collections And Playlists

  * Let users organize notes into private collections such as Midterm Prep, Week 3 Lectures, or Final Review
  * Allow adding and removing visible notes from personal collections
  * Keep collection ownership private unless a later task adds sharing
  * Return collection summaries and ordered note lists through DTOs
  * Add validation and tests for collection access control
  
* [x] EXT-005 Note Quality Scoring

  * Score notes using upvotes, downloads, views, preview availability, freshness, version history, and moderation status
  * Use scores to rank recommendations, dashboard sections, and search results
  * Keep scoring explainable through non-sensitive response fields where appropriate
  * Recalculate scores efficiently through scheduled jobs, events, or cached hot reads
  * Document Redis usage if caching score data improves performance

* [ ] EXT-001 File Text Extraction

  * Extract searchable text from supported files such as PDF, DOCX, PPTX, XLSX, and images where practical
  * Store extracted text separately from original files and never modify uploaded content
  * Use extracted text to improve search, AI chat, summaries, previews, and moderation
  * Keep extraction resilient so failed parsing does not break note upload
  * Document configuration and processing limits in `.env.example`

* [ ] EXT-002 Semantic Note Search

  * Add meaning-based search using embeddings or another semantic retrieval strategy
  * Combine semantic results with Elasticsearch keyword search where useful
  * Keep a safe database fallback when semantic or Elasticsearch infrastructure is unavailable
  * Restrict results to the current user's institution, degree program, subject/class, semester, and year visibility rules
  * Add OpenAPI documentation and tests for the search contract

* [ ] EXT-003 Smart Note Summaries

  * Generate concise summaries, key terms, formulas, and likely exam topics for uploaded notes
  * Store AI-generated summaries separately from note files and extracted text
  * Allow regeneration when a note version changes
  * Protect AI calls with rate limiting, validation, and resilience fallback behavior
  * Avoid logging note contents, prompts, provider secrets, or generated sensitive content

* [ ] EXT-007 Instructor And Verified Contributor Roles

  * Add trusted roles such as `ROLE_INSTRUCTOR` and `ROLE_VERIFIED_CONTRIBUTOR`
  * Allow admins to grant and revoke trusted contributor status
  * Highlight trusted uploads in note responses and ranking logic
  * Enforce role changes in JWT-protected requests without using sessions
  * Add audit events for role changes

* [ ] EXT-008 Note Version Diff

  * Allow users to compare note versions and edit proposals
  * Return metadata differences, summary differences, extracted-text differences where available, and changed file details
  * Keep access control aligned with note visibility rules
  * Avoid exposing raw file contents unless explicitly allowed by an endpoint
  * Add OpenAPI documentation and tests for diff responses


