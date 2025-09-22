# Smart Meeting Room System
 
## Features Implemented
 
### AI-related Features
 
-   **LLM-based purpose validation**: backend integrates AI service to evaluate booking purposes for clarity and compliance.
-   **AI-driven alternate-room suggestions**: suggests ranked alternate rooms using equipment, capacity, proximity, and historical approval scoring.
-   **Analytics insights**: provides utilization, approval rates, and room heatmaps to support planning and forecasting.

 
### Booking & Authorization
 
-   **Booking lifecycle**: create bookings with purpose, attendees, required facilities, and priority.
-   **Approval workflows**: Executive Boardroom requires **manual admin approval**.
-   **Booking history**: immutable audit history for transparency.

 
### Notifications & Reminders
 
-   **Scheduled reminders** for meeting owners (email + in-app).
-   **Facilities notifications** for AV setup, video conferencing, catering.
-   **WebSocket push** notifications via `/topic/notifications/user/{userId}`.

 
### Alternate Room Suggestions
 
-   **Suggestion engine**: `/api/bookings/suggest` returns scored alternate rooms.
-   **Scoring factors**: equipment similarity, capacity closeness, location, historical approval rate, earliest available time.

 
### Other Features
 
-   **Authentication** with seeded test users.
-   **Data seeding**: rooms, users, equipment, bookings auto-generated at startup.
-   **REST APIs**: `/api/bookings`, `/api/rooms`, `/api/auth`, `/api/admin`.

 
----------
 
## Project Structure
 
```
backend/   → Spring Boot app
  └─ src/main/java/com/example/meeting/
       ├─ controller/     (BookingController, AuthController)
       ├─ service/        (BookingService, NotificationService, AiDecisionService)
       ├─ repository/     (Spring Data JPA repositories)
       ├─ model/          (entities, DTOs)
       ├─ scheduler/      (NotificationScheduler)
       └─ config/         (security, data seeding)
  └─ src/main/resources/application.yml
  └─ fakesmtp/   (FakeSMTP for local testing)
 
frontend/ → React + TypeScript app
  └─ src/
       ├─ api/            (API wrappers)
       ├─ components/     (UI components: BookingForm, List, etc.)
       ├─ pages/          (Dashboard, AdminPanel, BookingPage)
       ├─ stores/         (state management)
       └─ styles/         (CSS)
 
tools/     → scripts, docker-compose, e2e helpers
README.md  → documentation
 
```
 
----------
 
## Prerequisites & Installation
 
### Required Software
 
-   Java 17+
-   Maven
-   Node.js 16+ & npm/yarn
-   Git

 
### Backend Setup
 
```powershell
cd backend
mvn -DskipTests package
mvn -DskipTests spring-boot:run
 
```
 
Optional: FakeSMTP for local email capture
 
```powershell
cd backend\fakesmtp
mvn -DskipTests package
java -jar target\fakeSMTP-2.1-SNAPSHOT.jar -s -p 2525 -a 127.0.0.1 -o fake-emails
 
```
 
### Frontend Setup
 
```powershell
cd frontend
npm install
npm start
 
```
 
Open:
 
-   App → `http://localhost:3000`
-   Backend → `http://localhost:8080`

 
----------
 
## User Details
 
-   **Admin** → view, approve/reject bookings, access `/api/admin/**`.
-   **Manager** → create bookings, assist with approvals (optional).
-   **Employee** → create/view own bookings.

 
Seeded users:
 
-   `alice / alicepass`
-   `bob / bobpass`
-   `admin / adminpass`

 
----------
 
## AI Details
 
-   **AiDecisionService** integrates LLM or rules engine.
-   Validates purpose → outputs `AUTO_APPROVE`, `REQUIRES_REVIEW`, `AUTO_REJECT` + confidence + rationale.
-   Suggestion generation: `/api/bookings/suggest`.
-   Analytics insights: utilization predictions, approval forecasting.

 
### Config
 
-   `AI_ENDPOINT` → LLM URL
-   `AI_API_KEY` → Key for provider
-   `AI_MODEL` → Model ID

 
----------
 
## Mock Data
 
-   ~15 rooms (including **Executive Boardroom**)
-   Equipment catalog seeded
-   Users: `alice`, `bob`, `admin`
-   ~100 sample bookings (30-day spread)

 
----------
 
## Configurations
 
-   **application.yml** (datasource, mail, AI config)
-   Env vars: `SPRING_DATASOURCE_URL`, `SPRING_MAIL_HOST`, `AI_ENDPOINT`, `AI_API_KEY`

 
----------
 
## Key Workflows
 
### Booking Creation
 
1.  User `POST /api/bookings`.
2.  AI validates purpose → decides APPROVED/PENDING/REJECTED.
3.  Executive Boardroom always → **PENDING (admin approval)**.
4.  Admin approves/rejects via `/api/bookings/pending`.

 
### Dashboard & Analytics
 
-   Aggregates utilization, approval rates.
-   Suggests alternate rooms.

 
### Notifications
 
-   `NotificationScheduler` runs every minute.

-   Sends email + WebSocket notifications.

 
----------
 
## Responsive Design
 
-   React + CSS responsive layouts.
-   Mobile/tablet → auto-stacked UI.

 
----------
 
## Testing
 
-   **Backend**: JUnit + Maven
-   **Frontend**: Jest + React Testing Library
-   **E2E**: sample scripts in `tools/`



 
----------
 
## Future Enhancements
 
-   Stronger LLM integration + caching.
-   HTML email templates (Thymeleaf).
-   Analytics with D3/Charts.
-   Role-based delegation, SSO.



