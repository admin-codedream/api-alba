package com.api.alba.dto.auth;

import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.util.Set;

@Getter
public class WithdrawRequest {
    private static final Set<String> VALID_REASON_TYPES = Set.of(
            "NOT_USED_OFTEN", "INCONVENIENT", "PRIVACY_CONCERN",
            "SWITCHING_SERVICE", "WORKPLACE_CLOSED", "OTHER"
    );

    @NotBlank
    private String reasonType;

    private String customReason;

    public boolean isValidReasonType() {
        return reasonType != null && VALID_REASON_TYPES.contains(reasonType.toUpperCase());
    }
}