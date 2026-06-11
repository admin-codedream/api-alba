# 웹 신규 API 명세서

> 모바일 앱 기준으로 개발되었으나 웹에 미적용된 API 목록입니다.
> 작성일: 2026-06-11

---

## 공통

### 인증
모든 API는 JWT Bearer 토큰 인증이 필요합니다.

```
Authorization: Bearer {token}
```

### 날짜/시간 형식
| 타입 | 형식 | 예시 |
|---|---|---|
| 날짜 | `yyyy-MM-dd` | `2026-06-11` |
| 날짜+시각 | `yyyy-MM-dd'T'HH:mm:ss` | `2026-06-11T09:00:00` |

### 권한
- **점주(OWNER)**: 해당 사업장의 소유자
- **매니저(MANAGER)**: 점주가 지정한 관리자 직원
- 권한 부족 시 `403 Forbidden` 반환

---

## 목차

1. [매장 공지사항](#1-매장-공지사항)
2. [직원 역할 변경](#2-직원-역할-변경)
3. [직원별 주휴수당 설정](#3-직원별-주휴수당-설정)
4. [출결 기록 수정](#4-출결-기록-수정)
5. [QR 출퇴근 설정](#5-qr-출퇴근-설정)
6. [QR 토큰 발급](#6-qr-토큰-발급)

---

## 1. 매장 공지사항

### 1-1. 공지사항 목록 조회

```
GET /api/workplaces/{workplaceId}/notices
```

**권한**: 해당 사업장 소속 멤버 (점주/매니저/직원)

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| workplaceId | Long | 사업장 ID |

**Response** `200 OK` — `List<WorkplaceNotice>`

```json
[
  {
    "id": 1,
    "workplaceId": 10,
    "authorId": 5,
    "title": "이번 주 공지사항",
    "content": "공지 내용입니다.",
    "pinned": true,
    "createdAt": "2026-06-11T09:00:00",
    "updatedAt": "2026-06-11T09:00:00"
  }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 공지사항 ID |
| workplaceId | Long | 사업장 ID |
| authorId | Long | 작성자 사용자 ID |
| title | String | 제목 |
| content | String | 내용 |
| pinned | Boolean | 상단 고정 여부 |
| createdAt | String | 생성일시 |
| updatedAt | String | 수정일시 |

---

### 1-2. 공지사항 단건 조회

```
GET /api/workplaces/{workplaceId}/notices/{noticeId}
```

**권한**: 해당 사업장 소속 멤버

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| workplaceId | Long | 사업장 ID |
| noticeId | Long | 공지사항 ID |

**Response** `200 OK` — `WorkplaceNotice` (1-1과 동일한 단건 객체)

---

### 1-3. 공지사항 등록

```
POST /api/workplaces/{workplaceId}/notices
```

**권한**: 점주, 매니저

**Request Body**

```json
{
  "title": "공지 제목",
  "content": "공지 내용입니다.",
  "pinned": false
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|---|---|---|---|---|
| title | String | Y | 최대 200자 | 공지 제목 |
| content | String | Y | 최대 5000자 | 공지 내용 |
| pinned | Boolean | N | 기본값 `false` | 상단 고정 여부 |

**Response** `201 Created` — `WorkplaceNotice` (1-1과 동일한 단건 객체)

---

### 1-4. 공지사항 수정

```
PUT /api/workplaces/{workplaceId}/notices/{noticeId}
```

**권한**: 점주, 매니저 (본인 작성 공지 또는 점주)

**Request Body**

```json
{
  "title": "수정된 제목",
  "content": "수정된 내용입니다."
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|---|---|---|---|---|
| title | String | Y | 최대 200자 | 공지 제목 |
| content | String | Y | 최대 5000자 | 공지 내용 |

**Response** `200 OK` — `WorkplaceNotice` (1-1과 동일한 단건 객체)

---

### 1-5. 공지사항 핀 고정 토글

```
PATCH /api/workplaces/{workplaceId}/notices/{noticeId}/pin
```

**권한**: 점주, 매니저

**Request Body**: 없음

**Response** `200 OK` — `WorkplaceNotice` (pinned 값이 반전된 상태로 반환)

---

### 1-6. 공지사항 삭제

```
DELETE /api/workplaces/{workplaceId}/notices/{noticeId}
```

**권한**: 점주, 매니저

**Response** `204 No Content`

---

## 2. 직원 역할 변경

```
PATCH /api/owner/workplaces/{workplaceId}/members/{memberId}/role
```

**권한**: 점주(OWNER)만 가능

직원을 일반 직원(STAFF)과 매니저(MANAGER) 사이에서 역할을 변경합니다.
매니저는 매장 공지사항 작성, 직원 출결 수정 등 일부 점주 기능을 사용할 수 있습니다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| workplaceId | Long | 사업장 ID |
| memberId | Long | 변경할 직원의 멤버 ID |

**Request Body**

```json
{
  "role": "MANAGER"
}
```

| 필드 | 타입 | 필수 | 허용값 | 설명 |
|---|---|---|---|---|
| role | String | Y | `STAFF`, `MANAGER` | 변경할 역할 |

**Response** `204 No Content`

---

## 3. 직원별 주휴수당 설정

```
PATCH /api/owner/workplaces/{workplaceId}/members/{memberId}/weekly-holiday-pay
```

**권한**: 점주(OWNER)

직원별로 주휴수당 사용 여부를 개별 설정합니다.
`null`로 전송하면 매장 전체 설정(`WORKPLACE_SETTINGS.USE_WEEKLY_HOLIDAY_PAY`)을 따릅니다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| workplaceId | Long | 사업장 ID |
| memberId | Long | 대상 직원 멤버 ID |

**Request Body**

```json
{
  "useWeeklyHolidayPay": true
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| useWeeklyHolidayPay | Boolean \| null | N | `true`: 사용, `false`: 미사용, `null`: 매장 설정 상속 |

**Response** `204 No Content`

---

## 4. 출결 기록 수정

```
PATCH /api/owner/workplaces/{workplaceId}/attendance-records/{recordId}
```

**권한**: 점주(OWNER), 매니저(MANAGER)

출퇴근 시각을 직접 수정합니다. 수정 후 근무시간과 급여가 자동으로 재계산됩니다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| workplaceId | Long | 사업장 ID |
| recordId | Long | 수정할 근태 기록 ID |

**Request Body**

```json
{
  "checkInAt": "2026-06-11T09:05:00",
  "checkOutAt": "2026-06-11T18:00:00",
  "note": "수정 사유"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| checkInAt | String | Y | 출근 시각 (`yyyy-MM-dd'T'HH:mm:ss`) |
| checkOutAt | String | N | 퇴근 시각 (`null`이면 현재 WORKING 상태 유지) |
| note | String | N | 비고 (최대 500자) |

**Response** `200 OK` — `AttendanceRecord`

```json
{
  "id": 101,
  "workplaceId": 10,
  "userId": 5,
  "userName": "홍길동",
  "profileColor": "#3B82F6",
  "wageType": "HOURLY",
  "monthlyWage": null,
  "dailyWage": null,
  "workDate": "2026-06-11",
  "checkInAt": "2026-06-11T09:05:00",
  "checkOutAt": "2026-06-11T18:00:00",
  "status": "LATE",
  "workedMinutes": 475,
  "baseWage": 47500,
  "finalWage": 47500,
  "note": "수정 사유",
  "createdAt": "2026-06-11T09:00:00",
  "updatedAt": "2026-06-11T10:00:00"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 근태 기록 ID |
| workplaceId | Long | 사업장 ID |
| userId | Long | 직원 사용자 ID |
| userName | String | 직원 이름 |
| profileColor | String | 프로필 색상 (#RRGGBB) |
| wageType | String | 급여 유형 (`HOURLY` / `MONTHLY` / `DAILY`) |
| monthlyWage | BigDecimal \| null | 월급 (월급제일 때) |
| dailyWage | BigDecimal \| null | 일급 (일급제일 때) |
| workDate | String | 근무일 |
| checkInAt | String \| null | 출근 시각 |
| checkOutAt | String \| null | 퇴근 시각 |
| status | String | 근태 상태 (`WORKING` / `COMPLETED` / `LATE` / `ABSENT`) |
| workedMinutes | Integer | 총 근무 분 |
| baseWage | BigDecimal | 기본 계산 급여 |
| finalWage | BigDecimal | 최종 급여 |
| note | String \| null | 비고 |
| createdAt | String | 생성일시 |
| updatedAt | String | 수정일시 |

---

## 5. QR 출퇴근 설정

```
PATCH /api/owner/workplaces/{workplaceId}/settings/qr-checkin
```

**권한**: 점주(OWNER)

QR 출퇴근 기능을 켜고 끕니다. QR 출퇴근을 활성화하면 위치 제한이 자동으로 비활성화됩니다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| workplaceId | Long | 사업장 ID |

**Request Body**

```json
{
  "useQrAttendance": true,
  "qrNoTimeLimit": false
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| useQrAttendance | Boolean | Y | QR 출퇴근 사용 여부 |
| qrNoTimeLimit | Boolean | N | `true`: 시간제한 없는 영구 QR, `false`/생략: 10분 만료 QR (기본값) |

> **참고**: `useQrAttendance: true` 저장 시 `USE_LOCATION_RESTRICTION`이 자동으로 `false`로 전환됩니다.

**Response** `204 No Content`

---

## 6. QR 토큰 발급

```
GET /api/workplaces/{workplaceId}/qr-token
```

**권한**: 점주(OWNER)

QR 코드에 인코딩할 토큰을 발급합니다.
`qrNoTimeLimit` 설정에 따라 만료 시간이 결정됩니다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| workplaceId | Long | 사업장 ID |

**Response** `200 OK`

**시간제한 있는 경우** (`qrNoTimeLimit: false`)
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-06-11T09:10:00"
}
```

**시간제한 없는 경우** (`qrNoTimeLimit: true`)
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": null
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| token | String | QR 코드에 인코딩할 JWT 토큰 |
| expiresAt | String \| null | 만료일시. 영구 QR이면 `null` |

> **웹 구현 가이드**
> - 발급된 `token` 값을 QR 코드 라이브러리로 이미지 생성
> - 시간제한 QR이면 `expiresAt` 기준으로 카운트다운 표시 후 자동 재발급 권장
> - 영구 QR(`expiresAt: null`)이면 한 번 발급 후 계속 사용 가능

---

## 기존 API 응답에 추가된 필드

웹에서 이미 사용 중인 API의 응답에 아래 필드들이 추가되었습니다.

### 매장 설정 조회 `GET /api/owner/workplaces/{workplaceId}/settings`

추가 응답 필드:

| 필드 | 타입 | 설명 |
|---|---|---|
| useQrAttendance | Boolean | QR 출퇴근 사용 여부 |
| qrNoTimeLimit | Boolean | 시간제한 없는 QR 여부 |

### 직원 목록 조회 `GET /api/owner/workplaces/{workplaceId}/members`

추가 응답 필드:

| 필드 | 타입 | 설명 |
|---|---|---|
| role | String | 직원 역할 (`STAFF` / `MANAGER` / `OWNER`) |

### 매장 목록 조회 `GET /api/staff/workplaces` (점주도 포함)

추가 응답 필드:

| 필드 | 타입 | 설명 |
|---|---|---|
| role | String | 본인의 해당 매장 역할 |
| useQrAttendance | Boolean | QR 출퇴근 사용 여부 |
| qrNoTimeLimit | Boolean | 시간제한 없는 QR 여부 |