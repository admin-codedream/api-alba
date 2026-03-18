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
  - Sign up (`loginId`, `password`, `name`)
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
