package com.api.alba.dto.owner;

import lombok.Getter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Getter
public class IssuePayslipRequest {
    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    @NotEmpty
    private List<Long> userIds;
}
