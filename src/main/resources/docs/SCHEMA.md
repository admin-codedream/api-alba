# 알밤 DB Schema 문서

## 1. 개요

- **DBMS**: MySQL / MariaDB 계열
- **Schema**: `albamm`
- **목적**: 알밤 서비스의 사용자, 사업장, 근태, 급여명세서, 휴게시간, 약관, 푸시 관련 테이블 구조를 관리합니다.

---

## 2. 테이블 요약

| 테이블명 | 설명 |
|---|---|
| `USERS` | 회원 및 로그인 정보 |
| `USER_SOCIAL_ACCOUNTS` | 소셜 로그인 연결 정보 |
| `TERMS` | 이용약관 및 개인정보처리방침 버전 관리 |
| `USER_TERMS_AGREEMENTS` | 사용자 약관 동의 이력 |
| `USER_WITHDRAWAL_REASONS` | 회원 탈퇴 사유 |
| `WORKPLACES` | 사업장 정보 |
| `WORKPLACE_MEMBERS` | 사업장 소속 사용자 및 권한 정보 |
| `WORKPLACE_SETTINGS` | 사업장별 근태/급여 계산 설정 |
| `WORKPLACE_BREAK_POLICIES` | 사업장별 휴게시간 정책 |
| `ATTENDANCE_RECORDS` | 출퇴근 및 근무시간 기록 |
| `ATTENDANCE_REQUESTS` | 근태 정정 요청 |
| `PAYSLIPS` | 급여명세서 |
| `PUSH_TOKENS` | 사용자 푸시 토큰 |
| `PASSWORD_RESET_CODES` | 비밀번호 재설정 인증 코드 |
| `NOTICES` | 공지사항 |
| `API_ERROR_LOGS` | API 에러 로그 |
| `LABOR_CONTRACTS` | 전자 근로계약서 |

---

## 3. 핵심 테이블 설명

### `USERS`
회원 계정의 기본 정보를 저장합니다. 일반 로그인 계정은 `LOGIN_ID`, `PASSWORD_HASH`를 사용하고, 소셜 로그인 계정은 `USER_SOCIAL_ACCOUNTS`와 연결됩니다.

### `WORKPLACES`
사업장 정보를 저장합니다. 사업장 소유자, 초대코드, 주소, 위치 기반 출퇴근 제한 여부, 출퇴근 허용 반경을 관리합니다.

### `WORKPLACE_MEMBERS`
사용자와 사업장의 소속 관계를 저장합니다. 역할, 급여 유형(시급제/월급제/일급제), 시급, 월급, 일급, 소속 상태, 출퇴근 푸시 수신 여부, 직원 메모를 관리합니다.

### `WORKPLACE_SETTINGS`
사업장별 근태/급여 계산 설정을 저장합니다. 급여 계산 단위, 반올림 정책, 기본 시급, 휴게시간 정책 사용 여부, 기본 출퇴근 시간을 관리합니다.

### `ATTENDANCE_RECORDS`
직원의 출퇴근 기록과 근무시간, 급여 계산 결과를 저장합니다. `(WORKPLACE_ID, USER_ID, WORK_DATE)` 기준으로 하루 1건만 생성됩니다.

### `ATTENDANCE_REQUESTS`
직원의 출퇴근 기록 정정 요청을 저장합니다. 출근 수정, 퇴근 수정, 출퇴근 모두 수정 요청을 구분합니다.

### `PAYSLIPS`
급여명세서 발행 정보를 저장합니다. 급여 기간, 급여 유형(시급제/월급제/일급제), 적용 시급·월급·일급, 근무일수, 근무시간, 기본급, 추가 지급액, 공제액, 최종 급여, 일별 근무 스냅샷을 관리합니다.

### `API_ERROR_LOGS`
API 요청 중 발생한 에러를 기록합니다. 요청 URI, HTTP 메서드, 컨트롤러명, 요청한 사용자/사업장 ID, 요청 파라미터·헤더, 에러 메시지, 클라이언트 IP, User-Agent를 저장합니다. 비인증 요청의 경우 `USER_ID`, `WORKPLACE_ID`는 null이 될 수 있습니다.

### `LABOR_CONTRACTS`
전자 근로계약서를 저장합니다. 점주가 계약 내용을 작성하고 직원에게 전송하면, 직원이 앱에서 내용을 확인 후 동의(서명) 또는 거절합니다. 계약서 내용은 발행 시점의 스냅샷으로 저장되어 이후 사업장 정보가 변경되어도 원본이 보존됩니다.

### `WORKPLACE_BREAK_POLICIES`
사업장별 휴게시간 정책을 저장합니다. 자동 차감 휴게와 고정 휴게를 구분하고, 유급/무급 여부를 관리합니다.

---

## 4. ENUM / 코드값 정의

### `USERS.USER_TYPE`
| 값 | 설명 |
|---|---|
| `OWNER` | 사장님 계정 |
| `STAFF` | 직원 계정 |
| `PERSONAL` | 개인 사용 계정 |
| `SUPER_ADMIN` | 관리자 계정 |

### `USERS.STATUS`
| 값 | 설명 |
|---|---|
| `ACTIVE` | 활성 계정 |
| `INACTIVE` | 비활성 계정 |
| `LOCKED` | 잠금 계정 |

### `WORKPLACE_MEMBERS.ROLE`
| 값 | 설명 |
|---|---|
| `OWNER` | 사업장 소유자 |
| `MANAGER` | 매니저 |
| `STAFF` | 직원 |

### `WORKPLACE_MEMBERS.WAGE_TYPE`
| 값 | 설명 |
|---|---|
| `HOURLY` | 시급제 |
| `MONTHLY` | 월급제 |
| `DAILY` | 일급제 |

### `WORKPLACE_MEMBERS.STATUS`
| 값 | 설명 |
|---|---|
| `ACTIVE` | 활성 소속 |
| `INVITED` | 초대 상태 |
| `INACTIVE` | 비활성 소속 |

### `ATTENDANCE_RECORDS.STATUS`
| 값 | 설명 |
|---|---|
| `WORKING` | 근무중 |
| `COMPLETED` | 퇴근 완료 |
| `LATE` | 지각 |
| `ABSENT` | 결근 |

