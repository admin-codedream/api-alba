package com.api.alba.exception;

public final class ExceptionMessages {
    private ExceptionMessages() {
    }

    public static final String AUTHENTICATION_REQUIRED = "로그인이 필요해요.";
    public static final String ALREADY_CHECKED_IN_FOR_DATE = "이미 출근했어요.";
    public static final String CHECK_IN_RECORD_NOT_FOUND_FOR_DATE = "출근 기록을 찾을 수 없어요.";
    public static final String ALREADY_CHECKED_OUT_FOR_DATE = "이미 퇴근했어요.";
    public static final String INVALID_DATE_RANGE = "조회 시작일은 종료일보다 늦을 수 없어요.";
    public static final String ACTIVE_WORKPLACE_MEMBER_NOT_FOUND = "활성화된 근무지 정보를 찾을 수 없어요.";
    public static final String WORKPLACE_NOT_FOUND = "근무지를 찾을 수 없어요.";
    public static final String LAT_LON_REQUIRED = "위치 정보가 필요해요.";
    public static final String LAT_LON_MUST_BE_PROVIDED_TOGETHER = "위도와 경도를 함께 입력해 주세요.";
    public static final String WORKPLACE_LOCATION_NOT_CONFIGURED = "근무지 위치가 아직 설정되지 않았어요.";
    public static final String OUTSIDE_ALLOWED_WORKPLACE_RADIUS = "근무지 반경 밖에서는 출퇴근할 수 없어요.";
    public static final String LOGIN_ID_ALREADY_IN_USE = "이미 사용 중인 아이디예요.";
    public static final String INVALID_LOGIN_ID_OR_PASSWORD = "아이디 또는 비밀번호가 올바르지 않아요.";
    public static final String ACCOUNT_NOT_ACTIVE = "비활성화된 계정이에요.";
    public static final String USER_NOT_FOUND = "사용자 정보를 찾을 수 없어요.";
    public static final String USER_NOT_FOUND_FOR_SOCIAL_ACCOUNT = "연결된 사용자 정보를 찾을 수 없어요.";
    public static final String SOCIAL_ACCOUNT_ALREADY_CONNECTED_TO_ANOTHER_USER =
            "이미 다른 계정에 연결된 소셜 계정이에요.";
    public static final String PROVIDER_ALREADY_CONNECTED = "이미 연결된 소셜 계정이에요.";
    public static final String ADDRESS_REQUIRED = "주소를 입력해 주세요.";
    public static final String NAVER_GEOCODE_API_KEY_NOT_CONFIGURED = "지도 서비스 설정이 올바르지 않아요.";
    public static final String NAVER_GEOCODE_API_CALL_FAILED = "주소를 확인하는 중 문제가 발생했어요.";
    public static final String NOTICE_NOT_FOUND = "공지사항을 찾을 수 없어요.";
    public static final String ATTENDANCE_REQUEST_NOT_FOUND = "근태 요청을 찾을 수 없어요.";
    public static final String ONLY_PENDING_REQUESTS_CAN_BE_PROCESSED = "대기 중인 요청만 처리할 수 있어요.";
    public static final String ATTENDANCE_RECORD_NOT_FOUND = "근태 기록을 찾을 수 없어요.";
    public static final String ONLY_OWNER_CAN_PROCESS_REQUEST = "사장님만 요청을 처리할 수 있어요.";
    public static final String WORKPLACE_SETTING_NOT_FOUND = "근무지 설정을 찾을 수 없어요.";
    public static final String OWNER_ACCESS_ONLY = "사장님만 이용할 수 있어요.";
    public static final String ONLY_OWNER_USER_TYPE_CAN_CREATE_WORKPLACE = "사장님 계정만 근무지를 만들 수 있어요.";
    public static final String LAT_LON_REQUIRED_WHEN_USE_LOCATION_RESTRICTION_TRUE =
            "위치 제한을 사용하는 경우 위치 정보가 필요해요.";
    public static final String STATUS_MUST_BE_PENDING_APPROVED_REJECTED =
            "요청 상태값이 올바르지 않아요.";
    public static final String INVALID_INVITE_CODE = "초대 코드가 올바르지 않아요.";
    public static final String FAILED_TO_JOIN_WORKPLACE = "근무지 참여에 실패했어요.";
    public static final String CORRECTION_ONLY_FOR_OWN_RECORD =
            "내 기록만 정정 요청할 수 있어요.";

    public static final String AT_LEAST_ONE_REQUESTED_CHECK_IN_OR_OUT_REQUIRED =
            "출근 시간이나 퇴근 시간 중 하나는 입력해 주세요.";
    public static final String REQUESTED_CHECK_OUT_MUST_BE_LATER_THAN_CHECK_IN =
            "퇴근 시간은 출근 시간보다 늦어야 해요.";
    public static final String PENDING_CORRECTION_REQUEST_EXISTS = "현재 처리 중인 정정 요청이 있습니다.";

}
