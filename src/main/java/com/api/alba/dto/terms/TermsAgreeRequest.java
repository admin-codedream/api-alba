package com.api.alba.dto.terms;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Setter
public class TermsAgreeRequest {
    @NotEmpty
    private List<Long> termsIds;
}