package com.api.alba.domain.terms;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserTermsAgreement {
    private Long id;
    private Long userId;
    private Long termsId;
    private LocalDateTime agreedAt;
}