# AGENTS.md

## 🎯 Objective

You are responsible for implementing a Spring Boot backend system with:

* JWT-based stateless authentication
* User management
* File upload and access control
* proper logging with Slf4j ecs format
* API documentation through OpenAPI / Swagger UI
* API versioning for maintainable REST evolution
* Redis and Elasticsearch where they improve performance, scalability, or maintainability

Follow `tasks.md` strictly.

---

## ⚙️ Execution Rules

1. Always read `tasks.md` before starting work
2. Pick the **first unchecked task**
3. Complete tasks **one at a time**
4. After completing a task:

   * Mark it as `[x]` in `tasks.md`
   * Move to the next task
5. Strictly ignore the `bin/` folder:

   * Do not read, edit, copy, delete, format, test, or rely on anything inside `bin/`
   * Treat `bin/` as unnecessary generated/legacy content
   * Apply changes only to the real project files outside `bin/`

---

## 🧱 Architecture Guidelines

* Use Spring Boot + Spring Security
* Use Lombok to reduce boilerplate where it improves readability
* Keep OpenAPI documentation available and accurate for all REST endpoints
* Keep REST endpoints under the configured API version prefix, `/api/v1` by default
* Use Hibernate Programmatic Criteria Queries (Jakarta Persistence API ) for persistence, implement HQL if Programmatic  
  Criteria Queries is not possible to use.
* Follow layered architecture:

  * Controller
  * Service
  * Repository
* Use DTOs (do NOT expose entities directly)
* Use Redis when caching, rate limiting, refresh-token/session-adjacent state, or short-lived distributed coordination improves efficiency
* Use Elasticsearch for full-text search, indexed discovery, analytics search, and large-result navigation where database Criteria queries would become inefficient
* Keep a safe database fallback for Elasticsearch-backed user-facing search whenever practical

---

## 🔐 Security Rules

* Never store plain passwords → use BCrypt
* JWT must be:

  * signed securely
  * include userId + roles
  * validated on every request
* Do NOT use HTTP sessions (stateless only)

---

## 📦 File Upload Rules

* Only allow:

  * image, pdf, doc, docx, xls, xlsx, ppt, pptx
* Reject:

  * executable or suspicious files
* Enforce file size limits
* Ensure:

  * DB save + file save must both succeed
  * otherwise rollback

---

## 🧪 Code Quality Rules

* Write clean and readable code
* Use meaningful class and method names
* Write comment above every method explaining what it does
* Add comment above every method explaining what it does
* Add a short comment above complex code blocks explaining why the logic exists, not just what the syntax does
* Prefer Lombok annotations such as `@Getter`, `@RequiredArgsConstructor`, `@Builder`, and `@Slf4j` where appropriate
* Use Lombok `@Slf4j` for logging whenever logging is necessary; do not manually create SLF4J logger fields unless there is a specific reason
* Keep logs compatible with ECS structured logging and the MDC values populated by `ContextLoggingFilter`
* Include useful contextual values in log messages without logging secrets, tokens, passwords, or sensitive file contents
* Avoid broad `@Data` on JPA entities; prefer explicit Lombok annotations to prevent lazy-loading, recursion, or equality issues
* Use `@NoArgsConstructor(access = AccessLevel.PROTECTED)` for JPA entities that need a no-args constructor
* Add comments where necessary
* Handle exceptions properly
* Avoid duplication
* Add log where necessary

---

## ⚡ Performance & Maintainability Rules

* Consider Redis for hot-read caching, idempotency keys, rate-limit buckets, token rotation metadata, and other short-lived state
* Consider Elasticsearch for user-facing full-text search, admin search, search analytics, and scalable note discovery
* Use Resilience4j or equivalent protective patterns around external systems such as Elasticsearch, Redis, S3, email, and AI providers when failures should not cascade
* Prefer graceful degradation for optional infrastructure; for example, use Hibernate Criteria fallback when Elasticsearch is unavailable
* Keep infrastructure configuration environment-driven and documented in `.env.example`

---

## 📖 OpenAPI Rules

* Maintain Springdoc OpenAPI / Swagger UI configuration when adding or changing endpoints
* Ensure secured endpoints are represented with JWT bearer authentication in OpenAPI
* Permit `/v3/api-docs/**`, `/v3/api-docs`, `/swagger-ui.html`, and `/swagger-ui/**` through Spring Security
* Document request/response DTOs clearly through validation annotations and meaningful DTO names
* Keep Swagger UI usable locally at `/swagger-ui.html`
* Add or update OpenAPI/contract tests when endpoint paths, auth requirements, versioning, or public API DTOs change

---

## 🧭 API Versioning Rules

* Keep application REST controllers behind the configured `API_BASE_PATH`
* Default to `/api/v1` unless the task explicitly requires a new version
* Keep generated links, email callbacks, OpenAPI server URLs, tests, and client-facing documentation aligned with `API_BASE_PATH`
* Do not hardcode versioned URLs in business logic when an existing configurable API base path can be used

---

## 🚫 Forbidden Actions

* Do not skip tasks
* Do not implement multiple tasks at once
* Do not ignore validation or security requirements
* Do not hardcode sensitive data
* Do not make any changes inside the `bin/` folder

---

## ✅ Definition of Done

A task is complete when:

* All requirements in `tasks.md` are implemented
* Code compiles and runs
* OpenAPI docs remain accessible and reflect new or changed REST endpoints
* Every method added or changed has a short explanatory comment above it
* Basic validation and error handling are included

---

## 🔄 Iteration Strategy

For each task:

1. Design classes and interfaces
2. Implement backend logic
3. Add necessary configurations
4. Verify functionality
5. Mark task complete

---

## 📌 Notes

* Prefer simplicity over over-engineering
* Keep the system extensible
* Follow best practices of Spring ecosystem
