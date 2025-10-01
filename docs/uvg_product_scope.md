# UVG Tutor App – Product & Architecture Scope

## 1. Platform Goals
- Deliver an academic mentor geared to Universidad del Valle de Guatemala (UVG) students.
- Combine institutional data (careers, courses, professors) with AI tutoring responses.
- Support UVG-managed Microsoft accounts (`@uvg.edu.gt`) for secure access.
- Track each student's plan of study and enrollment to contextualize tutoring sessions.

## 2. Backend Architecture Overview

### 2.1 Authentication (Azure AD)
- Use **Microsoft Entra ID** (formerly Azure AD) with UVG's tenant to handle OAuth2 sign-in.
- Register the Android application and backend API as separate applications.
- Configure the mobile app for **MSAL (Microsoft Authentication Library)** to obtain ID/access tokens.
- Enforce domain restriction so only `@uvg.edu.gt` identities are accepted.

### 2.2 Core Services
| Service | Responsibilities | Tech Notes |
| --- | --- | --- |
| Identity Gateway | Validate MSAL tokens, mint backend session tokens/JWTs with roles & student id. | Lightweight Kotlin Ktor or Node.js service; integrates with Azure AD.
| Academic Data API | CRUD over careers, programs, courses, schedules, professors. | Recommend **PostgreSQL** (managed: Azure Database for PostgreSQL or Supabase). Expose via REST/GraphQL.
| Student Profile API | Store each student’s academic plan, enrollment, completed courses. | Same database; guard with row-level security per student id.
| Tutoring Session API | Persist chat history, attachment summaries, OpenAI usage metrics. | Allows future analytics, cost tracking, resume sessions.

### 2.3 Data Model (Initial Draft)

```
careers
- id (uuid)
- name
- faculty
- description

program_terms
- id (uuid)
- career_id (fk careers)
- year (int)
- semester (enum: 1,2)

courses
- id (uuid)
- code (text)
- name (text)
- description (text)
- credits (int)

course_offerings
- id (uuid)
- course_id (fk courses)
- program_term_id (fk program_terms)
- professor_id (fk professors)
- schedule (jsonb: days/hours/location)

professors
- id (uuid)
- name
- email

students
- id (uuid)
- microsoft_oid (guid from Azure)
- email
- first_name
- last_name
- career_id (fk careers)
- admission_year (int)

student_enrollments
- id (uuid)
- student_id (fk students)
- course_offering_id (fk course_offerings)
- status (enum: enrolled, completed, dropped)
- grade (optional)

student_course_history
- id (uuid)
- student_id
- course_id
- term_completed_id (fk program_terms)

chat_sessions
- id (uuid)
- student_id
- course_offering_id (optional)
- created_at

chat_messages
- id (uuid)
- session_id (fk chat_sessions)
- role (enum: system/user/assistant)
- content (text)
- created_at

attachments
- id (uuid)
- message_id (fk chat_messages)
- type (enum: pdf/image)
- name
- summary
- storage_uri (for optional blob storage reference)
```

### 2.4 Storage & Infrastructure Stack Options
- **Database**: Managed PostgreSQL (Azure) + Prisma/Hasura + row-level policies.
- **Blob Storage**: Azure Blob Storage for original attachments (if needed later).
- **API Hosting**: Azure App Service (Ktor/Spring Boot) or Azure Functions (serverless) for smaller footprint.
- **Queue/Tasks**: Azure Service Bus/Storage Queue to offload heavy summarization or analytics tasks.

### 2.5 OpenAI Service
- Backend proxies OpenAI API using service account key (avoids storing key on device).
- Responsibilities:
  - Validate student’s entitlement (enrolled in course) before generating tutoring assistance.
  - Append server-side system prompt: same course pensum context plus dynamic data from the database (professor, schedule, deadlines).
  - Log usage (tokens, errors) for monitoring.

## 3. Frontend Roadmap (Android)

### 3.1 Authentication Flow
1. Launch screen → “Sign in with Microsoft”.
2. Use MSAL to trigger Azure AD login.
3. On success, exchange token with backend for app-specific JWT.
4. Store profile & JWT securely (EncryptedSharedPreferences / Jetpack DataStore).

### 3.2 Onboarding Selection
- After first login, fetch careers & programs list from backend to confirm student’s track.
- UI components:
  - Career dropdown (prefilled if backend already knows student’s career).
  - Semester/course multi-select for current term.
- Send selections to backend to populate `student_enrollments`.

### 3.3 Main Chat Experience
- Reuse existing chat UI with additional context pulled from backend (e.g., current assignments).
- Display active courses in overview screen with enrollment status.

### 3.4 Settings / Profile Screen
- Allow students to review completed courses, update enrollment (syncs with backend).
- Option to refresh data from SIS when available.

## 4. Implementation Phases

| Phase | Focus | Key Deliverables |
| --- | --- | --- |
| Phase 1 | Authentication skeleton | MSAL integration, backend token validation, secure storage. |
| Phase 2 | Academic data foundation | PostgreSQL schema, CRUD endpoints, seed UVG careers/courses. |
| Phase 3 | Student profile syncing | Enrollment workflows, onboarding UI, linking to backend. |
| Phase 4 | OpenAI proxy service | Server-side prompt assembly, logging, rate limiting. |
| Phase 5 | Production hardening | Telemetry, cost monitoring, attachment storage, caching, QA. |

## 5. Security & Compliance Notes
- Enforce least-privilege by issuing scoped backend tokens per student.
- Store minimal attachment content; summaries remain text for OpenAI payloads.
- Consider FERPA/GDPR-style policies: encrypt sensitive data at rest, audit access.
- Document consent for AI usage and provide opt-out in the app.

## 6. Next Steps Checklist
- [ ] Provision Azure AD applications for mobile & backend.
- [ ] Stand up development PostgreSQL instance with seed data for UVG programs.
- [ ] Implement authentication microservice that exchanges MSAL tokens.
- [ ] Extend Android app with MSAL login screen and onboarding flow.
- [ ] Migrate OpenAI requests to backend proxy for centralized key management and logging.
- [ ] Define CI/CD pipelines (GitHub Actions/Azure DevOps) for backend & Android app. 
