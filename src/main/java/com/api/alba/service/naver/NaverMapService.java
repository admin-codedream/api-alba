package com.api.alba.service.naver;

import com.api.alba.dto.naver.NaverGeocodeResponse;
import com.api.alba.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static com.api.alba.exception.ExceptionMessages.ADDRESS_REQUIRED;
import static com.api.alba.exception.ExceptionMessages.NAVER_GEOCODE_API_CALL_FAILED;
import static com.api.alba.exception.ExceptionMessages.NAVER_GEOCODE_API_KEY_NOT_CONFIGURED;

@Service
@RequiredArgsConstructor
public class NaverMapService {
    private static final String HEADER_API_KEY_ID = "x-ncp-apigw-api-key-id";
    private static final String HEADER_API_KEY = "x-ncp-apigw-api-key";

    @Value("${naver.cloud.base-url}")
    private String baseUrl;

    @Value("${naver.cloud.api-key}")
    private String apiKeyId;

    @Value("${naver.cloud.api-secret}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public NaverGeocodeResponse geocode(String address) {
        String normalizedAddress = address == null ? "" : address.trim();
        if (normalizedAddress.isEmpty()) {
            throw new ApiException(ADDRESS_REQUIRED);
        }
        if (apiKeyId == null || apiKeyId.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, NAVER_GEOCODE_API_KEY_NOT_CONFIGURED);
        }

        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/map-geocode/v2/geocode")
                .queryParam("query", normalizedAddress)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_API_KEY_ID, apiKeyId);
        headers.set(HEADER_API_KEY, apiKey);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<NaverGeocodeResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    NaverGeocodeResponse.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, NAVER_GEOCODE_API_CALL_FAILED);
            }
            return response.getBody();
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, NAVER_GEOCODE_API_CALL_FAILED);
        }
    }
}