### `ATTENDANCE_REQUESTS.TYPE`
| 값 | 설명 |
|---|---|
| `CHECK_IN_EDIT` | 출근 시각 수정 |
| `CHECK_OUT_EDIT` | 퇴근 시각 수정 |
| `BOTH_EDIT` | 출근/퇴근 시각 모두 수정 |

### `ATTENDANCE_REQUESTS.STATUS`
| 값 | 설명 |
|---|---|
| `PENDING` | 승인 대기 |
| `APPROVED` | 승인 |
| `REJECTED` | 반려 |

### `WORKPLACE_SETTINGS.SALARY_CALC_UNIT`
| 값 | 설명 |
|---|---|
| `MINUTE` | 1분 단위 계산 |
| `10MIN` | 10분 단위 계산 |
| `30MIN` | 30분 단위 계산 |
| `HOUR` | 1시간 단위 계산 |

### `WORKPLACE_SETTINGS.ROUNDING_POLICY`
| 값 | 설명 |
|---|---|
| `NONE` | 반올림 없음 |
| `FLOOR` | 버림 |
| `CEIL` | 올림 |
| `ROUND` | 반올림 |

### `WORKPLACE_BREAK_POLICIES.BREAK_TYPE`
| 값 | 설명 |
|---|---|
| `AUTO` | 근무시간 기준 자동 차감 |
| `FIXED` | 고정 휴게시간 |

### `PAYSLIPS.STATUS`
| 값 | 설명 |
|---|---|
| `ISSUED` | 발행됨 |
| `CANCELLED` | 취소됨 |

### `PUSH_TOKENS.PLATFORM`
| 값 | 설명 |
|---|---|
| `IOS` | iOS |
| `ANDROID` | Android |
| `WEB` | Web |

### `TERMS.TERMS_TYPE`
| 값 | 설명 |
|---|---|
| `TERMS_OF_SERVICE` | 서비스 이용약관 |
| `PRIVACY_POLICY` | 개인정보처리방침 |

### `USER_SOCIAL_ACCOUNTS.PROVIDER`
| 값 | 설명 |
|---|---|
| `KAKAO` | 카카오 로그인 |
| `APPLE` | 애플 로그인 |
| `GOOGLE` | 구글 로그인 |

### `USER_WITHDRAWAL_REASONS.REASON_TYPE`
| 값 | 설명 |
|---|---|
| `NOT_USED_OFTEN` | 자주 사용하지 않음 |
| `INCONVENIENT` | 사용이 불편함 |
| `PRIVACY_CONCERN` | 개인정보 우려 |
| `SWITCHING_SERVICE` | 다른 서비스로 이동 |
| `WORKPLACE_CLOSED` | 사업장 폐업/종료 |
| `OTHER` | 기타 |

### `LABOR_CONTRACTS.STATUS`
| 값 | 설명 |
|---|---|
| `DRAFT` | 작성 중 (점주가 저장만 한 상태) |
| `SENT` | 직원에게 전송됨 (직원 검토 대기) |
| `SIGNED` | 직원 서명 완료 |
| `REJECTED` | 직원이 거절 |
| `CANCELLED` | 점주가 취소 |

---

## 5. 테이블 관계

### 사용자 / 사업장

```text
USERS 1:N WORKPLACE_MEMBERS
WORKPLACES 1:N WORKPLACE_MEMBERS
```

### 근태

```text
USERS 1:N ATTENDANCE_RECORDS
WORKPLACES 1:N ATTENDANCE_RECORDS
ATTENDANCE_RECORDS 1:N ATTENDANCE_REQUESTS
```

### 급여명세서

```text
USERS 1:N PAYSLIPS
WORKPLACES 1:N PAYSLIPS
```

### 약관

```text
USERS 1:N USER_TERMS_AGREEMENTS
TERMS 1:N USER_TERMS_AGREEMENTS
```

### 전자 근로계약서

```text
WORKPLACES 1:N LABOR_CONTRACTS
USERS(직원) 1:N LABOR_CONTRACTS
```

---

## 6. 주요 제약조건

| 테이블 | 제약조건 | 설명 |
|---|---|---|
| `USERS` | `UK_USERS_LOGIN_ID` | 로그인 아이디 중복 방지 |
| `WORKPLACES` | `UK_WORKPLACES_INVITE_CODE` | 사업장 초대코드 중복 방지 |
| `WORKPLACE_MEMBERS` | `UK_WORKPLACE_MEMBERS_WORKPLACE_USER` | 동일 사업장 내 동일 사용자 중복 방지 |
| `WORKPLACE_SETTINGS` | `UK_WORKPLACE_SETTINGS_WORKPLACE_ID` | 사업장별 설정 1건 유지 |
| `ATTENDANCE_RECORDS` | `UK_ATTENDANCE_RECORDS_WORKPLACE_USER_WORKDATE` | 사업장/사용자/근무일 기준 근태 1건 유지 |
| `PUSH_TOKENS` | `UK_PUSH_TOKENS_TOKEN` | 동일 푸시 토큰 중복 방지 |
| `TERMS` | `UK_TERMS_TYPE_VERSION` | 약관 유형/버전 중복 방지 |
| `USER_SOCIAL_ACCOUNTS` | `UK_USER_SOCIAL_PROVIDER_USER` | 소셜 제공자별 사용자 중복 방지 |
| `USER_TERMS_AGREEMENTS` | `UK_USER_TERMS_AGREEMENTS_USER_TERMS` | 동일 약관 중복 동의 방지 |

---

