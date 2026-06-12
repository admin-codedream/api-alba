# 4대보험 자동공제 기능 정리

## 1. 개요

급여명세서 발행 시 직원별 설정에 따라 국민연금 · 건강보험 · 장기요양보험 · 고용보험을 자동으로 계산하여 공제 항목으로 삽입하는 기능입니다.

---

## 2. 핵심 개념

### 2-1. 과세급여 (Taxable Wage)
공제 계산의 기준이 되는 금액입니다.

```
과세급여 = 총 지급액 - 비과세 금액
총 지급액 = 기본급 + 주휴수당
```

> 비과세 금액은 직원별로 설정 가능합니다. (기본값 0원)

### 2-2. 계산 순서 및 요율 (2026년 기준)

| 순서 | 항목 | 계산 기준 | 근로자 요율 |
|---|---|---|---|
| 1 | 국민연금 | 과세급여 | 4.5% |
| 2 | 건강보험 | 과세급여 | 3.595% |
| 3 | 장기요양보험 | **건강보험 공제액** | 13.14% |
| 4 | 고용보험 | 과세급여 | 0.9% |

> 장기요양보험은 과세급여가 아닌 **건강보험 공제액을 기준**으로 계산합니다.
> 모든 항목은 **원 단위 절사**합니다.

### 2-3. 요율 관리 (`INSURANCE_RATE_RULES`)
요율은 코드에 하드코딩되지 않고 DB 테이블로 관리됩니다.
적용 기준일(급여 기간 종료일)을 기준으로 유효한 요율을 조회하므로, 요율 변경 시 DB에만 추가하면 됩니다.

---

## 3. 추가된 DB 테이블 및 컬럼

### `INSURANCE_RATE_RULES` (신규)
4대보험 요율을 기준일자별로 관리합니다.

| 컬럼 | 설명 |
|---|---|
| `INSURANCE_TYPE` | 보험 유형 (`NATIONAL_PENSION`, `HEALTH_INSURANCE`, `LONG_TERM_CARE`, `EMPLOYMENT_INSURANCE`) |
| `RATE_TARGET` | 부담 대상 (`EMPLOYEE`, `EMPLOYER`, `TOTAL`) |
| `RATE` | 요율 (예: `0.04500`) |
| `BASE_TYPE` | 계산 기준 (`TAXABLE_WAGE`, `HEALTH_INSURANCE_AMOUNT`) |
| `EFFECTIVE_FROM` | 적용 시작일 |
| `EFFECTIVE_TO` | 적용 종료일 (`NULL`이면 현재까지 유효) |

### `EMPLOYEE_INSURANCE_SETTINGS` (신규)
직원별 4대보험 적용 여부를 저장합니다.

| 컬럼 | 설명 |
|---|---|
| `WORKPLACE_MEMBER_ID` | 사업장 멤버 ID |
| `USE_NATIONAL_PENSION` | 국민연금 적용 여부 |
| `USE_HEALTH_INSURANCE` | 건강보험 적용 여부 |
| `USE_LONG_TERM_CARE` | 장기요양보험 적용 여부 |
| `USE_EMPLOYMENT_INSURANCE` | 고용보험 적용 여부 |
| `USE_INCOME_TAX` | 소득세 적용 여부 (현재 미구현) |
| `TAX_FREE_AMOUNT` | 비과세 금액 |

### `PAYSLIP_DEDUCTIONS` (컬럼 추가)
자동 계산된 공제 항목에 계산 근거를 함께 저장합니다.

| 추가 컬럼 | 설명 |
|---|---|
| `APPLIED_RATE` | 적용 요율 (자동 계산 시에만 저장) |
| `APPLIED_BASE_AMOUNT` | 계산 기준 금액 (자동 계산 시에만 저장) |

> `APPLIED_RATE`, `APPLIED_BASE_AMOUNT`가 `NULL`이면 수동 입력된 공제 항목입니다.

---

## 4. API 목록

