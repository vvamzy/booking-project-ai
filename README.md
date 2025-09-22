# Smart Meeting Room System

## Features Implemented

### AI-related Features
- LLM-based purpose validation: the backend integrates an AI decision service that evaluates booking purposes for clarity and policy compliance.
- AI-driven alternate-room suggestions: when a room is unavailable or a purpose is unclear, the system suggests ranked alternate rooms using equipment, capacity, proximity, and historical approval scoring.
- Analytics insights: basic analytics that surface utilization, approval rates, and room heatmaps to support planning and forecasting.

### Booking & Authorization
- Booking lifecycle: users can create bookings with purpose, attendees, required facilities, and priority.
- Roles & approval workflows: Admins can approve/reject bookings; some rooms (e.g. Executive Boardroom) require manual admin approval regardless of AI decision.
- Booking history: immutable history entries are recorded for audit and change tracking.

### Notifications & Reminders
- Scheduled reminders: the system schedules and persists reminders for meeting owners and sends them via in-app notifications and email.
- Facilities notifications: bookings that require AV, video or catering trigger scheduled email notifications to facilities and IT support addresses.
- WebSocket push: real-time in-app notifications are published to `/topic/notifications/user/{userId}`.

### Alternate Room Suggestions
- Suggestion engine: endpoint `/api/bookings/suggest` returns scored alternate rooms and next available slots.
- Scoring factors: equipment similarity, capacity closeness, proximity, historical approval rate, and earliest available time.

### Other Functionality
- Authentication: session-based login endpoints with seeded test users for local development.
- Data seeding: sample rooms, equipment, users and bookings are generated at startup for easier testing.
- REST API: comprehensive endpoints under `/api/bookings`, `/api/rooms`, `/api/auth`, and admin routes.

## Project Structure

Top-level layout:

- `backend/` — Spring Boot application (Java 17, Maven)
	- `src/main/java/com/example/meeting/` — primary Java packages
		- `controller/` — REST controllers (BookingController, AuthController, etc.)
		- `service/` — business logic (BookingService, NotificationService, AiDecisionService)
		- `repository/` — Spring Data JPA repositories
		- `model/` — JPA entities and DTOs
		- `scheduler/` — scheduled tasks (NotificationScheduler)
		- `config/` — security, data seeding and app configuration
	- `src/main/resources/application.yml` — main configuration, SMTP settings, and profiles
	- `fakesmtp/` — cloned FakeSMTP source for local email testing (builds `fakeSMTP-*.jar`)

- `frontend/` — React + TypeScript client (Create React App)
	- `src/` — application code
		- `api/` — client API wrappers
		- `components/` — UI components (BookingForm, BookingsList, Navigation, etc.)
		- `pages/` — page views (Dashboard, BookingPage, AdminPanel)
		- `stores/` — client-side state management
		- `styles/` — CSS files

- `tools/` — helper scripts, docker-compose and e2e helpers
- `README.md` — this file
- `documentation.md` — additional project notes

## Prerequisites & Installation Steps

### Required Software
- Java 17 (or newer)
- Maven (for backend builds)
- Node.js (16+ recommended) and `npm` or `yarn` (for frontend)
- Git

### Local Setup (backend)

1. Open a terminal and navigate to the project root.
2. Build the backend package:

```powershell
cd backend
mvn -DskipTests package
```

3. (Optional) Build and run FakeSMTP for local email capture. From `backend/fakesmtp`:

```powershell
cd backend\fakesmtp
mvn -DskipTests package
# then run (background):
Start-Process java -ArgumentList '-jar','target\fakeSMTP-2.1-SNAPSHOT.jar','-s','-p','2525','-a','127.0.0.1','-o','fake-emails'
```

4. Configure SMTP if you need custom settings: edit `backend/src/main/resources/application.yml` and set `spring.mail.host`, `spring.mail.port` and other `notifications.smtp` properties.

5. Start the backend application:

```powershell
cd backend
mvn -DskipTests spring-boot:run
```

### Local Setup (frontend)

1. Install dependencies and start the dev server:

```powershell
cd frontend
npm install
npm start
```

2. Open the app at `http://localhost:3000` and the backend at `http://localhost:8080`.

## User Details

- Admin
	- Permissions: view all bookings, approve/reject bookings, access `/api/admin/**` endpoints and admin dashboard.
- Manager
	- Permissions: similar to employees but typically higher priority in analytics and may assist with approvals depending on configuration.
- Employee
	- Permissions: create bookings, view own bookings, receive reminders and suggestions.

