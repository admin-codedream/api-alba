# Alba API

Alba API is a backend project for part-time workforce management.
It focuses on secure authentication first, then extends to workplace and attendance domains.

## Project Goal
- Provide a simple but production-oriented auth foundation
- Support both basic login and social login in one user model
- Keep API access protected by API key + JWT

## Tech Stack
- Java 17
- Spring Boot 2.7.5
- Spring Security
- MyBatis
- MariaDB 10
- Gradle

## Core Features
- Basic auth
  - Sign up (`loginId`, `password`, `name`, `userType`)
  - Login (JWT issue)
  - My profile (`/api/auth/me`)
- Social auth
  - Social login by `provider + providerUserId`
  - Auto-create user on first social login
  - Manual social account linking endpoint
  - No automatic email-based account linking
- Global API key guard
  - All endpoints require API key header
  - Then JWT auth is evaluated for protected APIs
- Attendance APIs
  - Check in (`POST /api/attendance/check-in`)
  - Check out (`POST /api/attendance/check-out`)
  - My records by date range (`GET /api/attendance/me`)
- Staff APIs (3.1)
  - Join workplace by invite code (`POST /api/staff/workplaces/join`)
  - Home today (current work time/expected wage) (`GET /api/staff/workplaces/{workplaceId}/home/today`)
  - Today + cumulative summary (`GET /api/staff/workplaces/{workplaceId}/summary/today`)
  - Submit attendance correction request (`POST /api/staff/attendance-records/{attendanceRecordId}/requests`)
- Owner APIs (3.2)
  - Create workplace (`POST /api/owner/workplaces`)
  - Get invite code (`GET /api/owner/workplaces/{workplaceId}/invite-code`)
  - Today dashboard (`GET /api/owner/workplaces/{workplaceId}/dashboard/today`)
  - Staff attendance records (`GET /api/owner/workplaces/{workplaceId}/attendance-records`)
  - Approve/reject attendance request (`PATCH /api/owner/attendance-requests/{requestId}`)
  - Expected wage summary by employee (`GET /api/owner/workplaces/{workplaceId}/wages/expected`)

## 3.0 Onboarding
- Sign up now requires user type selection:
  - `OWNER`
  - `STAFF`
- Example request:
```json
{
  "loginId": "owner01",
  "password": "password1234",
  "name": "Hong",
  "userType": "OWNER"
}
```

## Data Model (Current Focus)
- `USERS`: internal user account
- `USER_SOCIAL_ACCOUNTS`: social identity mapping
- Additional workplace/attendance tables are already included in DDL for next phases

## Configuration
- Shared config: `src/main/resources/application.yml`
- Environment config:
  - `src/main/resources/application-dev.properties`
  - `src/main/resources/application.properties` (external DB env-based)
- Key env vars:
  - `API_KEY_VALUE`, `API_KEY_HEADER_NAME`
  - `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER_CLASS_NAME`

## Run
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Notes
- Execute `src/main/resources/schema.sql` in DB manually (`spring.sql.init.mode=never`).
- Do not commit real secrets in profile property files.
- For existing DB, run migration:
```sql
ALTER TABLE USERS
ADD COLUMN USER_TYPE VARCHAR(30) NOT NULL DEFAULT 'STAFF' AFTER NAME,
ADD KEY IDX_USERS_USER_TYPE (USER_TYPE);
```
```sql
ALTER TABLE WORKPLACES
ADD COLUMN INVITE_CODE VARCHAR(20) NOT NULL,
ADD UNIQUE KEY UK_WORKPLACES_INVITE_CODE (INVITE_CODE);
```
