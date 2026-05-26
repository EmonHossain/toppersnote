# AGENTS.md

## 🎯 Objective

You are responsible for implementing a Spring Boot backend system with:

* JWT-based stateless authentication
* User management
* File upload and access control
* proper logging with Slf4j ecs format

Follow `tasks.md` strictly.

---

## ⚙️ Execution Rules

1. Always read `tasks.md` before starting work
2. Pick the **first unchecked task**
3. Complete tasks **one at a time**
4. After completing a task:

   * Mark it as `[x]` in `tasks.md`
   * Move to the next task

---

## 🧱 Architecture Guidelines

* Use Spring Boot + Spring Security
* Use Hibernate (JPA) for persistence
* Follow layered architecture:

  * Controller
  * Service
  * Repository
* Use DTOs (do NOT expose entities directly)

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
* Add comments where necessary
* Handle exceptions properly
* Avoid duplication
* Add log where necessary

---

## 🚫 Forbidden Actions

* Do not skip tasks
* Do not implement multiple tasks at once
* Do not ignore validation or security requirements
* Do not hardcode sensitive data

---

## ✅ Definition of Done

A task is complete when:

* All requirements in `tasks.md` are implemented
* Code compiles and runs
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