### 4-1. 직원 보험 설정 조회
```
GET /api/owner/workplaces/{workplaceId}/members/{memberId}/insurance-setting
```

Response:
```json
{
  "useNationalPension": true,
  "useHealthInsurance": true,
  "useLongTermCare": true,
  "useEmploymentInsurance": true,
  "useIncomeTax": false,
  "taxFreeAmount": 0
}
```

> 설정이 없는 직원은 모든 항목이 `false`, `taxFreeAmount`는 `0`으로 반환됩니다.

### 4-2. 직원 보험 설정 변경
```
PUT /api/owner/workplaces/{workplaceId}/members/{memberId}/insurance-setting
```

Request:
```json
{
  "useNationalPension": true,
  "useHealthInsurance": true,
  "useLongTermCare": true,
  "useEmploymentInsurance": true,
  "useIncomeTax": false,
  "taxFreeAmount": 0
}
```

> 설정 변경은 **다음 발행부터** 적용됩니다. 이미 발행된 명세서에는 영향 없습니다.

### 4-3. 급여명세서 발행 (기존 API, 자동 계산 포함)
```
POST /api/owner/workplaces/{workplaceId}/payslips
```

발행 시 직원 보험 설정을 자동으로 읽어 공제 항목을 삽입합니다.
적용된 보험이 없으면 공제 항목이 생성되지 않습니다.

### 4-4. 급여명세서 보너스 + 공제 통합 저장 (신규)
```
PUT /api/owner/workplaces/{workplaceId}/payslips/{payslipId}/full
```

Request:
```json
{
  "bonusAmount": 50000,
  "bonusNote": "성과급",
  "deductions": [
    { "deductionType": "NATIONAL_PENSION",     "name": "국민연금",      "amount": 49500, "displayOrder": 1 },
    { "deductionType": "HEALTH_INSURANCE",     "name": "건강보험",      "amount": 35100, "displayOrder": 2 },
    { "deductionType": "LONG_TERM_CARE",       "name": "장기요양보험",   "amount": 4530,  "displayOrder": 3 },
    { "deductionType": "EMPLOYMENT_INSURANCE", "name": "고용보험",      "amount": 8100,  "displayOrder": 4 }
  ]
}
```

> 보너스와 공제를 한 번에 저장하며, 실수령액을 한 번에 갱신합니다.
> 기존 공제 항목은 전부 삭제 후 재삽입됩니다.

---

## 5. 전체 흐름

```
[직원 관리 화면]
  1. GET /insurance-setting  → 현재 설정 조회
  2. PUT /insurance-setting  → 국민연금/건강보험 등 on/off, 비과세 금액 설정

[급여명세서 발행]
  3. POST /payslips          → 발행 시 백엔드가 자동 계산
                                - 과세급여 = 기본급 + 주휴수당 - 비과세 금액
                                - 국민연금, 건강보험, 장기요양, 고용보험 순으로 계산
                                - 공제 항목 자동 삽입 + 실수령액 갱신

[명세서 상세 조회]
  4. GET /payslips/{id}      → deductions 항목에 자동 계산된 공제 포함

[수동 수정 후 저장]
  5. PUT /payslips/{id}/full → 보너스 + 공제 항목 통합 저장
                               실수령액 = 기본급 + 주휴수당 + 보너스 - 공제합계
```

---

## 6. 주의사항

- **소득세(`useIncomeTax`)**: 필드는 존재하지만 현재 자동 계산 미구현입니다.
- **보너스는 공제 계산 기준에서 제외**: 발행 시점의 `총 지급액 = 기본급 + 주휴수당`이며, 보너스는 포함하지 않습니다.
- **요율 변경 시**: `INSURANCE_RATE_RULES`에 새 행을 INSERT하고, 기존 행의 `EFFECTIVE_TO`를 업데이트하면 됩니다. 코드 수정 불필요합니다.
- **수동 저장 시**: `PUT /full` API로 저장하면 `APPLIED_RATE`, `APPLIED_BASE_AMOUNT`가 `NULL`로 저장됩니다.