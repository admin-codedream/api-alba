package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class SaveBreakPoliciesRequest {
    @NotNull(message = "휴게 정책 사용 여부를 입력해 주세요.")
    private Boolean useBreakPolicy;

    @Valid
    private List<PolicyItem> policies;

    @Getter
    @Setter
    public static class PolicyItem {
        @NotBlank(message = "휴게 정책 이름을 입력해 주세요.")
        @Size(max = 100, message = "휴게 정책 이름은 100자 이하여야 해요.")
        private String name;

        @NotBlank(message = "휴게 유형을 입력해 주세요.")
        @Pattern(regexp = "AUTO|FIXED", message = "휴게 유형이 올바르지 않아요.")
        private String breakType;

        @Positive(message = "최소 근무 시간은 0보다 커야 해요.")
        private Integer minWorkMinutes;

        @NotNull(message = "휴게 시간을 입력해 주세요.")
        @Positive(message = "휴게 시간은 0보다 커야 해요.")
        private Integer breakMinutes;

        @NotNull(message = "유급 여부를 입력해 주세요.")
        private Boolean isPaid;
    }
}