### 전자 근로계약서 DDL
```sql
CREATE TABLE albamm.LABOR_CONTRACTS
(
    ID                    bigint unsigned auto_increment COMMENT '근로계약서 PK' PRIMARY KEY,
    WORKPLACE_ID          bigint unsigned                          NOT NULL COMMENT '사업장 ID',
    EMPLOYEE_USER_ID      bigint unsigned                          NOT NULL COMMENT '직원 사용자 ID',
    STATUS                varchar(20)    DEFAULT 'DRAFT'           NOT NULL COMMENT '계약 상태(DRAFT, SENT, SIGNED, REJECTED, CANCELLED)',
    CONTRACT_START_DATE   date                                     NOT NULL COMMENT '계약 시작일',
    CONTRACT_END_DATE     date                                     NULL COMMENT '계약 종료일(null이면 기간의 정함이 없는 근로계약)',
    WORKPLACE_NAME        varchar(150)                             NOT NULL COMMENT '사업장명 스냅샷',
    WORKPLACE_ADDRESS     varchar(255)                             NULL COMMENT '사업장 주소 스냅샷',
    OWNER_NAME            varchar(100)                             NOT NULL COMMENT '점주명 스냅샷',
    EMPLOYEE_NAME         varchar(100)                             NOT NULL COMMENT '직원명 스냅샷',
    JOB_DESCRIPTION       varchar(500)                             NULL COMMENT '담당 업무 내용',
    WORK_DAYS             tinyint unsigned                         NOT NULL COMMENT '근무 요일 비트마스크(bit0=월, bit1=화, bit2=수, bit3=목, bit4=금, bit5=토, bit6=일)',
    WORK_START_TIME       time                                     NOT NULL COMMENT '근무 시작 시간',
    WORK_END_TIME         time                                     NOT NULL COMMENT '근무 종료 시간',
    BREAK_MINUTES         int unsigned   DEFAULT 0                 NOT NULL COMMENT '휴게시간(분)',
    HOURLY_WAGE           decimal(10, 2) DEFAULT 0.00              NOT NULL COMMENT '시급',
    PAYMENT_DAY           tinyint unsigned                         NOT NULL COMMENT '급여 지급일(1~31)',
    USE_NATIONAL_PENSION  tinyint(1)     DEFAULT 0                 NOT NULL COMMENT '국민연금 적용 여부',
    USE_HEALTH_INSURANCE  tinyint(1)     DEFAULT 0                 NOT NULL COMMENT '건강보험 적용 여부',
    USE_EMP_INSURANCE     tinyint(1)     DEFAULT 0                 NOT NULL COMMENT '고용보험 적용 여부',
    OWNER_SIGNED_AT       datetime                                 NULL COMMENT '점주 서명 일시',
    EMPLOYEE_SIGNED_AT    datetime                                 NULL COMMENT '직원 서명 일시',
    SENT_AT               datetime                                 NULL COMMENT '직원에게 전송한 일시',
    REJECTED_REASON       varchar(500)                             NULL COMMENT '직원 거절 사유',
    CREATED_AT            timestamp      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    UPDATED_AT            timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시'
) COMMENT '전자 근로계약서' CHARSET = utf8mb4;

CREATE INDEX IDX_LABOR_CONTRACTS_WORKPLACE_ID
    ON albamm.LABOR_CONTRACTS (WORKPLACE_ID);

CREATE INDEX IDX_LABOR_CONTRACTS_EMPLOYEE_USER_ID
    ON albamm.LABOR_CONTRACTS (EMPLOYEE_USER_ID);

CREATE INDEX IDX_LABOR_CONTRACTS_STATUS
    ON albamm.LABOR_CONTRACTS (STATUS);
```

---

## 7. 설계 메모

- 근태 기록은 `WORK_DATE` 기준으로 하루 1건을 유지합니다.
- 급여명세서는 발행 시점의 데이터를 보존하는 snapshot 구조입니다.
- 휴게시간 정책 사용 여부는 `WORKPLACE_SETTINGS.USE_BREAK_POLICY`로 제어하고, 실제 정책은 `WORKPLACE_BREAK_POLICIES`에서 관리합니다.
- 기본 출퇴근 시간은 `WORKPLACE_SETTINGS.DEFAULT_CHECK_IN_TIME`, `WORKPLACE_SETTINGS.DEFAULT_CHECK_OUT_TIME`에서 관리합니다.
- 주휴수당은 `WORKPLACE_SETTINGS.USE_WEEKLY_HOLIDAY_PAY`로 활성화하며, 급여명세서 발행 시 주 단위로 계산되어 `PAYSLIPS.WEEKLY_HOLIDAY_PAY`에 저장됩니다. 주 15시간(900분) 이상 근무한 주에 대해 `(주 근무분 / 2400) × 8시간 × 시급` 공식으로 계산합니다.

### 직원 스케줄 테이블 DDL
```sql
CREATE TABLE WORKPLACE_MEMBER_SCHEDULES
(
    ID                       bigint unsigned auto_increment COMMENT '스케줄 PK' PRIMARY KEY,
    WORKPLACE_ID             bigint unsigned NOT NULL COMMENT '사업장 ID',
    USER_ID                  bigint unsigned NOT NULL COMMENT '직원 사용자 ID',
    DAY_OF_WEEK              tinyint unsigned NOT NULL COMMENT '요일 (1=월, 2=화, 3=수, 4=목, 5=금, 6=토, 7=일)',
    SCHEDULED_CHECK_IN_TIME  time NULL COMMENT '예정 출근 시간 (null이면 매장 기본값)',
    SCHEDULED_CHECK_OUT_TIME time NULL COMMENT '예정 퇴근 시간 (null이면 매장 기본값)',
    CREATED_AT               timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일시',
    UPDATED_AT               timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '수정일시',
    CONSTRAINT UK_MEMBER_SCHEDULE_DOW UNIQUE (WORKPLACE_ID, USER_ID, DAY_OF_WEEK)
) COMMENT '직원별 근무 요일 스케줄' CHARSET = utf8mb4;
```

### 주휴수당 컬럼 추가 DDL
```sql
ALTER TABLE WORKPLACE_SETTINGS
    ADD COLUMN USE_WEEKLY_HOLIDAY_PAY TINYINT(1) DEFAULT 0 NOT NULL COMMENT '주휴수당 사용 여부';

ALTER TABLE PAYSLIPS
    ADD COLUMN WEEKLY_HOLIDAY_PAY DECIMAL(12, 2) DEFAULT 0.00 NOT NULL COMMENT '주휴수당 금액';
```

#### 장시간 근무 알림 컬럼 추가 DDL
```sql
ALTER TABLE ATTENDANCE_RECORDS
    ADD COLUMN LONG_WORKING_NOTIFIED TINYINT(1) NOT NULL DEFAULT 0 COMMENT '장시간 근무 알림 발송 여부';
```

