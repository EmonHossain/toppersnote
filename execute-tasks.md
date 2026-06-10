Read **AGENTS.md** and **tasks.md** before doing anything.

Strictly ignore the `bin/` folder. Do not read, edit, copy, delete, format, test, or rely on anything inside `bin/`; it is unnecessary generated/legacy content.

Follow this strict workflow:

### Phase 1: Planning (MANDATORY)

1. Identify the **first unchecked task** in tasks.md

2. Analyze the task in detail

3. Create a **step-by-step implementation plan**, including:

   * required classes (Controller, Service, Repository, Config, etc.)
   * API endpoints (request/response structure)
   * data models and DTOs
   * security considerations
   * validation rules
   * Lombok annotations to reduce boilerplate safely
   * OpenAPI documentation updates, including JWT/security requirements
   * API versioning impact, including `API_BASE_PATH`, generated links, callbacks, and docs
   * whether Redis is useful for caching, rate limiting, token metadata, idempotency, or short-lived state
   * whether Elasticsearch is useful for full-text search, indexed discovery, analytics search, or large-result navigation
   * resilience/fallback behavior for optional infrastructure such as Redis, Elasticsearch, S3, email, and AI providers
   * comments needed above methods or complex code blocks for maintainability
   * dependencies and configuration changes

4. Clearly explain:

   * how the components interact
   * where each responsibility lies
   * any assumptions made

5. **STOP after planning**

   * Do NOT write any code yet
   * Wait for approval before proceeding

---

### Phase 2: Implementation (ONLY AFTER APPROVAL)

1. Implement the approved plan step by step
2. Follow:

   * clean architecture (Controller → Service → Repository)
   * all rules defined in AGENTS.md
3. Ensure:

   * secure implementation (JWT, validation, file safety, etc.)
   * clean, maintainable, production-ready code
   * logging with Lombok `@Slf4j` where appropriate
   * ECS-compatible SLF4J logs that work with MDC values from `ContextLoggingFilter`
   * No logging of secrets, JWTs, passwords, API keys, or sensitive file contents
   * Use Lombok whenever possible to reduce boilerplate code
   * Avoid broad Lombok `@Data` on JPA entities; prefer targeted annotations like `@Getter`
   * Keep OpenAPI / Swagger UI documentation updated for every new or changed REST endpoint
   * Ensure JWT-protected endpoints remain documented with bearer authentication
   * Keep REST endpoints and generated URLs aligned with the configured API version prefix
   * Use Redis where it improves efficiency or maintainability for caching, rate limiting, token metadata, idempotency, or short-lived state
   * Use Elasticsearch where it improves full-text search, scalable discovery, or analytics search
   * Add resilient fallback paths when optional infrastructure can fail without breaking the core user flow
   * Write comment above every method explaining what it does
   * Write comment above every method explaining what it does
   * Write a short comment above complex code explaining why the logic exists

---

### Phase 3: Completion

1. Verify the implementation is complete
2. Verify OpenAPI docs still load at `/swagger-ui.html` and `/v3/api-docs`
3. Verify API versioning remains consistent with `API_BASE_PATH`
4. Verify Redis and Elasticsearch usage, if added or changed, is documented and environment-configurable
5. Verify every added or changed method has a short explanatory comment above it
6. Verify complex code has a short explanatory comment where it improves maintainability
7. Mark the task as `[x]` in tasks.md
8. Do NOT start the next task automatically

---

### Strict Rules

* Do NOT skip tasks
* Do NOT implement multiple tasks at once
* Do NOT write code before approval
* Do NOT ignore AGENTS.md rules
* Keep the system stateless (no session usage)
* Do NOT touch or use anything inside the `bin/` folder

If something is unclear, make reasonable assumptions and state them explicitly in the plan.

Start with Phase 1 for the first task.
