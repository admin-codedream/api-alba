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
    @NotNull(message = "useBreakPolicy is required.")
    private Boolean useBreakPolicy;

    @Valid
    private List<PolicyItem> policies;

    @Getter
    @Setter
    public static class PolicyItem {
        @NotBlank(message = "name is required.")
        @Size(max = 100, message = "name must be 100 characters or fewer.")
        private String name;

        @NotBlank(message = "breakType is required.")
        @Pattern(regexp = "AUTO|FIXED", message = "breakType must be AUTO or FIXED.")
        private String breakType;

        @Positive(message = "minWorkMinutes must be positive.")
        private Integer minWorkMinutes;

        @NotNull(message = "breakMinutes is required.")
        @Positive(message = "breakMinutes must be positive.")
        private Integer breakMinutes;

        @NotNull(message = "isPaid is required.")
        private Boolean isPaid;
    }
}