### 월급제 지원 컬럼 추가 DDL
```sql
ALTER TABLE WORKPLACE_MEMBERS
    ADD COLUMN WAGE_TYPE    VARCHAR(10)    DEFAULT 'HOURLY' NOT NULL COMMENT '급여 유형(HOURLY: 시급제, MONTHLY: 월급제)'
        AFTER ROLE,
    ADD COLUMN MONTHLY_WAGE DECIMAL(12, 2) DEFAULT NULL     NULL     COMMENT '월급(월급제일 때 사용)'
        AFTER HOURLY_WAGE;

ALTER TABLE PAYSLIPS
    ADD COLUMN WAGE_TYPE    VARCHAR(10)    DEFAULT 'HOURLY' NOT NULL COMMENT '급여 유형(HOURLY: 시급제, MONTHLY: 월급제)'
        AFTER TO_DATE,
    ADD COLUMN MONTHLY_WAGE DECIMAL(12, 2) DEFAULT 0.00     NOT NULL COMMENT '월급 스냅샷(월급제일 때 사용)'
        AFTER HOURLY_WAGE;

ALTER TABLE LABOR_CONTRACTS
    ADD COLUMN WAGE_TYPE    VARCHAR(10)    DEFAULT 'HOURLY' NOT NULL COMMENT '급여 유형(HOURLY: 시급제, MONTHLY: 월급제)'
        AFTER BREAK_MINUTES,
    ADD COLUMN MONTHLY_WAGE DECIMAL(12, 2) DEFAULT 0.00     NOT NULL COMMENT '월급(월급제일 때 사용)'
        AFTER HOURLY_WAGE;

-- 2026-06-08: 직원별 주휴수당 설정
ALTER TABLE WORKPLACE_MEMBERS
    ADD COLUMN USE_WEEKLY_HOLIDAY_PAY TINYINT(1) DEFAULT NULL
        COMMENT '직원별 주휴수당 사용 여부, NULL이면 매장 설정(WORKPLACE_SETTINGS.USE_WEEKLY_HOLIDAY_PAY) 상속'
        AFTER BREAK_MINUTES;

-- 2026-06-10: 일급제 지원
ALTER TABLE WORKPLACE_MEMBERS
    ADD COLUMN DAILY_WAGE DECIMAL(12, 2) DEFAULT NULL NULL COMMENT '일급(일급제일 때 사용)'
        AFTER MONTHLY_WAGE;

ALTER TABLE PAYSLIPS
    ADD COLUMN DAILY_WAGE DECIMAL(12, 2) DEFAULT 0.00 NOT NULL COMMENT '일급 스냅샷(일급제일 때 사용)'
        AFTER MONTHLY_WAGE;

ALTER TABLE LABOR_CONTRACTS
    ADD COLUMN DAILY_WAGE DECIMAL(12, 2) DEFAULT 0.00 NOT NULL COMMENT '일급(일급제일 때 사용)'
        AFTER MONTHLY_WAGE;
```

### QR 출퇴근 컬럼 추가 DDL
```sql
ALTER TABLE WORKPLACES
    ADD COLUMN USE_QR_ATTENDANCE TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'QR 출퇴근 사용 여부 (위치 제한과 상호 배타)';

ALTER TABLE WORKPLACES
    ADD COLUMN QR_NO_TIME_LIMIT TINYINT(1) NULL DEFAULT 0 COMMENT 'QR 토큰 시간제한 없음 여부 (1이면 영구 QR)';
```

---

## 8. 변경 이력

| 일자 | 변경 내용 |
|---|---|
| 2026-04-29 | `WORKPLACE_SETTINGS.DEFAULT_CHECK_IN_TIME` 추가 |
| 2026-04-29 | `WORKPLACE_SETTINGS.DEFAULT_CHECK_OUT_TIME` 추가 |
| 2026-04-29 | `PAYSLIPS` 테이블 추가 |
| 2026-04-29 | `WORKPLACE_MEMBERS.BREAK_MINUTES` 추가 (직원별 무급 휴게 설정) |
| 2026-05-04 | `WORKPLACE_SETTINGS.USE_WEEKLY_HOLIDAY_PAY` 추가 (주휴수당 사용 여부) |
| 2026-05-04 | `PAYSLIPS.WEEKLY_HOLIDAY_PAY` 추가 (주휴수당 금액) |
| 2026-05-04 | `WORKPLACE_MEMBER_SCHEDULES` 테이블 추가 (직원별 근무 요일 스케줄) |
| 2026-05-12 | `API_ERROR_LOGS` 테이블 추가 (API 에러 로그 및 요청 파라미터 확인용) |
| 2026-05-13 | `LABOR_CONTRACTS` 테이블 추가 (전자 근로계약서) |
| 2026-05-14 | `WORKPLACE_MEMBERS.WAGE_TYPE`, `WORKPLACE_MEMBERS.MONTHLY_WAGE` 추가 (월급제 지원) |
| 2026-05-14 | `PAYSLIPS.WAGE_TYPE`, `PAYSLIPS.MONTHLY_WAGE` 추가 (월급제 급여명세서 스냅샷) |
| 2026-05-14 | `LABOR_CONTRACTS.WAGE_TYPE`, `LABOR_CONTRACTS.MONTHLY_WAGE` 추가 (월급제 계약서 지원) |
| 2026-05-19 | `ATTENDANCE_RECORDS.LONG_WORKING_NOTIFIED` 추가 (장시간 근무 알림 발송 여부) |
| 2026-06-06 | `USERS.NAME`, `USERS.PROFILE_INITIAL` — 이름 변경 API 추가 (`PATCH /api/staff/name`, 이름 변경 시 PROFILE_INITIAL 자동 갱신) |
| 2026-06-08 | `WORKPLACE_MEMBERS.USE_WEEKLY_HOLIDAY_PAY` 추가 (직원별 주휴수당 사용 여부, NULL이면 매장 설정 상속) |
| 2026-06-08 | `WORKPLACES.USE_QR_ATTENDANCE` 추가 (QR 출퇴근 사용 여부, 위치 제한과 상호 배타) |
| 2026-06-11 | `WORKPLACES.QR_NO_TIME_LIMIT` 추가 (QR 토큰 시간제한 없음 여부) |
| 2026-06-10 | `WORKPLACE_MEMBERS.DAILY_WAGE` 추가 (일급제 지원) |
| 2026-06-10 | `PAYSLIPS.DAILY_WAGE` 추가 (일급제 급여명세서 스냅샷) |
| 2026-06-10 | `LABOR_CONTRACTS.DAILY_WAGE` 추가 (일급제 계약서 지원) |