How users interact:
- Login via `POST /api/auth/login` (session-based authentication). Seeded users: `alice/alicepass`, `bob/bobpass`, `admin/adminpass` for local development.
- Create bookings via `POST /api/bookings` and receive AI validation via `POST /api/bookings/validate`.
- Admins use `/api/bookings/pending` and the admin UI to approve or reject bookings.

## AI Details

This project includes an `AiDecisionService` that integrates an LLM-style decision flow (pluggable):

- Purpose validation: the AI evaluates a booking's purpose and returns an action (AUTO_APPROVE, REQUIRES_REVIEW, AUTO_REJECT), a confidence score, rationale, and optional suggestions.
- Suggestion generation: when a REMINDER or suggest call is processed the NotificationService enriches payloads by calling `/api/bookings/suggest` to surface alternatives.
- Analytics insights: the AI components can be used to predict utilization, suggest times, and provide rationale for decision automation.

Implementation notes:
- The AI integration is provided via `AiDecisionService` (backend/service). The implementation can be replaced or extended to call external LLMs (OpenAI, Anthropic, local LLMs) or to use an internal rules-based fallback.
- Configuration: the LLM endpoint, API key and model settings can be configured via environment variables or `application.yml` (extend `ai:` keys as needed).

Example (environment variables):
- `AI_ENDPOINT` — URL to call for purpose validation
- `AI_API_KEY` — API key for the LLM provider
- `AI_MODEL` — model id

## Mock Data

- The application seeds data at startup via `DataSeeder`:
	- Rooms: ~15 seeded rooms including `Executive Boardroom` and many sample rooms.
	- Equipment: a catalog of equipment items linked to rooms.
	- Users: seeded users `alice`, `bob`, `admin`.
	- Bookings: ~100 sample bookings randomly distributed over the next 30 days.

To re-seed data: delete the H2 or application `target` database files and restart the backend. The `DataSeeder` will populate initial data automatically.

## Configurations

- `backend/src/main/resources/application.yml`: main application properties, including `spring.datasource`, `spring.mail` (SMTP), and `notifications.smtp` used by `NotificationService`.
- Environment variables supported (common): `SPRING_DATASOURCE_URL`, `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `AI_ENDPOINT`, `AI_API_KEY`.
- FakeSMTP is used for local email testing; run it and point `spring.mail.host` to `localhost` and `spring.mail.port` to the FakeSMTP port (default `2525`).

## Key Workflows

### Booking Creation & Approval
- User POSTs a booking to `/api/bookings`.
- `BookingService` validates the request and consults `AiDecisionService` for an approval decision.
- Booking is saved with a status (`APPROVED`, `PENDING`, `REJECTED`) depending on AI decision and room rules (executive rooms force `PENDING`).
- Admins view `GET /api/bookings/pending` and can POST `/api/bookings/{id}/approve` or `/api/bookings/{id}/reject`.

### Dashboard Analytics & AI Suggestions
- Dashboard aggregates booking counts, room utilization, and approval rates.
- Suggestion endpoints (`/api/bookings/suggest`) compute scored alternates used by AI to enrich reminders and by frontend suggestion UI.

### Notifications & Reminders
- `BookingService` schedules reminders and facilities notifications when bookings are created.
- `NotificationScheduler` dispatches `NotificationService.sendDueNotifications()` every minute.
- `NotificationService` sends emails using `JavaMailSender` (configured via `spring.mail.*`) and broadcasts in-app WebSocket notifications via `SimpMessagingTemplate`.

### Admin Resource Planning
- Admins can review upcoming bookings and facility requests, using analytics to plan AV/IT staffing.

## Responsive Design

- The React frontend uses responsive components and CSS to adapt to mobile, tablet and desktop form-factors.
- Key UI components scale and stack appropriately; grid/list views collapse into single-column panels on narrow screens.

## Testing

- Backend: Maven + JUnit test suite (run with `mvn test`).
- Frontend: Jest + React Testing Library (run with `npm test` in `frontend`).
- E2E: a sample `tools/e2e-test.js` exists for end-to-end checks — update as needed.

## Future Enhancements

- Replace or augment the `AiDecisionService` with a modern LLM provider and add caching/consistency checks for decisions.
- Add HTML email templates with Thymeleaf and richer email content for facility requests and reminders.
- Add retry/backoff and monitoring for email failures and notification delivery.
- Improve the frontend with richer analytics visualizations (D3, charts), user settings for notification preferences, and push notifications for mobile.
- Add role-based delegation and SSO integration (OAuth2 / SAML) for enterprise deployments.

