# 근태 흐름 정리

## 출퇴근 상태값

| 상태 | 설명 |
|------|------|
| `WORKING` | 출근 완료, 아직 퇴근 전 |
| `COMPLETED` | 정상 퇴근 |
| `LATE` | 지각 퇴근 (출근 시간 기준 초과 판정) |

---

## 출근 (checkIn)

- `workDate`는 클라이언트가 넘기거나, 없으면 `LocalDate.now()` 사용
- 같은 `(workplaceId, userId, workDate)` 조합으로 이미 레코드가 있으면 `ALREADY_CHECKED_IN_FOR_DATE` 에러
- 레코드 생성 시 status = `WORKING`, workedMinutes/wage = 0으로 초기화
- 위치 제한(`useLocationRestriction = true`)이 켜져 있으면 GPS 좌표 필수 → Haversine 공식으로 거리 검증
  - 기본 허용 반경: 100m
  - GPS 오차(`gpsAccuracy`) 최대 50m까지 버퍼로 인정

---

## 퇴근 (checkOut) — 야간 출근자 처리 포함

### 기본 흐름

1. 요청된 `workDate`로 `AttendanceRecord` 조회
2. **레코드가 없거나 checkInAt이 null이면 → 야간 근무 케이스로 판단, 전날(workDate - 1)로 fallback**
3. fallback 조건: 전날 레코드가 존재하고, checkInAt이 있고, checkOutAt이 null인 경우
4. fallback 성공 시 `workDate`도 전날로 교체됨
5. 여전히 레코드가 없으면 `CHECK_IN_RECORD_NOT_FOUND_FOR_DATE` 에러

### 야간 출근자 시나리오 예시

```
예) 직원이 화요일 밤 11시에 출근, 수요일 새벽 2시에 퇴근

- 출근: workDate = 화요일, checkInAt = 화요일 23:00 → record 생성 (status=WORKING)
- 퇴근 요청: workDate = 수요일 (클라이언트 기준 오늘)
  → 수요일 레코드 없음
  → 전날(화요일) 조회 → checkInAt 있고 checkOutAt null
  → 화요일 레코드로 퇴근 처리
```

> **주의**: workDate는 출근 시점의 날짜를 기준으로 DB에 기록됨.
> 퇴근 시 클라이언트가 workDate를 명시하지 않으면 `LocalDate.now()`가 사용되므로,
> 자정을 넘긴 퇴근은 fallback 로직이 동작한다.

### 퇴근 후 처리

- 실제 근무 시간 계산: `checkOutAt - checkInAt` (분 단위, 음수 방지)
- 휴게 정책 적용 → 실근무 시간 및 임금 계산 (`WageCalculationHelper`)
- 지각 여부 판정 → status 확정 (`COMPLETED` or `LATE`)
- FCM 푸시: 점주에게 퇴근 완료 알림

---

## 지각 판정 (LATE 상태)

지각 판정 로직은 **출퇴근 경로에 따라 기준이 다름**.

### 직원 직접 퇴근 시 (`AttendanceService`)

- `WorkplaceMemberSchedule`에서 **해당 요일의 출근 시간** 조회
- 스케줄이 없으면 지각 판정 불가 → 무조건 `COMPLETED`
- 스케줄 출근 시간 + `lateGraceMinutes` 이후 출근이면 `LATE`

### 점주 승인/직접 등록 시 (`OwnerService`)

- `WorkplaceSetting`의 `defaultCheckInTime` 기준
- 직원별 스케줄은 보지 않음
- `defaultCheckInTime`이 null이면 무조건 `COMPLETED`

```
grace 기준: checkInAt > (기준 출근 시간 + lateGraceMinutes) → LATE
기본 lateGraceMinutes = 0
```

---

## 근태 수정 요청 (AttendanceRequest)

### 요청 타입

| 타입 | 설명 |
|------|------|
| `NEW_RECORD` | 빠진 근태 기록 새로 추가 요청 |
| `CORRECTION` | 기존 기록의 시간 수정 요청 |

### 상태 흐름

```
PENDING → APPROVED : 요청 내용을 AttendanceRecord에 반영 + 임금 재계산
         → REJECTED : NEW_RECORD 타입이면 요청 시 생성된 빈 레코드 삭제
                      CORRECTION 타입은 레코드 유지 (기존 값 그대로)
```

### NEW_RECORD 특이사항

- 직원이 NEW_RECORD 요청 제출 시점에 **빈 AttendanceRecord가 먼저 생성됨**
- 점주가 반려(`REJECTED`)하면 이 빈 레코드도 함께 삭제됨
- 승인(`APPROVED`)하면 요청된 checkIn/checkOut 시간으로 레코드를 업데이트

---

## 관련 파일

- `AttendanceService.java` — 직원 출/퇴근 처리, 야간 fallback 로직
- `OwnerService.java` — 점주 승인/반려, 직접 근태 등록
- `StaffService.java` — 직원 근태 요청 생성
- `WageCalculationHelper.java` — 임금 계산 공통 컴포넌트