---

## 9. 원본 DDL

```sql
create table albamm.ATTENDANCE_RECORDS
(
ID             bigint unsigned auto_increment comment '근태 기록 PK'
primary key,
WORKPLACE_ID   bigint unsigned                          not null comment '사업장 ID',
USER_ID        bigint unsigned                          not null comment '사용자 ID',
WORK_DATE      date                                     not null comment '근무일',
CHECK_IN_AT    datetime                                 null comment '출근 시각',
CHECK_OUT_AT   datetime                                 null comment '퇴근 시각',
STATUS         varchar(30)    default 'WORKING'         not null comment '근태 상태(WORKING, COMPLETED, LATE, ABSENT)',
WORKED_MINUTES int unsigned   default 0                 not null comment '총 근무 분',
BASE_WAGE      decimal(12, 2) default 0.00              not null comment '기본 계산 급여',
FINAL_WAGE     decimal(12, 2) default 0.00              not null comment '최종 급여',
NOTE           varchar(500)                             null comment '비고',
CREATED_AT     timestamp      default CURRENT_TIMESTAMP not null comment '생성일시',
UPDATED_AT     timestamp      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '수정일시',
constraint UK_ATTENDANCE_RECORDS_WORKPLACE_USER_WORKDATE
unique (WORKPLACE_ID, USER_ID, WORK_DATE)
)
comment '출퇴근 및 근무시간 기록' charset = utf8mb4;

create index IDX_ATTENDANCE_RECORDS_STATUS
on albamm.ATTENDANCE_RECORDS (STATUS);

create index IDX_ATTENDANCE_RECORDS_USER_ID
on albamm.ATTENDANCE_RECORDS (USER_ID);

create index IDX_ATTENDANCE_RECORDS_WORK_DATE
on albamm.ATTENDANCE_RECORDS (WORK_DATE);

create table albamm.ATTENDANCE_REQUESTS
(
ID                     bigint unsigned auto_increment comment '근태 정정 요청 PK'
primary key,
ATTENDANCE_RECORD_ID   bigint unsigned                       not null comment '대상 근태 기록 ID',
USER_ID                bigint unsigned                       not null comment '요청 사용자 ID',
TYPE                   varchar(30)                           not null comment '요청 유형(CHECK_IN_EDIT, CHECK_OUT_EDIT, BOTH_EDIT)',
REQUESTED_CHECK_IN_AT  datetime                              null comment '요청 출근 시각',
REQUESTED_CHECK_OUT_AT datetime                              null comment '요청 퇴근 시각',
REASON                 varchar(1000)                         null comment '요청 사유',
STATUS                 varchar(30) default 'PENDING'         not null comment '요청 상태(PENDING, APPROVED, REJECTED)',
CREATED_AT             timestamp   default CURRENT_TIMESTAMP not null comment '생성일시'
)
comment '근태 정정 요청' charset = utf8mb4;

create index IDX_ATTENDANCE_REQUESTS_RECORD_ID
on albamm.ATTENDANCE_REQUESTS (ATTENDANCE_RECORD_ID);

create index IDX_ATTENDANCE_REQUESTS_STATUS
on albamm.ATTENDANCE_REQUESTS (STATUS);

create index IDX_ATTENDANCE_REQUESTS_TYPE
on albamm.ATTENDANCE_REQUESTS (TYPE);

create index IDX_ATTENDANCE_REQUESTS_USER_ID
on albamm.ATTENDANCE_REQUESTS (USER_ID);

create table albamm.NOTICES
(
ID         bigint unsigned auto_increment comment 'Notice PK'
primary key,
TITLE      varchar(200)                         not null comment 'Notice title',
CONTENT    varchar(5000)                        not null comment 'Notice content',
IS_PINNED  tinyint(1) default 0                 not null comment 'Pinned flag',
CREATED_AT timestamp  default CURRENT_TIMESTAMP not null comment 'Created at',
UPDATED_AT timestamp  default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Updated at'
)
comment 'Global notices' charset = utf8mb4;

create index IDX_NOTICES_CREATED_AT
on albamm.NOTICES (CREATED_AT);

create index IDX_NOTICES_PINNED_CREATED_AT
on albamm.NOTICES (IS_PINNED, CREATED_AT);

create table albamm.WORKPLACE_NOTICES
(
    ID           bigint unsigned auto_increment comment '매장 공지사항 PK'
        primary key,
    WORKPLACE_ID bigint unsigned                       not null comment '매장 FK',
    AUTHOR_ID    bigint unsigned                       not null comment '작성자 사용자 FK',
    TITLE        varchar(200)                          not null comment '공지 제목',
    CONTENT      varchar(5000)                         not null comment '공지 내용',
    IS_PINNED    tinyint(1) default 0                  not null comment '상단 고정 여부',
    CREATED_AT   timestamp  default CURRENT_TIMESTAMP  not null comment '생성일시',
    UPDATED_AT   timestamp  default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment '수정일시'
)
    comment '매장별 공지사항' charset = utf8mb4;

create index IDX_WORKPLACE_NOTICES_WORKPLACE_ID
    on albamm.WORKPLACE_NOTICES (WORKPLACE_ID, IS_PINNED, CREATED_AT);

create table albamm.PASSWORD_RESET_CODES
(
ID         bigint unsigned auto_increment comment 'Password reset code PK'
primary key,
USER_ID    bigint unsigned                     not null comment 'Target user ID',
EMAIL      varchar(191)                        not null comment 'Recipient email',
CODE       varchar(6)                          not null comment '6-digit reset code',
EXPIRES_AT datetime                            not null comment 'Code expiration time',
USED_AT    datetime                            null comment 'Code used time',
CREATED_AT timestamp default CURRENT_TIMESTAMP not null comment 'Created at'
)
comment 'Password reset verification codes' charset = utf8mb4;

create index IDX_PASSWORD_RESET_CODES_CODE
on albamm.PASSWORD_RESET_CODES (CODE);

create index IDX_PASSWORD_RESET_CODES_EXPIRES_AT
on albamm.PASSWORD_RESET_CODES (EXPIRES_AT);

create index IDX_PASSWORD_RESET_CODES_USER_ID
on albamm.PASSWORD_RESET_CODES (USER_ID);

create table albamm.PAYSLIPS
(
ID               bigint unsigned auto_increment comment '급여명세서 PK'
primary key,
WORKPLACE_ID     bigint unsigned                          not null comment '사업장 ID',
USER_ID          bigint unsigned                          not null comment '직원 사용자 ID',
FROM_DATE        date                                     not null comment '급여 기간 시작일',
TO_DATE          date                                     not null comment '급여 기간 종료일',
HOURLY_WAGE      decimal(10, 2) default 0.00              not null comment '적용 시급(발행 시점 스냅샷)',
WORKED_DAYS      int unsigned   default 0                 not null comment '총 근무일수',
WORKED_MINUTES   int unsigned   default 0                 not null comment '총 근무시간(분)',
BASE_WAGE        decimal(12, 2) default 0.00              not null comment '기본 급여(근무 기준 계산)',
BONUS_AMOUNT     decimal(12, 2) default 0.00              not null comment '추가 지급액',
BONUS_NOTE       varchar(500)                             null comment '추가 지급 사유',
DEDUCTION_AMOUNT decimal(12, 2) default 0.00              not null comment '공제액',
DEDUCTION_NOTE   varchar(500)                             null comment '공제 사유',
TOTAL_WAGE       decimal(12, 2) default 0.00              not null comment '최종 급여(기본 + 추가 - 공제)',
DAILY_SNAPSHOT   longtext                                 not null comment '일별 근무 내역 스냅샷(JSON)',
STATUS           varchar(20)    default 'ISSUED'          not null comment '명세서 상태(ISSUED, CANCELLED, CONFIRMED)',
CREATED_AT       timestamp      default CURRENT_TIMESTAMP not null comment '발행일시',
UPDATED_AT       timestamp      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '수정일시'
)
comment '급여 명세서' charset = utf8mb4;

create index IDX_PAYSLIPS_FROM_DATE
on albamm.PAYSLIPS (FROM_DATE);

create index IDX_PAYSLIPS_STATUS
on albamm.PAYSLIPS (STATUS);

create index IDX_PAYSLIPS_USER_ID
on albamm.PAYSLIPS (USER_ID);

create index IDX_PAYSLIPS_WORKPLACE_ID
on albamm.PAYSLIPS (WORKPLACE_ID);

create table albamm.PUSH_TOKENS
(
ID           bigint unsigned auto_increment comment '푸시 토큰 PK'
primary key,
USER_ID      bigint unsigned                      not null comment '사용자 ID',
PLATFORM     varchar(20)                          not null comment 'IOS, ANDROID, WEB',
TOKEN        varchar(191)                         not null comment 'FCM/APNS 토큰',
IS_ACTIVE    tinyint(1) default 1                 not null comment '사용 가능 여부',
LAST_SEEN_AT datetime                             null comment '앱에서 마지막 갱신 시각',
CREATED_AT   timestamp  default CURRENT_TIMESTAMP not null comment '생성일시',
UPDATED_AT   timestamp  default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '수정일시',
constraint UK_PUSH_TOKENS_TOKEN
unique (TOKEN)
)
comment '사용자 푸시 토큰' charset = utf8mb4;

create index IDX_PUSH_TOKENS_USER_ACTIVE
on albamm.PUSH_TOKENS (USER_ID, IS_ACTIVE);

create index IDX_PUSH_TOKENS_USER_ID
on albamm.PUSH_TOKENS (USER_ID);

create table albamm.TERMS
(
ID           bigint unsigned auto_increment comment '약관 PK'
primary key,
TERMS_TYPE   varchar(30)                          not null comment '약관 유형(TERMS_OF_SERVICE, PRIVACY_POLICY)',
VERSION      varchar(10)                          not null comment '약관 버전(예: 1.0, 1.1)',
TITLE        varchar(200)                         not null comment '약관 제목',
CONTENT      longtext                             not null comment '약관 본문',
IS_REQUIRED  tinyint(1) default 1                 not null comment '필수 동의 여부(1: 필수, 0: 선택)',
IS_ACTIVE    tinyint(1) default 0                 not null comment '현재 유효한 버전 여부(1: 활성, 0: 비활성)',
EFFECTIVE_AT datetime                             not null comment '약관 시행일',
CREATED_AT   timestamp  default CURRENT_TIMESTAMP not null comment '생성일시',
constraint UK_TERMS_TYPE_VERSION
unique (TERMS_TYPE, VERSION)
)
comment '이용약관 및 개인정보처리방침 버전 관리' charset = utf8mb4;

create index IDX_TERMS_TYPE_ACTIVE
on albamm.TERMS (TERMS_TYPE, IS_ACTIVE);

create table albamm.USERS
(
ID              bigint unsigned auto_increment comment '사용자 PK'
primary key,
LOGIN_ID        varchar(50)                           null comment '일반 로그인 아이디',
PASSWORD_HASH   varchar(255)                          null comment '일반 로그인 비밀번호 해시',
NAME            varchar(100)                          not null comment '사용자 이름',
USER_TYPE       varchar(30) default 'STAFF'           not null comment '사용자 유형(OWNER, STAFF, PERSONAL, SUPER_ADMIN)',
STATUS          varchar(20) default 'ACTIVE'          not null comment '계정 상태(ACTIVE, INACTIVE, LOCKED)',
LAST_LOGIN_AT   datetime                              null comment '마지막 로그인 일시',
CREATED_AT      timestamp   default CURRENT_TIMESTAMP not null comment '생성일시',
UPDATED_AT      timestamp   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '수정일시',
PROFILE_INITIAL varchar(2)  default '?'               not null comment '프로필 이니셜',
PROFILE_COLOR   varchar(7)  default '#3B82F6'         not null comment '프로필 색상(#RRGGBB)',
constraint UK_USERS_LOGIN_ID
unique (LOGIN_ID)
)
comment '회원 및 로그인 정보' charset = utf8mb4;

create index IDX_USERS_STATUS
on albamm.USERS (STATUS);

create index IDX_USERS_USER_TYPE
on albamm.USERS (USER_TYPE);

create table albamm.USER_SOCIAL_ACCOUNTS
(
ID               bigint unsigned auto_increment comment '소셜 계정 연결 PK'
primary key,
USER_ID          bigint unsigned                     not null comment '사용자 ID',
PROVIDER         varchar(30)                         not null comment '소셜 제공자(KAKAO, APPLE, GOOGLE)',
PROVIDER_USER_ID varchar(191)                        not null comment '소셜 제공자의 사용자 고유 ID',
PROVIDER_EMAIL   varchar(255)                        null comment '소셜 제공자 이메일',
PROVIDER_NAME    varchar(100)                        null comment '소셜 제공자 이름',
ACCESS_TOKEN     varchar(1000)                       null comment '소셜 액세스 토큰(필요 시만 저장)',
REFRESH_TOKEN    varchar(1000)                       null comment '소셜 리프레시 토큰(필요 시만 저장)',
TOKEN_EXPIRES_AT datetime                            null comment '토큰 만료 일시',
CONNECTED_AT     timestamp default CURRENT_TIMESTAMP not null comment '연결일시',
LAST_LOGIN_AT    datetime                            null comment '해당 소셜 계정 마지막 로그인 일시',
constraint UK_USER_SOCIAL_PROVIDER_USER
unique (PROVIDER, PROVIDER_USER_ID)
)
comment '사용자 소셜 로그인 연결 정보' charset = utf8mb4;

create index IDX_USER_SOCIAL_PROVIDER_EMAIL
on albamm.USER_SOCIAL_ACCOUNTS (PROVIDER_EMAIL(191));

create index IDX_USER_SOCIAL_USER_ID
on albamm.USER_SOCIAL_ACCOUNTS (USER_ID);

create table albamm.USER_TERMS_AGREEMENTS
(
ID        bigint unsigned auto_increment comment '동의 이력 PK'
primary key,
USER_ID   bigint unsigned                     not null comment '사용자 ID',
TERMS_ID  bigint unsigned                     not null comment '동의한 약관 ID',
AGREED_AT timestamp default CURRENT_TIMESTAMP not null comment '동의 일시',
constraint UK_USER_TERMS_AGREEMENTS_USER_TERMS
unique (USER_ID, TERMS_ID)
)
comment '사용자 약관 동의 이력' charset = utf8mb4;

create index IDX_USER_TERMS_AGREEMENTS_TERMS_ID
on albamm.USER_TERMS_AGREEMENTS (TERMS_ID);

create index IDX_USER_TERMS_AGREEMENTS_USER_ID
on albamm.USER_TERMS_AGREEMENTS (USER_ID);

create table albamm.USER_WITHDRAWAL_REASONS
(
ID            bigint unsigned auto_increment comment '탈퇴 사유 PK'
primary key,
USER_ID       bigint unsigned                     not null comment '탈퇴한 사용자 ID',
REASON_TYPE   varchar(50)                         not null comment '탈퇴 사유 유형(NOT_USED_OFTEN, INCONVENIENT, PRIVACY_CONCERN, SWITCHING_SERVICE, WORKPLACE_CLOSED, OTHER)',
CUSTOM_REASON varchar(1000)                       null comment '기타 직접 입력 사유',
CREATED_AT    timestamp default CURRENT_TIMESTAMP not null comment '생성일시'
)
comment '회원 탈퇴 사유' charset = utf8mb4;

create index IDX_USER_WITHDRAWAL_REASONS_REASON_TYPE
on albamm.USER_WITHDRAWAL_REASONS (REASON_TYPE);

create index IDX_USER_WITHDRAWAL_REASONS_USER_ID
on albamm.USER_WITHDRAWAL_REASONS (USER_ID);

create table albamm.WORKPLACES
(
ID                       bigint unsigned auto_increment comment '사업장 PK'
primary key,
OWNER_ID                 bigint unsigned                        not null comment '사업장 소유자 사용자 ID',
NAME                     varchar(150)                           not null comment '사업장명',
ADDRESS                  varchar(255)                           null comment '사업장 주소',
INVITE_CODE              varchar(20)                            not null comment '사업장 초대코드',
LATITUDE                 decimal(10, 7)                         null comment '사업장 기준 위도',
LONGITUDE                decimal(10, 7)                         null comment '사업장 기준 경도',
ALLOWED_RADIUS_METERS    int unsigned default 100               not null comment '출퇴근 허용 반경(m)',
USE_LOCATION_RESTRICTION tinyint(1)   default 0                 not null comment '위치 기반 출퇴근 제한 사용 여부',
CREATED_AT               timestamp    default CURRENT_TIMESTAMP not null comment '생성일시',
IS_PERSONAL              tinyint(1)   default 0                 not null comment '개인 모드 사업장 여부',
constraint UK_WORKPLACES_INVITE_CODE
unique (INVITE_CODE)
)
comment '사업장 정보' charset = utf8mb4;

create index IDX_WORKPLACES_OWNER_ID
on albamm.WORKPLACES (OWNER_ID);

create table albamm.WORKPLACE_BREAK_POLICIES
(
ID               bigint unsigned auto_increment comment '휴게시간 정책 PK'
primary key,
WORKPLACE_ID     bigint unsigned                      not null comment '사업장 ID',
NAME             varchar(100)                         not null comment '휴게시간 정책명',
BREAK_TYPE       varchar(20)                          not null comment '휴게 유형(AUTO: 자동 차감, FIXED: 고정 휴게)',
MIN_WORK_MINUTES int unsigned                         null comment '최소 근무시간(분)',
BREAK_MINUTES    int unsigned                         not null comment '휴게시간(분)',
IS_PAID          tinyint(1) default 0                 not null comment '유급 휴게 여부',
IS_ACTIVE        tinyint(1) default 1                 not null comment '사용 여부',
CREATED_AT       timestamp  default CURRENT_TIMESTAMP not null comment '생성일시',
UPDATED_AT       timestamp  default CURRENT_TIMESTAMP not null comment '수정일시',
constraint FK_WORKPLACE_BREAK_POLICIES_WORKPLACE_ID
foreign key (WORKPLACE_ID) references albamm.WORKPLACES (ID)
)
comment '사업장별 휴게시간 정책' charset = utf8mb4;

create table albamm.WORKPLACE_MEMBERS
(
ID                      bigint unsigned auto_increment comment '사업장 멤버 PK'
primary key,
WORKPLACE_ID            bigint unsigned                       not null comment '사업장 ID',
USER_ID                 bigint unsigned                       not null comment '사용자 ID',
ROLE                    varchar(30)                           not null comment '사업장 내 역할(OWNER, MANAGER, STAFF)',
HOURLY_WAGE             decimal(10, 2)                        null comment '시급',
JOINED_AT               timestamp   default CURRENT_TIMESTAMP not null comment '합류일시',
STATUS                  varchar(30) default 'ACTIVE'          not null comment '소속 상태(ACTIVE, INVITED, INACTIVE)',
RECEIVE_ATTENDANCE_PUSH tinyint(1)  default 1                 not null comment '직원 출퇴근 푸시 수신 여부',
MEMO                    varchar(1000)                         null comment '직원 메모',
BREAK_MINUTES           int unsigned                          null comment '직원별 무급 휴게(분), null이면 매장 정책 적용',
USE_WEEKLY_HOLIDAY_PAY  tinyint(1)                            null comment '직원별 주휴수당 사용 여부, null이면 매장 설정 상속',
constraint UK_WORKPLACE_MEMBERS_WORKPLACE_USER
unique (WORKPLACE_ID, USER_ID)
)
comment '사업장 소속 사용자 및 권한 정보' charset = utf8mb4;

create index IDX_WORKPLACE_MEMBERS_ROLE
on albamm.WORKPLACE_MEMBERS (ROLE);

create index IDX_WORKPLACE_MEMBERS_STATUS
on albamm.WORKPLACE_MEMBERS (STATUS);

create index IDX_WORKPLACE_MEMBERS_USER_ID
on albamm.WORKPLACE_MEMBERS (USER_ID);

create table albamm.WORKPLACE_SETTINGS
(
ID                     bigint unsigned auto_increment comment '사업장 설정 PK'
primary key,
WORKPLACE_ID           bigint unsigned                not null comment '사업장 ID',
LATE_GRACE_MINUTES     int unsigned   default 0       not null comment '지각 인정 유예 분',
SALARY_CALC_UNIT       varchar(20)    default '10MIN' not null comment '급여 계산 단위(MINUTE, 10MIN, HOUR)',
ROUNDING_POLICY        varchar(30)    default 'NONE'  not null comment '반올림 정책(NONE, FLOOR, CEIL, ROUND)',
DEFAULT_HOURLY_WAGE    decimal(10, 2) default 0.00    not null comment '사업장 기본 시급',
USE_BREAK_POLICY       tinyint(1)     default 0       not null comment '휴게시간 정책 사용 여부',
DEFAULT_CHECK_IN_TIME  time                           null comment '기본 출근 시간',
DEFAULT_CHECK_OUT_TIME time                           null comment '기본 퇴근 시간',
constraint UK_WORKPLACE_SETTINGS_WORKPLACE_ID
unique (WORKPLACE_ID)
)
comment '사업장별 근태/급여 계산 설정' charset = utf8mb4;

create table albamm.PAYSLIP_DEDUCTIONS
(
    ID             bigint unsigned auto_increment comment '급여명세서 공제 항목 PK'
        primary key,
    PAYSLIP_ID     bigint unsigned                          not null comment '급여명세서 ID',
    DEDUCTION_TYPE varchar(50)    default 'ETC'             not null comment '공제 유형(NATIONAL_PENSION: 국민연금, HEALTH_INSURANCE: 건강보험, LONG_TERM_CARE: 장기요양보험, EMPLOYMENT_INSURANCE: 고용보험, INCOME_TAX: 소득세, LOCAL_INCOME_TAX: 지방소득세, ETC: 기타)',
    NAME           varchar(100)                             not null comment '공제 항목명',
    AMOUNT         decimal(12, 2) default 0.00              not null comment '공제 금액',
    NOTE           varchar(500)                             null comment '공제 메모',
    DISPLAY_ORDER  int unsigned   default 0                 not null comment '표시 순서',
    CREATED_AT     timestamp      default CURRENT_TIMESTAMP not null comment '생성일시',
    UPDATED_AT     timestamp      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '수정일시',
    constraint FK_PAYSLIP_DEDUCTIONS_PAYSLIP_ID
        foreign key (PAYSLIP_ID) references albamm.PAYSLIPS (ID)
)
    comment '급여명세서 공제 항목 상세' charset = utf8mb4;

create index IDX_PAYSLIP_DEDUCTIONS_PAYSLIP_ID
    on albamm.PAYSLIP_DEDUCTIONS (PAYSLIP_ID);

create index IDX_PAYSLIP_DEDUCTIONS_TYPE
    on albamm.PAYSLIP_DEDUCTIONS (DEDUCTION_TYPE);



create table albamm.API_ERROR_LOGS
(
    ID             bigint unsigned auto_increment comment '에러 로그 PK'
        primary key,
    REQUEST_URI    varchar(500)                          null comment '요청 URI',
    HTTP_METHOD    varchar(10)                           null comment 'HTTP Method(GET, POST 등)',
    CONTROLLER     varchar(200)                          null comment '컨트롤러/핸들러명',
    USER_ID        bigint unsigned                       null comment '요청 사용자 ID',
    WORKPLACE_ID   bigint unsigned                       null comment '사업장 ID',
    REQUEST_PARAMS longtext                              null comment '요청 파라미터/쿼리스트링/바디 JSON',
    REQUEST_HEADERS longtext                             null comment '요청 헤더 JSON',
    ERROR_MESSAGE  varchar(2000)                         null comment '에러 메시지',
    CLIENT_IP     varchar(45)                           null comment '요청 IP',
    USER_AGENT     varchar(500)                          null comment 'User-Agent',
    CREATED_AT     timestamp default CURRENT_TIMESTAMP   not null comment '생성일시'
)
    comment 'API 에러 로그 및 요청 파라미터 확인용' charset = utf8mb4;

create index IDX_API_ERROR_LOGS_CREATED_AT
    on albamm.API_ERROR_LOGS (CREATED_AT);

create index IDX_API_ERROR_LOGS_USER_ID
    on albamm.API_ERROR_LOGS (USER_ID);

create index IDX_API_ERROR_LOGS_WORKPLACE_ID
    on albamm.API_ERROR_LOGS (WORKPLACE_ID);


```

