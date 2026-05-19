# 급여 계산 규칙

## 계산 흐름 요약

```
총 근무 시간 (grossWorkedMinutes)
  → 무급 휴게시간 차감
  → 급여 계산 단위 절사 (salaryCalcUnit)
  = 실지급 근무 분 (payableWorkedMinutes)
  → 시급 × (payableWorkedMinutes / 60)
  → 10원 단위 버림
  = 최종 임금 (finalWage)
```

> 핵심 로직: `WageCalculationHelper.java`

---

## 1. 임금 유형 (wageType)

| 유형 | 설명 |
|------|------|
| `HOURLY` | 시급제. 실근무 시간 기반으로 매일 임금 계산 |
| `MONTHLY` | 월급제. 근태 레코드의 finalWage = 0, hourlyWage = 0으로 저장. 급여명세서 발행 시 monthlyWage 고정값 사용 |

- 월급제 전환 시 기존 근태 레코드의 wage 필드는 0으로 초기화됨 (`clearWagesByMember`)
- 월급제 직원은 `WageCalculationHelper.calculate()` 호출 시 hourlyWage = 0 전달

---

## 2. 무급 휴게시간 차감

### 우선순위

1. **직원별 고정 휴게 (`WorkplaceMember.breakMinutes`)** — 설정된 경우 매장 정책보다 우선 적용
2. **매장 휴게 정책 (`WorkplaceBreakPolicy`)** — `useBreakPolicy = true`인 경우에만 활성화

### 매장 휴게 정책 타입

| breakType | 설명 | 적용 방식 |
|-----------|------|-----------|
| `AUTO` | 근무 시간이 기준(minWorkMinutes) 이상이면 자동 차감 | 조건을 만족하는 정책 중 **minWorkMinutes가 가장 큰 1개**만 적용 |
| `FIXED` | 조건 없이 항상 차감 | 해당 타입 정책의 breakMinutes 모두 **합산** |

- `isPaid = true`인 정책은 차감 계산에서 제외 (유급 휴게이므로 임금 그대로)
- `isActive = false`인 정책도 제외

### AUTO 정책 적용 예시

```
정책 A: minWorkMinutes=240, breakMinutes=30
정책 B: minWorkMinutes=480, breakMinutes=60

총 근무 540분인 경우:
  → A, B 모두 조건 충족
  → minWorkMinutes 더 큰 B 선택 → 60분 차감
```

---

## 3. 급여 계산 단위 (salaryCalcUnit)

무급 휴게를 차감한 뒤, 해당 단위 기준으로 **버림(floor)** 처리.

| 값 | 설명 |
|----|------|
| `10MIN` | 10분 단위 버림. 기본값 |
| `HOUR` | 60분 단위 버림 |
| 그 외 / null | 분 단위 그대로 사용 |

```
예) 실근무 83분, salaryCalcUnit=10MIN → 80분으로 절사
예) 실근무 83분, salaryCalcUnit=HOUR  → 60분으로 절사
```

---

## 4. 임금 계산 및 끝수 처리

```
임금 = 시급 × (payableWorkedMinutes / 60)
     → 소수점 2자리 반올림
     → 10원 단위 버림 (truncateToTenWonUnit)
```

```
예) 시급 9,860원, 실근무 80분
  = 9860 × (80 / 60) = 13,146.67원
  → 10원 버림 → 13,140원
```

---

## 5. 주휴수당 (useWeeklyHolidayPay)

매장 설정에서 `useWeeklyHolidayPay = true`인 경우, 급여명세서 발행 시 주단위로 계산해 합산.

### 조건

- 해당 주(월~일) 총 근무 시간이 **15시간(900분) 이상**인 경우에만 지급
- 주 근무 시간 상한: **40시간(2400분)** — 초과분 무시

### 계산 공식

```
주휴수당 = (주 근무분 / 2400) × 8시간 × 시급
         = 주 근무분 × 시급 / 300
→ 10원 단위 버림
```

```
예) 주 근무 2400분, 시급 9,860원
  = 2400 × 9860 / 300 = 78,880원
```

- 월급제 직원은 주휴수당 계산 대상 제외

---

## 6. 시급 우선순위 (resolveHourlyWage)

1. `WorkplaceMember.hourlyWage` — 직원 개별 시급
2. `WorkplaceSetting.defaultHourlyWage` — 매장 기본 시급
3. 둘 다 없으면 `0`

---

## 관련 파일

- `WageCalculationHelper.java` — 핵심 계산 로직
- `AttendanceService.java` — 퇴근 시 실시간 임금 계산
- `OwnerService.java` — 급여명세서 발행, 주휴수당 계산, 임금 재계산
- `WorkplaceSetting.java` — salaryCalcUnit, useBreakPolicy, useWeeklyHolidayPay 등 설정값
- `WorkplaceBreakPolicy.java` — 휴게 정책 정의