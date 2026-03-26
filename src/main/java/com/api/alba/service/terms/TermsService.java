package com.api.alba.service.terms;

import com.api.alba.domain.terms.Terms;
import com.api.alba.domain.terms.UserTermsAgreement;
import com.api.alba.dto.terms.TermsAgreeRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.terms.TermsMapper;
import com.api.alba.mapper.terms.UserTermsAgreementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.api.alba.exception.ExceptionMessages.ALREADY_AGREED_TO_TERMS;
import static com.api.alba.exception.ExceptionMessages.TERMS_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class TermsService {
    private final TermsMapper termsMapper;
    private final UserTermsAgreementMapper userTermsAgreementMapper;

    public List<Terms> getActiveTerms() {
        return termsMapper.findActiveAll();
    }

    public Terms getActiveTermsByType(String termsType) {
        Terms terms = termsMapper.findActiveByType(termsType);
        if (terms == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, TERMS_NOT_FOUND);
        }
        return terms;
    }

    public void agreeToTerms(Long userId, TermsAgreeRequest request) {
        for (Long termsId : request.getTermsIds()) {
            Terms terms = termsMapper.findById(termsId);
            if (terms == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, TERMS_NOT_FOUND);
            }
            if (userTermsAgreementMapper.existsByUserIdAndTermsId(userId, termsId)) {
                throw new ApiException(HttpStatus.CONFLICT, ALREADY_AGREED_TO_TERMS);
            }
            UserTermsAgreement agreement = new UserTermsAgreement();
            agreement.setUserId(userId);
            agreement.setTermsId(termsId);
            userTermsAgreementMapper.insert(agreement);
        }
    }
}