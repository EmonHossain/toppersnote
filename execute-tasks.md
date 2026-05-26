Read **AGENTS.md** and **tasks.md** before doing anything.

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
   * logging (use Lombok for log)
   * Use Lombok whenever possible to reduce boilerplate code

---

### Phase 3: Completion

1. Verify the implementation is complete
2. Mark the task as `[x]` in tasks.md
3. Do NOT start the next task automatically

---

### Strict Rules

* Do NOT skip tasks
* Do NOT implement multiple tasks at once
* Do NOT write code before approval
* Do NOT ignore AGENTS.md rules
* Keep the system stateless (no session usage)

If something is unclear, make reasonable assumptions and state them explicitly in the plan.

Start with Phase 1 for the first task.
