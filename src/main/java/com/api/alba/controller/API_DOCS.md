# API 엔드포인트 목록

총 **48개** 엔드포인트 / 컨트롤러 **9개**

---

## 목차
- [AuthController](#1-authcontroller---앱-인증)
- [WebAuthController](#2-webauthcontroller---웹-인증)
- [AttendanceController](#3-attendancecontroller---출퇴근)
- [StaffController](#4-staffcontroller---직원)
- [OwnerController](#5-ownercontroller---점주)
- [NoticeController](#6-noticecontroller---공지사항)
- [TermsController](#7-termscontroller---약관)
- [PushTokenController](#8-pushtokencontroller---푸시-토큰)
- [NaverMapController](#9-navermapcontroller---네이버-지도)
- [중복 검토](#중복-검토)

---

## 1. AuthController - 앱 인증
**Base path:** `/api/auth`

- `POST /api/auth/signup` `201` — 회원가입
- `POST /api/auth/login` `200` — 이메일/비밀번호 로그인, JWT 반환
- `POST /api/auth/social/login` `200` — 소셜 로그인 (카카오 등), JWT 반환
- `POST /api/auth/password/reset/request` `204` — 비밀번호 재설정 요청 (이메일 발송)
- `POST /api/auth/password/reset/confirm` `204` — 비밀번호 재설정 확인 (새 비밀번호 적용)
- `GET /api/auth/me` `200` — 현재 로그인한 사용자 정보 조회 🔒
- `POST /api/auth/withdraw` `204` — 회원 탈퇴 🔒

---

## 2. WebAuthController - 웹 인증
**Base path:** `/api/web/auth`
> 웹 관리자 페이지 전용 인증. 앱과 다른 응답 형식 사용.

- `POST /api/web/auth/login` `200` — 웹 로그인 (이메일/비밀번호)
- `POST /api/web/auth/login-method` `200` — 이메일로 로그인 방식 조회 (일반/소셜 여부)
- `POST /api/web/auth/otp/confirm` `200` — OTP 인증 확인 후 웹 세션 발급

---

## 3. AttendanceController - 출퇴근
**Base path:** `/api/attendance`

- `POST /api/attendance/check-in` `201` — 출근 등록 🔒
- `POST /api/attendance/check-out` `200` — 퇴근 등록 + 근무시간/급여 자동 계산 🔒
- `GET /api/attendance/me` `200` — 본인의 기간별 근태 기록 조회 (`workplaceId`, `fromDate`, `toDate`) 🔒

---

## 4. StaffController - 직원
**Base path:** `/api/staff`

- `POST /api/staff/workplaces/join` `200` — 초대 코드로 사업장 참여 🔒
- `GET /api/staff/workplaces/{workplaceId}/home/today` `200` — 직원 홈: 오늘 출퇴근 상태 및 근무 정보 🔒
- `GET /api/staff/workplaces/{workplaceId}/summary/today` `200` — 오늘 근무 요약 (근무시간, 예상 급여 등) 🔒
- `GET /api/staff/workplaces/{workplaceId}/calendar/monthly` `200` — 본인 월별 캘린더 (`yearMonth`) 🔒
- `GET /api/staff/workplaces/{workplaceId}/calendar/daily` `200` — 특정일 근무 상세 (`workDate`) 🔒
- `POST /api/staff/attendance-records/{attendanceRecordId}/requests` `201` — 근태 수정 요청 제출 🔒

---

## 5. OwnerController - 점주
**Base path:** `/api/owner`

### 사업장 관리
- `POST /api/owner/workplaces` `201` — 사업장 생성 🔒
- `GET /api/owner/workplaces/{workplaceId}/invite-code` `200` — 사업장 초대 코드 조회 🔒
- `GET /api/owner/workplaces/{workplaceId}/members` `200` — 직원 목록 조회 🔒
- `DELETE /api/owner/workplaces/{workplaceId}/members/{memberId}` `204` — 직원 내보내기 🔒
- `PATCH /api/owner/workplaces/{workplaceId}/members/{memberId}/memo` `204` — 직원 메모 수정 🔒

### 대시보드 & 캘린더
- `GET /api/owner/workplaces/{workplaceId}/dashboard/today` `200` — 오늘 대시보드 (출근 현황 등) 🔒
- `GET /api/owner/workplaces/{workplaceId}/calendar/daily` `200` — 특정일 전체 직원 출퇴근 조회 (`workDate`) 🔒
- `GET /api/owner/workplaces/{workplaceId}/calendar/monthly` `200` — 직원 월별 캘린더 (`yearMonth`, `userId` 선택) 🔒

### 근태 기록 관리
- `GET /api/owner/workplaces/{workplaceId}/attendance-records` `200` — 근태 기록 목록 조회 (`userId`, `userName`, `fromDate`, `toDate`) 🔒
- `POST /api/owner/workplaces/{workplaceId}/attendance-records` `201` — 점주가 근태 기록 수동 생성 🔒
- `POST /api/owner/workplaces/{workplaceId}/attendance-records/recalculate-wages` `200` — 특정 월 급여 일괄 재계산 (`month`) 🔒
- `GET /api/owner/workplaces/{workplaceId}/attendance-requests` `200` — 근태 수정 요청 목록 조회 (`status` 선택) 🔒
- `PATCH /api/owner/attendance-requests/{requestId}` `204` — 근태 수정 요청 승인/반려 🔒

### 급여
- `GET /api/owner/workplaces/{workplaceId}/wages/expected` `200` — 직원별 예상 급여 합계 (`fromDate`, `toDate`) 🔒

### 휴게 정책
- `GET /api/owner/workplaces/{workplaceId}/break-policies` `200` — 휴게 정책 조회 🔒
- `PUT /api/owner/workplaces/{workplaceId}/break-policies` `204` — 휴게 정책 저장 (전체 교체) 🔒

### 사업장 설정
- `GET /api/owner/workplaces/{workplaceId}/settings` `200` — 출퇴근 알림 설정 조회 🔒
- `PATCH /api/owner/workplaces/{workplaceId}/settings/name` `204` — 사업장 이름 변경 🔒
- `PATCH /api/owner/workplaces/{workplaceId}/settings/location-restriction` `204` — 위치 제한 설정 변경 🔒
- `PATCH /api/owner/workplaces/{workplaceId}/settings/attendance-push` `204` — 출퇴근 푸시 알림 활성화/비활성화 🔒
- `PATCH /api/owner/workplaces/{workplaceId}/settings/hourly-wage` `204` — 기본 시급 변경 🔒
- `PATCH /api/owner/workplaces/{workplaceId}/settings/salary-calc-unit` `204` — 급여 계산 단위 변경 🔒

---

## 6. NoticeController - 공지사항
**Base path:** `/api/notices`

- `GET /api/notices` `200` — 공지사항 전체 목록 조회
- `GET /api/notices/{noticeId}` `200` — 공지사항 단건 조회

---

## 7. TermsController - 약관
**Base path:** `/api/terms`

- `GET /api/terms` `200` — 활성화된 약관 전체 목록 조회
- `GET /api/terms/{termsType}` `200` — 특정 타입의 활성 약관 조회
- `POST /api/terms/agree` `204` — 약관 동의 🔒

---

## 8. PushTokenController - 푸시 토큰
**Base path:** `/api/push-tokens`

- `POST /api/push-tokens` `204` — FCM 푸시 토큰 등록/갱신 (upsert) 🔒

---

## 9. NaverMapController - 네이버 지도
**Base path:** `/api/naver`

- `GET /api/naver/geocode` `200` — 주소 문자열을 좌표로 변환 (`address`)
